package com.miti99.loto.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.miti99.loto.ui.theme.EmptyCellPurple

/**
 * Composition local carrying the user-selected empty-cell background color.
 * Provided at root in [com.miti99.loto.ui.LotoAppRoot]; consumed by
 * [com.miti99.loto.ui.board.PlayerCell] and [com.miti99.loto.ui.master.MasterCell].
 */
val LocalEmptyCellColor = compositionLocalOf<Color> { EmptyCellPurple }
