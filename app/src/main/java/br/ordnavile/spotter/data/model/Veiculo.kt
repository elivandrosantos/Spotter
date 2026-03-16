package br.ordnavile.spotter.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "veiculos")
data class Veiculo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placa: String,
    val modelo: String,
    val entrada: String
)
