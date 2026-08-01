# Presets and scientific evidence

This document records the proposed Open Binaural Lab presets, the rationale behind
each parameter, and what the cited literature does and does not support. These are
editable experimental starting points, not validated treatment protocols or promises
that a particular mental state will occur.

## Evidence labels

The labels below are an editorial scale for comparing presets in this catalogue. They
are not formal GRADE ratings and do not rate the exact preset as a whole:

- **Good**: the target frequency band or outcome has comparatively stronger support
  in reviews, although the exact carrier, duration, envelope, and noise combination
  has not been validated.
- **Moderate**: some relevant studies exist, but results or protocols vary materially.
- **Limited**: evidence is sparse, indirect, or based on small studies.
- **Low**: the configuration is mainly an experimental product hypothesis.

No row should be described in the app as “scientifically proven.”

## Proposed built-in presets

| Preset | Carrier start | Carrier end | Beat | Duration | Intensity | Noise | Evidence | Rationale / source |
| --- | ---: | ---: | ---: | ---: | --- | --- | --- | --- |
| Focus | 400 Hz | 400 Hz | 14 Hz | 20 min | Progressive (3 min) | None | Good | Beta-band beats (approximately 13–30 Hz) are commonly studied for attention and cognition. Meta-analyses report aggregate effects, but frequency-specific findings remain mixed. [Garcia-Argibay et al. (2019)](#1-garcia-argibay-et-al-2019); [Basu & Banerjee (2023)](#3-basu-and-banerjee-2023). |
| Deep Focus | 450 Hz | 450 Hz | 18 Hz | 30 min | Progressive | White | Moderate | Uses the same beta-band rationale. White noise is included for auditory masking and comfort, not because it has been shown to strengthen a binaural-beat effect. [Garcia-Argibay et al. (2019)](#1-garcia-argibay-et-al-2019). |
| Creativity | 380 Hz | 380 Hz | 10 Hz | 20 min | Progressive | Pink | Moderate | Alpha-band stimulation has been investigated for creativity, but responses varied between individuals and did not improve every form of creative thinking. [Reedijk et al. (2013)](#4-reedijk-et-al-2013). |
| Relax | 400 Hz | 380 Hz | 8 Hz | 20 min | Progressive | Pink | Good | Alpha-range beats are frequently used in relaxation and anxiety research, but no optimal protocol has been established. The descending carrier and pink noise are comfort and masking choices, not validated active ingredients. [Garcia-Argibay et al. (2019)](#1-garcia-argibay-et-al-2019); [Platt & Hammond (2024)](#6-platt-and-hammond-2024). |
| Meditation | 400 Hz | 350 Hz | 6 Hz | 30 min | Slow progressive | Pink | Moderate | A 6 Hz theta beat has produced measurable theta-related responses in an individual study, while systematic evidence for reliable brainwave entrainment remains inconsistent. [Jirakittayakorn & Wongsawat (2017)](#5-jirakittayakorn-and-wongsawat-2017); [Ingendoh et al. (2023)](#2-ingendoh-et-al-2023). |
| Deep Meditation | 380 Hz | 320 Hz | 4 Hz | 30 min | Slow progressive | Brown | Limited | Evidence specific to low-theta/delta-border beats is sparse. Brown noise is a comfort and masking choice; no cited comparative evidence establishes it as superior to pink noise or as an entrainment enhancer. [Ingendoh et al. (2023)](#2-ingendoh-et-al-2023). |
| Sleep Onset | 350 Hz | 250 Hz | 3 Hz | 30 min | Slow progressive | Brown | Limited | Delta-range binaural-beat sleep studies remain limited and heterogeneous. Neither this 3 Hz value nor the descending carrier is a validated sleep-onset protocol. [Ingendoh et al. (2023)](#2-ingendoh-et-al-2023); [Fan et al. (2024)](#7-fan-et-al-2024). |
| Deep Sleep | 300 Hz | 180 Hz | 2 Hz | 45 min | Very slow progressive | Brown | Low | This is primarily an experimental configuration informed by the conventional EEG delta band, not by a controlled study of this exact signal. Recent sleep work uses different beats and carriers and has not established neural entrainment as the mechanism. [Fan et al. (2024)](#7-fan-et-al-2024). |

The engine centers each beat around the instantaneous carrier:

```text
leftHz  = carrierHz - beatHz / 2
rightHz = carrierHz + beatHz / 2
```

“Carrier start” and “carrier end” describe a linear, click-free transition orchestrated
by the host application over the full session. A changing carrier is an experimental
feature of this engine: the references below do not compare fixed and progressive
carriers.

“Progressive” describes a host-controlled gain envelope toward the user's selected
listening level. Only the Focus row currently specifies a three-minute ramp. The other
profile labels require explicit ramp durations before they become implementation-ready;
the literature does not provide a validated choice. Noise mix and acoustic output level
are intentionally not prescribed here. They must remain user-adjustable and conservative.

## What the literature supports

### Binaural beats overall

Garcia-Argibay et al. pooled 22 studies and reported a medium aggregate effect across
cognition, anxiety, and pain outcomes. Their meta-regression associated longer exposure
with larger effects and found no effectiveness advantage from masking beats with white
or pink noise. This supports investigating session duration; it does **not** validate a
universal 20–30 minute optimum or any complete preset in the table.
[Garcia-Argibay et al. (2019)](#1-garcia-argibay-et-al-2019)

Later reviews make the uncertainty explicit. Ingendoh et al. found the EEG entrainment
literature inconsistent and highly heterogeneous. Basu and Banerjee reported a medium
aggregate effect for memory and attention while also finding conflicting theta- and
beta-specific results. [Ingendoh et al. (2023)](#2-ingendoh-et-al-2023);
[Basu & Banerjee (2023)](#3-basu-and-banerjee-2023)

### Carrier frequency

Ingendoh et al.'s reviewed studies used carriers from 100 to 900 Hz; only four used
400 Hz. The review does not establish 400 Hz—or any other carrier—as optimal. A recent
sleep experiment explicitly states that its 250 Hz carrier was chosen from precedent,
not from a specific theoretical or empirical advantage.
[Ingendoh et al. (2023)](#2-ingendoh-et-al-2023);
[Fan et al. (2024)](#7-fan-et-al-2024)

Consequently, every carrier in the preset table is an app design choice for perception
and comfort. The descending carriers are experiments unique to the product until a
controlled fixed-versus-progressive comparison is available.

### White, pink, and brown noise

Noise can mask environmental distractions and make pure tones more comfortable. The
2019 meta-analysis found similar binaural-beat effects with and without white or pink
masking. It does not show that masking increases entrainment. No cited head-to-head
evidence shows that brown noise is superior to pink noise for binaural beats.

Noise itself must not be presented as a proven sleep aid. A systematic review rated the
evidence that continuous broadband noise improves sleep as very low and found outcomes
ranging from improvement to sleep disruption. [Riedy et al. (2021)](#8-riedy-et-al-2021)

### Scientific honesty in the app

Each preset detail should distinguish three layers:

1. **Established:** how a binaural beat is generated and the conventional EEG band in
   which the selected beat falls.
2. **Plausible but uncertain:** outcome-specific findings reported by the cited studies.
3. **Experimental:** the exact carrier, carrier sweep, duration, gain envelope, noise
   color, and noise mix selected by Open Binaural Lab.

All settings must remain editable. The app should invite users to evaluate their own
experience without implying diagnosis, treatment, neurological synchronization, or a
guaranteed effect.

## References

### 1. Garcia-Argibay et al. (2019)

Garcia-Argibay, M., Santed, M. A., & Reales, J. M. (2019). *Efficacy of
binaural auditory beats in cognition, anxiety, and pain perception: a meta-analysis.*
Psychological Research, 83, 357–372.
[DOI: 10.1007/s00426-018-1066-8](https://doi.org/10.1007/s00426-018-1066-8)

### 2. Ingendoh et al. (2023)

Ingendoh, R. M., Posny, E. S., & Heine, A. (2023). *Binaural beats to entrain
the brain? A systematic review of the effects of binaural beat stimulation on brain
oscillatory activity, and the implications for psychological research and intervention.*
PLOS ONE, 18(5), e0286023.
[DOI: 10.1371/journal.pone.0286023](https://doi.org/10.1371/journal.pone.0286023)

> Bibliographic correction: the 2023 systematic review describing 100–900 Hz carriers
> and heterogeneous protocols is by Ingendoh, Posny, and Heine, not Becher et al.
> [Becher et al. (2015)](https://doi.org/10.1111/ejn.12760) is a primary intracranial
> EEG study, not that systematic review.

### 3. Basu and Banerjee (2023)

Basu, S., & Banerjee, B. (2023). *Potential of binaural beats intervention for
improving memory and attention: insights from meta-analysis and systematic review.*
Psychological Research, 87, 951–963.
[DOI: 10.1007/s00426-022-01706-7](https://doi.org/10.1007/s00426-022-01706-7)

### 4. Reedijk et al. (2013)

Reedijk, S. A., Bolders, A., & Hommel, B. (2013). *The impact of binaural beats
on creativity.* Frontiers in Human Neuroscience, 7, 786.
[DOI: 10.3389/fnhum.2013.00786](https://doi.org/10.3389/fnhum.2013.00786)

### 5. Jirakittayakorn and Wongsawat (2017)

Jirakittayakorn, N., & Wongsawat, Y. (2017). *Brain responses to a 6-Hz binaural
beat: effects on general theta rhythm and frontal midline theta activity.* Frontiers
in Neuroscience, 11, 365.
[DOI: 10.3389/fnins.2017.00365](https://doi.org/10.3389/fnins.2017.00365)

### 6. Platt and Hammond (2024)

Platt, J., & Hammond, L. (2025; published online 2024). *Is non-clinical, personal use
of binaural beats audio an effective stress-management strategy? A systematic review
of randomised control trials.* Advances in Mental Health, 23(2), 258–286.
[DOI: 10.1080/18387357.2024.2374759](https://doi.org/10.1080/18387357.2024.2374759)

### 7. Fan et al. (2024)

Fan, Z., Zhu, Y., Suzuki, C., Suzuki, Y., Watanabe, Y., Watanabe, T., & Abe, T.
(2024). *Binaural beats at 0.25 Hz shorten the latency to slow-wave sleep during
daytime naps.* Scientific Reports, 14, 26062.
[DOI: 10.1038/s41598-024-76059-9](https://doi.org/10.1038/s41598-024-76059-9)

### 8. Riedy et al. (2021)

Riedy, S. M., Smith, M. G., Rocha, S., & Basner, M. (2021). *Noise as a sleep aid:
A systematic review.* Sleep Medicine Reviews, 55, 101385.
[DOI: 10.1016/j.smrv.2020.101385](https://doi.org/10.1016/j.smrv.2020.101385)

## Safety and scope

Use stereo headphones at a low, comfortable level. Stop listening if the sound causes
discomfort, dizziness, headache, anxiety, or sleep disruption. Do not use the presets
while driving or when attention to the environment is required. The engine cannot
measure sound pressure at the ear and does not provide medical advice.
