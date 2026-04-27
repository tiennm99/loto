package com.miti99.loto.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.miti99.loto.R
import com.miti99.loto.state.BingoEvent
import com.miti99.loto.ui.theme.BrandAmberLight
import com.miti99.loto.ui.theme.BrandRoseLight

/**
 * "Kinh!" bingo celebration dialog.
 *
 * Shown when [bingoEvent] is non-null. Tier 2 (≥ 3 rows complete) also renders
 * [Confetti] behind the dialog via a sibling Box in [PlayerBoardScreen].
 */
@Composable
fun KinhModal(
    bingoEvent: BingoEvent,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Gradient title "Kinh!"
            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(BrandRoseLight, BrandAmberLight, BrandRoseLight),
            )
            Text(
                text = stringResource(R.string.kinh_title),
                style = TextStyle(
                    brush = gradientBrush,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f,
                    ),
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row completion message
            Text(
                text = "${stringResource(R.string.kinh_row_done_prefix)} ${bingoEvent.row1Based} ${stringResource(R.string.kinh_row_done_suffix)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
            )

            Text(
                text = stringResource(R.string.kinh_shout_hint),
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Primary dismiss button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRoseLight,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.kinh_dismiss),
                    fontWeight = FontWeight.Bold,
                )
            }

            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.kinh_close),
                    color = Color(0xFF9CA3AF),
                )
            }
        }
    }
}
