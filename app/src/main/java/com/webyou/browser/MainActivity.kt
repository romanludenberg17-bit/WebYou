package com.webyou.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Incognito
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.Executors

private const val HOME_URL = "https://www.google.com"
private const val TRANSLATE_BASE = "https://translate.google.com/translate?sl=auto&tl=ru&u="

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { WebYouRoot() }
    }
}

data class BrowserTab(
    val id: Int,
    var url: String = HOME_URL,
    var title: String = "Google",
    var preview: Bitmap? = null,
    var incognito: Boolean = false
)

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }

class WebYouState {
    val tabs = mutableStateListOf(BrowserTab(1))
    var activeTabId by mutableIntStateOf(1)
    var screen by mutableStateOf("home")
    var theme by mutableStateOf(AppTheme.SYSTEM)
    var adBlockEnabled by mutableStateOf(true)
    var proxyEnabled by mutableStateOf(false)
    var proxyHost by mutableStateOf("")
    var blockedCount by mutableIntStateOf(0)
    var accent by mutableStateOf(0)
    var waveDepth by mutableStateOf(0.08f)
    var waveCount by mutableIntStateOf(8)

    fun activeTab(): BrowserTab = tabs.first { it.id == activeTabId }
    fun addTab(incognito: Boolean = false) {
        val id = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        tabs.add(BrowserTab(id, incognito = incognito, title = if (incognito) "Инкогнито" else "Новая вкладка"))
        activeTabId = id
        screen = "home"
    }
    fun closeTab(id: Int) {
        if (tabs.size == 1) {
            tabs[0] = BrowserTab(1)
            activeTabId = 1
            return
        }
        val index = tabs.indexOfFirst { it.id == id }
        tabs.removeAll { it.id == id }
        if (activeTabId == id) activeTabId = tabs[index.coerceAtMost(tabs.lastIndex)].id
    }
}

@Composable
fun WebYouRoot() {
    val state = remember { WebYouState() }
    val isDark = when (state.theme) {
        AppTheme.DARK, AppTheme.AMOLED -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val lightPrimary = listOf(Color(0xFF7656D6), Color(0xFF3F63D8), Color(0xFFAF4D7B), Color(0xFF4B8C7A))
    val darkPrimary = listOf(Color(0xFFB8A6FF), Color(0xFFA9B9FF), Color(0xFFFFA8D2), Color(0xFF9FE0CA))
    val primary = if (isDark) darkPrimary[state.accent] else lightPrimary[state.accent]
    val background = if (state.theme == AppTheme.AMOLED) Color.Black else MaterialTheme.colorScheme.background

    MaterialTheme(
        colorScheme = if (isDark) androidx.compose.material3.darkColorScheme(primary = primary, background = background) else androidx.compose.material3.lightColorScheme(primary = primary, background = background)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    if (state.screen != "home") {
                        TopAppBar(
                            title = { Text(screenTitle(state.screen), fontWeight = FontWeight.SemiBold) },
                            navigationIcon = { IconButton(onClick = { state.screen = "home" }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) } },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
                    AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                        when (screen) {
                            "home" -> HomeScreen(state)
                            "tabs" -> TabsScreen(state)
                            "settings" -> SettingsScreen(state)
                            "privacy" -> PrivacyScreen(state)
                            "translate" -> TranslateScreen(state)
                            "proxy" -> ProxyScreen(state)
                            else -> HomeScreen(state)
                        }
                    }
                }
            }
        }
    }
}

private fun screenTitle(screen: String) = when (screen) {
    "tabs" -> "Вкладки"
    "settings" -> "Настройки"
    "privacy" -> "Приватность"
    "translate" -> "Перевод"
    "proxy" -> "Прокси"
    else -> "WebYou"
}

@Composable
private fun ExpressiveBackground(
    modifier: Modifier = Modifier,
    depth: Float = 0.08f,
    count: Int = 8,
    dark: Boolean
) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(if (dark) Color(0xFF161717) else Color(0xFFF9F7FF))
        val blob = if (dark) Color(0xFF312842) else Color(0xFFECE5FF)
        val wave = size.height * depth
        repeat(count.coerceIn(3, 12)) { i ->
            val y = size.height * 0.08f + wave * i / count.coerceAtLeast(1)
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(size.width * 0.22f, y - wave * 0.55f, size.width * 0.42f, y + wave * 0.55f, size.width * 0.66f, y)
                cubicTo(size.width * 0.82f, y - wave * 0.35f, size.width * 0.93f, y + wave * 0.35f, size.width, y - wave * 0.08f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, blob.copy(alpha = 0.10f + i * 0.008f))
        }
    }
}

@Composable
private fun HomeScreen(s: WebYouState) {
    var query by remember { mutableStateOf("") }
    val tab = s.activeTab()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("WebYou", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("быстрый, приватный браузер", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { openQuery(tab, query) }) { Icon(Icons.Rounded.OpenInNew, null) } },
            placeholder = { Text("поиск или адрес") }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeAction("Вкладки", Icons.Rounded.MoreVert, Modifier.weight(1f)) { s.screen = "tabs" }
            HomeAction("Настройки", Icons.Rounded.Tune, Modifier.weight(1f)) { s.screen = "settings" }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeAction("Приватность", Icons.Rounded.PrivacyTip, Modifier.weight(1f)) { s.screen = "privacy" }
            HomeAction("Перевод", Icons.Rounded.Translate, Modifier.weight(1f)) { s.screen = "translate" }
        }
        if (s.proxyEnabled) AssistChip(onClick = { s.screen = "proxy" }, label = { Text("Прокси включён") }, leadingIcon = { Icon(Icons.Rounded.Wifi, null) })
        Spacer(Modifier.weight(1f))
        Text("текущая вкладка: ${tab.title}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun HomeAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun openQuery(tab: BrowserTab, query: String) {
    tab.url = if (query.startsWith("http://") || query.startsWith("https://")) query else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8")
    tab.title = query
}

