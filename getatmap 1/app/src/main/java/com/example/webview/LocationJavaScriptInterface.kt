package com.example.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.data.LocationStateManager

/**
 * JavaScript interface mapped to window.AndroidLocation inside the WebView.
 * Restricted strictly to getatmap.com and www.getatmap.com to prevent
 * unauthorized external domains from accessing location data.
 */
class LocationJavaScriptInterface(
    private val context: Context,
    private val locationStateManager: LocationStateManager,
    private val getCurrentUrl: () -> String?
) {

    private fun isAuthorizedCaller(): Boolean {
        val currentUrl = getCurrentUrl() ?: return false
        val uri = Uri.parse(currentUrl)
        val host = uri.host?.lowercase() ?: ""
        return host == "getatmap.com" || host == "www.getatmap.com" || host.endsWith(".getatmap.com") || currentUrl.startsWith("file://")
    }

    @JavascriptInterface
    fun getLatitude(): Double {
        if (!isAuthorizedCaller()) return 0.0
        return locationStateManager.currentLocation.value.latitude
    }

    @JavascriptInterface
    fun getLongitude(): Double {
        if (!isAuthorizedCaller()) return 0.0
        return locationStateManager.currentLocation.value.longitude
    }

    @JavascriptInterface
    fun getAccuracy(): Float {
        if (!isAuthorizedCaller()) return 0f
        return locationStateManager.currentLocation.value.accuracy
    }

    @JavascriptInterface
    fun isGpsEnabled(): Boolean {
        if (!isAuthorizedCaller()) return false
        return locationStateManager.isGpsEnabled.value
    }

    @JavascriptInterface
    fun hasPermission(): Boolean {
        if (!isAuthorizedCaller()) return false
        return locationStateManager.hasPermission.value
    }

    @JavascriptInterface
    fun getCoordinatesJson(): String {
        if (!isAuthorizedCaller()) return "{}"
        val loc = locationStateManager.currentLocation.value
        return """{"latitude":${loc.latitude},"longitude":${loc.longitude},"accuracy":${loc.accuracy},"gpsEnabled":${locationStateManager.isGpsEnabled.value}}"""
    }

    @JavascriptInterface
    fun openNavigation(lat: Double, lng: Double, label: String = "Location") {
        if (!isAuthorizedCaller()) return
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        if (!isAuthorizedCaller()) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
