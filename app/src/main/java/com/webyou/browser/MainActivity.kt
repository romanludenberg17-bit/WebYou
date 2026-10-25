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
    Canvas(Modifier.fillMaxSize()) {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        drawRect(if (dark) Color(0xFF161717) else Color(0xFFF9F7FF))
        val blob = if (dark) Color(0xFF312842) else Color(0xFFECE5FF)
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
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill("Настроить", Icons.Rounded.Tune) { s.screen = "customize" }
            }
            Spacer(Modifier.height(45.dp))
            Surface(
                Modifier.size(84.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Wifi, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("WebYou", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Быстрый. Приватный. Твой.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            SearchBox(query, { query = it }) { open(s, query) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Google", Icons.Rounded.Search) { open(s, HOME) }
                Pill("YouTube", Icons.Rounded.OpenInNew) { open(s, "https://youtube.com") }
                Pill("Wikipedia", Icons.Rounded.Language) { open(s, "https://wikipedia.org") }
            }
            Spacer(Modifier.height(18.dp))
            Pill("Новая вкладка", Icons.Rounded.Add, Modifier.fillMaxWidth()) { s.add() }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Вкладки", Icons.Rounded.Visibility, Modifier.weight(1f)) { s.screen = "tabs" }
                Pill("Приватность", Icons.Rounded.PrivacyTip, Modifier.weight(1f)) { s.screen = "privacy" }
            }
            Spacer(Modifier.height(10.dp))
            Pill("Перевод страницы", Icons.Rounded.Translate, Modifier.fillMaxWidth()) { s.screen = "translate" }
            Spacer(Modifier.weight(1f))
            Nav(s)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Browser(s: State) {
    val tab = s.tab()
    var address by remember(tab.id, tab.url) { mutableStateOf(tab.url) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { s.screen = "home" }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
            Surface(
                Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                BasicTextField(address, { address = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
            IconButton(onClick = { open(s, address) }) { Icon(Icons.Rounded.Search, null) }
            IconButton(onClick = { s.screen = "menu" }) { Icon(Icons.Rounded.MoreVert, null) }
        }
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val host = request.url.host ?: return null
                            if (s.adblock && AD_HOSTS.any { host == it || host.endsWith(".$it") }) {
                                s.blocked++
                                return WebResourceResponse("text/plain", "utf-8", null)
                            }
                            return null
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            tab.url = url
                            tab.title = view.title ?: url
                        }
                    }
                    loadUrl(tab.url)
                }
            },
            modifier = Modifier.weight(1f)
        )
        Nav(s)
    }
}

@Composable
fun Nav(s: State) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp, 22.dp, 38.dp, 26.dp),
        tonalElevation = 4.dp
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { s.screen = "home" }) { Icon(Icons.Rounded.Home, null) }
            IconButton(onClick = { s.screen = "tabs" }) { Icon(Icons.Rounded.Visibility, null) }
            IconButton(onClick = { s.screen = "menu" }) { Icon(Icons.Rounded.MoreVert, null) }
            IconButton(onClick = { s.add() }) { Icon(Icons.Rounded.Add, null) }
        }
    }
}

@Composable
fun Tabs(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Top("Вкладки", s)
        s.tabs.forEach { tab ->
            Surface(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable {
                    s.active = tab.id
                    s.screen = "browser"
                },
                shape = RoundedCornerShape(28.dp),
                color = if (tab.id == s.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tab.title, fontWeight = FontWeight.Bold)
                        Text(tab.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { s.close(tab.id) }) { Icon(Icons.Rounded.Close, null) }
                }
            }
        }
        Pill("Новая вкладка инкогнито", Icons.Rounded.Security, Modifier.fillMaxWidth()) { s.add(true) }
    }
}

@Composable
fun Menu(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Top("Меню", s)
        MenuItem("Новая вкладка", Icons.Rounded.Add) { s.add() }
        MenuItem("Инкогнито", Icons.Rounded.Security) { s.add(true) }
        MenuItem("Настройки", Icons.Rounded.Settings) { s.screen = "settings" }
        MenuItem("Блокировка рекламы", Icons.Rounded.Shield) { s.screen = "adblock" }
        MenuItem("Перевод", Icons.Rounded.Translate) { s.screen = "translate" }
        MenuItem("Приватность", Icons.Rounded.PrivacyTip) { s.screen = "privacy" }
    }
}

@Composable
fun MenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Pill(text, icon, Modifier.fillMaxWidth(), onClick = onClick)
    Spacer(Modifier.height(9.dp))
}

@Composable
fun Settings(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Top("Настройки", s)
        MenuItem("Внешний вид", Icons.Rounded.Palette) { s.screen = "customize" }
        MenuItem("Блокировка рекламы", Icons.Rounded.Shield) { s.screen = "adblock" }
        MenuItem("Конфиденциальность", Icons.Rounded.PrivacyTip) { s.screen = "privacy" }
    }
}

@Composable
fun Customize(s: State) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Top("Внешний вид", s)
        Text("Тема", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            AppTheme.values().forEach { theme ->
                Pill(theme.name, selected = theme == s.theme) { s.theme = theme }
            }
        }
        Text("Акцент", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Surface(
                    Modifier.size(45.dp).clickable { s.accent = index },
                    shape = RoundedCornerShape(16.dp),
                    color = listOf(Color(0xFF7656D6), Color(0xFF3C6EDB), Color(0xFF008A78), Color(0xFFC85C8E))[index]
                ) {}
            }
        }
        Text("Глубина волн: ${(s.waveDepth * 100).toInt()}%")
        Slider(s.waveDepth, { s.waveDepth = it }, valueRange = 0.02f..0.18f)
        Text("Количество волн: ${s.waveCount}")
        Slider(s.waveCount.toFloat(), { s.waveCount = it.toInt() }, valueRange = 2f..12f, steps = 9)
    }
}

@Composable
fun Adblock(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Top("Блокировка рекламы", s)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Чистый интернет", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Блокировать рекламу", Modifier.weight(1f))
                    Switch(checked = s.adblock, onCheckedChange = { s.adblock = it })
                }
                Text("Заблокировано запросов: ${s.blocked}")
            }
        }
    }
}

@Composable
fun Privacy(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Top("Конфиденциальность", s)
        MenuItem("Блокировать трекеры", Icons.Rounded.PrivacyTip) {}
        MenuItem("Безопасный просмотр", Icons.Rounded.Security) {}
        MenuItem("Инкогнито", Icons.Rounded.Security) { s.add(true) }
    }
}

@Composable
fun Translate(s: State) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Top("Перевод страницы", s)
        Text(s.tab().url, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Pill("Перевести через Google", Icons.Rounded.Translate, Modifier.fillMaxWidth()) {
            val encoded = URLEncoder.encode(s.tab().url, "UTF-8")
            open(s, "https://translate.google.com/translate?sl=auto&tl=ru&u=$encoded")
        }
    }
}

@Composable
fun Top(title: String, s: State) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { s.screen = if (s.tab().url == HOME) "home" else "browser" }) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
        }
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    }
}

private fun open(s: State, input: String) {
    val q = input.trim()
    if (q.isEmpty()) return
    s.tab().url = when {
        q.startsWith("http://") || q.startsWith("https://") -> q
        q.contains(".") && !q.contains(" ") -> "https://$q"
        else -> "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8")
    }
    s.screen = "browser"
}
