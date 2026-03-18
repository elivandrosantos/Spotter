package br.ordnavile.spotter.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "veiculos")
data class Veiculo(
    @DocumentId @PrimaryKey val placa: String = "",
    val modelo: String = "",
    val entrada: String = "",
    val valorFixo: Double? = null,
    val entradaUnix: Long = 0L  // Epoch ms - used for precise elapsed-time calculation
)
