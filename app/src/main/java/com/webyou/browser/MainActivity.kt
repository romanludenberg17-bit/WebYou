package com.webyou.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import java.net.URLEncoder
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
    var accent by mutableIntStateOf(0)
    var waveDepth by mutableStateOf(0.08f)
    var waveCount by mutableIntStateOf(8)
    var menuOpen by mutableStateOf(false)
    var exitPromptUntil by mutableStateOf(0L)

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WebYouRoot() {
    val state = remember { WebYouState() }
    val context = LocalContext.current
    val isDark = when (state.theme) {
        AppTheme.DARK, AppTheme.AMOLED -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val lightPrimary = listOf(Color(0xFF7656D6), Color(0xFF3F63D8), Color(0xFFAF4D7B), Color(0xFF4B8C7A))
    val darkPrimary = listOf(Color(0xFFB8A6FF), Color(0xFFA9B9FF), Color(0xFFFFA8D2), Color(0xFF9FE0CA))
    val primary = if (isDark) darkPrimary[state.accent] else lightPrimary[state.accent]
    val baseScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        isDark -> darkColorScheme(primary = primary)
        else -> lightColorScheme(primary = primary)
    }
    val colorScheme = baseScheme.copy(
        primary = primary,
        background = if (state.theme == AppTheme.AMOLED) Color.Black else baseScheme.background,
        surface = if (state.theme == AppTheme.AMOLED) Color.Black else baseScheme.surface
    )

    MaterialTheme(colorScheme = colorScheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    if (state.screen != "home" && state.screen != "browser") {
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
                    when (state.screen) {
                        "home" -> HomeScreen(state)
                        "browser" -> BrowserView(state)
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

        if (state.menuOpen) {
            BrowserBottomSheet(state, onDismiss = { state.menuOpen = false })
        }
    }

    BackHandler(enabled = state.menuOpen) {
        state.menuOpen = false
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
private fun ExpressiveBackground(modifier: Modifier = Modifier, depth: Float = 0.08f, count: Int = 8, dark: Boolean) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(if (dark) Color(0xFF0F0F12) else Color(0xFFFAF8FF))
        val blob = if (dark) Color(0xFF3A2D52) else Color(0xFFE7DFFF)
        val width = size.width
        val height = size.height
        val upper = Path().apply {
            moveTo(-width * 0.12f, height * 0.20f)
            cubicTo(width * 0.16f, height * 0.05f, width * 0.42f, height * 0.32f, width * 0.72f, height * 0.16f)
            cubicTo(width * 0.88f, height * 0.08f, width * 1.06f, height * 0.12f, width * 1.10f, height * 0.24f)
            lineTo(width * 1.10f, height * 0.46f)
            cubicTo(width * 0.78f, height * 0.36f, width * 0.42f, height * 0.48f, -width * 0.12f, height * 0.34f)
            close()
        }
        drawPath(upper, blob.copy(alpha = if (dark) 0.18f else 0.34f))
        val lower = Path().apply {
            moveTo(-width * 0.10f, height * 0.78f)
            cubicTo(width * 0.18f, height * 0.64f, width * 0.52f, height * 0.88f, width * 0.84f, height * 0.72f)
            cubicTo(width * 0.98f, height * 0.65f, width * 1.08f, height * 0.68f, width * 1.10f, height * 0.76f)
            lineTo(width * 1.10f, height * 0.98f)
            lineTo(-width * 0.10f, height * 0.98f)
            close()
        }
        drawPath(lower, blob.copy(alpha = if (dark) 0.10f else 0.16f))
    }
}

@Composable
private fun HomeScreen(s: WebYouState) {
    var query by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        WebYouMark()
        Spacer(Modifier.height(12.dp))
        Text("WebYou", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("твой браузер, по-твоему", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(32.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("поиск или адрес") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { openQuery(s, query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.weight(1f))
        HomeBottomBar(s)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BrowserBottomSheet(s: WebYouState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("WebYou", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile("Назад", Icons.AutoMirrored.Rounded.ArrowBack, Modifier.weight(1f)) { s.menuOpen = false; s.screen = "browser" }
                MenuTile("Обновить", Icons.Rounded.Refresh, Modifier.weight(1f)) { s.menuOpen = false }
                MenuTile("Домой", Icons.Rounded.Home, Modifier.weight(1f)) { s.menuOpen = false; s.screen = "home" }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile("Новая", Icons.Rounded.Add, Modifier.weight(1f)) { s.addTab(); s.menuOpen = false }
                MenuTile("Инкогнито", Icons.Rounded.VisibilityOff, Modifier.weight(1f)) { s.addTab(true); s.menuOpen = false }
                MenuTile("Вкладки", Icons.Rounded.Language, Modifier.weight(1f)) { s.screen = "tabs"; s.menuOpen = false }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile("Настройки", Icons.Rounded.Settings, Modifier.weight(1f)) { s.screen = "settings"; s.menuOpen = false }
                MenuTile("Приватность", Icons.Rounded.PrivacyTip, Modifier.weight(1f)) { s.screen = "privacy"; s.menuOpen = false }
                MenuTile("Перевод", Icons.Rounded.Translate, Modifier.weight(1f)) { s.screen = "translate"; s.menuOpen = false }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTile("Прокси", Icons.Rounded.Wifi, Modifier.weight(1f)) { s.screen = "proxy"; s.menuOpen = false }
                Spacer(Modifier.weight(2f))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MenuTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(76.dp).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserView(s: WebYouState) {
    val tab = s.activeTab()
    var address by remember(tab.id) { mutableStateOf(tab.url) }
    var webView by remember(tab.id) { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    BackHandler {
        val current = webView
        when {
            current?.canGoBack() == true -> current.goBack()
            s.screen == "browser" -> s.screen = "home"
            else -> {
                val now = System.currentTimeMillis()
                if (now <= s.exitPromptUntil) {
                    (context as? ComponentActivity)?.finish()
                } else {
                    s.exitPromptUntil = now + 2_000L
                    Toast.makeText(context, "Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(tab.url) {
        if (address != tab.url) address = tab.url
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 1.dp) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f).height(58.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(if (address.startsWith("https://")) Icons.Rounded.Security else Icons.Rounded.Search, null) },
                    trailingIcon = { IconButton(onClick = { val target = normalizeAddress(address); tab.url = target; webView?.loadUrl(target) }) { Icon(Icons.Rounded.Search, "Перейти") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { val target = normalizeAddress(address); tab.url = target; webView?.loadUrl(target) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                WebView(context).apply {
                    webView = this
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
                            if (url != null) { tab.url = url; address = url }
                            tab.title = view?.title ?: tab.title
                        }
                    }
                    loadUrl(tab.url)
                }
            },
            update = { current ->
                webView = current
                if (current.url != tab.url && tab.url.isNotBlank()) current.loadUrl(tab.url)
            }
        )

        BrowserBottomBar(
            onHome = { s.screen = "home" },
            onBack = { webView?.goBack() },
            onForward = { webView?.goForward() },
            onRefresh = { webView?.reload() },
            onTabs = { s.screen = "tabs" },
            onMore = { s.menuOpen = true }
        )
    }
}

@Composable
private fun BrowserBottomBar(onHome: () -> Unit, onBack: () -> Unit, onForward: () -> Unit, onRefresh: () -> Unit, onTabs: () -> Unit, onMore: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f), tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomIcon("Домой", Icons.Rounded.Home, onHome)
            BottomIcon("Назад", Icons.AutoMirrored.Rounded.ArrowBack, onBack)
            BottomIcon("Вперёд", Icons.AutoMirrored.Rounded.ArrowForward, onForward)
            BottomIcon("Обновить", Icons.Rounded.Refresh, onRefresh)
            BottomIcon("Вкладки", Icons.Rounded.Language, onTabs)
            BottomIcon("Ещё", Icons.Rounded.MoreVert, onMore)
        }
    }
}

@Composable
private fun BottomIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(56.dp).clickable(onClick = onClick).padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.padding(5.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun normalizeAddress(query: String): String {
    val value = query.trim()
    if (value.isBlank()) return HOME_URL
    return if (value.startsWith("http://") || value.startsWith("https://")) value
    else if (value.contains(".") && !value.contains(" ")) "https://$value"
    else "https://www.google.com/search?q=${URLEncoder.encode(value, "UTF-8")}"
}

private fun openQuery(s: WebYouState, query: String) {
    val target = normalizeAddress(query)
    s.activeTab().url = target
    s.activeTab().title = query.ifBlank { "Google" }
    s.screen = "browser"
}

@Composable
private fun TabsScreen(s: WebYouState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { s.addTab() }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Новая") }
            Button(onClick = { s.addTab(true) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.VisibilityOff, null); Spacer(Modifier.width(6.dp)); Text("Инкогнито") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(s.tabs, key = { it.id }) { tab ->
                Card(Modifier.fillMaxWidth().clickable { s.activeTabId = tab.id; s.screen = "browser" }, shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (tab.incognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Language, null)
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
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection("Внешний вид") {
            SettingRow("Тема", Icons.Rounded.Palette) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppTheme.entries.forEach { theme ->
                        AssistChip(onClick = { s.theme = theme }, label = { Text(theme.name.lowercase()) })
                    }
                }
            }
            SettingRow("Акцент", Icons.Rounded.Tune) {
                Slider(value = s.accent.toFloat(), onValueChange = { s.accent = it.toInt().coerceIn(0, 3) }, valueRange = 0f..3f, steps = 2)
            }
        }
        SettingsSection("Приватность") {
            SettingToggle("Блокировка рекламы", Icons.Rounded.Security, s.adBlockEnabled) { s.adBlockEnabled = it }
        }
        SettingsSection("Прокси") {
            OutlinedTextField(value = s.proxyHost, onValueChange = { s.proxyHost = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("host:port") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Включить прокси")
                Switch(checked = s.proxyEnabled, onCheckedChange = { enabled -> s.proxyEnabled = enabled; applyProxy(s, enabled) })
            }
        }
        Text("WebYou • Material 3 • Android 17", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SettingToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(label)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PrivacyScreen(s: WebYouState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Блокировка рекламы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Заблокировано: ${s.blockedCount}")
                Divider()
                Text("WebYou не отправляет историю просмотров на сервер.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TranslateScreen(s: WebYouState) {
    var url by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("URL страницы") })
        Button(onClick = {
            val target = TRANSLATE_BASE + URLEncoder.encode(url.trim(), "UTF-8")
            s.activeTab().url = target
            s.screen = "browser"
        }, modifier = Modifier.fillMaxWidth()) { Text("Перевести") }
    }
}

@Composable
private fun ProxyScreen(s: WebYouState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = s.proxyHost, onValueChange = { s.proxyHost = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Прокси host:port") })
        Button(onClick = { s.proxyEnabled = !s.proxyEnabled; applyProxy(s, s.proxyEnabled) }, modifier = Modifier.fillMaxWidth()) { Text(if (s.proxyEnabled) "Отключить" else "Включить") }
    }
}

private fun applyProxy(s: WebYouState, enabled: Boolean) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
    val controller = ProxyController.getInstance()
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        val config = if (enabled && s.proxyHost.isNotBlank()) {
            ProxyConfig.Builder().addProxyRule("http://${s.proxyHost.trim()}").build()
        } else {
            ProxyConfig.Builder().addDirect().build()
        }
        controller.setProxyOverride(config, executor) {}
    }
}

@Composable
private fun HomeBottomBar(s: WebYouState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f), tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomIcon("Вкладки", Icons.Rounded.Language, { s.screen = "tabs" })
            BottomIcon("Настройки", Icons.Rounded.Settings, { s.screen = "settings" })
            BottomIcon("Ещё", Icons.Rounded.MoreVert, { s.menuOpen = true })
        }
    }
}

@Composable
private fun WebYouMark() {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.width(58.dp).height(58.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("W", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun isAdHost(host: String): Boolean {
    val h = host.lowercase()
    return h.contains("doubleclick") || h.contains("googlesyndication") || h.contains("adservice") || h.contains("adsystem") || h.contains("adnxs")
}
