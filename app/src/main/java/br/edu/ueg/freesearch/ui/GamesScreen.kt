package br.edu.ueg.freesearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ueg.freesearch.R
import br.edu.ueg.freesearch.model.Game
import br.edu.ueg.freesearch.model.GamePlatform
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@Composable
fun GamesRoute(viewModel: GamesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val platform by viewModel.platform.collectAsStateWithLifecycle()
    var selectedGameId by rememberSaveable { mutableStateOf<Int?>(null) }
    val screenStateHolder = rememberSaveableStateHolder()
    val id = selectedGameId
    if (id == null) {
        screenStateHolder.SaveableStateProvider("catalog") {
            GamesScreen(state, query, platform, viewModel::onQueryChange,
                viewModel::onPlatformChange, viewModel::load, { selectedGameId = it })
        }
    } else {
        GameDetailsRoute(id, onBack = { selectedGameId = null })
    }
}

@Composable
fun GamesScreen(state: GamesUiState, query: String, platform: String,
                onQuery: (String) -> Unit, onPlatform: (GamePlatform) -> Unit,
                onRetry: () -> Unit, onDetails: (Int) -> Unit) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    fun openLink(url: String) {
        // URLs inválidas nunca encerram o aplicativo.
        if (!url.startsWith("https://") && !url.startsWith("http://")) { linkError = true; return }
        runCatching { uriHandler.openUri(url) }.onFailure { linkError = true }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface {
                TextButton(onClick = { openLink("https://www.freetogame.com/") },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Text("Dados de FreeToGame.com  •  Visitar fonte", fontSize = 12.sp)
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Freesearch", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Seus jogos gratis aqui!", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onRetry, enabled = state !is GamesUiState.Loading) { Text("Atualizar") }
            }
            OutlinedTextField(value = query, onValueChange = onQuery, singleLine = true,
                label = { Text("Buscar título, gênero ou editora") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { onQuery("") }) { Text("Limpar") } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                GamePlatform.entries.forEach { entry ->
                    FilterChip(selected = platform == entry.name, onClick = { onPlatform(entry) }, label = { Text(entry.label) })
                }
            }
            when (state) {
                GamesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Carregando jogos…")
                    }
                }
                is GamesUiState.Error -> MessageContent("Não foi possível carregar", state.message,
                    "Tentar Novamente", onRetry)
                is GamesUiState.Success -> {
                    Text(if (state.games.size == 1) "1 jogo encontrado" else "${state.games.size} jogos encontrados", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.games.isEmpty()) {
                        MessageContent("Nenhum jogo encontrado",
                            if (state.total == 0) "O catálogo está vazio no momento. Tente atualizar."
                            else "Tente outro termo ou selecione Todos.",
                            if (state.total == 0) "Atualizar" else "Limpar filtros",
                            { if (state.total == 0) onRetry() else { onQuery(""); onPlatform(GamePlatform.ALL) } })
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(state.games, key = { it.id }) { game -> GameCard(game) { onDetails(game.id) } }
                        }
                    }
                }
            }
        }
    }
    if (linkError) AlertDialog(onDismissRequest = { linkError = false },
        title = { Text("Link indisponível") }, text = { Text("Não foi possível abrir este endereço no navegador.") },
        confirmButton = { TextButton(onClick = { linkError = false }) { Text("Entendi") } })
}

@Composable
private fun MessageContent(title: String, description: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun GameCard(game: Game, onOpen: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(model = game.thumbnail, contentDescription = "Capa de ${game.title}",
            placeholder = painterResource(R.drawable.ic_freesearch), error = painterResource(R.drawable.ic_freesearch),
            modifier = Modifier.fillMaxWidth().aspectRatio(365f / 206f), contentScale = ContentScale.Crop)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(game.genre.uppercase(), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(game.description, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
            Text("${game.platform} • ${game.publisher}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpen) { Text("Ver detalhes") }
        }
    }
}
