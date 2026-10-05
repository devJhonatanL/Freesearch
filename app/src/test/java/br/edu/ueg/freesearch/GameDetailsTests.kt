package br.edu.ueg.freesearch

import br.edu.ueg.freesearch.data.*
import br.edu.ueg.freesearch.model.GameDetails
import br.edu.ueg.freesearch.ui.*
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class GameDetailsTests {
    private val dispatcher = StandardTestDispatcher()
    private fun game(id: Int = 5) = GameDetailsDto(id = id, title = "Game $id").toDomainDetails()!!
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun mapsOptionalDataAndFormatsDate() {
        val result = GameDetailsDto(id = 5, title = "Game", description = " ", shortDescription = "Resumo",
            releaseDate = "2020-03-15", status = "Live",
            minimumRequirements = RequirementsDto(memory = "8 GB", processor = " "),
            screenshots = listOf(null, ScreenshotDto(""), ScreenshotDto("https://example.com/a.jpg"),
                ScreenshotDto("https://example.com/a.jpg"))).toDomainDetails()!!
        assertEquals("Resumo", result.description)
        assertEquals("15/03/2020", result.releaseDate)
        assertEquals("Disponível", result.status)
        assertEquals("Não informado", result.developer)
        assertEquals(1, result.requirements.size)
        assertEquals(1, result.screenshots.size)
        assertTrue(GameDetailsDto(id=5, title="Game").toDomainDetails()!!.requirements.isEmpty())
    }
    @Test fun rejectsInvalidIdentityAndDateDoesNotCrash() {
        assertNull(GameDetailsDto(id=0, title="Game").toDomainDetails())
        assertNull(GameDetailsDto(id=2, title=" ").toDomainDetails())
        assertEquals("Não informada", GameDetailsDto(id=5, title="Game", releaseDate="invalid").toDomainDetails()!!.releaseDate)
    }
    @Test fun retrofitRequestsGameIdAndMapsNestedJson() = runTest {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"id":5,"title":"Game","description":"Complete","developer":"Studio","release_date":"2020-03-15","minimum_system_requirements":{"memory":"8 GB"},"screenshots":[{"id":1,"image":"https://example.com/a.jpg"}]}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(FreeToGameApi::class.java)
            val result = RemoteGameDetailsRepository(api).getGame(5)
            assertEquals("Complete", result.description)
            assertEquals("Studio", result.developer)
            assertEquals("8 GB", result.requirements.single().value)
            assertEquals("/api/game?id=5", server.takeRequest().path)
        } finally { server.shutdown() }
    }
    @Test fun repositoryRejectsWrongGame() = runTest {
        val api = object : FreeToGameApi {
            override suspend fun getGames(sortBy: String) = emptyList<GameDto>()
            override suspend fun getGame(id: Int) = GameDetailsDto(id=99, title="Wrong")
        }
        try { RemoteGameDetailsRepository(api).getGame(5); fail("Should reject wrong ID") }
        catch (_: JsonParseException) { /* expected */ }
    }
    @Test fun loadingSuccessAndSameGameDoNotReloadUntilRefresh() = runTest(dispatcher) {
        var calls=0
        val vm = GameDetailsViewModel(object : GameDetailsRepository {
            override suspend fun getGame(id: Int): GameDetails { calls++; return game(id) }
        })
        vm.load(5)
        assertEquals(DetailsUiState.Loading(5), vm.state.value)
        advanceUntilIdle()
        assertEquals(5, (vm.state.value as DetailsUiState.Success).game.id)
        vm.load(5); advanceUntilIdle(); assertEquals(1,calls)
        vm.load(5, force=true); advanceUntilIdle(); assertEquals(2,calls)
    }
    @Test fun detailsErrorCanRecoverOnRetry() = runTest(dispatcher) {
        var fail=true
        val vm = GameDetailsViewModel(object : GameDetailsRepository {
            override suspend fun getGame(id: Int): GameDetails {
                if(fail) throw IOException("offline")
                return game(id)
            }
        })
        vm.load(5); advanceUntilIdle()
        assertTrue((vm.state.value as DetailsUiState.Error).message.contains("internet"))
        fail=false; vm.load(5, force=true); advanceUntilIdle()
        assertTrue(vm.state.value is DetailsUiState.Success)
    }
    @Test fun switchingGamesCancelsOldRequest() = runTest(dispatcher) {
        val vm = GameDetailsViewModel(object : GameDetailsRepository {
            override suspend fun getGame(id: Int): GameDetails { delay(if(id==5) 1000L else 10L); return game(id) }
        })
        vm.load(5); runCurrent()
        vm.load(6); advanceUntilIdle()
        assertEquals(6, (vm.state.value as DetailsUiState.Success).game.id)
    }
}
