package com.example.dibros

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.dibros.ui.theme.DibrosandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DibrosandroidTheme(darkTheme = true) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ComposeColor.Black
                ) { innerPadding ->
                    val meuSiteUrl = "https://dibros.com.br"

                    WebViewScreen(
                        url = meuSiteUrl,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var backEnabled by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }

    Column(modifier = modifier.fillMaxSize().background(ComposeColor.Black)) {
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = ComposeColor(0xFF2196F3)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.BLACK)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        // Habilita cookies e armazenamento local
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            // Garante que o CSS Flexbox/Grid meça a altura nativa do celular
                            useWideViewPort = false
                            loadWithOverviewMode = false
                            textZoom = 100

                            allowFileAccess = true
                            allowContentAccess = true
                            loadsImagesAutomatically = true
                            blockNetworkImage = false
                            defaultTextEncodingName = "utf-8"
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, false)
                            }

                            // User-Agent idêntico ao Chrome do celular
                            val rawUserAgent = userAgentString
                            userAgentString = rawUserAgent.replace("; wv", "").replace("Version/4.0 ", "")
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.e("WebViewJS", "${consoleMessage?.message()} -- Linha ${consoleMessage?.lineNumber()} em ${consoleMessage?.sourceId()}")
                                return true
                            }

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                backEnabled = view?.canGoBack() == true
                                cookieManager.flush()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    errorMessage = "Erro ao carregar: ${error?.description}"
                                }
                            }

                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.proceed()
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                update = {
                    webView = it
                }
            )

            errorMessage?.let { errorText ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorText,
                        color = ComposeColor.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
