package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.LocationStateManager
import com.example.webview.GetAtMapChromeClient
import com.example.webview.GetAtMapWebClient
import com.example.webview.LocationJavaScriptInterface

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    urlToLoad: String,
    locationStateManager: LocationStateManager,
    onProgressChanged: (Int) -> Unit,
    onTitleReceived: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onErrorOccurred: (Boolean, String) -> Unit,
    onFileChooserRequested: (ValueCallback<Array<Uri>>?) -> Boolean,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Essential WebSettings for modern OLX-style interactive web application
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setGeolocationEnabled(true)
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                // Append custom identifier to User Agent for server-side device detection
                val originalUa = userAgentString
                userAgentString = "$originalUa GetAtMapApp/1.0 (Android)"
            }

            // Expose Native GPS & Location Bridge to window.AndroidLocation (restricted to getatmap.com)
            addJavascriptInterface(
                LocationJavaScriptInterface(context, locationStateManager) { url },
                "AndroidLocation"
            )

            // Configure WebChromeClient
            webChromeClient = GetAtMapChromeClient(
                locationStateManager = locationStateManager,
                onProgressUpdate = onProgressChanged,
                onTitleReceived = onTitleReceived,
                onFileChooserRequested = onFileChooserRequested
            )

            // Configure WebViewClient
            webViewClient = GetAtMapWebClient(
                context = context,
                onPageStartedCallback = { currentUrl ->
                    onUrlChanged(currentUrl)
                },
                onPageFinishedCallback = { currentUrl ->
                    onUrlChanged(currentUrl)
                },
                onErrorReceived = onErrorOccurred
            )
        }
    }

    DisposableEffect(Unit) {
        onWebViewCreated(webView)
        onDispose {
            webView.stopLoading()
        }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            // Only load if URL is significantly different
            if (view.url != urlToLoad && urlToLoad.isNotEmpty()) {
                view.loadUrl(urlToLoad)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
