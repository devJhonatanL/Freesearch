package br.edu.ueg.freesearch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun FreesearchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4EA8DE),          // Azul claro vibrante para elementos principais
            onPrimary = Color(0xFF003554),        // Azul escuro para texto sobre a cor primária
            background = Color(0xFF0B0F19),       // Azul-escuro quase preto (fundo principal)
            onBackground = Color(0xFFE2EAFC),     // Gelo/Cinza azulado muito claro para textos do fundo
            surface = Color(0xFF111827),          // Cinza escuro azulado para superfícies (cards, dialogs)
            onSurface = Color(0xFFE2EAFC),        // Gelo para textos sobre superfícies
            surfaceVariant = Color(0xFF1F2937),   // Variação de superfície levemente mais clara
            onSurfaceVariant = Color(0xFF9CA3AF), // Texto secundário/auxiliar de menor ênfase
            secondaryContainer = Color(0xFF1E3A8A), // Contêiner secundário em azul oceano escuro
            onSecondaryContainer = Color(0xFFBFDBFE) // Texto claro para o contêiner secundário
        ),
        content = content
    )
}
