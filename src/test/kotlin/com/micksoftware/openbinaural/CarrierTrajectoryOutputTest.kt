// SPDX-License-Identifier: Apache-2.0
package com.micksoftware.openbinaural

import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarrierTrajectoryOutputTest {
    @Test
    fun renderedChannelsFollowTrajectoryAtStartMiddleAndEnd() {
        val sampleRate = 8_000
        val seconds = 30
        for (start in listOf(100.0, 299.661415, 400.0, 440.0, 900.0)) {
            for (fall in listOf(0, -2, -4, -7)) {
                val end = start * 2.0.pow(fall / 12.0)
                val generator = BinauralGenerator(sampleRate, BinauralSignal(start, 8.0), 7L)
                generator.fadeTo(0.5, 0)
                generator.transitionFrequenciesTo(end, 8.0, (seconds * sampleRate).toLong())
                val pcm = FloatArray(seconds * sampleRate * 2)
                generator.renderInterleaved(pcm)
                for (second in listOf(0, 14, 29)) {
                    val expected = start + (end - start) * (second + 0.5) / seconds
                    val left = measuredFrequency(pcm, second * sampleRate, sampleRate, 0, sampleRate)
                    val right = measuredFrequency(pcm, second * sampleRate, sampleRate, 1, sampleRate)
                    assertEquals("left at $second s", expected - 4.0, left, 0.35)
                    assertEquals("right at $second s", expected + 4.0, right, 0.35)
                    assertEquals("fixed beat at $second s", 8.0, right - left, 0.1)
                }
                assertEquals(end, generator.currentSignal.carrierHz, 0.000001)
                assertTrue(pcm.all { it.isFinite() && abs(it) <= 0.5f })
                val maximumStep = 0.5 * 2.0 * Math.PI * (start + 4.0) / sampleRate + 0.00001
                for (i in 2 until pcm.size) assertTrue("amplitude discontinuity", abs(pcm[i] - pcm[i - 2]) <= maximumStep)
            }
        }
    }

    @Test
    fun suspendedRenderingResumesWithIdenticalPhaseAndTrajectory() {
        val signal = BinauralSignal(440.0, 8.0)
        val uninterrupted = BinauralGenerator(8_000, signal, 1L)
        val paused = BinauralGenerator(8_000, signal, 1L)
        listOf(uninterrupted, paused).forEach {
            it.fadeTo(0.4, 0)
            it.transitionFrequenciesTo(300.0, 8.0, 80_000)
        }
        val expected = FloatArray(160_000)
        uninterrupted.renderInterleaved(expected)
        val first = FloatArray(64_000)
        paused.renderInterleaved(first)
        val beforePause = paused.currentSignal
        // A suspended renderer supplies no frames: querying state must not advance it.
        repeat(100) { assertEquals(beforePause, paused.currentSignal) }
        val second = FloatArray(96_000)
        paused.renderInterleaved(second)
        assertArrayEquals(expected, first + second, 0f)
    }

    private fun measuredFrequency(pcm: FloatArray, offset: Int, frames: Int, channel: Int, rate: Int): Double {
        val crossings = mutableListOf<Double>()
        for (frame in offset + 1 until offset + frames) {
            val before = pcm[(frame - 1) * 2 + channel]
            val after = pcm[frame * 2 + channel]
            if (before <= 0 && after > 0) crossings += frame - 1.0 - before / (after - before)
        }
        assertTrue(crossings.size > 2)
        return (crossings.size - 1) * rate / (crossings.last() - crossings.first())
    }
}
