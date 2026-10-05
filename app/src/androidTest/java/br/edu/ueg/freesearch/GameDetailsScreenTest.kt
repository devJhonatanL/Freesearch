package br.edu.ueg.freesearch

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.ueg.freesearch.data.GameDetailsDto
import br.edu.ueg.freesearch.data.toDomainDetails
import br.edu.ueg.freesearch.ui.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDetailsScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun detailsLoadingAndBackAreVisible() {
        var back = false
        compose.setContent { FreesearchTheme { GameDetailsScreen(DetailsUiState.Loading(5), { back=true }, {}) } }
        compose.onNodeWithText("Carregando detalhes…").assertIsDisplayed()
        compose.onNodeWithText("‹ Voltar").performClick()
        assertTrue(back)
    }
    @Test fun detailsRetrySendsEvent() {
        var retried=false
        compose.setContent { FreesearchTheme { GameDetailsScreen(DetailsUiState.Error(5,"Sem conexão"), {}, {retried=true}) } }
        compose.onNodeWithText("Detalhes indisponíveis").assertIsDisplayed()
        compose.onNodeWithText("Tentar Novamente").performClick()
        assertTrue(retried)
    }
    @Test fun showsMetadataAndHandlesMissingSections() {
        val game = GameDetailsDto(id=5,title="Game",developer="Studio",releaseDate="2020-03-15",description="Descrição completa").toDomainDetails()!!
        compose.setContent { FreesearchTheme { GameDetailsScreen(DetailsUiState.Success(game), {}, {}) } }
        compose.onNodeWithText("Studio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ler descrição completa").performScrollTo().performClick()
        compose.onNodeWithText("Recolher descrição").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Requisitos não informados pela API para este jogo."))
        compose.onNodeWithText("Requisitos não informados pela API para este jogo.").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Visitar site do jogo ↗"))
        compose.onNodeWithText("Visitar site do jogo ↗").assertIsDisplayed()
    }
}