@Composable
private fun TabsScreen(s: WebYouState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { s.addTab() }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Новая") }
            Button(onClick = { s.addTab(true) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Incognito, null); Spacer(Modifier.width(6.dp)); Text("Инкогнито") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(s.tabs, key = { it.id }) { tab ->
                Card(Modifier.fillMaxWidth().clickable { s.activeTabId = tab.id; s.screen = "home" }, shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (tab.incognito) Icons.Rounded.Visibility else Icons.Rounded.Language, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tab.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tab.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { s.closeTab(tab.id) }) { Icon(Icons.Rounded.Close, null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(s: WebYouState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SettingCard("Тема", Icons.Rounded.Palette) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    AssistChip(onClick = { s.theme = theme }, label = { Text(theme.name.lowercase()) })
                }
            }
        } }
        item { SettingCard("Акцент", Icons.Rounded.Palette) {
            Slider(value = s.accent.toFloat(), onValueChange = { s.accent = it.roundToInt().coerceIn(0, 3) }, valueRange = 0f..3f, steps = 2)
        } }
        item { SettingCard("Выразительный фон", Icons.Rounded.Tune) {
            Text("глубина")
            Slider(value = s.waveDepth, onValueChange = { s.waveDepth = it }, valueRange = 0.02f..0.18f)
            Text("слоёв: ${s.waveCount}")
            Slider(value = s.waveCount.toFloat(), onValueChange = { s.waveCount = it.roundToInt().coerceIn(3, 12) }, valueRange = 3f..12f, steps = 8)
        } }
        item { SettingToggle("Блокировка рекламы", Icons.Rounded.Security, s.adBlockEnabled) { s.adBlockEnabled = it } }
        item { SettingCard("Прокси", Icons.Rounded.Wifi) { Button(onClick = { s.screen = "proxy" }) { Text(if (s.proxyEnabled) "настроить" else "включить") } } }
    }
}

@Composable
private fun SettingCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Text(title, fontWeight = FontWeight.SemiBold) }
            content()
        }
    }
}

@Composable
private fun SettingToggle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null); Spacer(Modifier.width(10.dp)); Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked, onCheckedChange)
        }
    }
}

@Composable
private fun PrivacyScreen(s: WebYouState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingCard("Приватность", Icons.Rounded.PrivacyTip) {
            Text("WebYou не требует учётной записи. Инкогнито-вкладки не сохраняют историю внутри состояния браузера.")
            Divider()
            Text("Заблокировано запросов: ${s.blockedCount}")
            Button(onClick = { s.blockedCount = 0 }) { Text("Сбросить счётчик") }
        }
    }
}

@Composable
private fun TranslateScreen(s: WebYouState) {
    var url by remember { mutableStateOf(s.activeTab().url) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL") })
        Button(onClick = { s.activeTab().url = TRANSLATE_BASE + java.net.URLEncoder.encode(url, "UTF-8"); s.screen = "home" }) { Icon(Icons.Rounded.Translate, null); Spacer(Modifier.width(8.dp)); Text("Открыть перевод") }
    }
}

@Composable
private fun ProxyScreen(s: WebYouState) {
    var host by remember { mutableStateOf(s.proxyHost) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingCard("Прокси", Icons.Rounded.Wifi) {
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("host:port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            SettingToggle("Включить", Icons.Rounded.Wifi, s.proxyEnabled) { enabled ->
                s.proxyEnabled = enabled
                s.proxyHost = host
                applyProxy(enabled, host)
            }
        }
    }
}

private fun applyProxy(enabled: Boolean, host: String) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
    val controller = ProxyController.getInstance()
    if (!enabled || host.isBlank()) {
        controller.clearProxyOverride(Executors.newSingleThreadExecutor(), Runnable {})
        return
    }
    val config = ProxyConfig.Builder().addProxyRule("http://$host").addProxyRule("https://$host").build()
    controller.setProxyOverride(config, Executors.newSingleThreadExecutor(), Runnable {})
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserView(s: WebYouState) {
    val tab = s.activeTab()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        if (s.adBlockEnabled && request?.url != null && isAdHost(request.url.host ?: "")) {
                            s.blockedCount++
                            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (url != null) tab.url = url
                        tab.title = view?.title ?: tab.title
                    }
                }
                loadUrl(tab.url)
            }
        },
        update = { webView -> if (webView.url != tab.url) webView.loadUrl(tab.url) }
    )
}

private fun isAdHost(host: String): Boolean {
    val lower = host.lowercase()
    val hosts = listOf("doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "adnxs.com", "taboola.com", "outbrain.com", "criteo.com", "rubiconproject.com", "pubmatic.com", "scorecardresearch.com")
    return hosts.any { lower == it || lower.endsWith(".$it") }
}

private fun Int.roundToInt(): Int = kotlin.math.round(this.toFloat()).toInt()
private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
