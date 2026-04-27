package com.miti99.loto.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R

// Stub numbers for the ghost 9-col grid — 27 cells total (3 rows × 9 cols).
// Every 3rd position (index % 3 == 0) is treated as a "filled" number slot.
private val GHOST_ROWS = listOf(
    listOf(5, 0, 0, 23, 0, 0, 47, 0, 0),
    listOf(0, 12, 0, 0, 34, 0, 0, 56, 0),
    listOf(0, 0, 18, 0, 0, 41, 0, 0, 72),
)

/**
 * Cold-start placeholder shown when `grid == null`.
 * Renders a faded 9-col ghost grid with a prompt instructing the user
 * to tap "Tạo bảng mới" to generate a real card.
 */
@Composable
fun GhostPreview(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Faded ghost grid
        Column(modifier = Modifier.alpha(0.3f).fillMaxWidth()) {
            GHOST_ROWS.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { num ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(3f / 4f)
                                .background(
                                    if (num > 0) Color(0xFFE2E8F0)
                                    else Color(0xFF7030A0),
                                ),
                        ) {
                            if (num > 0) {
                                Text(
                                    text = num.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Prompt text
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.ghost_press_to_start_prefix))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.ghost_quote_word))
                }
                append(" ")
                append(stringResource(R.string.ghost_press_to_start_suffix))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.ghost_have_fun),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
