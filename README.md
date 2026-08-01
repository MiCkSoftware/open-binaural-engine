# Open Binaural Engine

Open Binaural Engine is a small, platform-independent Kotlin/JVM library that renders
real-time stereo PCM for binaural-beat experiments. It has no Android dependency and
does not open, route, or play audio by itself.

For a carrier frequency `C` and requested beat frequency `B`, it generates:

- left: `C - B / 2`
- right: `C + B / 2`

This keeps the perceived carrier centered while maintaining a frequency difference of
`B` hertz between the ears.

## Usage

```kotlin
val signal = BinauralSignal(
    carrierHz = 400.0,
    beatHz = 8.0,
    noiseColor = NoiseColor.PINK,
    noiseMix = 0.3,
)
val generator = BinauralGenerator(
    sampleRateHz = 48_000,
    initialSignal = signal,
)
generator.fadeTo(targetGain = 0.25, durationFrames = 96_000)

val interleavedStereoPcm = FloatArray(512 * 2)
generator.renderInterleaved(interleavedStereoPcm)
```

Call `renderInterleaved` repeatedly from one audio-rendering thread. The generator
keeps oscillator phase, noise filters, transitions, and fades continuous across blocks.
Use `transitionTo` for click-free live changes and `reset` to begin a new stream.

Proposed application presets and the evidence supporting—or not supporting—each
parameter are documented in [PRESETS.md](PRESETS.md).

## Signal and safety notes

- Binaural beats require separate left and right channels; stereo headphones are
  recommended.
- White, pink, and brown noise are generated algorithmically. Noise is correlated
  between the left and right channels so it does not add an unrelated stereo image.
- The generated floating-point PCM remains in `[-1, 1]`, with mix headroom.
- Start at a low listening level. The library cannot determine a safe acoustic output
  level because that depends on the amplifier and headphones.
- This software is an experimental audio generator. It does not diagnose, treat, cure,
  or prevent any medical condition, and it makes no claim of neurological efficacy.

## Building

```shell
./gradlew test
```

The public API is Kotlin/JVM and targets Java 11. Rendering is allocation-free, but the
generator is intentionally not thread-safe; configuration and rendering calls must be
serialized by the host.

## License

Copyright 2026 MiCkSoftware

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
