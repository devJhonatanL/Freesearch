package br.edu.ueg.freesearch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.edu.ueg.freesearch.R
import br.edu.ueg.freesearch.model.GameDetails
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

//Esse arquivo apresenta a ficha, a descrição, a galeria e os requisitos.

@Composable
fun GameDetailsRoute(gameId: Int, onBack: () -> Unit,
                     viewModel: GameDetailsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(gameId) { viewModel.load(gameId) }
    BackHandler(onBack = onBack)
    GameDetailsScreen(if (state.gameId == gameId) state else DetailsUiState.Loading(gameId),
        onBack, { viewModel.load(gameId, force = true) })
}

@Composable
fun GameDetailsScreen(state: DetailsUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    fun openLink(url: String) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) { linkError = true; return }
        runCatching { uriHandler.openUri(url) }.onFailure { linkError = true }
    }
    Scaffold(topBar = {
        Surface {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Voltar") }
                Text("Detalhes do jogo", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRetry, enabled = state !is DetailsUiState.Loading) { Text("Atualizar") }
            }
        }
    }, bottomBar = {
        TextButton(onClick = { openLink("https://www.freetogame.com/") },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text("Dados de FreeToGame.com") }
    }) { padding ->
        when (state) {
            is DetailsUiState.Loading -> Column(Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Carregando detalhes…")
            }
            is DetailsUiState.Error -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center) {
                Text("Detalhes indisponíveis", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Text(state.message)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRetry) { Text("Tentar Novamente") }
            }
            is DetailsUiState.Success -> key(state.game.id) {
                DetailsContent(state.game, Modifier.padding(padding), { openLink(state.game.url) })
            }
        }
    }
    if (linkError) AlertDialog(onDismissRequest = { linkError = false },
        title = { Text("Link indisponível") }, text = { Text("Não foi possível abrir este endereço no navegador.") },
        confirmButton = { TextButton(onClick = { linkError = false }) { Text("Entendi") } })
}

@Composable
private fun DetailsContent(game: GameDetails, modifier: Modifier, onWebsite: () -> Unit) {
    var expanded by rememberSaveable(game.id) { mutableStateOf(false) }
    var selectedImage by rememberSaveable(game.id) { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            AsyncImage(model = game.thumbnail, contentDescription = "Capa de ${game.title}",
                error = painterResource(R.drawable.ic_freesearch),
                modifier = Modifier.fillMaxWidth().aspectRatio(365f / 206f).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop)
        }
        item {
            Text(game.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${game.genre} • ${game.platform}", color = MaterialTheme.colorScheme.primary)
        }
        item {
            DetailHeading("Ficha do jogo")
            DetailRow("Desenvolvedora", game.developer)
            DetailRow("Editora", game.publisher)
            DetailRow("Lançamento", game.releaseDate)
            DetailRow("Situação na FreeToGame", game.status)
        }
        item {
            DetailHeading("Sobre o jogo")
            Text(game.description, maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Recolher descrição" else "Ler descrição completa") }
            Text("Descrição no idioma fornecido pela API.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            DetailHeading("Imagens do jogo")
            if (game.screenshots.isEmpty()) Text("A API não forneceu imagens adicionais para este jogo.")
            else {
                Text("Deslize e toque em uma imagem para ampliar.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(game.screenshots) { url ->
                        AsyncImage(model = url, contentDescription = "Ampliar imagem de ${game.title}",
                            error = painterResource(R.drawable.ic_freesearch),
                            modifier = Modifier.width(280.dp).aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp)).clickable { selectedImage = url },
                            contentScale = ContentScale.Crop)
                    }
                }
            }
        }
        item {
            DetailHeading("Requisitos mínimos")
            Text("Requisitos do jogo na plataforma informada, não do aplicativo Android.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (game.requirements.isEmpty()) Text("Requisitos não informados pela API para este jogo.")
            else game.requirements.forEach { DetailRow(it.label, it.value) }
        }
        item {
            OutlinedButton(onClick = onWebsite, modifier = Modifier.fillMaxWidth()) { Text("Visitar site do jogo ↗") }
        }
    }
    selectedImage?.let { image ->
        Dialog(onDismissRequest = { selectedImage = null }) {
            Surface(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp)) {
                    AsyncImage(model = image, contentDescription = "Imagem ampliada de ${game.title}",
                        error = painterResource(R.drawable.ic_freesearch),
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Fit)
                    TextButton(onClick = { selectedImage = null }, modifier = Modifier.align(Alignment.End)) { Text("Fechar imagem") }
                }
            }
        }
    }
}

@Composable
private fun DetailHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
