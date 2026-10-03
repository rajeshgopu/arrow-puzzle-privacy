package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.gopu.arrow.puzzle.game.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * How a tapped arrow is animated as it leaves the board.
 *
 * This is a presentation choice and nothing else: no puzzle, collision or level
 * completion logic reads it, and changing it mid-level affects only arrows
 * launched after the change.
 *
 * Every style is a [LaunchAnimationProfile] built by [launchProfile], so adding a
 * style is one constant here and one arm of that function — the board, the level
 * and the reducer are untouched.
 *
 * [labelRes] is a resource rather than a literal because the settings screen is
 * translated. What is persisted is [name], which is unchanged, so a style chosen
 * before this build still reads back correctly.
 */
enum class LaunchAnimationStyle(@StringRes val labelRes: Int, val implemented: Boolean) {

    /** No launch animation at all. */
    OFF(R.string.launch_style_off, true),

    /** A brief, quiet bloom on the arrow and nothing else. */
    MINIMAL(R.string.launch_style_minimal, true),

    /** A soft glow pulse on the tapped arrow, then the usual departure. */
    GLOW_PULSE(R.string.launch_style_glow_pulse, true),

    /** The glow pulse, plus sparks and a shock ring thrown off the arrow. */
    GLOW_ENERGY(R.string.launch_style_glow_energy, true),

    /** Everything, at its widest and brightest. */
    FULL(R.string.launch_style_full, true);

    /** The style's name in the player's language. */
    val label: String
        @Composable @ReadOnlyComposable get() = stringResource(labelRes)

    /** True when a valid tap plays anything at all. */
    val animatesTap: Boolean get() = implemented && this != OFF

    companion object {
        /**
         * What the game ships with.
         *
         * The glow pulse is the default so the effect is already there for
         * everyone who never opens the settings screen.
         */
        val Default = GLOW_PULSE

        /** Every option, in the order the settings screen lists them. */
        val options: List<LaunchAnimationStyle> = entries

        /** Reads a stored name back, falling back to [Default] for anything unknown. */
        fun fromName(value: String?): LaunchAnimationStyle =
            options.firstOrNull { it.name == value } ?: Default
    }
}

/**
 * What one style actually plays: a glow pulse, an energy burst, or both.
 *
 * A null [burst] means the style throws nothing off, which is how `Minimal` and
 * `Glow Pulse` stay quiet and `Glow + Energy` and `Full` do not. The run is timed
 * once, by [durationMillis], so the two halves of a style can never drift apart.
 */
internal data class LaunchAnimationProfile(
    val style: LaunchAnimationStyle,
    val pulse: TapGlowPulseConfig,
    val burst: TapEnergyBurstConfig?
) {
    /** How long the whole style runs for, in milliseconds. */
    val durationMillis: Int get() = pulse.durationMillis
}

/**
 * The look of each style.
 *
 * Every number below is either a duration or a multiplier on the tapped arrow's
 * own size, so a style is the same size relative to its arrow on any screen,
 * density, board size or arrow shape. Styles differ only in how far, how bright
 * and how long — never in how anything is measured.
 *
 * All four playing styles stay inside the same short run, because a launch
 * animation that stalls a tap is a launch animation nobody wants.
 */
