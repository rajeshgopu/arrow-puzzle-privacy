package com.gopu.arrow.puzzle.game.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gopu.arrow.puzzle.game.R

/**
 * Display serif used for the ARROW PUZZLE wordmark. Playfair Display is
 * subset to Latin (see `tools/fonts/build_fonts.py`) and instanced to static
 * Bold and Black, so the two weights render identically on API 24 as well.
 */
val DisplaySerif = FontFamily(
    Font(R.font.playfair_display_bold, FontWeight.Bold),
    Font(R.font.playfair_display_black, FontWeight.Black)
)

/**
 * Interface sans used for every label, card and button. Poppins, subset to
 * Latin and kept as static Medium/SemiBold/Bold/ExtraBold.
 */
val UiSans = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)