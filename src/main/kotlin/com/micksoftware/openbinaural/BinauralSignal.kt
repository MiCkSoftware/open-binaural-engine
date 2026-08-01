// SPDX-License-Identifier: Apache-2.0
package com.micksoftware.openbinaural

/**
 * Describes a centered binaural signal.
 *
 * The left frequency is `carrierHz - beatHz / 2` and the right frequency is
 * `carrierHz + beatHz / 2`. [noiseMix] is an equal-power mix: zero is a pure
 * carrier and one is noise only.
 */
public data class BinauralSignal(
    val carrierHz: Double,
    val beatHz: Double,
    val noiseColor: NoiseColor = NoiseColor.NONE,
    val noiseMix: Double = 0.0,
) {
    init {
        require(carrierHz.isFinite() && carrierHz > 0.0) {
            "carrierHz must be finite and greater than zero"
        }
        require(beatHz.isFinite() && beatHz >= 0.0) {
            "beatHz must be finite and non-negative"
        }
        require(noiseMix.isFinite() && noiseMix in 0.0..1.0) {
            "noiseMix must be between zero and one"
        }
    }
}

