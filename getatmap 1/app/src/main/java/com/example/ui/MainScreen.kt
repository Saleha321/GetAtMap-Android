package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LocationStateManager
import com.example.data.NetworkMonitor
import com.example.ui.screens.ComingSoonScreen
import com.example.ui.screens.GpsRequiredScreen
import com.example.ui.screens.PrivacyPolicyDialog
import com.example.ui.screens.WebViewContainer
import com.example.ui.theme.MapBlueAccent
import com.example.ui.theme.MapCyanLight
import com.example.ui.theme.MapEmerald
import com.example.ui.theme.MapErrorRed
import com.example.ui.theme.MapNavyPrimary
import kotlinx.coroutines.launch

const val DEFAULT_GETATMAP_URL = "https://www.getatmap.com"
const val DEMO_MARKETPLACE_URL = "file:///android_asset/demo_marketplace.html"

enum class ScreenState {
    GPS_ENFORCEMENT,
    COMING_SOON,
    WEBVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
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

    // Web navigation states
    var activeUrl by remember { mutableStateOf(initialDeepLinkUrl ?: DEFAULT_GETATMAP_URL) }
    var currentDisplayUrl by remember { mutableStateOf(activeUrl) }
    var currentTitle by remember { mutableStateOf("GetAtMap") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // App state: Default to WEBVIEW so that www.getatmap.com directly opens on launch
    var screenState by remember { mutableStateOf(ScreenState.WEBVIEW) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Dialog states (Change URL option removed for release)
    var showExitDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Synchronize deep links if arrived while running
    LaunchedEffect(initialDeepLinkUrl) {
        if (!initialDeepLinkUrl.isNullOrEmpty()) {
            activeUrl = initialDeepLinkUrl
            webViewInstance?.loadUrl(initialDeepLinkUrl)
            screenState = ScreenState.WEBVIEW
        }
    }

    // Refresh navigation back/forward capabilities periodically from WebView
    LaunchedEffect(pageProgress) {
        webViewInstance?.let {
            canGoBack = it.canGoBack()
            canGoForward = it.canGoForward()
        }
    }

    // Mandatory GPS enforcement check
    val isLocationValid = isGpsEnabled && hasLocationPermission
    val currentEffectiveScreen = if (!isLocationValid) {
        ScreenState.GPS_ENFORCEMENT
    } else {
        screenState
    }

    // Handle Android system back button
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top App Bar
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MapNavyPrimary,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Official App Logo in Top Bar
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_foreground),
                                    contentDescription = "GetAtMap Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFFD4AF37), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "GetAtMap",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "SSL Secure",
                                            tint = MapCyanLight,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (activeUrl.startsWith("file://")) "Demo Mode" else "www.getatmap.com",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // GPS Active Status Chip
                            Box(
                                modifier = Modifier
                                    .background(MapEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MapEmerald, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MapEmerald, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GPS Active",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Refresh
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.testTag("appbar_refresh_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Page")
                            }

                            // Share
                            IconButton(
                                onClick = { triggerShare(currentDisplayUrl) },
                                modifier = Modifier.testTag("appbar_share_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Link")
                            }

                            // Overflow Menu (No Change URL option)
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.testTag("appbar_more_menu_button")
                                ) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Coming Soon Mode") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = MapNavyPrimary)
                                        },
                                        onClick = {
                                            showMenu = false
                                            screenState = ScreenState.COMING_SOON
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open Demo Marketplace") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MapEmerald)
                                        },
                                        onClick = {
                                            showMenu = false
                                            activeUrl = DEMO_MARKETPLACE_URL
                                            webViewInstance?.loadUrl(DEMO_MARKETPLACE_URL)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Privacy Policy") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Policy, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showPrivacyPolicy = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Exit App", color = MapErrorRed) },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MapErrorRed)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showExitDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // Page Loading Linear Progress
                    AnimatedVisibility(
                        visible = pageProgress in 1..99,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MapCyanLight,
                            trackColor = MapNavyPrimary
                        )
                    }

                    // Main WebView Component
                    Box(modifier = Modifier.weight(1f)) {
                        WebViewContainer(
                            urlToLoad = activeUrl,
                            locationStateManager = locationStateManager,
                            onProgressChanged = { progress ->
                                pageProgress = progress
                                webViewInstance?.let {
                                    canGoBack = it.canGoBack()
                                    canGoForward = it.canGoForward()
                                }
                            },
                            onTitleReceived = { title ->
                                currentTitle = title
                            },
                            onUrlChanged = { newUrl ->
                                currentDisplayUrl = newUrl
                                webViewInstance?.let {
                                    canGoBack = it.canGoBack()
                                    canGoForward = it.canGoForward()
                                }
                            },
                            onErrorOccurred = { unavailable, _ ->
                                if (unavailable && activeUrl != DEMO_MARKETPLACE_URL) {
                                    screenState = ScreenState.COMING_SOON
                                }
                            },
                            onFileChooserRequested = onFileChooserRequested,
                            onWebViewCreated = { webView ->
                                webViewInstance = webView
                                canGoBack = webView.canGoBack()
                                canGoForward = webView.canGoForward()
                            }
                        )
                    }

                    // Bottom Navigation Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button
                            FilledIconButton(
                                onClick = { webViewInstance?.goBack() },
                                enabled = canGoBack,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate Back",
                                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Forward Button
                            FilledIconButton(
                                onClick = { webViewInstance?.goForward() },
                                enabled = canGoForward,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Navigate Forward",
                                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Refresh Button
                            FilledIconButton(
                                onClick = { webViewInstance?.reload() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload Page",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Share Button
                            FilledIconButton(
                                onClick = { triggerShare(currentDisplayUrl) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MapBlueAccent.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MapBlueAccent
                                )
                            }

                            // Exit Button
                            FilledIconButton(
                                onClick = { showExitDialog = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MapErrorRed.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit App",
                                    tint = MapErrorRed
                                )
                            }
                        }
                    }
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

        // Privacy Policy Dialog
        if (showPrivacyPolicy) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
        }
    }
}
