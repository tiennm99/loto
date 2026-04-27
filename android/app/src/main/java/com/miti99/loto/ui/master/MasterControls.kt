package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.BrandAmberLight
import com.miti99.loto.ui.theme.BrandEmeraldLight
import com.miti99.loto.ui.theme.BrandRedLight
import com.miti99.loto.ui.theme.BrandRoseLight

/**
 * Button row for the master panel.
 *
 * - "Ván mới" (orange→red gradient) — always visible.
 * - When [canDraw]:
 *   - [autoCallEnabled] true  → "Bắt đầu" (emerald) / "Dừng" (red) toggle.
 *   - [autoCallEnabled] false → "Xổ số" (emerald) draw button.
 */
@Composable
fun MasterControls(
    hasState: Boolean,
    canDraw: Boolean,
    autoCallEnabled: Boolean,
    autoRunning: Boolean,
    onNewGame: () -> Unit,
    onDrawNext: () -> Unit,
    onToggleAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "Ván mới" — gradient background via Box + transparent Button overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    brush = Brush.horizontalGradient(listOf(BrandAmberLight, BrandRoseLight)),
                    shape = RoundedCornerShape(50),
                ),
        ) {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.btn_new_game),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // Draw / auto toggle — only when there are remaining numbers
        if (canDraw) {
            Spacer(modifier = Modifier.width(0.dp))

            if (autoCallEnabled) {
                val (label, color) = if (autoRunning)
                    Pair(stringResource(R.string.btn_auto_stop), BrandRedLight)
                else
                    Pair(stringResource(R.string.btn_auto_start), BrandEmeraldLight)

                Button(
                    onClick = onToggleAuto,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDrawNext,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldLight),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.btn_draw_next),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Small chip showing "Tự động: Ns/số" when auto-call is running. */
@Composable
fun AutoCallChip(
    speedSeconds: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(
                color = BrandEmeraldLight.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.master_auto_label_format, speedSeconds),
            color = BrandEmeraldLight,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
