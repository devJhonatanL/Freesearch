package br.edu.ueg.freesearch.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FreeToGameApi {
    @GET("games")
    suspend fun getGames(@Query("sort-by") sortBy: String = "alphabetical"): List<GameDto>
    //pede a lista de jogos em ordem alfabética.
    @GET("game")
    suspend fun getGame(@Query("id") id: Int): GameDetailsDto
}   //pede os detalhes de um jogo específico (ID).

//obs: O suspend permite que as chamadas aguardem a resposta usando Coroutines.
//alem disso, por conta da integração do Retrofit, a espera pela rede não trava a interface.
