package com.example.mercadopago

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mercadopago.data.Cuenta
import com.example.mercadopago.data.Movimiento
import com.example.mercadopago.databinding.ActivityMovementsBinding
import com.example.mercadopago.databinding.ItemMovimientoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Pantalla encargada de mostrar el historial de movimientos registrados en Firestore
 * y permitir su eliminación en tiempo real con confirmación.
 */
class MovementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovementsBinding
    private lateinit var adapter: MovimientosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMovementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = MovimientosAdapter(
            items = Cuenta.movimientos.toMutableList(),
            onDeleteClick = { position ->
                confirmarEliminacion(position)
            }
        )

        binding.rvMovimientos.layoutManager = LinearLayoutManager(this)
        binding.rvMovimientos.adapter = adapter

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Cuenta.iniciarEscuchadores {
            refrescarLista()
        }
        refrescarLista()
    }

    /**
     * Muestra un diálogo de confirmación antes de borrar el movimiento seleccionado.
     */
    private fun confirmarEliminacion(position: Int) {
        if (position !in Cuenta.movimientos.indices) return

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.titulo_confirmar_eliminacion))
            .setMessage(getString(R.string.msg_confirmar_eliminacion))
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .setPositiveButton(getString(R.string.btn_confirmar)) { _, _ ->
                Cuenta.eliminarMovimiento(position) { exito ->
                    if (exito) {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_movimiento_eliminado),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun refrescarLista() {
        adapter.actualizarDatos(Cuenta.movimientos)
        binding.tvVacio.visibility = if (Cuenta.movimientos.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Adaptador del RecyclerView implementado íntegramente con ViewBinding ([ItemMovimientoBinding]).
     */
    private class MovimientosAdapter(
        private var items: MutableList<Movimiento>,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MovimientosAdapter.ViewHolder>() {

        class ViewHolder(val itemBinding: ItemMovimientoBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemMovimientoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemBinding.tvDetalleMovimiento.text = items[position].toString()
            holder.itemBinding.btnEliminarMovimiento.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(pos)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun actualizarDatos(nuevosItems: List<Movimiento>) {
            items = nuevosItems.toMutableList()
            notifyDataSetChanged()
        }
    }
}