internal fun launchProfile(style: LaunchAnimationStyle): LaunchAnimationProfile = when (style) {

    LaunchAnimationStyle.OFF -> LaunchAnimationProfile(
        style = style,
        pulse = TapGlowPulseConfig(
            durationMillis = 1,
            glowIntensity = 0f,
            ringIntensity = 0f,
            coreIntensity = 0f,
            arrowGlowBoost = 0f
        ),
        burst = null
    )

    // The quietest style that still reads as a launch: a small bloom, no ring,
    // no sparks, and the arrow barely lifting.
    LaunchAnimationStyle.MINIMAL -> LaunchAnimationProfile(
        style = style,
        pulse = TapGlowPulseConfig(
            durationMillis = 85,
            pulseRadiusMultiplier = 0.46f,
            glowRadiusMultiplier = 0.62f,
            ringRadiusMultiplier = 0.36f,
            ringThicknessMultiplier = 0.030f,
            coreRadiusMultiplier = 0.20f,
            glowIntensity = 0.26f,
            ringIntensity = 0f,
            coreIntensity = 0.16f,
            arrowGlowBoost = 0.16f
        ),
        burst = null
    )

    // The polished default: a soft glow, a subtle light ring and a hot centre,
    // all sized off the arrow and all gone inside a tenth of a second.
    LaunchAnimationStyle.GLOW_PULSE -> LaunchAnimationProfile(
        style = style,
        pulse = TapGlowPulseConfig(),
        burst = null
    )

    // The glow pulse, plus energy thrown off the arrow: sparks carrying the
    // colour outward and one shock ring running ahead of them.
    LaunchAnimationStyle.GLOW_ENERGY -> LaunchAnimationProfile(
        style = style,
        pulse = TapGlowPulseConfig(
            durationMillis = 115,
            pulseRadiusMultiplier = 1.02f,
            glowRadiusMultiplier = 1.55f,
            ringRadiusMultiplier = 0.78f,
            ringThicknessMultiplier = 0.058f,
            coreRadiusMultiplier = 0.36f,
            glowIntensity = 0.54f,
            ringIntensity = 0.36f,
            coreIntensity = 0.44f,
            arrowGlowBoost = 0.40f
        ),
        burst = TapEnergyBurstConfig(
            particleCount = 8,
            particleRadiusMultiplier = 0.055f,
            particleReachMultiplier = 1.25f,
            particleTrailMultiplier = 0.55f,
            particleIntensity = 0.55f,
            particleHeadIntensity = 0.60f,
            ringRadiusMultiplier = 1.15f,
            ringThicknessMultiplier = 0.035f,
            ringIntensity = 0.40f,
            ringCount = 1
        )
    )

    // Everything: the widest glow, the most sparks, two rings trailing each
    // other, and the strongest lift on the arrow itself.
    LaunchAnimationStyle.FULL -> LaunchAnimationProfile(
        style = style,
        pulse = TapGlowPulseConfig(
            durationMillis = 120,
            pulseRadiusMultiplier = 1.15f,
            glowRadiusMultiplier = 1.85f,
            ringRadiusMultiplier = 0.86f,
            ringThicknessMultiplier = 0.062f,
            coreRadiusMultiplier = 0.40f,
            glowIntensity = 0.58f,
            ringIntensity = 0.42f,
            coreIntensity = 0.52f,
            arrowGlowBoost = 0.46f
        ),
        burst = TapEnergyBurstConfig(
            particleCount = 12,
            particleRadiusMultiplier = 0.070f,
            particleReachMultiplier = 1.60f,
            particleTrailMultiplier = 0.70f,
            particleIntensity = 0.68f,
            particleHeadIntensity = 0.75f,
            ringRadiusMultiplier = 1.50f,
            ringThicknessMultiplier = 0.042f,
            ringIntensity = 0.50f,
            ringCount = 2,
            ringLag = 0.32f
        )
    )
}

/**
 * The one seam the board uses to animate a launch.
 *
 * The board asks for an effect and nothing more: [playTapEffect] takes the tapped
 * arrow and its colour, and this decides from the selected [style] what that
 * means. Where the effect is drawn, how long it lasts, how big it is and what it
 * looks like all live in the profile and the effect classes, so a new style is
 * added in [launchProfile] and nowhere in the board, the level or the reducer.
 *
 * A slot owns one pulse effect, one burst effect and one clock, all allocated
 * once. A tap re-arms a slot in place rather than creating one, and a slot at
 * rest is a single null check. Drawing allocates nothing at all, so a level of
 * taps stays cheap on a phone.
 *
 * @param scope where the short animations run; the caller's scope, so they are
 *  cancelled with the screen that started them.
 */
