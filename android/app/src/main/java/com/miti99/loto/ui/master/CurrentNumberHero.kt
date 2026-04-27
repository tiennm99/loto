package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.BrandEmeraldLight
import com.miti99.loto.ui.theme.BrandPinkLight
import com.miti99.loto.ui.theme.CalledCellCream

/**
 * Large circle hero showing the most-recently drawn number.
 *
 * Pink ring for numbers ≤ 49; emerald ring for ≥ 50 (port MasterPanel.svelte:283).
 * TalkBack announces each new draw via [LiveRegionMode.Assertive].
 */
@Composable
fun CurrentNumberHero(
    last: Int,
    calledCount: Int,
    remainingCount: Int,
    modifier: Modifier = Modifier,
) {
    val isLow = last <= 49
    val ring = if (isLow) BrandPinkLight else BrandEmeraldLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.master_current_label),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(CalledCellCream)
                .border(8.dp, ring, CircleShape)
                .semantics { liveRegion = LiveRegionMode.Assertive },
        ) {
            Text(
                text = last.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = ring,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${stringResource(R.string.master_called_count_prefix)} $calledCount/90 · " +
                "${stringResource(R.string.master_remaining_prefix)} $remainingCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
