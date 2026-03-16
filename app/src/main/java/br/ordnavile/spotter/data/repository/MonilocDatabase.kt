package br.ordnavile.spotter.data.repository

import androidx.room.Database
import br.ordnavile.spotter.data.model.Veiculo
import androidx.room.RoomDatabase

@Database(entities = [Veiculo::class], version = 1, exportSchema = false)
abstract class MonilocDatabase : RoomDatabase() {
    abstract fun veiculoDao(): VeiculoDao
}
