package br.ordnavile.spotter.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "historico_veiculos")
data class HistoricoVeiculo(
    @DocumentId @PrimaryKey val firestoreId: String = "",
    val placa: String = "",
    val modelo: String = "",
    val entrada: String = "",
    val saida: String = "",
    val valorPago: Double = 0.0,
    val dataUnix: Long = System.currentTimeMillis()
)
