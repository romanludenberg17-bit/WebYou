package com.webyou.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.material.icons.rounded.Shield
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

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }

data class BrowserTab(
    val id: Int,
    var url: String = HOME_URL,
    var title: String = "Google",
    var preview: Bitmap? = null,
    var incognito: Boolean = false
)

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
        if (activeTabId == id) activeTabId = tabs[(index.coerceAtMost(tabs.lastIndex))].id
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
    WebYouTheme(dark = isDark, accent = state.accent, amoled = state.theme == AppTheme.AMOLED) {
        AnimatedContent(targetState = state.screen, label = "screen") { screen ->
            when (screen) {
                "home" -> HomeScreen(state)
                "browser" -> BrowserScreen(state)
                "tabs" -> TabsScreen(state)
                "menu" -> MenuScreen(state)
                "customize" -> CustomizeScreen(state)
                "settings" -> SettingsScreen(state)
                "adblock" -> AdBlockScreen(state)
                "translate" -> TranslateScreen(state)
                "proxy" -> ProxyScreen(state)
                "privacy" -> PrivacyScreen(state)
                else -> HomeScreen(state)
            }
        }
    }
}

@Composable
fun WebYouTheme(dark: Boolean, accent: Int, amoled: Boolean, content: @Composable () -> Unit) {
    val lightPrimary = listOf(Color(0xFF7656D6), Color(0xFF3C6EDB), Color(0xFF008A78), Color(0xFFC85C8E))[accent.coerceIn(0, 3)]
    val darkPrimary = listOf(Color(0xFFC9B7FF), Color(0xFFA9C5FF), Color(0xFF8BE0D0), Color(0xFFFFB1D0))[accent.coerceIn(0, 3)]
    val scheme = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = darkPrimary,
            onPrimary = Color(0xFF2A2043),
            primaryContainer = Color(0xFF3C3156),
            onPrimaryContainer = Color(0xFFEADDFF),
            background = if (amoled) Color.Black else Color(0xFF191A1A),
            surface = if (amoled) Color.Black else Color(0xFF222424),
            surfaceContainer = if (amoled) Color(0xFF111212) else Color(0xFF292B2B),
            onSurface = Color(0xFFE7E1E6),
            onSurfaceVariant = Color(0xFFC8C3C8)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF25153F),
            background = Color(0xFFF9F7FF),
            surface = Color(0xFFFCF9FF),
            surfaceContainer = Color(0xFFF1EDF7),
            onSurface = Color(0xFF1C1B20),
            onSurfaceVariant = Color(0xFF625E66)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun ExpressiveBackground(modifier: Modifier = Modifier, depth: Float = 0.08f, count: Int = 8, dark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()) {
    Canvas(modifier.fillMaxSize()) {
        val base = if (dark) Color(0xFF161717) else Color(0xFFF9F7FF)
        drawRect(base)
        val blob = if (dark) Color(0xFF312842) else Color(0xFFECE5FF)
        val blob2 = if (dark) Color(0xFF273036) else Color(0xFFF7EAF5)
        val wave = size.height * depth
        val path = Path().apply {
            moveTo(0f, 0f)
            cubicTo(size.width * .28f, wave, size.width * .38f, -wave * .4f, size.width * .62f, wave * .55f)
            cubicTo(size.width * .82f, wave * .95f, size.width * .9f, wave * .15f, size.width, wave * .48f)
            lineTo(size.width, 0f)
            close()
        }
        drawPath(path, blob, style = Fill)
        repeat(count.coerceAtMost(12)) { i ->
            val x = (i * 137f) % size.width
            val y = size.height * (.1f + (i % 5) * .17f)
            drawCircle(if (i % 2 == 0) blob2 else blob, radius = 28f + i * 3f, center = Offset(x, y))
        }
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp = 64.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(24.dp)).background(
            Brush.linearGradient(listOf(Color(0xFFBCA3FF), Color(0xFF7656D6)))
        ), contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Wifi, null, tint = Color.White, modifier = Modifier.size(size * .55f))
    }
}

