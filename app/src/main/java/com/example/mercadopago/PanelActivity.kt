package com.example.mercadopago

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mercadopago.data.Cuenta
import com.example.mercadopago.databinding.ActivityPanelBinding

class PanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firestore y escuchar cambios de saldo en tiempo real
        Cuenta.inicializar {
            binding.tvSaldo.text = String.format("$%,.2f", Cuenta.saldo)
        }

        binding.btnTransferir.setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }

        binding.btnMovimientos.setOnClickListener {
            startActivity(Intent(this, MovementsActivity::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Cuenta.iniciarEscuchadores {
            binding.tvSaldo.text = String.format("$%,.2f", Cuenta.saldo)
        }
        binding.tvSaldo.text = String.format("$%,.2f", Cuenta.saldo)
    }
}
