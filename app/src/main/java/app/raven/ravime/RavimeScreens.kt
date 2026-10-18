package app.raven.ravime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.util.Calendar

private val pagePadding = PaddingValues(
    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp
)

@Composable
internal fun RavimeHome(
    openSite: (String) -> Unit,
    openLibrary: () -> Unit,
    openSettings: () -> Unit
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pagePadding,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            RavenTintedHeader(greeting, "Anime for every moment")
        }
        item { RavenTag("Anime") }
        item { RavenSection("Discover anime", action = "Browse all") { openSite("/home") } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RavenQuickTile(
                    "Browse", "Explore anime",
                    Icons.Outlined.Explore,
                    Modifier.weight(1f)
                ) { openSite("/home") }
                RavenQuickTile(
                    "Search", "Find a series",
                    Icons.Outlined.AutoAwesome,
                    Modifier.weight(1f)
                ) { openSite("/search") }
            }
        }
        item { RavenSection("Keep up to date") }
        item {
            RavenActionRow(
                "Release schedule",
                "See what's airing this week",
                Icons.Outlined.CalendarMonth
            ) { openSite("/schedule") }
        }
        item {
            RavenActionRow(
                "New and trending",
                "Recently updated and popular shows",
                Icons.Outlined.LiveTv
            ) { openSite("/home") }
        }
        item { RavenSection("Your space") }
        item {
            RavenActionRow(
                "Your library",
                "Profile, watchlist and account",
                Icons.Outlined.VideoLibrary,
                onClick = openLibrary
            )
        }
        item {
            RavenActionRow(
                "Community",
                "Posts and discussions",
                Icons.Outlined.Forum
            ) { openSite("/community") }
        }
        item {
            RavenActionRow(
                "Watch together",
                "Join community viewing rooms",
                Icons.Outlined.PlayCircleOutline
            ) { openSite("/watch2gether") }
        }
        item {
            RavenActionRow(
                "Settings",
                "Ravime and Re:ANIME preferences",
                Icons.Outlined.Settings,
                onClick = openSettings
            )
        }
        item {
            Spacer(Modifier.height(5.dp))
            RavenNote("Content and accounts are provided by Re:ANIME. Playback depends on WebView and the streaming provider.")
        }
    }
}

@Composable
private fun RavenQuickTile(
    name: String,
    detail: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(107.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(RavenClassic.panel, RavenClassic.raised)
                ),
                RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, modifier = Modifier.size(23.dp), tint = RavenClassic.accent)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RavenLabel(name, 13, bold = true)
            RavenLabel(detail, 10, RavenClassic.secondary)
        }
    }
}

@Composable
internal fun RavimeLibrary(
    openSite: (String) -> Unit,
    openSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pagePadding,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { RavenTintedHeader("Your library", "Keep your anime and account together") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(62.dp)
                        .background(RavenClassic.raised, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountCircle, null, Modifier.size(37.dp),
                        tint = RavenClassic.accent
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RavenLabel("Re:ANIME account", 17, bold = true)
                    RavenLabel("Sign in and manage your profile", 11, RavenClassic.secondary)
                }
            }
        }
        item {
            RavenActionRow(
                "Open account",
                "Sign in, profile details and your website settings",
                Icons.Outlined.OpenInBrowser
            ) { openSite("/profile") }
        }
        item { RavenSection("Account preferences") }
        item {
            RavenActionRow(
                "Ads & playback",
                "Disable ads, autoplay, intro skips and dubbed audio",
                Icons.Outlined.Tune
            ) { openSite("/profile") }
        }
        item {
            RavenActionRow(
                "Watchlist & progress",
                "Manage your watchlist and episode tracking preferences",
                Icons.Outlined.CheckCircleOutline
            ) { openSite("/profile") }
        }
        item {
            RavenActionRow(
                "AniList / MyAnimeList",
                "Link an anime tracker and configure progress syncing",
                Icons.Outlined.CloudSync
            ) { openSite("/profile") }
        }
        item {
            RavenActionRow(
                "Privacy & security",
                "Profile visibility, passwords and account security",
                Icons.Outlined.Lock
            ) { openSite("/profile") }
        }
        item { RavenSection("Ravime") }
        item {
            RavenActionRow(
                "App settings",
                "Appearance, browser behaviour and storage",
                Icons.Outlined.Settings,
                onClick = openSettings
            )
        }
        item {
            Spacer(Modifier.height(3.dp))
            RavenNote(
                "Your Re:ANIME profile and settings stay on the website. " +
                    "Each account shortcut opens the original settings page " +
                    "so changes are saved by Re:ANIME itself."
            )
        }
    }
}