@Composable
private fun WavePill(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(50.dp, 32.dp, 42.dp, 18.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (selected) 1.dp else 0.dp,
        shape = shape
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text(text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
fun HomeScreen(state: WebYouState) {
    var query by remember { mutableStateOf("") }
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = Color.Transparent,
        bottomBar = { BrowserBar(state, home = true) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WavePill("Настроить", Icons.Rounded.Tune) { state.screen = "customize" }
                }
                Spacer(Modifier.height(48.dp))
                BrandMark(86.dp)
                Spacer(Modifier.height(18.dp))
                Text("WebYou", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("Быстрый. Приватный. Твой.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(34.dp))
                SearchBox(query, { query = it }, { openQuery(state, query) })
                Spacer(Modifier.height(20.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut("Google", Icons.Rounded.Search) { state.screen = "browser"; state.activeTab().url = HOME_URL }
                    Shortcut("YouTube", Icons.Rounded.OpenInNew) { openUrl(state, "https://www.youtube.com") }
                    Shortcut("Wikipedia", Icons.Rounded.Language) { openUrl(state, "https://wikipedia.org") }
                    Shortcut("Добавить", Icons.Rounded.Add) { state.screen = "customize" }
                }
                Spacer(Modifier.height(26.dp))
                WavePill("Новая вкладка", Icons.Rounded.Add, Modifier.fillMaxWidth()) { state.addTab() }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureMini("Вкладки", Icons.Rounded.Visibility) { state.screen = "tabs" }
                    FeatureMini("Приватность", Icons.Rounded.PrivacyTip) { state.screen = "privacy" }
                }
                Spacer(Modifier.height(12.dp))
                WavePill("Перевод страницы", Icons.Rounded.Translate, Modifier.fillMaxWidth()) { state.screen = "translate" }
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onValue: (String) -> Unit, onGo: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp, 18.dp, 30.dp, 24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(28.dp))
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 15.dp),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Введите запрос или URL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            )
            IconButton(onClick = onGo) { Icon(Icons.Rounded.OpenInNew, "Открыть") }
        }
    }
}

@Composable
private fun Shortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable(onClick = onClick)) {
        Surface(Modifier.size(50.dp), shape = RoundedCornerShape(18.dp, 24.dp, 16.dp, 25.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(6.dp)); Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FeatureMini(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(Modifier.weight(1f).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp, 14.dp, 28.dp, 18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BrowserScreen(state: WebYouState) {
    var address by remember(state.activeTabId) { mutableStateOf(state.activeTab().url) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(false) }
    val tab = state.activeTab()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.padding(top = 8.dp)) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { state.screen = "home" }) { Icon(Icons.Rounded.Home, null) }
                    Surface(shape = RoundedCornerShape(22.dp, 16.dp, 24.dp, 18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.weight(1f)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Security, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.text.BasicTextField(address, { address = it }, Modifier.weight(1f).padding(horizontal = 8.dp), singleLine = true)
                            IconButton(onClick = { webView?.loadUrl(normalizeUrl(address)) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.OpenInNew, null) }
                        }
                    }
                    IconButton(onClick = { state.screen = "menu" }) { Icon(Icons.Rounded.MoreVert, null) }
                }
                if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { webView?.goBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                IconButton(onClick = { webView?.goForward() }) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(28.dp, 18.dp, 30.dp, 22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { state.screen = "home" }) { Icon(Icons.Rounded.Home, null) }
                        IconButton(onClick = { state.screen = "tabs" }) { Icon(Icons.Rounded.Visibility, null) }
                        IconButton(onClick = { webView?.reload() }) { Icon(Icons.Rounded.Refresh, null) }
                    }
                }
                IconButton(onClick = { state.screen = "tabs" }) { Icon(Icons.Rounded.Add, null) }
            }
        }
    ) { pad ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(pad),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    configureWebView(this, state) { newUrl, title, bitmap ->
                        state.activeTab().url = newUrl
                        state.activeTab().title = title.ifBlank { "WebYou" }
                        state.activeTab().preview = bitmap
                        address = newUrl
                    }
                    loadUrl(tab.url)
                    webView = this
                }
            },
            update = { webView = it }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView, state: WebYouState, onPage: (String, String, Bitmap?) -> Unit) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        builtInZoomControls = false
        displayZoomControls = false
        cacheMode = WebSettings.LOAD_DEFAULT
        mediaPlaybackRequiresUserGesture = false
        allowFileAccess = false
        allowContentAccess = false
        userAgentString = "$userAgentString WebYou/0.1"
    }
    webView.webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null
            if (state.adBlockEnabled && AdBlocker.shouldBlock(url)) {
                state.blockedCount++
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            return super.shouldInterceptRequest(view, request)
        }
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val safeUrl = url ?: return
            val title = view?.title.orEmpty()
            val bitmap = view?.let { capturePreview(it) }
            onPage(safeUrl, title, bitmap)
        }
    }
}

