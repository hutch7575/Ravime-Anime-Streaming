package app.raven.ravime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val SITE = "https://reanime.to"
private object Raven {
    val background = Color(0xFF101113)
    val panel = Color(0xFF1B1D1E)
    val raised = Color(0xFF282B2D)
    val text = Color(0xFFF7F7F7)
    val secondary = Color(0xFFB8BCBD)
    val muted = Color(0xFF8E9294)
    val accent = Color(0xFFB9A4EB)
}

private enum class Tab(val title: String, val route: String?, val icon: ImageVector) {
    HOME("Home", null, Icons.Outlined.Home),
    DISCOVER("Discover", "/home", Icons.Outlined.Explore),
    SEARCH("Search", "/search", Icons.Outlined.Search),
    COMMUNITY("Community", "/community", Icons.Outlined.Groups),
    PROFILE("Profile", "/profile", Icons.Outlined.AccountCircle)
}

@Composable
fun RavimeApp(activity: MainActivity) {
    var selected by rememberSaveable { mutableStateOf(Tab.HOME) }
    var currentUrl by remember { mutableStateOf(SITE + "/home") }
    var progress by remember { mutableIntStateOf(100) }
    var failed by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val browser = remember(activity) {
        makeReanimeWebView(
            activity,
            onProgress = { progress = it },
            onCurrentUrl = { currentUrl = it },
            onPageError = { failed = it }
        )
    }
    DisposableEffect(browser) {
        onDispose {
            browser.stopLoading()
            browser.destroy()
        }
    }

    fun openTab(destination: Tab) {
        selected = destination
        destination.route?.let { browser.loadUrl(SITE + it) }
    }

    fun openRoute(route: String) {
        selected = Tab.DISCOVER
        browser.loadUrl(SITE + route)
    }

    fun openBrowser() {
        val safeUri = Uri.parse(currentUrl)
        if (safeUri.scheme == "https" &&
            (safeUri.host == "reanime.to" || safeUri.host == "www.reanime.to")
        ) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, safeUri))
            } catch (_: ActivityNotFoundException) { }
        }
    }

    BackHandler(enabled = selected != Tab.HOME) {
        if (browser.canGoBack()) browser.goBack()
        else selected = Tab.HOME
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Raven.background,
        surface = Raven.panel,
        primary = Raven.accent,
        onPrimary = Raven.background,
        onSurface = Raven.text
    )) {
        Surface(color = Raven.background, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .background(Raven.panel).padding(start = 17.dp, end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).clickable { selected = Tab.HOME }) {
                        Label("RAVIME", 19, Raven.text, true)
                        Label("ANIME STREAMING", 9, Raven.secondary)
                    }
                    if (selected != Tab.HOME) {
                        IconButton(onClick = { browser.reload() }) {
                            Icon(Icons.Outlined.Refresh, "Refresh", tint = Raven.secondary)
                        }
                        IconButton(onClick = ::openBrowser) {
                            Icon(Icons.Outlined.OpenInBrowser, "Open in browser", tint = Raven.secondary)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, "More", tint = Raven.secondary)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Raven.raised)
                        ) {
                            listOf(
                                "Release schedule" to "/schedule",
                                "Watch parties" to "/watch2gether",
                                "Account settings" to "/profile"
                            ).forEach { (name, route) ->
                                DropdownMenuItem(
                                    text = { Label(name, 13) },
                                    onClick = { menuExpanded = false; openRoute(route) }
                                )
                            }
                        }
                    }
                }
                if (selected != Tab.HOME && progress < 100) {
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Raven.raised)) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(progress / 100f)
                                .background(Raven.accent)
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    if (selected == Tab.HOME) {
                        HomeScreen(::openRoute)
                    } else {
                        AndroidView(
                            factory = { browser },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (failed) {
                            Column(
                                modifier = Modifier.fillMaxSize().background(Raven.background)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Label("Unable to load this page", 18, Raven.text, true)
                                Spacer(Modifier.height(8.dp))
                                Label("Check your connection or try again.", 12, Raven.secondary)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { failed = false; browser.reload() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Raven.accent,
                                        contentColor = Raven.background
                                    )
                                ) { Text("Retry") }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .background(Raven.panel),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Tab.entries.forEach { destination ->
                        val active = selected == destination
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clickable { openTab(destination) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                destination.icon,
                                destination.title,
                                modifier = Modifier.size(20.dp),
                                tint = if (active) Raven.accent else Raven.secondary
                            )
                            Spacer(Modifier.height(3.dp))
                            Label(
                                destination.title, 10,
                                if (active) Raven.accent else Raven.secondary,
                                active
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(open: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(
                            Raven.panel,
                            Raven.accent.copy(alpha = .19f),
                            Raven.panel
                        )),
                        RoundedCornerShape(9.dp)
                    ).border(
                        1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(9.dp)
                    ).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Label("Welcome to Ravime", 23, Raven.text, true)
                Label(
                    "Discover, watch and discuss anime in the RAVEN Classic experience.",
                    12, Raven.secondary, lines = 3
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { open("/home") },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Raven.accent,
                        contentColor = Raven.background
                    )
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Browse anime", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Label("EXPLORE", 12, Raven.muted, true) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeTile(
                    "Search anime", "Find your next series",
                    Icons.Outlined.Search, Modifier.weight(1f)
                ) { open("/search") }
                HomeTile(
                    "Release schedule", "What's airing this week",
                    Icons.Outlined.CalendarMonth, Modifier.weight(1f)
                ) { open("/schedule") }
            }
        }
        item { Label("YOUR SPACE", 12, Raven.muted, true) }
        item {
            HomeLink(
                "Your profile", "Account, watchlist and preferences",
                Icons.Outlined.AccountCircle
            ) { open("/profile") }
        }
        item {
            HomeLink(
                "Community", "Posts, discussions and social activity",
                Icons.Outlined.Groups
            ) { open("/community") }
        }
        item {
            HomeLink(
                "Watch together", "Shared anime rooms and watch parties",
                Icons.Outlined.VideoLibrary
            ) { open("/watch2gether") }
        }
        item {
            Label(
                "Connected to Re:ANIME. Streaming, accounts and community " +
                    "features are provided by the website; availability varies by device.",
                11, Raven.muted, lines = 4
            )
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    image: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.height(118.dp)
            .background(Raven.panel, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(image, null, Modifier.size(23.dp), tint = Raven.accent)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label(title, 13, Raven.text, true)
            Label(subtitle, 10, Raven.secondary)
        }
    }
}

@Composable
private fun HomeLink(
    title: String,
    subtitle: String,
    image: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(Raven.raised, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(image, null, Modifier.size(20.dp), tint = Raven.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Label(title, 13, Raven.text, true)
            Label(subtitle, 11, Raven.secondary)
        }
        Icon(
            Icons.Outlined.ArrowForward, null, Modifier.size(17.dp),
            tint = Raven.muted
        )
    }
}

@Composable
private fun Label(
    value: String,
    size: Int,
    color: Color = Raven.text,
    bold: Boolean = false,
    lines: Int = 1
) {
    Text(
        value,
        fontSize = size.sp,
        color = color,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = lines,
        lineHeight = (size + 4).sp,
        overflow = TextOverflow.Ellipsis
    )
}