@Composable
internal fun RavimeSettings(
    autoPlayInWebView: Boolean,
    websiteThemeEnabled: Boolean,
    onWebsiteThemeChange: (Boolean) -> Unit,
    onAutoPlayChange: (Boolean) -> Unit,
    onClearSiteData: () -> Unit,
    openSite: (String) -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pagePadding,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { RavenTintedHeader("Settings", "RAVEN Classic for anime") }
        item { RavenSection("Appearance") }
        item {
            RavenInfoRow(
                "Theme",
                "RAVEN Classic · Dark",
                Icons.Outlined.Settings
            )
        }
        item {
            RavenInfoRow(
                "Accent colour",
                "RAVEN Green",
                Icons.Outlined.AutoAwesome
            )
        }
        item { RavenSection("Website appearance") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RavenLabel("RAVEN website theme", 13, bold = true)
                    RavenLabel(
                        "Apply RAVEN Green to Re:ANIME pages, search, player, community and account settings.",
                        11, RavenClassic.secondary, lines = 3
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = websiteThemeEnabled,
                    onCheckedChange = onWebsiteThemeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RavenClassic.background,
                        checkedTrackColor = RavenClassic.accent,
                        uncheckedThumbColor = RavenClassic.secondary,
                        uncheckedTrackColor = RavenClassic.raised
                    )
                )
            }
        }
        item {
            RavenNote(
                "Theme changes are visual only. The website still handles video, " +
                    "search results, login, ads and comments. Turn this off if a " +
                    "site update causes an interface problem."
            )
        }
        item { RavenSection("Browser") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RavenLabel("Allow video autoplay", 13, bold = true)
                    RavenLabel(
                        "Allow websites to start video without a tap. " +
                            "Re:ANIME also has its own autoplay option.",
                        11, RavenClassic.secondary, lines = 3
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = autoPlayInWebView,
                    onCheckedChange = onAutoPlayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RavenClassic.background,
                        checkedTrackColor = RavenClassic.accent,
                        uncheckedThumbColor = RavenClassic.secondary,
                        uncheckedTrackColor = RavenClassic.raised
                    )
                )
            }
        }
        item {
            RavenActionRow(
                "Clear browser data",
                "Sign out and remove cached website data",
                Icons.Outlined.History,
                onClick = { showClearConfirmation = true }
            )
        }
        item { RavenSection("Re:ANIME settings") }
        item {
            RavenActionRow(
                "Ad preferences",
                "Disable Ads is available after signing in",
                Icons.Outlined.Tune
            ) { openSite("/profile") }
        }
        item {
            RavenActionRow(
                "Playback & privacy",
                "Sub / Dub, skip intro, notifications, watchlist visibility",
                Icons.Outlined.VideoLibrary
            ) { openSite("/profile") }
        }
        item {
            RavenActionRow(
                "Account & tracking",
                "Update your account, AniList and MyAnimeList",
                Icons.Outlined.AccountCircle
            ) { openSite("/profile") }
        }
        item {
            RavenNote(
                "Website settings are controlled by Re:ANIME. " +
                    "Ravime does not read your password or modify private account settings."
            )
        }
        item { RavenSection("About") }
        item {
            RavenLabel("Ravime Anime Streaming", 13, bold = true)
            RavenLabel("Version 0.3.0 · RAVEN Classic", 11, RavenClassic.secondary)
            Spacer(Modifier.height(5.dp))
            RavenNote("Independent and unofficial companion for Re:ANIME.")
        }
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { RavenLabel("Clear website data?", 18, bold = true) },
            text = {
                RavenLabel(
                    "This removes site cookies, saved web storage and cache. " +
                        "You'll have to sign in again.",
                    13, RavenClassic.secondary, lines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearSiteData()
                    }
                ) { RavenLabel("Clear data", color = RavenClassic.accent, bold = true) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    RavenLabel("Cancel", color = RavenClassic.secondary)
                }
            },
            containerColor = RavenClassic.panel
        )
    }
}
