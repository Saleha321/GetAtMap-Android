package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.LocationStateManager
import com.example.data.NetworkMonitor
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var locationStateManager: LocationStateManager
    private lateinit var networkMonitor: NetworkMonitor

    private var initialDeepLinkUrl by mutableStateOf<String?>(null)

    // File upload callback for WebChromeClient
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Photo & file picker launcher for WebView
    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uriList = mutableListOf<Uri>()

                data?.data?.let { uriList.add(it) }

                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                }

                if (uriList.isNotEmpty()) {
                    filePathCallback?.onReceiveValue(uriList.toTypedArray())
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

    // Runtime location permission launcher
    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            locationStateManager.refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationStateManager = LocationStateManager(this)
        networkMonitor = NetworkMonitor(this)

        locationStateManager.startListening()
        networkMonitor.startMonitoring()

        // Extract deep link from launching intent
        handleDeepLinkIntent(intent)

        // Request location permission on launch if needed
        if (!locationStateManager.checkLocationPermission()) {
            requestLocationPermissions()
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        locationStateManager = locationStateManager,
                        networkMonitor = networkMonitor,
                        initialDeepLinkUrl = initialDeepLinkUrl,
                        onRequestLocationPermission = { requestLocationPermissions() },
                        onFileChooserRequested = { callback ->
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback

                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            try {
                                fileChooserLauncher.launch(
                                    Intent.createChooser(intent, "Select Media to Upload")
                                )
                                true
                            } catch (_: Exception) {
                                filePathCallback = null
                                false
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme?.lowercase() ?: return

        when (scheme) {
            "http", "https" -> {
                // If it's a getatmap link e.g. https://getatmap.com/listings/123
                initialDeepLinkUrl = uri.toString()
            }
            "getatmap" -> {
                // Custom scheme: getatmap://item/123 -> map to https://getatmap.com/item/123
                val path = uri.path ?: ""
                val host = uri.host ?: ""
                initialDeepLinkUrl = "https://getatmap.com/$host$path"
            }
        }
    }

    fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh GPS hardware and permission state when resuming from Settings
        locationStateManager.refreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationStateManager.stopListening()
        networkMonitor.stopMonitoring()
    }
}
