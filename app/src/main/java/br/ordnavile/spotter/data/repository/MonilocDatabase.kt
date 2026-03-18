package br.ordnavile.spotter.data.repository

import androidx.room.Database
import br.ordnavile.spotter.data.model.Veiculo
import br.ordnavile.spotter.data.model.HistoricoVeiculo
import androidx.room.RoomDatabase

@Database(entities = [Veiculo::class, HistoricoVeiculo::class], version = 5, exportSchema = false)
abstract class MonilocDatabase : RoomDatabase() {
    abstract fun veiculoDao(): VeiculoDao
}
