package com.example.mercadopago

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mercadopago.data.Cuenta
import com.example.mercadopago.databinding.ActivityTransferBinding

/**
 * Pantalla para realizar transferencias con validación de campos
 * y registro en Firebase Cloud Firestore mediante [Cuenta].
 */
class TransferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransferBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        actualizarSaldoUI()
        Cuenta.iniciarEscuchadores {
            actualizarSaldoUI()
        }

        binding.btnEnviarDinero.setOnClickListener {
            validarYEnviarTransferencia()
        }

        binding.btnVolverPanel.setOnClickListener {
            finish()
        }
    }

    private fun actualizarSaldoUI() {
        binding.tvSaldoDisponible.text =
            getString(R.string.saldo_disponible_fmt, String.format("$%,.2f", Cuenta.saldo))
    }

    private fun validarYEnviarTransferencia() {
        val destinatario = binding.etDestinatario.text?.toString()?.trim().orEmpty()
        val montoTexto = binding.etMonto.text?.toString()?.trim().orEmpty()
        val concepto = binding.etConcepto.text?.toString()?.trim().orEmpty()

        binding.tilDestinatario.error = null
        binding.tilMonto.error = null

        if (destinatario.isEmpty()) {
            binding.tilDestinatario.error = getString(R.string.error_destinatario_vacio)
            binding.etDestinatario.requestFocus()
            return
        }

        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            binding.tilMonto.error = getString(R.string.error_monto_invalido)
            binding.etMonto.requestFocus()
            return
        }

        if (monto > Cuenta.saldo) {
            binding.tilMonto.error = getString(R.string.error_saldo_insuficiente)
            binding.etMonto.requestFocus()
            return
        }

        binding.btnEnviarDinero.isEnabled = false

        Cuenta.registrarTransferencia(destinatario, monto, concepto) { exito ->
            binding.btnEnviarDinero.isEnabled = true
            if (exito) {
                Toast.makeText(this, getString(R.string.transferencia_exito), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.transferencia_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
