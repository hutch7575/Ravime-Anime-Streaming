package app.raven.ravime

import android.content.Context
import android.webkit.WebView
import org.json.JSONObject

/**
 * Opt-in, reversible presentation layer on the first-party Re:ANIME document.
 * No JS interfaces, API interception, input listeners, login interception,
 * bypasses, or player/source URL manipulation.
 */
internal object RavimeWebTheme {
    fun apply(context: Context, webView: WebView, enabled: Boolean) {
        if (!enabled) {
            webView.evaluateJavascript(
                """
                (function(){
                  if(window.__ravimeThemeBridge) window.__ravimeThemeBridge.teardown();
                  document.documentElement.classList.remove('rv-theme');
                  var style = document.getElementById('ravime-theme-css');
                  if(style) style.remove();
                })();
                """.trimIndent(), null
            )
            return
        }
        val css = context.assets.open("ravime.css").bufferedReader().use { it.readText() }
        val bootstrap = context.assets.open("ravime.js").bufferedReader().use { it.readText() }
        val styleScript = """
            (function(){
              if(!document.documentElement) return;
              var style = document.getElementById('ravime-theme-css');
              if(!style) {
                style = document.createElement('style');
                style.id = 'ravime-theme-css';
                (document.head || document.documentElement).appendChild(style);
              }
              if(style.textContent !== CSS_TEXT) style.textContent = CSS_TEXT;
            })();
        """.trimIndent().replace("CSS_TEXT", JSONObject.quote(css))
        webView.evaluateJavascript(styleScript + "\n" + bootstrap, null)
    }
}
