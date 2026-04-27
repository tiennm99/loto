package com.miti99.loto.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — sourced from the SvelteKit app's Tailwind tokens.
// Light/Dark variants are the same family one shade apart so the wordmark
// gradient and game tokens read consistently in either mode.

// Rose — wordmark start, generate button, accent action.
val BrandRoseLight = Color(0xFFF43F5E)   // rose-500
val BrandRoseDark = Color(0xFFFB7185)    // rose-400

// Amber — wordmark end, called-cell cream fill.
val BrandAmberLight = Color(0xFFF59E0B)  // amber-500
val BrandAmberDark = Color(0xFFFBBF24)   // amber-400

// Emerald — called numbers ≥ 50, draw button, completed-row cells.
val BrandEmeraldLight = Color(0xFF10B981)  // emerald-500
val BrandEmeraldDark = Color(0xFF34D399)   // emerald-400

// Pink — called numbers ≤ 49, modal subtitle.
val BrandPinkLight = Color(0xFFEC4899)   // pink-500
val BrandPinkDark = Color(0xFFF472B6)    // pink-400

// Purple — secondary brand (legacy Excel purple — also default empty cell).
val BrandPurpleLight = Color(0xFF8B5CF6) // violet-500
val BrandPurpleDark = Color(0xFFA78BFA)  // violet-400

// Red — last-draw ring, marked-but-incomplete cell text.
val BrandRedLight = Color(0xFFEF4444)    // red-500
val BrandRedDark = Color(0xFFF87171)     // red-400

// Indigo — player accent (settings highlight, "Tạo bảng mới" gradient).
val BrandIndigoLight = Color(0xFF6366F1) // indigo-500
val BrandIndigoDark = Color(0xFF818CF8)  // indigo-400

// Empty cell legacy default (Excel "Standard Color: Purple"). User-overridable
// via settings; mirrors web app's `--empty-cell-bg` CSS variable.
val EmptyCellPurple = Color(0xFF7030A0)

// Surfaces
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF1E293B)      // slate-800
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF0F172A)   // slate-900

// Cell tokens
val CalledCellCream = Color(0xFFFFFBEB)  // amber-50 — fill for called number tokens
val CalledCellCreamDark = Color(0xFFFEF3C7)  // amber-100 — slightly warmer in dark