private fun capturePreview(webView: WebView): Bitmap? {
    if (webView.width <= 0 || webView.height <= 0) return null
    val w = 480
    val h = (webView.height.toFloat() / webView.width * w).toInt().coerceAtLeast(240).coerceAtMost(900)
    return runCatching {
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.scale(w.toFloat() / webView.width, h.toFloat() / webView.height)
            webView.draw(canvas)
        }
    }.getOrNull()
}

@Composable
fun TabsScreen(state: WebYouState) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { SimpleTopBar("Вкладки", state) },
        bottomBar = { BrowserBar(state) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Открыто • ${state.tabs.size}", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        WavePill("+", Icons.Rounded.Add) { state.addTab() }
                    }
                }
                items(state.tabs, key = { it.id }) { tab ->
                    TabCard(tab, selected = tab.id == state.activeTabId, onOpen = { state.activeTabId = tab.id; state.screen = if (tab.url == HOME_URL) "home" else "browser" }, onClose = { state.closeTab(tab.id) })
                }
                item { WavePill("Новая вкладка инкогнито", Icons.Rounded.Incognito, Modifier.fillMaxWidth()) { state.addTab(true) } }
            }
        }
    }
}

@Composable
private fun TabCard(tab: BrowserTab, selected: Boolean, onOpen: () -> Unit, onClose: () -> Unit) {
    Card(onClick = onOpen, shape = RoundedCornerShape(28.dp, 18.dp, 30.dp, 22.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = .96f))) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(28.dp, 18.dp, 12.dp, 12.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
                tab.preview?.let { ImagePreview(it) }
                IconButton(onClick = onClose, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Close, null) }
            }
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tab.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tab.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (tab.incognito) Icon(Icons.Rounded.Incognito, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ImagePreview(bitmap: Bitmap) {
    androidx.compose.foundation.Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
}

@Composable
fun MenuScreen(state: WebYouState) {
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("WebYou", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { MenuItem("Новая вкладка", Icons.Rounded.Add) { state.screen = "home" } }
                item { MenuItem("Новая вкладка инкогнито", Icons.Rounded.Incognito) { state.addTab(true) } }
                item { MenuItem("Закладки", Icons.Rounded.BookmarkBorder) {} }
                item { MenuItem("История", Icons.Rounded.Visibility) {} }
                item { MenuItem("Загрузки", Icons.Rounded.Download) {} }
                item { MenuItem("Перевод страницы", Icons.Rounded.Translate) { state.screen = "translate" } }
                item { MenuItem("Блокировка рекламы", Icons.Rounded.Shield, state.adBlockEnabled) { state.screen = "adblock" } }
                item { MenuItem("DNS / Proxy", Icons.Rounded.Wifi, state.proxyEnabled) { state.screen = "proxy" } }
                item { MenuItem("Настройки", Icons.Rounded.Settings) { state.screen = "settings" } }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean = false, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp, 16.dp, 26.dp, 18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(50.dp, 16.dp, 26.dp, 20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(14.dp)); Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (active) Switch(true, {}, modifier = Modifier.size(48.dp, 28.dp)) else Icon(Icons.Rounded.ArrowBackIosNew, null, Modifier.size(16.dp))
        }
    }
}

