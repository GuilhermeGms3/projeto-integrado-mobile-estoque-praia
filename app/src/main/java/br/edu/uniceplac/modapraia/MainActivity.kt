package br.edu.uniceplac.modapraia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.edu.uniceplac.modapraia.ui.InventoryApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EstoquePraiaTheme { InventoryApp() } }
    }
}

private val PraiaLight = lightColorScheme(
    primary = Color(0xFF0F6B57),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0E8),
    secondary = Color(0xFFB85A22),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7)
)

private val PraiaDark = darkColorScheme(
    primary = Color(0xFF8FD4BD),
    secondary = Color(0xFFFFB68E)
)

@Composable
private fun EstoquePraiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) PraiaDark else PraiaLight, content = content)
}
