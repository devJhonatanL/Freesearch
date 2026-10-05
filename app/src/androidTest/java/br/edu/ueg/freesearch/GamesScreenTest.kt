package br.edu.ueg.freesearch

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.ueg.freesearch.model.*
import br.edu.ueg.freesearch.ui.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamesScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun showsLoading() {
        compose.setContent { FreesearchTheme { GamesScreen(GamesUiState.Loading, "", "ALL", {}, {}, {}, {}) } }
        compose.onNodeWithText("Carregando jogos…").assertIsDisplayed()
    }
    @Test fun retryButtonDeliversEvent() {
        var retried = false
        compose.setContent { FreesearchTheme { GamesScreen(GamesUiState.Error("Sem conexão"), "", "ALL", {}, {}, { retried = true }, {}) } }
        compose.onNodeWithText("Tentar Novamente").performClick()
        assertTrue(retried)
    }
    @Test fun showsEmptySearchAndClearsFilters() {
        var cleared = false
        compose.setContent { FreesearchTheme { GamesScreen(GamesUiState.Success(emptyList(), 10), "zzzz", "PC", { cleared = it.isEmpty() }, {}, {}, {}) } }
        compose.onNodeWithText("Nenhum jogo encontrado").assertIsDisplayed()
        compose.onNodeWithText("Limpar filtros").performClick()
        assertTrue(cleared)
    }
    @Test fun showsGameAndSendsSearchEvent() {
        var typed = ""
        val game = Game(1, "Warframe", "", "Ação", "Shooter", "PC (Windows)", "Digital Extremes", "", "")
        compose.setContent { FreesearchTheme { GamesScreen(GamesUiState.Success(listOf(game), 1), "", "ALL", { typed = it }, {}, {}, {}) } }
        compose.onNodeWithText("Warframe").assertIsDisplayed()
        compose.onNodeWithText("Buscar título, gênero ou editora").performTextInput("war")
        assertTrue(typed == "war")
    }
    @Test fun detailsButtonOpensSelectedGame() {
        var selectedId = -1
        val game = Game(42, "Example", "", "Resumo", "Shooter", "PC", "Studio", "", "")
        compose.setContent { FreesearchTheme { GamesScreen(GamesUiState.Success(listOf(game),1), "", "ALL", {}, {}, {}, { selectedId=it }) } }
        compose.onNodeWithText("Ver detalhes").performScrollTo().performClick()
        assertTrue(selectedId == 42)
    }
}
