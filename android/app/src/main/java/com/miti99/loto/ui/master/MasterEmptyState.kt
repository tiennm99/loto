package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.settings.Settings
import com.miti99.loto.ui.theme.LotoTheme
import com.miti99.loto.ui.toComposeColor

/**
 * Empty-state hero for master mode, ported from `MasterEmptyState.svelte`:
 * a ghost mock of the 11×9 tracking grid keeps the page silhouette
 * consistent with mid-game.
 */
@Composable
fun MasterEmptyState(modifier: Modifier = Modifier) {
    val palette = LotoTheme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .alpha(0.3f)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.buttonSecondaryBorder, RoundedCornerShape(6.dp))
                .padding(bottom = 24.dp),
        ) {
            for (rowStart in 0 until 99 step 9) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in rowStart until rowStart + 9) {
                        val filled = i % 11 < 9 && i % 7 == 0
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (filled) palette.cellBg
                                    else Settings.DEFAULT_EMPTY_CELL_COLOR.toComposeColor(),
                                ),
                        ) {
                            if (filled) {
                                Text(
                                    text = "${(i * 13) % 90 + 1}",
                                    fontSize = 8.sp,
                                    color = palette.cellText,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.master_empty_badge).uppercase(),
            color = palette.masterHeading,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.em,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.tokenBg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.master_empty_hint_prefix))
                append(" ")
                withStyle(
                    SpanStyle(
                        color = palette.masterHeading,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Normal,
                    ),
                ) {
                    append("\"" + stringResource(R.string.master_new_game) + "\"")
                }
                append(" ")
                append(stringResource(R.string.master_empty_hint_suffix))
            },
            color = palette.subtitle,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.master_empty_ready),
            color = palette.subtitle,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
