package br.edu.ueg.freesearch.di

import br.edu.ueg.freesearch.data.*
import br.edu.ueg.freesearch.ui.GamesViewModel
import br.edu.ueg.freesearch.ui.GameDetailsViewModel
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build() }
    single { Retrofit.Builder().baseUrl("https://www.freetogame.com/api/")
        .client(get()).addConverterFactory(GsonConverterFactory.create()).build() }
    single<FreeToGameApi> { get<Retrofit>().create(FreeToGameApi::class.java) }
    single<GamesRepository> { RemoteGamesRepository(get()) }
    single<GameDetailsRepository> { RemoteGameDetailsRepository(get()) }
    viewModel { GamesViewModel(get(), get()) }
    viewModel { GameDetailsViewModel(get()) }
}
