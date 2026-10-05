package br.edu.ueg.freesearch.data

import br.edu.ueg.freesearch.model.GameDetails
import br.edu.ueg.freesearch.model.SystemRequirement
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
//Mesma coisa do GameDto so q pros detalhes do jogo.
//Tambem possui algumas informações adicionais e duas estruturas extras RequirementsDto (requisito) e ScreenshotDto (foto)
data class GameDetailsDto(
    val id: Int? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val description: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    val genre: String? = null,
    val platform: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    val status: String? = null,
    @SerializedName("game_url") val gameUrl: String? = null,
    @SerializedName("minimum_system_requirements") val minimumRequirements: RequirementsDto? = null,
    val screenshots: List<ScreenshotDto?>? = null
)

data class RequirementsDto(val os: String? = null, val processor: String? = null,
    val memory: String? = null, val graphics: String? = null, val storage: String? = null)
data class ScreenshotDto(val image: String? = null)

private fun String?.orUnknown() = this?.trim()?.takeIf { it.isNotEmpty() } ?: "Não informado"

fun GameDetailsDto.toDomainDetails(): GameDetails? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val requirements = minimumRequirements?.let { req ->
        listOf("Sistema operacional" to req.os, "Processador" to req.processor,
            "Memória RAM" to req.memory, "Placa de vídeo" to req.graphics, "Armazenamento" to req.storage)
            .mapNotNull { (label, value) -> value?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { SystemRequirement(label, it) } }
    }.orEmpty()
    val formattedDate = releaseDate?.let { raw ->
        runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
    } ?: "Não informada"
    val readableStatus = when (status?.lowercase()) {
        "live" -> "Disponível"
        "offline" -> "Offline"
        else -> status.orUnknown()
    }
    return GameDetails(validId, validTitle, thumbnail.orEmpty(),
        description?.trim()?.takeIf { it.isNotEmpty() }
            ?: shortDescription?.trim()?.takeIf { it.isNotEmpty() } ?: "Descrição não informada.",
        genre.orUnknown(), platform.orUnknown(), developer.orUnknown(), publisher.orUnknown(),
        formattedDate, readableStatus, gameUrl.orEmpty(), requirements,
        screenshots.orEmpty().mapNotNull { it?.image?.trim()?.takeIf { url -> url.startsWith("https://") } }.distinct())
}
