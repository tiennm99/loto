package com.miti99.loto.ui.board

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.state.WaitingToast
import kotlinx.coroutines.delay

private val AmberBackground = Color(0xFFF59E0B)  // amber-500
private val AmberText = Color(0xFF1C1917)         // stone-900 — high contrast on amber

/**
 * Amber capsule overlay showing "Chờ N" when one number remains in a row.
 *
 * Auto-dismisses after 5 s. Tap dismisses immediately.
 * Caller wraps this inside a Box with absolute positioning over the board.
 */
@Composable
fun ChoToast(
    toast: WaitingToast?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Auto-dismiss after 5 seconds, keyed on seenAt so a new toast resets the timer.
    LaunchedEffect(toast?.seenAt) {
        if (toast != null) {
            delay(5_000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AmberBackground)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.toast_close)) {
                    onDismiss()
                }
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = toast?.message ?: "",
                color = AmberText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}
