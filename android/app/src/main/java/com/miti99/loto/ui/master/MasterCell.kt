package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.ui.common.LocalEmptyCellColor
import com.miti99.loto.ui.theme.BrandEmeraldLight
import com.miti99.loto.ui.theme.BrandPinkLight
import com.miti99.loto.ui.theme.BrandRedLight
import com.miti99.loto.ui.theme.CalledCellCream

private val UncalledRing = Color(0xFFCBD5E1)    // slate-300
private val UncalledBg   = Color(0xFFF8FAFC)    // slate-50
private val UncalledText = Color(0xFF94A3B8)    // slate-400

/**
 * Single token on the 11×9 master board.
 *
 * @param num       0 = empty slot; >0 = a lô tô number
 * @param callOrder 1-based draw order if this number has been called, else null
 * @param isLast    true for the most-recently drawn number (red ring + scale 1.10)
 */
@Composable
fun MasterCell(
    num: Int,
    callOrder: Int?,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    if (num == 0) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(LocalEmptyCellColor.current),
        )
        return
    }

    val isLow    = num <= 49
    val isCalled = callOrder != null

    val ringColor = when {
        isLast   -> BrandRedLight
        isCalled -> if (isLow) BrandPinkLight else BrandEmeraldLight
        else     -> UncalledRing
    }
    val bgColor   = if (isCalled) CalledCellCream else UncalledBg
    val textColor = when {
        isCalled -> if (isLow) BrandPinkLight else BrandEmeraldLight
        else     -> UncalledText
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
    ) {
        // Token circle — scale up + red border when latest
        val tokenModifier = Modifier
            .fillMaxSize(0.82f)
            .let { if (isLast) it.scale(1.10f) else it }
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isLast) 3.dp else 2.dp,
                color = ringColor,
                shape = CircleShape,
            )

        Box(tokenModifier, contentAlignment = Alignment.Center) {
            Text(
                text = num.toString(),
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
            )
        }

        // Draw-order superscript (top-end corner)
        if (callOrder != null) {
            Text(
                text = callOrder.toString(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(1.dp),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}
