package com.example.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient that restricts navigation exclusively to getatmap.com
 * and www.getatmap.com inside the app. All other external websites are opened
 * in the external system browser.
 *
 * Connection failures and 5xx errors trigger the Coming Soon screen, while
 * standard 404 pages are rendered normally.
 */
class GetAtMapWebClient(
    private val context: Context,
    private val onPageStartedCallback: (url: String) -> Unit,
    private val onPageFinishedCallback: (url: String) -> Unit,
    private val onErrorReceived: (isWebsiteUnavailable: Boolean, errorMessage: String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: ""
        val url = uri.toString()

        // Handle native intent protocols (tel, mailto, sms, geo, whatsapp)
        when (scheme) {
            "tel", "mailto", "sms", "geo", "whatsapp" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    return true
                }
            }
            "intent" -> {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        context.startActivity(intent)
                        return true
                    }
                } catch (_: Exception) {
                    return true
                }
            }
        }

        // Restrict internal loading to getatmap.com and www.getatmap.com
        val host = uri.host?.lowercase() ?: ""
        val isAllowedInternalHost = host == "getatmap.com" || host == "www.getatmap.com" || host.endsWith(".getatmap.com") || scheme == "file"

        if (isAllowedInternalHost) {
            // Load inside the app
            return false
        }

        // Any other external website must be opened in the external browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            // Ignore if no browser found
        }
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStartedCallback(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinishedCallback(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // Trigger Coming Soon screen on connection failures for main frame
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Connection failed"
            onErrorReceived(true, description)
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true) {
            val statusCode = errorResponse?.statusCode ?: 200
            // Show Coming Soon screen ONLY for 5xx server errors, NOT for 404 pages
            if (statusCode in 500..599) {
                onErrorReceived(true, "Server Status $statusCode")
            }
        }
    }
}
