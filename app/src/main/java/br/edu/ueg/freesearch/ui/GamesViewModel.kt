package br.edu.ueg.freesearch.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ueg.freesearch.data.GamesRepository
import br.edu.ueg.freesearch.model.Game
import br.edu.ueg.freesearch.model.GamePlatform
import br.edu.ueg.freesearch.model.filterGames
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Success(val games: List<Game>, val total: Int) : GamesUiState
    data class Error(val message: String) : GamesUiState
}

class GamesViewModel(
    private val repository: GamesRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow<GamesUiState>(GamesUiState.Loading)
    val state: StateFlow<GamesUiState> = _state.asStateFlow()
    val query = savedState.getStateFlow("query", "")
    val platform = savedState.getStateFlow("platform", GamePlatform.ALL.name)
    private var allGames = emptyList<Game>()
    private var request: Job? = null

    init { load() }

    fun load() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            _state.value = GamesUiState.Loading
            try {
                allGames = repository.getGames()
                publishGames()
            } catch (cancelled: CancellationException) {
                throw cancelled // Não converte cancelamento de ciclo de vida em erro.
            } catch (error: Exception) {
                _state.value = GamesUiState.Error(when (error) {
                    is HttpException -> "O serviço de jogos está indisponível (HTTP ${error.code()}). Tente novamente."
                    is IOException -> "Não foi possível conectar. Verifique sua internet e tente novamente."
                    is JsonParseException -> "O serviço retornou dados inesperados. Tente novamente mais tarde."
                    else -> "Não foi possível carregar os jogos. Tente novamente."
                })
            }
        }
    }

    fun onQueryChange(value: String) {
        savedState["query"] = value
        if (_state.value is GamesUiState.Success) publishGames()
    }

    fun onPlatformChange(value: GamePlatform) {
        savedState["platform"] = value.name
        if (_state.value is GamesUiState.Success) publishGames()
    }

    private fun publishGames() {
        val selected = GamePlatform.entries.firstOrNull { it.name == platform.value } ?: GamePlatform.ALL
        _state.update { GamesUiState.Success(filterGames(allGames, query.value, selected), allGames.size) }
    }
}
