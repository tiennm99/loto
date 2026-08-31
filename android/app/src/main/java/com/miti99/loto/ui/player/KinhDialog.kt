package com.miti99.loto.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.miti99.loto.R
import com.miti99.loto.ui.theme.LotoTheme

/**
 * The "Kinh!" (row win) celebration modal, ported from PlayerBoard.svelte.
 * Back press and backdrop tap dismiss it (Dialog's onDismissRequest).
 */
@Composable
fun KinhDialog(
    congratsRow: Int,
    onDismiss: () -> Unit,
) {
    val palette = LotoTheme.palette
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.dialogBg)
                .padding(32.dp),
        ) {
            Text(text = "🎉", fontSize = 48.sp)
            Text(
                text = stringResource(R.string.kinh_title),
                color = palette.kinhTitle,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.kinh_row_prefix),
                color = palette.dialogText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = congratsRow.toString(),
                color = palette.kinhRow,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Text(
                text = stringResource(R.string.kinh_row_suffix),
                color = palette.dialogText,
                fontSize = 16.sp,
            )
            Text(
                text = stringResource(R.string.kinh_shout),
                color = palette.subtitle,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.buttonPrimary,
                    contentColor = Color.White,
                ),
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.kinh_dismiss),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}
