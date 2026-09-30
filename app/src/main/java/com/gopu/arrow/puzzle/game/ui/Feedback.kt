package com.gopu.arrow.puzzle.game.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Drives device vibration directly through the system [Vibrator] so feedback is
 * felt regardless of touch-sound settings. The in-app Haptics preference is the
 * only gate; every call is guarded against devices without a vibrator.
 */
class Haptics(private val context: Context, private val enabled: Boolean) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun buzz(durationMs: Long, amplitude: Int) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    fun success() = buzz(18, VibrationEffect.DEFAULT_AMPLITUDE)
    fun reject() = buzz(45, VibrationEffect.DEFAULT_AMPLITUDE)
    fun celebrate() = buzz(28, VibrationEffect.DEFAULT_AMPLITUDE)
    fun sample() = buzz(30, VibrationEffect.DEFAULT_AMPLITUDE)
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val context = LocalContext.current
    return remember(context, enabled) { Haptics(context, enabled) }
}

/**
 * Short synthesized tones for tap feedback, driven by [ToneGenerator] so the
 * game ships without bundled audio assets. Every call is guarded: audio that
 * fails to initialize simply stays silent and never blocks input.
 */
class Sounds(private val enabled: Boolean) {
    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    }.getOrNull()

    private fun play(toneType: Int, durationMs: Int) {
        if (!enabled) return
        runCatching { tone?.startTone(toneType, durationMs) }
    }

    fun success() = play(ToneGenerator.TONE_PROP_BEEP, 55)
    fun reject() = play(ToneGenerator.TONE_PROP_NACK, 130)
    fun celebrate() = play(ToneGenerator.TONE_PROP_BEEP, 120)
    fun sample() = play(ToneGenerator.TONE_PROP_BEEP, 80)

    /**
     * Rocket launch whoosh: a short synthesized rising sweep mixed with noise,
     * generated in memory so no audio asset is bundled. Failure is silent.
     */
    fun rocketLaunch() {
        if (!enabled) return
        runCatching {
            val sampleRate = 44_100
            val durationMs = 520
            val total = sampleRate * durationMs / 1000
            val samples = ShortArray(total)
            val random = java.util.Random()
            for (i in 0 until total) {
                val t = i.toFloat() / total
                val envelope = (1f - t) * (1f - t)
                val sweepHz = 180f + 1_100f * t * t
                val tone = kotlin.math.sin(2.0 * Math.PI * sweepHz * (i.toFloat() / sampleRate)).toFloat()
                val noise = random.nextFloat() * 2f - 1f
                val value = ((tone * 0.45f + noise * 0.55f) * envelope * 0.8f)
                    .coerceIn(-1f, 1f)
                samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread {
                runCatching { Thread.sleep((durationMs + 120).toLong()) }
                runCatching { track.release() }
            }.apply { isDaemon = true }.start()
        }
    }

    fun release() {
        runCatching { tone?.release() }
    }
}

@Composable
fun rememberSounds(enabled: Boolean): Sounds {
    val sounds = remember(enabled) { Sounds(enabled) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    return sounds
}

fun Modifier.shake(trigger: Int, magnitude: Float = 10f): Modifier = composed {
    if (trigger == 0) {
        this
    } else {
        val offset = remember { Animatable(0f) }
        LaunchedEffect(trigger) {
            offset.snapTo(0f)
            repeat(3) {
                offset.animateTo(magnitude, tween(45))
                offset.animateTo(-magnitude, tween(45))
            }
            offset.animateTo(0f, tween(45))
        }
        this.offset { IntOffset(offset.value.roundToInt(), 0) }
    }
}

/** One-shot emphasis used to draw attention to a hinted tile. */
fun Modifier.pulse(trigger: Int, scale: Float = 1.12f): Modifier = composed {
    if (trigger == 0) {
        this
    } else {
        val anim = remember { Animatable(1f) }
        LaunchedEffect(trigger) {
            anim.snapTo(1f)
            repeat(3) {
                anim.animateTo(scale, tween(160))
                anim.animateTo(1f, tween(160))
            }
        }
        graphicsLayer { scaleX = anim.value; scaleY = anim.value }
    }
}
