package com.miti99.loto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.BrandAmberLight
import com.miti99.loto.ui.theme.BrandRoseLight

/**
 * Centered wordmark: italic "Lô tô" in rose→amber→rose gradient, with a small
 * "Hội chợ TN1" subtitle beneath. Port of the SvelteKit root heading.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(BrandRoseLight, BrandAmberLight, BrandRoseLight),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lô tô",
            style = TextStyle(
                brush = gradient,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                shadow = Shadow(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f,
                ),
            ),
        )
        Text(
            text = stringResource(R.string.wordmark_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
