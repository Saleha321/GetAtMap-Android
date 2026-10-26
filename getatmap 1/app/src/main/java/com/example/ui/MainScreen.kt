package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.data.LocationStateManager
import com.example.data.NetworkMonitor
import com.example.ui.screens.ComingSoonScreen
import com.example.ui.screens.GpsRequiredScreen
import com.example.ui.screens.WebViewContainer
import com.example.ui.theme.MapErrorRed
import kotlinx.coroutines.launch

const val DEFAULT_GETATMAP_URL = "https://www.getatmap.com"
const val DEMO_MARKETPLACE_URL = "file:///android_asset/demo_marketplace.html"

enum class ScreenState {
    GPS_ENFORCEMENT,
    COMING_SOON,
    WEBVIEW
}

@Composable
fun MainScreen(
    locationStateManager: LocationStateManager,
    networkMonitor: NetworkMonitor,
    initialDeepLinkUrl: String?,
    onRequestLocationPermission: () -> Unit,
    onFileChooserRequested: (ValueCallback<Array<Uri>>?) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Location & Network States
    val isGpsEnabled by locationStateManager.isGpsEnabled.collectAsState()
    val hasLocationPermission by locationStateManager.hasPermission.collectAsState()
    val isOnline by networkMonitor.isOnline.collectAsState()

    // Web state
    var activeUrl by remember { mutableStateOf(initialDeepLinkUrl ?: DEFAULT_GETATMAP_URL) }
    var currentDisplayUrl by remember { mutableStateOf(activeUrl) }
    var screenState by remember { mutableStateOf(ScreenState.WEBVIEW) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Handle deep links that arrive while the app is running
    LaunchedEffect(initialDeepLinkUrl) {
        if (!initialDeepLinkUrl.isNullOrEmpty()) {
            activeUrl = initialDeepLinkUrl
            webViewInstance?.loadUrl(initialDeepLinkUrl)
            screenState = ScreenState.WEBVIEW
        }
    }

    // Mandatory GPS enforcement check
    val isLocationValid = isGpsEnabled && hasLocationPermission
    val currentEffectiveScreen = if (!isLocationValid) {
        ScreenState.GPS_ENFORCEMENT
    } else {
        screenState
    }

    // System back button: go back inside the website, otherwise ask to exit
    BackHandler {
        if (currentEffectiveScreen == ScreenState.WEBVIEW && webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            showExitDialog = true
        }
    }

    fun triggerShare(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out GetAtMap")
            putExtra(
                Intent.EXTRA_TEXT,
                "Discover local listings, property, and maps near you on GetAtMap: $url"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share GetAtMap Link via"))
    }

    fun checkAndLoadLiveWebsite() {
        coroutineScope.launch {
            val target = if (activeUrl.startsWith("file://")) DEFAULT_GETATMAP_URL else activeUrl
            val reachable = networkMonitor.checkUrlReachable(target)
            if (reachable) {
                activeUrl = target
                screenState = ScreenState.WEBVIEW
                webViewInstance?.loadUrl(target)
            } else {
                screenState = ScreenState.COMING_SOON
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentEffectiveScreen) {
            ScreenState.GPS_ENFORCEMENT -> {
                GpsRequiredScreen(
                    isGpsDisabled = !isGpsEnabled,
                    isPermissionMissing = !hasLocationPermission,
                    onOpenGpsSettings = {
                        context.startActivity(locationStateManager.createLocationSettingsIntent())
                    },
                    onRequestPermission = onRequestLocationPermission,
                    onRefreshCheck = {
                        locationStateManager.refreshStatus()
                    },
                    onExitApp = {
                        (context as? Activity)?.finishAffinity()
                    }
                )
            }

            ScreenState.COMING_SOON -> {
                ComingSoonScreen(
                    currentUrl = activeUrl,
                    isOffline = !isOnline,
                    onRefresh = {
                        checkAndLoadLiveWebsite()
                    },
                    onPreviewDemo = {
                        activeUrl = DEMO_MARKETPLACE_URL
                        screenState = ScreenState.WEBVIEW
                        webViewInstance?.loadUrl(DEMO_MARKETPLACE_URL)
                    },
                    onShare = {
                        triggerShare(DEFAULT_GETATMAP_URL)
                    },
                    onExit = {
                        showExitDialog = true
                    }
                )
            }

            ScreenState.WEBVIEW -> {
                // WebView only: no top bar, no bottom bar, no menu.
                // Padding keeps the page clear of the status bar and navigation bar.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    WebViewContainer(
                        urlToLoad = activeUrl,
                        locationStateManager = locationStateManager,
                        onProgressChanged = { },
                        onTitleReceived = { },
                        onUrlChanged = { newUrl ->
                            currentDisplayUrl = newUrl
                        },
                        onErrorOccurred = { unavailable, _ ->
                            if (unavailable && activeUrl != DEMO_MARKETPLACE_URL) {
                                screenState = ScreenState.COMING_SOON
                            }
                        },
                        onFileChooserRequested = onFileChooserRequested,
                        onWebViewCreated = { webView ->
                            webViewInstance = webView
                        }
                    )
                }
            }
        }

        // Exit Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Exit GetAtMap?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to close GetAtMap?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            (context as? Activity)?.finishAffinity()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MapErrorRed)
                    ) {
                        Text("Exit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
