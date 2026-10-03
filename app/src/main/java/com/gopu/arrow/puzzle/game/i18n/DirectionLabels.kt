package com.gopu.arrow.puzzle.game.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gopu.arrow.puzzle.game.Direction
import com.gopu.arrow.puzzle.game.R

/**
 * How a [Direction] is spoken to a screen reader.
 *
 * The mapping lives here rather than on the enum on purpose. `Direction` is game
 * state - it is parsed out of the level JSON, compared in the reducer and written
 * into the level assets - so giving it a localised string would put a language
 * dependency in the engine and make the asset format locale-sensitive. The engine
 * keeps its four stable identifiers; only the screen announces them in words.
 *
 * [androidx.compose.ui.semantics.SemanticsPropertyReceiver.contentDescription]
 * is read outside a composable, so callers resolve these into a plain [String]
 * first.
 */
@Composable
fun Direction.spoken(): String = stringResource(
    when (this) {
        Direction.UP -> R.string.a11y_direction_up
        Direction.RIGHT -> R.string.a11y_direction_right
        Direction.DOWN -> R.string.a11y_direction_down
        Direction.LEFT -> R.string.a11y_direction_left
    }
)