package com.example.mercadopago.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class Movimiento(
    var id: String = "",
    val titulo: String = "",
    val destinatario: String = "",
    val monto: Double = 0.0,
    val esIngreso: Boolean = false,
    val fecha: Long = System.currentTimeMillis()
) {
    override fun toString(): String {
        val signo = if (esIngreso) "+" else "-"
        val montoFmt = String.format("%s$%,.2f", signo, monto)
        val dePara = if (destinatario.isNotBlank()) " ($destinatario)" else ""
        return "$titulo$dePara — $montoFmt"
    }
}

object Cuenta {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val cuentaRef by lazy { db.collection("cuenta").document("principal") }
    private val movimientosRef by lazy { db.collection("movimientos") }

    private var saldoListener: ListenerRegistration? = null
    private var movimientosListener: ListenerRegistration? = null

    var saldo: Double = 12500.00
        private set

    val movimientos = mutableListOf<Movimiento>()

    private var estaInicializado = false

    fun inicializar(onUpdate: (() -> Unit)? = null) {
        if (estaInicializado) {
            onUpdate?.invoke()
            return
        }

        estaInicializado = true

        cuentaRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Si la base de datos está vacía en Firebase Console, cargamos los datos iniciales
                val datosCuenta = mapOf("saldo" to 12500.00)
                cuentaRef.set(datosCuenta).addOnSuccessListener {
                    val iniciales = listOf(
                        Movimiento(titulo = "Depósito inicial", destinatario = "Cuenta Mercado Pago", monto = 12500.00, esIngreso = true, fecha = System.currentTimeMillis() - 3000),
                        Movimiento(titulo = "Pago de luz", destinatario = "CFE", monto = 650.00, esIngreso = false, fecha = System.currentTimeMillis() - 2000),
                        Movimiento(titulo = "Envío a contacto", destinatario = "Ana López", monto = 350.00, esIngreso = false, fecha = System.currentTimeMillis() - 1000)
                    )
                    var creados = 0
                    for (mov in iniciales) {
                        movimientosRef.add(mov).addOnCompleteListener {
                            creados++
                            if (creados == iniciales.size) {
                                iniciarEscuchadores(onUpdate)
                            }
                        }
                    }
                }
            } else {
                iniciarEscuchadores(onUpdate)
            }
        }.addOnFailureListener {
            iniciarEscuchadores(onUpdate)
        }
    }

    fun iniciarEscuchadores(onUpdate: (() -> Unit)? = null) {
        saldoListener?.remove()
        movimientosListener?.remove()

        saldoListener = cuentaRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                saldo = snapshot.getDouble("saldo") ?: 12500.00
                onUpdate?.invoke()
            }
        }

        movimientosListener = movimientosRef
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    movimientos.clear()
                    for (doc in snapshot.documents) {
                        val mov = doc.toObject(Movimiento::class.java)
                        if (mov != null) {
                            mov.id = doc.id
                            movimientos.add(mov)
                        }
                    }
                    onUpdate?.invoke()
                }
            }
    }

    fun registrarTransferencia(
        destinatario: String,
        monto: Double,
        concepto: String,
        onResult: (Boolean) -> Unit
    ) {
        if (monto <= 0.0 || monto > saldo) {
            onResult(false)
            return
        }

        val nuevoSaldo = saldo - monto
        val tituloFinal = if (concepto.isBlank()) "Transferencia" else concepto.trim()
        val nuevoMovimiento = Movimiento(
            titulo = tituloFinal,
            destinatario = destinatario.trim(),
            monto = monto,
            esIngreso = false,
            fecha = System.currentTimeMillis()
        )

        db.runTransaction { transaction ->
            transaction.update(cuentaRef, "saldo", nuevoSaldo)
            val docRef = movimientosRef.document()
            transaction.set(docRef, nuevoMovimiento)
        }.addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }

    fun eliminarMovimiento(position: Int, onResult: (Boolean) -> Unit) {
        if (position !in movimientos.indices) {
            onResult(false)
            return
        }

        val item = movimientos[position]
        if (item.id.isBlank()) {
            onResult(false)
            return
        }

        val nuevoSaldo = if (item.esIngreso) saldo - item.monto else saldo + item.monto

        db.runTransaction { transaction ->
            transaction.update(cuentaRef, "saldo", nuevoSaldo)
            transaction.delete(movimientosRef.document(item.id))
        }.addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener {
            onResult(false)
        }
    }
}
