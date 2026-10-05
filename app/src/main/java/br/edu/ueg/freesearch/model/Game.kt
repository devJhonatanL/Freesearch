package br.edu.ueg.freesearch.model

data class Game(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val description: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val releaseDate: String,
    val url: String
)

enum class GamePlatform(val label: String) {
    ALL("Todos"), PC("PC"), BROWSER("Navegador")
}

// A busca usa o catálogo já carregado, sem uma chamada HTTP por letra.
fun filterGames(games: List<Game>, query: String, platform: GamePlatform): List<Game> {
    val term = query.trim()
    return games.filter { game ->
        val matchesText = term.isBlank() || listOf(game.title, game.genre, game.publisher)
            .any { it.contains(term, ignoreCase = true) }
        val matchesPlatform = when (platform) {
            GamePlatform.ALL -> true
            GamePlatform.PC -> game.platform.contains("PC", ignoreCase = true)
            GamePlatform.BROWSER -> game.platform.contains("browser", ignoreCase = true)
        }
        matchesText && matchesPlatform
    }
}