@Composable
fun CustomizeScreen(state: WebYouState) {
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("Кастомизация", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("Тема", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("Система", state.theme == AppTheme.SYSTEM) { state.theme = AppTheme.SYSTEM }
                        ThemeChip("Светлая", state.theme == AppTheme.LIGHT) { state.theme = AppTheme.LIGHT }
                        ThemeChip("Тёмная", state.theme == AppTheme.DARK) { state.theme = AppTheme.DARK }
                        ThemeChip("AMOLED", state.theme == AppTheme.AMOLED) { state.theme = AppTheme.AMOLED }
                    }
                }
                item { SectionCard("Цветовая схема") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(Color(0xFF8B5CF6), Color(0xFF3C6EDB), Color(0xFF008A78), Color(0xFFC85C8E)).forEachIndexed { i, c ->
                            Surface(Modifier.size(46.dp).clickable { state.accent = i }, shape = RoundedCornerShape(50.dp, 18.dp, 26.dp, 20.dp), color = c, border = if (state.accent == i) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {}
                        }
                    }
                } }
                item { SectionCard("Угловатые волны") {
                    Text("Количество волн: ${state.waveCount}")
                    Slider(value = state.waveCount.toFloat(), onValueChange = { state.waveCount = it.toInt() }, valueRange = 2f..12f, steps = 9)
                    Text("Глубина: ${(state.waveDepth * 100).toInt()}%")
                    Slider(value = state.waveDepth, onValueChange = { state.waveDepth = it }, valueRange = 0.02f..0.18f)
                } }
                item { SectionCard("Стиль интерфейса") {
                    Text("Органические формы, pill-кнопки, мягкие поверхности и лёгкие волны — стиль WebYou.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) { WavePill(label, selected = selected, onClick = onClick, modifier = Modifier.weight(1f)) }

@Composable
fun SettingsScreen(state: WebYouState) {
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("Настройки", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(depth = state.waveDepth, count = state.waveCount, dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SettingsRow("Основные", "Поиск, стартовая страница, язык", Icons.Rounded.Settings) {} }
                item { SettingsRow("Внешний вид", "Тема, цвет, формы и волны", Icons.Rounded.Palette) { state.screen = "customize" } }
                item { SettingsRow("Блокировка рекламы", "${if (state.adBlockEnabled) "Включена" else "Выключена"} • ${state.blockedCount} запросов", Icons.Rounded.Shield) { state.screen = "adblock" } }
                item { SettingsRow("DNS / Proxy", if (state.proxyEnabled) "Прокси включён" else "Прямое соединение", Icons.Rounded.Wifi) { state.screen = "proxy" } }
                item { SettingsRow("Конфиденциальность", "История, cookies, трекеры", Icons.Rounded.PrivacyTip) { state.screen = "privacy" } }
                item { SettingsRow("Перевод", "Google Translate", Icons.Rounded.Translate) { state.screen = "translate" } }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp, 18.dp, 28.dp, 20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .93f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Rounded.ArrowBackIosNew, null, Modifier.size(16.dp))
        }
    }
}

@Composable
fun AdBlockScreen(state: WebYouState) {
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("Блокировка рекламы", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard("Чистый интернет") {
                    Icon(Icons.Rounded.Shield, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (state.adBlockEnabled) "Реклама блокируется" else "Блокировка отключена", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("WebYou перехватывает запросы к известным рекламным и трекинговым доменам.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Блокировать рекламу", Modifier.weight(1f)); Switch(state.adBlockEnabled, { state.adBlockEnabled = it }) }
                    Text("Заблокировано запросов: ${state.blockedCount}")
                }
                SectionCard("Списки") {
                    WavePill("EasyList • встроенный набор")
                    WavePill("Списки трекеров")
                    Text("в MVP используется компактный встроенный набор доменов. отдельные подписки можно добавить позже.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TranslateScreen(state: WebYouState) {
    var language by remember { mutableStateOf("Русский") }
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("Перевод страницы", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard("Перевод") {
                    Text("Страница", fontWeight = FontWeight.SemiBold)
                    Text(state.activeTab().url, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Язык") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val url = TRANSLATE_BASE + java.net.URLEncoder.encode(state.activeTab().url, "UTF-8")
                        openUrl(state, url)
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Translate, null); Spacer(Modifier.width(8.dp)); Text("Перевести") }
                }
            }
        }
    }
}

@Composable
fun ProxyScreen(state: WebYouState) {
    var host by remember { mutableStateOf(state.proxyHost) }
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("DNS / Proxy", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionCard("Обход блокировок") {
                    Text("Прокси WebView", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("укажи HTTP(S)-прокси в формате host:port. WebView поддерживает process-level proxy override на современных Android WebView.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("proxy.example:8080") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Использовать прокси", Modifier.weight(1f)); Switch(state.proxyEnabled, { state.proxyEnabled = it; if (it) applyProxy(host) else clearProxy() }) }
                    Button(onClick = { state.proxyHost = host; if (state.proxyEnabled) applyProxy(host) }, modifier = Modifier.fillMaxWidth()) { Text("Применить") }
                }
                SectionCard("DNS") {
                    WavePill("Cloudflare 1.1.1.1", Icons.Rounded.Wifi)
                    WavePill("Google DNS 8.8.8.8", Icons.Rounded.Wifi)
                    Text("WebView использует системный DNS. отдельный DoH-режим требует сетевого слоя/VPN и вынесен в следующий этап.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun applyProxy(value: String) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
    val parts = value.trim().removePrefix("http://").removePrefix("https://").split(":")
    if (parts.size != 2) return
    val host = parts[0]
    val port = parts[1].toIntOrNull() ?: return
    val config = ProxyConfig.Builder().addProxyRule("http://$host:$port").addProxyRule("https://$host:$port").build()
    ProxyController.getInstance().setProxyOverride(config, Executors.newSingleThreadExecutor(), Runnable {})
}

private fun clearProxy() {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
    ProxyController.getInstance().clearProxyOverride(Executors.newSingleThreadExecutor(), Runnable {})
}

@Composable
fun PrivacyScreen(state: WebYouState) {
    Scaffold(containerColor = Color.Transparent, topBar = { SimpleTopBar("Конфиденциальность", state) }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            ExpressiveBackground(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsRow("Блокировать трекеры", "работает вместе с блокировщиком рекламы", Icons.Rounded.PrivacyTip) {}
                SettingsRow("Безопасный просмотр", "Safe Browsing WebView", Icons.Rounded.Security) {}
                SettingsRow("Очистить данные", "cookies, cache, история", Icons.Rounded.DeleteSweep) {}
                SettingsRow("Инкогнито", "отдельная вкладка без сохранения истории", Icons.Rounded.Incognito) { state.addTab(true) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp, 18.dp, 34.dp, 22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SimpleTopBar(title: String, state: WebYouState) {
    TopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = { state.screen = if (state.activeTab().url == HOME_URL) "home" else "browser" }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) } }, actions = { IconButton(onClick = { state.screen = "menu" }) { Icon(Icons.Rounded.MoreVert, null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

@Composable
private fun BrowserBar(state: WebYouState, home: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), tonalElevation = 4.dp, shape = RoundedCornerShape(34.dp, 22.dp, 38.dp, 26.dp), modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { state.screen = "home" }) { Icon(Icons.Rounded.Home, null) }
            IconButton(onClick = { state.screen = "tabs" }) { Icon(Icons.Rounded.Visibility, null) }
            IconButton(onClick = { state.screen = "menu" }) { Icon(Icons.Rounded.MoreVert, null) }
            IconButton(onClick = { state.addTab() }) { Icon(Icons.Rounded.Add, null) }
        }
    }
}

private fun openQuery(state: WebYouState, query: String) {
    if (query.isBlank()) return
    openUrl(state, normalizeUrl(query))
}

private fun openUrl(state: WebYouState, url: String) {
    state.activeTab().url = normalizeUrl(url)
    state.screen = "browser"
}

private fun normalizeUrl(input: String): String {
    val value = input.trim()
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return if (value.contains(".") && !value.contains(" ")) "https://$value" else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
}

private object AdBlocker {
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "ads.yahoo.com", "adnxs.com", "taboola.com", "outbrain.com", "scorecardresearch.com",
        "zedo.com", "rubiconproject.com", "pubmatic.com", "criteo.com", "amazon-adsystem.com",
        "adsrvr.org", "openx.net", "smartadserver.com", "moatads.com", "casalemedia.com"
    )
    fun shouldBlock(url: String): Boolean {
        val host = runCatching { URI(url).host ?: "" }.getOrDefault("").lowercase()
        return blockedHosts.any { host == it || host.endsWith(".$it") }
    }
}
