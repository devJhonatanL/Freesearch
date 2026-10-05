package br.edu.ueg.freesearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ueg.freesearch.data.GameDetailsRepository
import br.edu.ueg.freesearch.model.GameDetails
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface DetailsUiState {
    val gameId: Int
    data class Loading(override val gameId: Int) : DetailsUiState
    data class Success(val game: GameDetails) : DetailsUiState { override val gameId = game.id }
    data class Error(override val gameId: Int, val message: String) : DetailsUiState
}

class GameDetailsViewModel(private val repository: GameDetailsRepository) : ViewModel() {
    private val _state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading(-1))
    val state = _state.asStateFlow()
    private var request: Job? = null

    fun load(id: Int, force: Boolean = false) {
        if (_state.value.gameId == id && request?.isActive == true) return
        if (!force && _state.value.gameId == id && _state.value is DetailsUiState.Success) return
        request?.cancel() // Uma resposta antiga nunca substitui o jogo recém-selecionado.
        _state.value = DetailsUiState.Loading(id)
        request = viewModelScope.launch {
            try {
                val game = repository.getGame(id)
                if (_state.value.gameId == id) _state.value = DetailsUiState.Success(game)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_state.value.gameId == id) _state.value = DetailsUiState.Error(id, when (error) {
                    is HttpException -> if (error.code() == 404) "Este jogo não está mais disponível no catálogo. Volte e escolha outro."
                        else "O serviço está indisponível (HTTP ${error.code()}). Tente novamente."
                    is IOException -> "Não foi possível conectar. Verifique sua internet e tente novamente."
                    is JsonParseException -> "Os detalhes recebidos estão incompletos. Tente novamente mais tarde."
                    else -> "Não foi possível carregar os detalhes. Tente novamente."
                })
            }
        }
    }
}
