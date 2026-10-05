package br.edu.ueg.freesearch.model

data class GameDetails(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val description: String,
    val genre: String,
    val platform: String,
    val developer: String,
    val publisher: String,
    val releaseDate: String,
    val status: String,
    val url: String,
    val requirements: List<SystemRequirement>,
    val screenshots: List<String>
)

data class SystemRequirement(val label: String, val value: String)
