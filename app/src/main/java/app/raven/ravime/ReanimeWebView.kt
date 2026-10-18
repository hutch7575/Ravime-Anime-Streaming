package app.raven.ravime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View

private val allowedHosts = setOf("reanime.to", "www.reanime.to")

/**
 * Web compatibility adapter. No private endpoints, DOM injection, JS bridge,
 * cookie interception, media extraction or custom account handling.
 */
fun makeReanimeWebView(
    activity: MainActivity,
    onProgress: (Int) -> Unit,
    onCurrentUrl: (String) -> Unit,
    onPageError: (Boolean) -> Unit
): WebView = WebView(activity).apply {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    setBackgroundColor(android.graphics.Color.rgb(16, 17, 19))
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        loadsImagesAutomatically = true
        allowFileAccess = false
        allowContentAccess = true // Android system-selected upload URIs
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        setSupportMultipleWindows(false)
        javaScriptCanOpenWindowsAutomatically = false
        safeBrowsingEnabled = true
    }
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            if (!request.isForMainFrame) return false
            val target = request.url
            if (target.scheme == "https" && target.host in allowedHosts) {
                return false
            }
            if (target.scheme == "https" || target.scheme == "mailto") {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, target))
                } catch (_: ActivityNotFoundException) { }
            }
            // Never navigate top-level WebView to untrusted hosts or HTTP.
            return true
        }

        override fun onPageStarted(
            view: WebView,
            url: String?,
            favicon: android.graphics.Bitmap?
        ) {
            onPageError(false)
            onCurrentUrl(url ?: "https://reanime.to/home")
        }

        override fun onPageFinished(view: WebView, url: String?) {
            onCurrentUrl(url ?: "https://reanime.to/home")
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: android.webkit.WebResourceError
        ) {
            if (request.isForMainFrame) onPageError(true)
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgress(newProgress)
        }

        override fun onShowCustomView(
            view: View,
            callback: CustomViewCallback
        ) = activity.enterVideoFullscreen(view, callback)

        override fun onHideCustomView() = activity.leaveVideoFullscreen()

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean = activity.chooseFiles(filePathCallback, fileChooserParams)

        override fun onPermissionRequest(request: PermissionRequest) {
            // Deliberately do not grant camera, microphone or other device access.
            request.deny()
        }
    }
}
