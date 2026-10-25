package com.webyou.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import java.net.URLEncoder

private const val HOME = "https://www.google.com"
private val AD_HOSTS = setOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com",
    "adservice.google.com", "adnxs.com", "taboola.com", "outbrain.com",
    "criteo.com", "rubiconproject.com", "pubmatic.com", "scorecardresearch.com"
)

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }

data class Tab(
    val id: Int,
    var url: String = HOME,
    var title: String = "Google",
    var incognito: Boolean = false
)

class State {
    val tabs = mutableStateListOf(Tab(1))
    var active by mutableIntStateOf(1)
    var screen by mutableStateOf("home")
    var theme by mutableStateOf(AppTheme.SYSTEM)
    var accent by mutableIntStateOf(0)
    var adblock by mutableStateOf(true)
    var blocked by mutableIntStateOf(0)
    var waveDepth by mutableFloatStateOf(0.08f)
    var waveCount by mutableIntStateOf(8)

    fun tab(): Tab = tabs.first { it.id == active }

    fun add(incognito: Boolean = false) {
        val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        tabs.add(Tab(id, title = if (incognito) "Инкогнито" else "Новая вкладка", incognito = incognito))
        active = id
        screen = "home"
    }

    fun close(id: Int) {
        if (tabs.size == 1) {
            tabs[0] = Tab(1)
            active = 1
            return
        }
        val index = tabs.indexOfFirst { it.id == id }
        tabs.removeAll { it.id == id }
        if (active == id) active = tabs[index.coerceAtMost(tabs.lastIndex)].id
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { App() }
    }
}

@Composable
fun App() {
    val s = remember { State() }
    val dark = when (s.theme) {
        AppTheme.DARK, AppTheme.AMOLED -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val lightPrimary = listOf(
        Color(0xFF7656D6), Color(0xFF3C6EDB), Color(0xFF008A78), Color(0xFFC85C8E)
    )[s.accent]
    val darkPrimary = listOf(
        Color(0xFFC9B7FF), Color(0xFFA9C5FF), Color(0xFF8BE0D0), Color(0xFFFFB1D0)
    )[s.accent]

    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = darkPrimary,
                background = if (s.theme == AppTheme.AMOLED) Color.Black else Color(0xFF191A1A),
                surface = if (s.theme == AppTheme.AMOLED) Color.Black else Color(0xFF222424),
                surfaceContainer = if (s.theme == AppTheme.AMOLED) Color(0xFF111212) else Color(0xFF292B2B)
            )
        } else {
            lightColorScheme(
                primary = lightPrimary,
                background = Color(0xFFF9F7FF),
                surface = Color(0xFFFCF9FF),
                surfaceContainer = Color(0xFFF1EDF7)
            )
        }
    ) {
        AnimatedContent(targetState = s.screen, label = "screen") { screen ->
            when (screen) {
                "home" -> Home(s)
                "browser" -> Browser(s)
                "tabs" -> Tabs(s)
                "menu" -> Menu(s)
                "settings" -> Settings(s)
                "customize" -> Customize(s)
                "adblock" -> Adblock(s)
                "privacy" -> Privacy(s)
                "translate" -> Translate(s)
                else -> Home(s)
            }
        }
    }
}

@Composable
fun Background(s: State) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (dark) Color(0xFF161717) else Color(0xFFF9F7FF)
    val blob = if (dark) Color(0xFF312842) else Color(0xFFECE5FF)

    Canvas(Modifier.fillMaxSize()) {
        drawRect(background)
        val wave = size.height * s.waveDepth
        val path = Path().apply {
            moveTo(0f, 0f)
            cubicTo(size.width * .25f, wave, size.width * .45f, -wave * .4f, size.width * .65f, wave * .6f)
            cubicTo(size.width * .82f, wave, size.width * .92f, wave * .2f, size.width, wave * .5f)
            lineTo(size.width, 0f)
            close()
        }
        drawPath(path, blob)
    }
}

@Composable
fun Pill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(50.dp, 32.dp, 42.dp, 18.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
fun SearchBox(value: String, onValue: (String) -> Unit, onGo: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp, 18.dp, 30.dp, 24.dp),
        tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Box(Modifier.weight(1f).padding(12.dp)) {
                if (value.isEmpty()) {
                    Text("Введите запрос или URL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(value, onValue, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            IconButton(onClick = onGo) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
            }
        }
    }
}

@Composable
fun Home(s: State) {
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        Background(s)
