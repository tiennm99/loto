package com.miti99.loto.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import com.miti99.loto.state.MasterPanelViewModel
import com.miti99.loto.state.PlayerBoardViewModel
import com.miti99.loto.ui.master.MasterPanelScreen
import com.miti99.loto.ui.player.PlayerBoardScreen
import com.miti99.loto.ui.theme.LotoTheme

/**
 * Page composition, ported from `web/src/routes/+page.svelte`: header,
 * player board (mode ≠ master), master section (mode ≠ player), footer —
 * one shared scroll, content capped at the web's max-w-2xl.
 */
@Composable
fun LotoAppRoot(
    settings: Settings,
    playerViewModel: PlayerBoardViewModel,
    masterViewModel: MasterPanelViewModel,
    settingsButton: @Composable () -> Unit = {},
) {
    val palette = LotoTheme.palette
    // Hold the screen awake while numbers are still to be called.
    val master by masterViewModel.masterState.collectAsState()
    KeepScreenOn(enabled = shouldKeepScreenOn(settings.mode, master.remaining.size))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .safeDrawingPadding(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.widthIn(max = 672.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    PageHeader()
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        settingsButton()
                    }
                }
                if (settings.mode != AppMode.MASTER) {
                    PlayerBoardScreen(viewModel = playerViewModel, settings = settings)
                }
                if (settings.mode != AppMode.PLAYER) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (settings.mode == AppMode.BOTH) 40.dp else 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.master_heading).uppercase(),
                            color = palette.masterHeading,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.em,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        )
                        MasterPanelScreen(viewModel = masterViewModel, settings = settings)
                    }
                }
                PageFooter()
            }
        }
    }
}

@Composable
private fun PageHeader() {
    val palette = LotoTheme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = palette.title,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderRule()
            Text(
                text = stringResource(R.string.app_subtitle),
                color = palette.subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HeaderRule()
        }
    }
}

@Composable
private fun HeaderRule() {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(1.dp)
            .background(LotoTheme.palette.subtitle.copy(alpha = 0.6f)),
    )
}

@Composable
private fun PageFooter() {
    val palette = LotoTheme.palette
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.footer_inspiration),
            color = palette.subtitle,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.footer_credit),
            color = palette.subtitle,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    // Opens the browser; the app itself stays offline-only.
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://miti99.com".toUri()),
                        )
                    }
                },
        )
    }
}
