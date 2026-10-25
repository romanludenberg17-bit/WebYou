package com.webyou.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

private const val HOME = "https://www.google.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { WebYouApp() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebYouApp() {
    var address by remember { mutableStateOf(HOME) }
    var page by remember { mutableStateOf(HOME) }
    var dark by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = {
                                val target = if (address.startsWith("http://") || address.startsWith("https://")) address else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(address, "UTF-8")
                                address = target
                                page = target
                                webView?.loadUrl(target)
                            }) { Icon(Icons.Rounded.Search, null) }
                        }
                    )
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(page)
                            webView = this
                        }
                    },
                    modifier = Modifier.weight(1f),
                    update = { view -> webView = view }
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { webView?.goBack() }) { Icon(Icons.Rounded.ArrowBack, null) }
                    IconButton(onClick = { webView?.loadUrl(HOME); address = HOME }) { Icon(Icons.Rounded.Home, null) }
                    IconButton(onClick = { webView?.reload() }) { Icon(Icons.Rounded.Refresh, null) }
                    IconButton(onClick = { webView?.goForward() }) { Icon(Icons.Rounded.ArrowForward, null) }
                    TextButton(onClick = { dark = !dark }) { Text(if (dark) "light" else "dark") }
                }
            }
        }
    }
}
