package br.ordnavile.spotter.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import br.ordnavile.spotter.data.model.Veiculo
@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos")
    suspend fun listarTodos(): List<Veiculo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: Veiculo)

    @Query("DELETE FROM veiculos WHERE placa = :placa")
    suspend fun deletarPorPlaca(placa: String)
}
