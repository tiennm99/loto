package com.miti99.loto.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BgCrossedComplete = Color(0xFFD1FAE5)   // emerald-100
private val BgCrossedIncomplete = Color(0xFFFEF2F2) // red-50
private val BgDark = Color(0xFF1E293B)               // slate-800
private val CrossLineColor = Color(0xFFF43F5E)       // rose-500

/**
 * Single player-card cell.
 *
 * @param num       0 = empty slot (colored background, non-interactive)
 * @param crossed   whether this number has been marked
 * @param rowComplete  precomputed from VM; shifts crossed-cell bg to emerald
 * @param emptyCellColor background for num==0 cells (from settings)
 * @param onClick   null when num==0 (cell is non-interactive)
 */
@Composable
fun PlayerCell(
    num: Int,
    crossed: Boolean,
    rowComplete: Boolean,
    emptyCellColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dark = isSystemInDarkTheme()

    if (num == 0) {
        Box(
            modifier = modifier
                .aspectRatio(3f / 4f)
                .background(emptyCellColor)
                .testTag("empty_cell"),
        ) {
            if (dark) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                )
            }
        }
        return
    }

    val bg = when {
        crossed && rowComplete -> BgCrossedComplete
        crossed && !rowComplete -> BgCrossedIncomplete
        dark -> BgDark
        else -> Color.White
    }
    val desc = "Số $num${if (crossed) ", đã đánh dấu" else ""}"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(3f / 4f)
            .background(bg)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                } else Modifier,
            )
            .semantics { contentDescription = desc }
            .testTag("player_cell")
            .let { m ->
                if (crossed) m.drawBehind {
                    // Diagonal slash from top-start to bottom-end
                    drawLine(
                        color = CrossLineColor.copy(alpha = 0.6f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.5f,
                    )
                } else m
            },
    ) {
        Text(
            text = num.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (dark && !crossed) Color(0xFFE2E8F0) else Color.Unspecified,
        )
    }
}
