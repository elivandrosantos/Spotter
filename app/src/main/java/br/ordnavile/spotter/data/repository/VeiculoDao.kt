package br.ordnavile.spotter.data.repository

import androidx.room.*
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.model.HistoricoVeiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos")
    fun listarTodos(): Flow<List<Veiculo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: Veiculo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarios(veiculos: List<Veiculo>)

    @Query("SELECT * FROM veiculos WHERE placa = :placa LIMIT 1")
    suspend fun buscarPorPlaca(placa: String): Veiculo?

    @Query("DELETE FROM veiculos WHERE placa = :placa")
    suspend fun deletarPorPlaca(placa: String)

    // Histórico
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirHistorico(historico: HistoricoVeiculo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVariosHistorico(historico: List<HistoricoVeiculo>)

    @Query("SELECT * FROM historico_veiculos ORDER BY dataUnix DESC")
    fun listarHistorico(): Flow<List<HistoricoVeiculo>>
}
