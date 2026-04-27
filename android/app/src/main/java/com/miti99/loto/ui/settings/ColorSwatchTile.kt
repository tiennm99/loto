package com.miti99.loto.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.miti99.loto.ui.theme.BrandEmeraldLight

/**
 * 40 dp filled circle swatch. Shows a double-ring (white inner + emerald outer)
 * when [selected].
 */
@Composable
internal fun ColorSwatchTile(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fillColor = parseHex(hex)

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (selected) Modifier
                    .border(2.dp, Color.White, CircleShape)
                    .border(4.dp, BrandEmeraldLight, CircleShape)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}

/** "Tùy chỉnh" tile — shown as a gradient or checkerboard placeholder. */
@Composable
internal fun CustomTile(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Use a simple dark-bordered circle as the custom trigger tile
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) BrandEmeraldLight.copy(alpha = 0.2f) else Color(0xFFF1F5F9))
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) BrandEmeraldLight else Color(0xFFCBD5E1),
                shape = CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick),
    )
}
