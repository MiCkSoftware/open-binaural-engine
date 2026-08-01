// SPDX-License-Identifier: Apache-2.0
package com.micksoftware.openbinaural

import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stateful, allocation-free renderer of interleaved stereo floating-point PCM.
 *
 * This class deliberately performs no locking and must be owned by one rendering
 * thread. Configuration calls and [renderInterleaved] must not run concurrently.
 */
public class BinauralGenerator(
    public val sampleRateHz: Int,
    initialSignal: BinauralSignal,
    randomSeed: Long = System.nanoTime(),
) {
    private val random = Random(randomSeed)

    private var carrierHz = initialSignal.carrierHz
    private var beatHz = initialSignal.beatHz
    private var noiseMix = initialSignal.noiseMix
    private var targetCarrierHz = carrierHz
    private var targetBeatHz = beatHz
    private var targetNoiseMix = noiseMix
    private var carrierStep = 0.0
    private var beatStep = 0.0
    private var noiseMixStep = 0.0
    private var signalTransitionFrames = 0L

    private var leftPhase = 0.0
    private var rightPhase = 0.0

    private var gain = 0.0
    private var targetGain = 0.0
    private var gainStep = 0.0
    private var gainTransitionFrames = 0L

    private var noiseColor = initialSignal.noiseColor
    private var noiseGenerator = createNoise(initialSignal.noiseColor)
    private var nextNoiseColor = initialSignal.noiseColor
    private var nextNoiseGenerator = noiseGenerator
    private var noiseTransitionFrames = 0L
    private var noiseTransitionTotalFrames = 0L

    init {
        require(sampleRateHz > 0) { "sampleRateHz must be greater than zero" }
        validateForSampleRate(initialSignal)
    }

    /** The latest requested signal, including a transition currently in progress. */
    public val targetSignal: BinauralSignal
        get() = BinauralSignal(targetCarrierHz, targetBeatHz, nextNoiseColor, targetNoiseMix)

    /** The latest requested output gain. */
    public val requestedGain: Double
        get() = targetGain

    /**
     * Smoothly changes frequency, noise color and noise amount.
     *
     * A zero-frame transition applies the new values before the next rendered frame.
     */
    public fun transitionTo(signal: BinauralSignal, durationFrames: Long) {
        require(durationFrames >= 0L) { "durationFrames must be non-negative" }
        validateForSampleRate(signal)

        targetCarrierHz = signal.carrierHz
        targetBeatHz = signal.beatHz
        targetNoiseMix = signal.noiseMix
        signalTransitionFrames = durationFrames

        if (durationFrames == 0L) {
            carrierHz = targetCarrierHz
            beatHz = targetBeatHz
            noiseMix = targetNoiseMix
            carrierStep = 0.0
            beatStep = 0.0
            noiseMixStep = 0.0
        } else {
            carrierStep = (targetCarrierHz - carrierHz) / durationFrames
            beatStep = (targetBeatHz - beatHz) / durationFrames
            noiseMixStep = (targetNoiseMix - noiseMix) / durationFrames
        }

        if (signal.noiseColor != nextNoiseColor) {
            if (noiseTransitionFrames > 0L) {
                noiseColor = nextNoiseColor
                noiseGenerator = nextNoiseGenerator
            }
            nextNoiseColor = signal.noiseColor
            nextNoiseGenerator = createNoise(signal.noiseColor)
            noiseTransitionFrames = durationFrames
            noiseTransitionTotalFrames = durationFrames
            if (durationFrames == 0L) {
                noiseColor = nextNoiseColor
                noiseGenerator = nextNoiseGenerator
            }
        }
    }

    /** Smoothly changes the PCM amplitude. Zero is silence and one is full scale. */
    public fun fadeTo(targetGain: Double, durationFrames: Long) {
        require(targetGain.isFinite() && targetGain in 0.0..1.0) {
            "targetGain must be between zero and one"
        }
        require(durationFrames >= 0L) { "durationFrames must be non-negative" }

        this.targetGain = targetGain
        gainTransitionFrames = durationFrames
        if (durationFrames == 0L) {
            gain = targetGain
            gainStep = 0.0
        } else {
            gainStep = (targetGain - gain) / durationFrames
        }
    }

    /**
     * Writes [frameCount] stereo frames starting at [offsetFrames].
     * No temporary buffers or per-frame objects are allocated.
     */
    @JvmOverloads
    public fun renderInterleaved(
        output: FloatArray,
        offsetFrames: Int = 0,
        frameCount: Int = output.size / 2 - offsetFrames,
    ) {
        require(offsetFrames >= 0) { "offsetFrames must be non-negative" }
        require(frameCount >= 0) { "frameCount must be non-negative" }
        require(offsetFrames.toLong() + frameCount <= output.size.toLong() / 2L) {
            "The requested stereo frames do not fit in output"
        }

        var outputIndex = offsetFrames * 2
        repeat(frameCount) {
            if (signalTransitionFrames > 0L) {
                carrierHz += carrierStep
                beatHz += beatStep
                noiseMix += noiseMixStep
                signalTransitionFrames--
                if (signalTransitionFrames == 0L) {
                    carrierHz = targetCarrierHz
                    beatHz = targetBeatHz
                    noiseMix = targetNoiseMix
                }
            }

            if (gainTransitionFrames > 0L) {
                gain += gainStep
                gainTransitionFrames--
                if (gainTransitionFrames == 0L) {
                    gain = targetGain
                }
            }

            val leftFrequency = carrierHz - beatHz / 2.0
            val rightFrequency = carrierHz + beatHz / 2.0
            leftPhase += TWO_PI * leftFrequency / sampleRateHz
            rightPhase += TWO_PI * rightFrequency / sampleRateHz
            if (leftPhase >= TWO_PI) leftPhase %= TWO_PI
            if (rightPhase >= TWO_PI) rightPhase %= TWO_PI

            var noiseSample = noiseGenerator.next()
            if (noiseTransitionFrames > 0L) {
                val nextSample = nextNoiseGenerator.next()
                val progress = 1.0 - noiseTransitionFrames.toDouble() / noiseTransitionTotalFrames
                noiseSample = noiseSample * (1.0 - progress) + nextSample * progress
                noiseTransitionFrames--
                if (noiseTransitionFrames == 0L) {
                    noiseColor = nextNoiseColor
                    noiseGenerator = nextNoiseGenerator
                }
            }

            val effectiveNoiseMix = if (noiseColor == NoiseColor.NONE &&
                nextNoiseColor == NoiseColor.NONE
            ) {
                0.0
            } else {
                noiseMix
            }
            val toneWeight = cos(effectiveNoiseMix * HALF_PI)
            val noiseWeight = sin(effectiveNoiseMix * HALF_PI)
            val amplitude = gain * HEADROOM
            output[outputIndex++] = ((sin(leftPhase) * toneWeight + noiseSample * noiseWeight) *
                amplitude).toFloat()
            output[outputIndex++] = ((sin(rightPhase) * toneWeight + noiseSample * noiseWeight) *
                amplitude).toFloat()
        }
    }

    /** Resets phase, filters, random state and gain to a known initial state. */
    @JvmOverloads
    public fun reset(signal: BinauralSignal, initialGain: Double = 0.0, randomSeed: Long = 0L) {
        require(initialGain.isFinite() && initialGain in 0.0..1.0) {
            "initialGain must be between zero and one"
        }
        validateForSampleRate(signal)
        random.setSeed(randomSeed)
        carrierHz = signal.carrierHz
        beatHz = signal.beatHz
        noiseMix = signal.noiseMix
        targetCarrierHz = carrierHz
        targetBeatHz = beatHz
        targetNoiseMix = noiseMix
        carrierStep = 0.0
        beatStep = 0.0
        noiseMixStep = 0.0
        signalTransitionFrames = 0L
        leftPhase = 0.0
        rightPhase = 0.0
        gain = initialGain
        targetGain = initialGain
        gainStep = 0.0
        gainTransitionFrames = 0L
        noiseColor = signal.noiseColor
        noiseGenerator = createNoise(signal.noiseColor)
        nextNoiseColor = signal.noiseColor
        nextNoiseGenerator = noiseGenerator
        noiseTransitionFrames = 0L
        noiseTransitionTotalFrames = 0L
    }

    private fun validateForSampleRate(signal: BinauralSignal) {
        val leftFrequency = signal.carrierHz - signal.beatHz / 2.0
        val rightFrequency = signal.carrierHz + signal.beatHz / 2.0
        val nyquist = sampleRateHz / 2.0
        require(leftFrequency > 0.0) { "The derived left frequency must be greater than zero" }
        require(rightFrequency < nyquist) {
            "The derived right frequency must be below the Nyquist frequency"
        }
    }

    private fun createNoise(color: NoiseColor): ColoredNoise = when (color) {
        NoiseColor.NONE -> SilentNoise
        NoiseColor.WHITE -> WhiteNoise(Random(random.nextLong()))
        NoiseColor.PINK -> PinkNoise(Random(random.nextLong()))
        NoiseColor.BROWN -> BrownNoise(Random(random.nextLong()))
    }

    private fun interface ColoredNoise {
        fun next(): Double
    }

    private data object SilentNoise : ColoredNoise {
        override fun next(): Double = 0.0
    }

    private class WhiteNoise(private val random: Random) : ColoredNoise {
        override fun next(): Double = random.nextDouble() * 2.0 - 1.0
    }

    private class PinkNoise(private val random: Random) : ColoredNoise {
        private var b0 = 0.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var b3 = 0.0
        private var b4 = 0.0
        private var b5 = 0.0
        private var b6 = 0.0

        override fun next(): Double {
            val white = random.nextDouble() * 2.0 - 1.0
            b0 = 0.99886 * b0 + white * 0.0555179
            b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520
            b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522
            b5 = -0.7616 * b5 - white * 0.0168980
            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
            b6 = white * 0.115926
            return (pink * 0.11).coerceIn(-1.0, 1.0)
        }
    }

    private class BrownNoise(private val random: Random) : ColoredNoise {
        private var previous = 0.0

        override fun next(): Double {
            val white = random.nextDouble() * 2.0 - 1.0
            previous = (previous + white * 0.02) / 1.02
            return (previous * 3.5).coerceIn(-1.0, 1.0)
        }
    }

    private companion object {
        const val TWO_PI = 2.0 * PI
        const val HALF_PI = PI / 2.0
        const val HEADROOM = 0.7071067811865476
    }
}
