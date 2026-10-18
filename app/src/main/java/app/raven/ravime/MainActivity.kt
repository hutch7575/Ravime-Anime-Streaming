package app.raven.ravime

import android.content.ActivityNotFoundException
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Bundle

/** Owns only Android-specific operations; the visual shell lives in Compose. */
class MainActivity : ComponentActivity() {
    private var pendingFiles: ValueCallback<Array<Uri>>? = null
    private var videoView: View? = null
    private var videoCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private lateinit var videoBack: OnBackPressedCallback

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingFiles?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        pendingFiles = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        videoBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                videoCallback?.onCustomViewHidden()
                leaveVideoFullscreen()
            }
        }
        onBackPressedDispatcher.addCallback(this, videoBack)
        setContent { RavimeApp(this) }
    }

    fun chooseFiles(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        pendingFiles?.onReceiveValue(null)
        pendingFiles = callback
        return try {
            filePicker.launch(params.createIntent())
            true
        } catch (_: ActivityNotFoundException) {
            pendingFiles?.onReceiveValue(null)
            pendingFiles = null
            false
        } catch (_: Exception) {
            pendingFiles?.onReceiveValue(null)
            pendingFiles = null
            false
        }
    }

    fun enterVideoFullscreen(
        view: View,
        callback: WebChromeClient.CustomViewCallback
    ) {
        if (videoView != null) {
            callback.onCustomViewHidden()
            return
        }
        videoView = view
        videoCallback = callback
        originalOrientation = requestedOrientation
        (window.decorView as ViewGroup).addView(
            view, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        videoBack.isEnabled = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun leaveVideoFullscreen() {
        val view = videoView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        videoView = null
        videoCallback = null
        videoBack.isEnabled = false
        requestedOrientation = originalOrientation
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        pendingFiles?.onReceiveValue(null)
        pendingFiles = null
        leaveVideoFullscreen()
        super.onDestroy()
    }
}
