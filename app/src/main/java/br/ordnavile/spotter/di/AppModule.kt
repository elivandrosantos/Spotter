package br.ordnavile.spotter.di

import androidx.room.Room
import br.ordnavile.spotter.data.repository.MonilocDatabase
import br.ordnavile.spotter.data.remote.MonilocApi
import br.ordnavile.spotter.data.repository.MonilocRepository
import br.ordnavile.spotter.viewmodel.MonilocViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            MonilocDatabase::class.java,
            "moniloc_db"
        ).build()
    }

    // Dao
    single { get<MonilocDatabase>().veiculoDao() }

    // Retrofit & OkHttp
    single {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val httpClient = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://pdv.monilocapp.com.br")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Service
    single { get<Retrofit>().create(MonilocApi::class.java) }

    // Repository
    single { MonilocRepository(get(), get()) }

    // ViewModel
    viewModel { MonilocViewModel(get()) }
}
