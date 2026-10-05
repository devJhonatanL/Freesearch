package br.edu.ueg.freesearch.data

import br.edu.ueg.freesearch.model.Game

//Responsavel por buscar e organizar o catálogo
//Alem disso é aqui que declara que deve existir uma operação getGames() e implementa essa operação consultando a API.

interface GamesRepository { suspend fun getGames(): List<Game> }

class RemoteGamesRepository(private val api: FreeToGameApi) : GamesRepository {
    override suspend fun getGames(): List<Game> = api.getGames()
        .mapNotNull { it.toDomain() }.distinctBy { it.id }
}
