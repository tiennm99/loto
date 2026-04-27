package com.miti99.loto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.miti99.loto.R

/**
 * App bar — empty center title (wordmark lives in the content area below),
 * settings gear icon on the trailing end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotoTopBar(onSettingsClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { /* wordmark is in the scrollable content below */ },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        },
    )
}
