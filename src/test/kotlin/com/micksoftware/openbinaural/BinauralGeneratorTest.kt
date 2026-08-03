// SPDX-License-Identifier: Apache-2.0
package com.micksoftware.openbinaural

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BinauralGeneratorTest {
    @Test
    fun centeredCarrierProducesExpectedChannelFrequencies() {
        val generator = BinauralGenerator(48_000, BinauralSignal(400.0, 8.0), 1L)
        generator.fadeTo(1.0, 0)
        val pcm = FloatArray(48_000 * 2)

        generator.renderInterleaved(pcm)

        assertEquals(396.0, dominantFrequency(pcm, 0), 1.0)
        assertEquals(404.0, dominantFrequency(pcm, 1), 1.0)
    }

    @Test
    fun renderingInChunksIsIdenticalToOneLargeBuffer() {
        val signal = BinauralSignal(400.0, 8.0, NoiseColor.PINK, 0.4)
        val wholeGenerator = BinauralGenerator(48_000, signal, 42L)
        val chunkedGenerator = BinauralGenerator(48_000, signal, 42L)
        wholeGenerator.fadeTo(0.7, 173)
        chunkedGenerator.fadeTo(0.7, 173)
        val whole = FloatArray(4096)
        val chunked = FloatArray(4096)

        wholeGenerator.renderInterleaved(whole)
        chunkedGenerator.renderInterleaved(chunked, 0, 511)
        chunkedGenerator.renderInterleaved(chunked, 511, 537)
        chunkedGenerator.renderInterleaved(chunked, 1048, 1000)

        assertArrayEquals(whole, chunked, 0.0f)
    }

    @Test
    fun fixedSeedMakesNoiseDeterministicAndCorrelatedAcrossChannels() {
        val signal = BinauralSignal(400.0, 8.0, NoiseColor.WHITE, 1.0)
        val first = BinauralGenerator(48_000, signal, 7L)
        val second = BinauralGenerator(48_000, signal, 7L)
        first.fadeTo(1.0, 0)
        second.fadeTo(1.0, 0)
        val firstPcm = FloatArray(2048)
        val secondPcm = FloatArray(2048)

        first.renderInterleaved(firstPcm)
        second.renderInterleaved(secondPcm)

        assertArrayEquals(firstPcm, secondPcm, 0.0f)
        for (index in firstPcm.indices step 2) {
            assertEquals(firstPcm[index], firstPcm[index + 1], 0.0f)
        }
    }

    @Test
    fun gainRampReachesTargetWithoutExceedingPcmBounds() {
        val generator = BinauralGenerator(48_000, BinauralSignal(400.0, 8.0), 1L)
        generator.fadeTo(1.0, 480)
        val pcm = FloatArray(4096)

        generator.renderInterleaved(pcm)

        assertTrue(pcm.all { it.isFinite() && abs(it) <= 1.0f })
        assertTrue(pcm.take(100).maxOf { abs(it) } < pcm.takeLast(1000).maxOf { abs(it) })
        assertEquals(1.0, generator.requestedGain, 0.0)
    }

    @Test
    fun liveTransitionRemainsBoundedAndReachesRequestedSignal() {
        val generator = BinauralGenerator(48_000, BinauralSignal(300.0, 4.0), 1L)
        generator.fadeTo(0.8, 0)
        val pcm = FloatArray(4096)
        generator.renderInterleaved(pcm, 0, 512)
        val target = BinauralSignal(600.0, 12.0, NoiseColor.BROWN, 0.6)

        generator.transitionTo(target, 480)
        generator.renderInterleaved(pcm, 512, 1536)

        assertEquals(target, generator.targetSignal)
        assertTrue(pcm.all { it.isFinite() && abs(it) <= 1.0f })
        assertTrue(abs(pcm[1024] - pcm[1022]) < 0.25f)
    }

    @Test
    fun frequencyTrajectoryAdvancesOnlyOnRenderedFrames() {
        val generator = BinauralGenerator(1_000, BinauralSignal(400.0, 8.0), 1L)
        generator.transitionFrequenciesTo(300.0, 8.0, 100)

        generator.renderInterleaved(FloatArray(100), frameCount = 50)

        assertEquals(350.0, generator.currentSignal.carrierHz, 0.000001)
        assertEquals(8.0, generator.currentSignal.beatHz, 0.0)
        assertEquals(300.0, generator.targetSignal.carrierHz, 0.0)
    }

    @Test
    fun noiseUpdateDoesNotRestartFrequencyTrajectory() {
        val generator = BinauralGenerator(1_000, BinauralSignal(400.0, 8.0), 1L)
        generator.transitionFrequenciesTo(300.0, 8.0, 100)
        generator.renderInterleaved(FloatArray(80), frameCount = 40)

        generator.transitionNoiseTo(NoiseColor.PINK, 0.4, 10)
        generator.renderInterleaved(FloatArray(120), frameCount = 60)

        assertEquals(300.0, generator.currentSignal.carrierHz, 0.000001)
        assertEquals(NoiseColor.PINK, generator.targetSignal.noiseColor)
        assertEquals(0.4, generator.targetSignal.noiseMix, 0.0)
    }

    @Test
    fun coloredNoiseHasExpectedRelativeSpectralSlope() {
        val white = octavePower(NoiseColor.WHITE)
        val pink = octavePower(NoiseColor.PINK)
        val brown = octavePower(NoiseColor.BROWN)

        assertTrue("white slope ${white.second - white.first}", abs(white.second - white.first) < 4.0)
        assertTrue("pink slope ${pink.second - pink.first}", pink.second - pink.first < -1.0)
        assertTrue("brown slope ${brown.second - brown.first}", brown.second - brown.first < -3.5)
    }

    @Test
    fun invalidConfigurationsAreRejected() {
        assertFails { BinauralSignal(0.0, 8.0) }
        assertFails { BinauralSignal(400.0, -1.0) }
        assertFails { BinauralSignal(400.0, 8.0, noiseMix = 1.1) }
        assertFails { BinauralGenerator(48_000, BinauralSignal(10.0, 40.0)) }
        assertFails { BinauralGenerator(48_000, BinauralSignal(23_999.0, 4.0)) }
        val generator = BinauralGenerator(48_000, BinauralSignal(400.0, 8.0))
        assertFails { generator.fadeTo(1.1, 0) }
        assertFails { generator.renderInterleaved(FloatArray(4), 1, 2) }
    }

    private fun dominantFrequency(pcm: FloatArray, channel: Int): Double {
        var bestFrequency = 0
        var bestPower = Double.NEGATIVE_INFINITY
        for (frequency in 380..420) {
            var real = 0.0
            var imaginary = 0.0
            for (frame in 0 until pcm.size / 2) {
                val angle = 2.0 * PI * frequency * frame / 48_000.0
                val sample = pcm[frame * 2 + channel]
                real += sample * cos(angle)
                imaginary -= sample * sin(angle)
            }
            val power = real * real + imaginary * imaginary
            if (power > bestPower) {
                bestPower = power
                bestFrequency = frequency
            }
        }
        return bestFrequency.toDouble()
    }

    private fun octavePower(color: NoiseColor): Pair<Double, Double> {
        val generator = BinauralGenerator(48_000, BinauralSignal(400.0, 0.0, color, 1.0), 9L)
        generator.fadeTo(1.0, 0)
        val pcm = FloatArray(32_768 * 2)
        generator.renderInterleaved(pcm)
        return bandPowerDb(pcm, 250, 500) to bandPowerDb(pcm, 2_000, 4_000)
    }

    private fun bandPowerDb(pcm: FloatArray, fromHz: Int, toHz: Int): Double {
        var total = 0.0
        var bins = 0
        for (frequency in fromHz until toHz step 25) {
            var real = 0.0
            var imaginary = 0.0
            for (frame in 2048 until pcm.size / 2) {
                val angle = 2.0 * PI * frequency * frame / 48_000.0
                val sample = pcm[frame * 2]
                real += sample * cos(angle)
                imaginary -= sample * sin(angle)
            }
            total += real * real + imaginary * imaginary
            bins++
        }
        return 10.0 * ln(total / bins) / ln(10.0)
    }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
