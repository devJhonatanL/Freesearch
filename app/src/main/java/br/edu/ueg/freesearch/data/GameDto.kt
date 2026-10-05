package br.edu.ueg.freesearch.data

import br.edu.ueg.freesearch.model.Game
import com.google.gson.annotations.SerializedName

//Essa parte represeta o jogo bruto. Fresquinho da API.
//So que a api manda em JSON, ai tem os campos correspondentes.
//Outra coisa é que alguns campos tem nomes diferentes aki no Kotlin, ai tem a conversão
data class GameDto(
    val id: Int? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    @SerializedName("game_url") val gameUrl: String? = null,
    val genre: String? = null,
    val platform: String? = null,
    val publisher: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null
)
// Esssa função transforma os dados em um objeto do tipo Game, alem de conferir dados e por texto em caso ausencia de informação.
fun GameDto.toDomain(): Game? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Game(validId, validTitle, thumbnail.orEmpty(),
        shortDescription?.takeIf { it.isNotBlank() } ?: "Descrição não informada.",
        genre ?: "Não informado", platform ?: "Não informada",
        publisher ?: "Não informada", releaseDate.orEmpty(), gameUrl.orEmpty())
}
