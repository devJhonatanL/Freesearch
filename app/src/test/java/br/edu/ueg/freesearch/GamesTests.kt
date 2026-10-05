package br.edu.ueg.freesearch

import androidx.lifecycle.SavedStateHandle
import br.edu.ueg.freesearch.data.*
import br.edu.ueg.freesearch.model.*
import br.edu.ueg.freesearch.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GamesTests {
    private val dispatcher = StandardTestDispatcher()
    private val game = Game(1, "Warframe", "", "Ação cooperativa", "Shooter", "PC (Windows)", "Digital Extremes", "2013-03-25", "https://example.com")
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun searchIgnoresCaseAndWhitespace() {
        assertEquals(listOf(game), filterGames(listOf(game), "  WAR  ", GamePlatform.ALL))
        assertEquals(listOf(game), filterGames(listOf(game), "digital", GamePlatform.ALL))
        assertEquals(listOf(game), filterGames(listOf(game), "shooter", GamePlatform.ALL))
    }
    @Test fun combinesPlatformAndText() {
        assertTrue(filterGames(listOf(game), "war", GamePlatform.BROWSER).isEmpty())
        assertEquals(listOf(game), filterGames(listOf(game), "", GamePlatform.PC))
        assertTrue(filterGames(listOf(game), "zzzz", GamePlatform.ALL).isEmpty())
    }
    @Test fun invalidDtoIsRejectedAndOptionalFieldsHaveFallbacks() {
        assertNull(GameDto(id = 0, title = "Invalid").toDomain())
        assertNull(GameDto(id = 2, title = " ").toDomain())
        assertEquals("Descrição não informada.", GameDto(id = 2, title = "Game").toDomain()!!.description)
    }
    @Test fun loadingThenSuccessAndQueryDoNotRefetch() = runTest(dispatcher) {
        var calls = 0
        val repository = object : GamesRepository {
            override suspend fun getGames(): List<Game> { calls++; return listOf(game) }
        }
        val vm = GamesViewModel(repository, SavedStateHandle())
        assertEquals(GamesUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals(1, (vm.state.value as GamesUiState.Success).games.size)
        vm.onQueryChange("nonexistent")
        assertTrue((vm.state.value as GamesUiState.Success).games.isEmpty())
        vm.onQueryChange("")
        assertEquals(1, (vm.state.value as GamesUiState.Success).games.size)
        assertEquals(1, calls)
    }
    @Test fun connectionErrorCanRecoverOnRetry() = runTest(dispatcher) {
        var failing = true
        val vm = GamesViewModel(object : GamesRepository {
            override suspend fun getGames(): List<Game> {
                if (failing) throw IOException("offline")
                return listOf(game)
            }
        }, SavedStateHandle())
        advanceUntilIdle()
        assertTrue((vm.state.value as GamesUiState.Error).message.contains("internet"))
        failing = false
        vm.load()
        advanceUntilIdle()
        assertEquals(listOf(game), (vm.state.value as GamesUiState.Success).games)
    }
    @Test fun savedQueryAndPlatformAreAppliedAfterReload() = runTest(dispatcher) {
        val state = SavedStateHandle(mapOf("query" to "war", "platform" to "BROWSER"))
        val vm = GamesViewModel(object : GamesRepository {
            override suspend fun getGames() = listOf(game)
        }, state)
        advanceUntilIdle()
        assertEquals("war", vm.query.value)
        assertTrue((vm.state.value as GamesUiState.Success).games.isEmpty())
    }
    @Test fun retrofitMapsJsonAndUsesExpectedEndpoint() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{"id":1,"title":"Warframe","short_description":"Space","game_url":"https://example.com","platform":"PC (Windows)"},{"id":1,"title":"Duplicate"},{"id":0,"title":"Invalid"}]"""))
            val api = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(FreeToGameApi::class.java)
            val games = RemoteGamesRepository(api).getGames()
            assertEquals(1, games.size)
            assertEquals("Space", games.single().description)
            assertEquals("/api/games?sort-by=alphabetical", server.takeRequest().path)
        } finally { server.shutdown() }
    }
    @Test fun httpFailureProducesFriendlyError() = runTest(dispatcher) {
        val vm = GamesViewModel(object : GamesRepository {
            override suspend fun getGames(): List<Game> = throw retrofit2.HttpException(
                retrofit2.Response.error<Any>(503, okhttp3.ResponseBody.create(null, "unavailable")))
        }, SavedStateHandle())
        advanceUntilIdle()
        assertTrue((vm.state.value as GamesUiState.Error).message.contains("503"))
    }
}
