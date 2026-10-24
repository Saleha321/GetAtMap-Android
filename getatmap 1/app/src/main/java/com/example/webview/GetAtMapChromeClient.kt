package com.example.webview

import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.example.data.LocationStateManager

/**
 * Custom WebChromeClient that handles progress updates, page titles,
 * HTML5 Geolocation permissions, and file uploads.
 */
class GetAtMapChromeClient(
    private val locationStateManager: LocationStateManager,
    private val onProgressUpdate: (progress: Int) -> Unit,
    private val onTitleReceived: (title: String) -> Unit,
    private val onFileChooserRequested: (ValueCallback<Array<Uri>>?) -> Boolean
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        val hasPermission = locationStateManager.hasPermission.value
        val gpsEnabled = locationStateManager.isGpsEnabled.value
        // If the native app already verified GPS and user granted permission,
        // automatically grant HTML5 Geolocation to the origin!
        if (hasPermission && gpsEnabled) {
            callback?.invoke(origin, true, false)
        } else {
            callback?.invoke(origin, false, false)
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooserRequested(filePathCallback)
    }
}
