package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.BrandEmeraldLight
import com.miti99.loto.ui.theme.BrandPinkLight
import com.miti99.loto.ui.theme.CalledCellCream

/**
 * Horizontally scrollable chip row showing the full draw history in order.
 *
 * Auto-scrolls to the last chip whenever [called] changes.
 */
@Composable
fun CalledHistory(
    called: List<Int>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Scroll to the latest chip on every new draw
    LaunchedEffect(called.size) {
        if (called.isNotEmpty()) {
            listState.animateScrollToItem(called.lastIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.master_called_history_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )

        LazyRow(
            state = listState,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            items(called, key = { it }) { num ->
                CalledChip(num = num)
            }
        }
    }
}

@Composable
private fun CalledChip(num: Int) {
    val isLow = num <= 49
    val ring = if (isLow) BrandPinkLight else BrandEmeraldLight

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(CalledCellCream)
            .border(2.dp, ring, CircleShape),
    ) {
        Text(
            text = num.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ring,
        )
    }
}
