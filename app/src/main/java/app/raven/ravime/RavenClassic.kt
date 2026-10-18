package app.raven.ravime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sourced from Ravyn's current RAVEN Classic palette; green is Ravime-specific. */
internal object RavenClassic {
    val background = Color(0xFF101113)
    val panel = Color(0xFF1B1D1E)
    val raised = Color(0xFF282B2D)
    val text = Color(0xFFF7F7F7)
    val secondary = Color(0xFFB8BCBD)
    val muted = Color(0xFF8E9294)
    val accent = Color(0xFFA5D66B)
    val shape = RoundedCornerShape(10.dp)
}

@Composable
internal fun RavenLabel(
    value: String,
    size: Int = 13,
    color: Color = RavenClassic.text,
    bold: Boolean = false,
    lines: Int = 1,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        modifier = modifier,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun RavenSection(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RavenLabel(title, 16, bold = true, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            RavenLabel(
                action, 11, RavenClassic.accent, true,
                modifier = Modifier.clickable(onClick = onAction).padding(5.dp)
            )
        }
    }
}

@Composable
internal fun RavenTintedHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(
                            RavenClassic.panel, RavenClassic.accent, 0.13f
                        ),
                        RavenClassic.background
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        RavenLabel(title, 21, bold = true)
        RavenLabel(subtitle, 11, RavenClassic.secondary, lines = 2)
    }
}

@Composable
internal fun RavenTag(text: String) {
    Box(
        modifier = Modifier
            .background(RavenClassic.raised, CircleShape)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) { RavenLabel(text, 11, RavenClassic.secondary) }
}

@Composable
internal fun RavenActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(43.dp)
                .background(RavenClassic.panel, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(21.dp), tint = RavenClassic.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            RavenLabel(title, 13, bold = true)
            RavenLabel(subtitle, 11, RavenClassic.secondary, lines = 2)
        }
        Spacer(Modifier.width(7.dp))
        Icon(
            Icons.Outlined.ChevronRight, null,
            modifier = Modifier.size(19.dp), tint = RavenClassic.muted
        )
    }
}

@Composable
internal fun RavenNote(text: String) {
    RavenLabel(text, 11, RavenClassic.muted, lines = 5)
}

@Composable
internal fun RavenInfoRow(title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(43.dp)
                .background(RavenClassic.panel, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(21.dp), tint = RavenClassic.accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RavenLabel(title, 13, bold = true)
            RavenLabel(subtitle, 11, RavenClassic.secondary)
        }
    }
}
