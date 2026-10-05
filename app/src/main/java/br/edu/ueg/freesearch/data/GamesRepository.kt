package br.edu.ueg.freesearch.data

import br.edu.ueg.freesearch.model.Game

interface GamesRepository { suspend fun getGames(): List<Game> }

class RemoteGamesRepository(private val api: FreeToGameApi) : GamesRepository {
    override suspend fun getGames(): List<Game> = api.getGames()
        .mapNotNull { it.toDomain() }.distinctBy { it.id }
}
