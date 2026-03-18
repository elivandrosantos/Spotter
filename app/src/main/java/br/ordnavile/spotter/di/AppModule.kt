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
import com.google.firebase.firestore.FirebaseFirestore
import android.provider.Settings
import android.annotation.SuppressLint
import android.content.Context


// Extraímos a busca do ID para uma função isolada com a anotação para remover o aviso (Warning) do Android Studio.
// Usar um fallback ("unknown_device") garante que o app não quebre se por algum motivo o sistema negar a leitura.
@SuppressLint("HardwareIds")
fun obterDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
}

val appModule = module {

    // SharedPreferences (Adicionado para persistir as configurações - Bug 2)
    single {
        androidContext().getSharedPreferences("moniloc_prefs", Context.MODE_PRIVATE)
    }

    // Device ID (Usando a função segura sem alertas)
    single {
        obterDeviceId(androidContext())
    }

    // Firebase
    single { FirebaseFirestore.getInstance() }

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            // Substitua 'MonilocDatabase' pelo import correto do seu banco se necessário
            MonilocDatabase::class.java,
            "moniloc_db"
        ).fallbackToDestructiveMigration().build()
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

    // Repository - Agora com 5 parâmetros (O quinto 'get()' entrega o SharedPreferences)
    single { MonilocRepository(get(), get(), get(), get(), get()) }

    // ViewModel
    viewModel { MonilocViewModel(get()) }
}


//val appModule = module {
//
//    // Device ID
//    single {
//        Settings.Secure.getString(androidContext().contentResolver, Settings.Secure.ANDROID_ID)
//    }
//
//    // Firebase
//    single { FirebaseFirestore.getInstance() }
//
//    // Room Database
//    single {
//        Room.databaseBuilder(
//            androidContext(),
//            MonilocDatabase::class.java,
//            "moniloc_db"
//        ).fallbackToDestructiveMigration().build()
//    }
//
//    // Dao
//    single { get<MonilocDatabase>().veiculoDao() }
//
//    // Retrofit & OkHttp
//    single {
//        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
//        val httpClient = OkHttpClient.Builder().addInterceptor(logging).build()
//        Retrofit.Builder()
//            .baseUrl("https://pdv.monilocapp.com.br")
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    // API Service
//    single { get<Retrofit>().create(MonilocApi::class.java) }
//
//    // Repository
//    single { MonilocRepository(get(), get(), get(), get()) }
//
//    // ViewModel
//    viewModel { MonilocViewModel(get()) }
//}
