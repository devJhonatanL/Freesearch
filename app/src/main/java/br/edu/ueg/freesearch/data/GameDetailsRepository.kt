package br.edu.ueg.freesearch.data
//Faz o mesmo papel do repositório anterior, so que para um único jogo.
import br.edu.ueg.freesearch.model.GameDetails
import com.google.gson.JsonParseException

interface GameDetailsRepository { suspend fun getGame(id: Int): GameDetails }

class RemoteGameDetailsRepository(private val api: FreeToGameApi) : GameDetailsRepository {
    override suspend fun getGame(id: Int): GameDetails {
        val game = api.getGame(id).toDomainDetails()
        if (game == null || game.id != id) throw JsonParseException("Resposta de detalhes inválida")
        return game
    }
}