internal class LaunchAnimation(
    private val scope: CoroutineScope
) {

    /** The style currently in force. Set from composition, read when drawing. */
    var style: LaunchAnimationStyle = LaunchAnimationStyle.Default

    /**
     * One profile per style, built once.
     *
     * Held as an array keyed by ordinal so that reading the active style's look
     * during a frame is an index and not a lookup or an allocation.
     */
    private val profiles: Array<LaunchAnimationProfile> =
        Array(LaunchAnimationStyle.options.size) { index -> launchProfile(LaunchAnimationStyle.options[index]) }

    private val slots = Array(PulseSlots) { LaunchSlot() }
    private var cursor = 0

    /**
     * Plays the tap effect for [arrow] in [color].
     *
     * [tileIndex] is only used to route the brief brightness lift back to this
     * arrow while it flies; pass [NoTile] for an effect that marks no arrow.
     * Does nothing at all when the selected style does not animate taps, which
     * is what makes `Off` free rather than merely invisible.
     */
    fun playTapEffect(arrow: ArrowShape, color: Color, tileIndex: Int = NoTile) {
        if (!style.animatesTap) return

        val profile = profiles[style.ordinal]
        val slot = slots[cursor]
        cursor = (cursor + 1) % PulseSlots

        // Armed from the arrow itself: its own centre, its own rendered size and
        // its own colour, so nothing here has to know anything about the board.
        val center = arrow.pivot
        val size = arrow.unit
        slot.pulse.arm(center = center, arrowSize = size, color = color)
        slot.burst.arm(center = center, arrowSize = size, color = color)
        slot.owner = tileIndex
        slot.profile = profile

        scope.launch {
            slot.clock.snapTo(0f)
            slot.clock.animateTo(1f, tween(profile.durationMillis, easing = profile.pulse.expansionEasing))
        }
    }

    /** Draws every animation currently running, under the arrows they are marking. */
    fun draw(scope: DrawScope) {
        for (index in slots.indices) {
            val slot = slots[index]
            val profile = slot.profile ?: continue
            val progress = slot.clock.value
            if (progress >= 1f) continue
            slot.pulse.draw(scope, progress, profile.pulse)
            profile.burst?.let { slot.burst.draw(scope, progress, it) }
        }
    }

    /**
     * The extra glow the arrow at [tileIndex] gets for as long as its animation is
     * running, and zero at every other time.
     *
     * Read from the slot's own profile rather than the current [style], so an
     * animation already on screen finishes the way it started even if the player
     * switches to `Off` mid-run.
     */
    fun arrowGlowBoost(tileIndex: Int): Float {
        if (tileIndex == NoTile) return 0f
        var boost = 0f
        for (index in slots.indices) {
            val slot = slots[index]
            val profile = slot.profile ?: continue
            if (slot.owner != tileIndex) continue
            boost = max(boost, arrowGlowBoostAt(slot.clock.value, profile.pulse.arrowGlowBoost))
        }
        return boost
    }

    /** One in-flight animation: its two effects, its clock and what it is for. */
    private class LaunchSlot {
        val clock = Animatable(0f)
        val pulse = TapGlowPulseEffect()
        val burst = TapEnergyBurstEffect()

        /** The profile this slot is playing, or null while the slot is idle. */
        var profile: LaunchAnimationProfile? = null

        /** The arrow being lifted, or [NoTile] when the effect marks no arrow. */
        var owner: Int = NoTile
    }
}

/**
 * Remembers a [LaunchAnimation] for the current [style].
 *
 * The instance survives style changes so an animation already on screen finishes
 * instead of vanishing, and the new style applies from the next tap onwards — so
 * a change made in the settings overlay takes effect on the very next arrow, with
 * no restart. It is keyed on [levelId], so moving to another level starts with
 * nothing in flight. System-wide reduced motion turns the effect off outright
 * rather than leaving the player with a flash they have already opted out of.
 */
@Composable
internal fun rememberLaunchAnimation(
    style: LaunchAnimationStyle,
    levelId: String
): LaunchAnimation {
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberSystemReduceMotion()
    val animation = remember(levelId) { LaunchAnimation(scope) }
    SideEffect {
        animation.style = if (reduceMotion) LaunchAnimationStyle.OFF else style
    }
    return animation
}

/**
 * Taps far enough apart in practice that two animations never overlap, but three
 * slots means the tenth tap in a burst still starts clean instead of queueing
 * behind the one before it.
 */
private const val PulseSlots = 3

/** Tile index meaning "no arrow", for effects that mark nothing. */
internal const val NoTile = -1