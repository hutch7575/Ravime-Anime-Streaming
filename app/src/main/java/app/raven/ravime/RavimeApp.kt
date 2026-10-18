package app.raven.ravime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val SITE = "https://reanime.to"
private enum class Page { HOME, BROWSER, LIBRARY, SETTINGS }

/**
 * RAVEN Classic shell. Website pages remain owned and rendered by Re:ANIME;
 * native profile and settings are intentionally honest about that boundary.
 */
@Composable
fun RavimeApp(activity: MainActivity) {
    var page by rememberSaveable { mutableStateOf(Page.BROWSER) }
    var returnFromBrowser by remember { mutableStateOf(Page.HOME) }
    var returnFromSettings by remember { mutableStateOf(Page.HOME) }
    var browserIsSearch by rememberSaveable { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("$SITE/home") }
    var progress by remember { mutableIntStateOf(100) }
    var failed by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    val preferences = remember(activity) {
        activity.getSharedPreferences("ravime_preferences", android.content.Context.MODE_PRIVATE)
    }
    var allowVideoAutoplay by remember {
        mutableStateOf(preferences.getBoolean("allow_video_autoplay", true))
    }
    var websiteThemeEnabled by remember {
        mutableStateOf(preferences.getBoolean("website_theme_enabled", true))
    }
    val browser = remember(activity) {
        makeReanimeWebView(
            activity,
            onProgress = { progress = it },
            onCurrentUrl = { currentUrl = it; browserIsSearch = Uri.parse(it).path?.startsWith("/search") == true },
            onPageError = { failed = it },
            isThemeEnabled = { websiteThemeEnabled }
        )
    }
    LaunchedEffect(browser) { browser.loadUrl("$SITE/home") }
    LaunchedEffect(browser, allowVideoAutoplay) {
        browser.settings.mediaPlaybackRequiresUserGesture = !allowVideoAutoplay
    }
    LaunchedEffect(browser, page) {
        if (page == Page.BROWSER) browser.onResume() else browser.onPause()
    }
    DisposableEffect(browser) {
        onDispose {
            browser.stopLoading()
            browser.destroy()
        }
    }

    fun openWeb(route: String, isSearch: Boolean = false) {
        returnFromBrowser = if (page == Page.BROWSER) returnFromBrowser else page
        browserIsSearch = isSearch
        failed = false
        browser.loadUrl(SITE + route)
        page = Page.BROWSER
    }

    fun openLibrary() {
        page = Page.LIBRARY
    }

    fun openSettings() {
        if (page != Page.SETTINGS) returnFromSettings = page
        page = Page.SETTINGS
    }

    fun goBack() {
        when (page) {
            Page.BROWSER -> {
                if (browser.canGoBack()) browser.goBack()
                else if (currentUrl != "$SITE/home") browser.loadUrl("$SITE/home")
                else activity.finish()
            }
            Page.SETTINGS -> page = returnFromSettings
            Page.LIBRARY -> openWeb("/home")
            Page.HOME -> Unit
        }
    }

    fun openInBrowser() {
        val target = Uri.parse(currentUrl)
        if (target.scheme == "https" &&
            (target.host == "reanime.to" || target.host == "www.reanime.to")
        ) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, target))
            } catch (_: ActivityNotFoundException) { }
        }
    }

    fun clearSiteData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        browser.clearCache(true)
        browser.clearHistory()
        browser.loadUrl("$SITE/home")
        page = Page.BROWSER
    }

    BackHandler(enabled = page != Page.HOME) { goBack() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = RavenClassic.background,
            surface = RavenClassic.panel,
            primary = RavenClassic.accent,
            onPrimary = RavenClassic.background,
            onSurface = RavenClassic.text
        )
    ) {
        Surface(
            color = RavenClassic.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                        .background(RavenClassic.panel)
                        .padding(start = 12.dp, end = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page == Page.SETTINGS) {
                        IconButton(onClick = ::goBack) {
                            Icon(Icons.Outlined.ArrowBack, "Back", tint = RavenClassic.text)
                        }
                    }
                    RavenLabel(
                        if (page == Page.SETTINGS) "Settings" else "Ravime",
                        23, bold = true,
                        modifier = Modifier.weight(1f).clickable {
                            if (page != Page.SETTINGS) openWeb("/home")
                        }
                    )
                    if (page != Page.SETTINGS) {
                        IconButton(onClick = { openWeb("/search", isSearch = true) }) {
                            Icon(Icons.Outlined.Search, "Search", tint = RavenClassic.text)
                        }
                        Box {
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(Icons.Outlined.MoreVert, "More", tint = RavenClassic.text)
                            }
                            DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                                modifier = Modifier.background(RavenClassic.panel)
                            ) {
                                val destinations = listOf(
                                    "Discover anime" to "/home",
                                    "Community & messaging" to "/community",
                                    "Release schedule" to "/schedule",
                                    "Watch together" to "/watch2gether",
                                    "Re:ANIME profile" to "/profile"
                                )
                                destinations.forEach { (label, route) ->
                                    DropdownMenuItem(
                                        text = { RavenLabel(label, 13) },
                                        onClick = {
                                            moreExpanded = false
                                            openWeb(route)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { RavenLabel("Refresh page", 13) },
                                    onClick = { moreExpanded = false; browser.reload() }
                                )
                                DropdownMenuItem(
                                    text = { RavenLabel("Open in browser", 13) },
                                    onClick = { moreExpanded = false; openInBrowser() }
                                )
                            }
                        }
                        IconButton(onClick = ::openSettings) {
                            Icon(Icons.Outlined.Settings, "Settings", tint = RavenClassic.text)
                        }
                    }
                }
                if (page == Page.BROWSER && progress < 100) {
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(RavenClassic.raised)
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth(progress / 100f)
                                .background(RavenClassic.accent)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (page) {
                        Page.HOME -> RavimeHome(
                            openSite = { openWeb(it) },
                            openLibrary = ::openLibrary,
                            openSettings = ::openSettings
                        )
                        Page.LIBRARY -> RavimeLibrary(
                            openSite = { openWeb(it) },
                            openSettings = ::openSettings
                        )
                        Page.SETTINGS -> RavimeSettings(
                            autoPlayInWebView = allowVideoAutoplay,
                            websiteThemeEnabled = websiteThemeEnabled,
                            onWebsiteThemeChange = {
                                websiteThemeEnabled = it
                                preferences.edit().putBoolean("website_theme_enabled", it).apply()
                                RavimeWebTheme.apply(activity, browser, it)
                            },
                            onAutoPlayChange = {
                                allowVideoAutoplay = it
                                preferences.edit().putBoolean("allow_video_autoplay", it).apply()
                            },
                            onClearSiteData = ::clearSiteData,
                            openSite = { openWeb(it) }
                        )
                        Page.BROWSER -> {
                            AndroidView(
                                factory = {
                                    (browser.parent as? ViewGroup)?.removeView(browser)
                                    browser
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (failed) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .background(RavenClassic.background)
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RavenLabel("Unable to load the page", 18, bold = true)
                                    Spacer(Modifier.height(8.dp))
                                    RavenLabel(
                                        "Check your connection, then try again.",
                                        12, RavenClassic.secondary, lines = 2
                                    )
                                    Spacer(Modifier.height(15.dp))
                                    RavenLabel(
                                        "Retry",
                                        13, RavenClassic.accent, true,
                                        modifier = Modifier.clickable {
                                            failed = false
                                            browser.reload()
                                        }.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .background(RavenClassic.panel),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val selectedIndex = when (page) {
                        Page.LIBRARY -> 2
                        Page.BROWSER -> if (browserIsSearch) 1 else 0
                        Page.SETTINGS -> if (returnFromSettings == Page.LIBRARY) 2 else 0
                        Page.HOME -> 0
                    }
                    RavimeNavItem("Home", Icons.Outlined.Home, selectedIndex == 0,
                        Modifier.weight(1f)) { openWeb("/home") }
                    RavimeNavItem("Search", Icons.Outlined.Search, selectedIndex == 1,
                        Modifier.weight(1f)) { openWeb("/search", isSearch = true) }
                    RavimeNavItem("Your Library", Icons.Outlined.MenuBook, selectedIndex == 2,
                        Modifier.weight(1f)) { openLibrary() }
                }
            }
        }
    }
}

@Composable
private fun RavimeNavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = if (active) RavenClassic.accent else RavenClassic.secondary
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        RavenLabel(label, 10, tint, bold = active)
    }
}
