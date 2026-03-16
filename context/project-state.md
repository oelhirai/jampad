# Project State

## Overview

**App:** JamPad — record a guitar riff, layer digital drums and bass, jam on one screen.
**Package:** `com.jampad`
**Min SDK:** 26 · **Target SDK:** 35
**Stack:** Kotlin · Jetpack Compose · Clean Architecture (MVVM) · Hilt · Coroutines + Flow · Oboe · TarsosDSP

---

## Current Phase

**Phase 4 — Drum Step Sequencer** `✅ Done`

---

## Milestones

| # | Milestone | Status | Harvest | Notes |
|---|---|---|---|---|
| M0 | Scaffold & AI workflow | ✅ Done | ⬜ | Clean build verified |
| M1 | Foundation (dark theme, Hilt, permissions) | ✅ Done | ⬜ | Bundled with M0 scaffold |
| M2 | Audio loop engine | ✅ Done | ⬜ | Record, loop, overdub, waveform, playhead verified on device |
| M3 | Tempo & key detection | ⬜ Pending | ⬜ | Auto BPM, tap-tempo, pitch detection |
| M4 | Drum step sequencer | ✅ Done | ⬜ | Grid UI, synth samples, presets, verified on device |
| M5 | Drum tap pads | ⬜ Pending | ⬜ | MPC pads, quantize, toggle |
| M6 | Bass generator | ⬜ Pending | ⬜ | Pattern styles, key-aware |
| M7 | Mixer & layering | ⬜ Pending | ⬜ | 3-channel mix, volume, mute |
| M8 | Export | ⬜ Pending | ⬜ | WAV/MP3 mixdown, share |
| M9 | Session persistence | ⬜ Pending | ⬜ | Save/load via Room |
| M10 | Polish & release | ⬜ Pending | ⬜ | Icons, onboarding, Play Store |

**Status key:** ✅ Done · 🔄 In progress · ⬜ Pending · ❌ Blocked
**Harvest:** ✅ Done · ⬜ Pending — run before starting the next milestone

---

## Technical Decisions

| Decision | Choice | Status |
|---|---|---|
| Language | Kotlin | ✅ Locked |
| UI | Jetpack Compose + Material 3 (dark theme) | ✅ Locked |
| Architecture | Clean Architecture + MVVM | ✅ Locked |
| DI | Hilt | ✅ Locked |
| Async | Coroutines + StateFlow | ✅ Locked |
| Navigation | Navigation Compose | ✅ Locked |
| Audio recording | AudioRecord (PCM access) | ✅ Locked |
| Audio playback | Oboe (low-latency, C++ via JNI) | ⬜ TBD — evaluate vs AudioTrack in M2 |
| Pitch detection | TarsosDSP (YIN algorithm) | ⬜ TBD — evaluate in M3 |
| Drum/bass sounds | Pre-recorded WAV samples | ✅ Locked |
| Export format | WAV + MP3 (via MediaCodec) | ✅ Locked |
| Local DB | Room (deferred to M9) | ✅ Locked |
| Networking | None — fully offline | ✅ Locked |
| Cloud sync | None | ✅ Locked |
| Module structure | Single `:app` until build > 3 min | ✅ Locked |

---

## Module Structure

```
:app
  presentation/
    jam/              JamScreen, JamViewModel (main loop station)
    settings/         SettingsScreen, SettingsViewModel
    common/           shared UI composables (waveform, pads, mixer)
  domain/
    model/            JamSession, DrumPattern, BassConfig, MixState, etc.
    repository/       AudioRepository, SessionRepository (interfaces)
    usecase/          RecordLoop, DetectPitch, GenerateBass, ExportMix, etc.
  data/
    audio/            AudioRecorder, AudioPlayer, AudioMixer (Oboe wrappers)
    samples/          SampleBank (loads drum/bass WAV assets)
    detection/        PitchDetector, TempoDetector (TarsosDSP wrappers)
    repository/       AudioRepositoryImpl, SessionRepositoryImpl
    di/               AudioModule, RepositoryModule
```

---

## Phase Details

### M0 — Scaffold *(current)*
- [ ] AI workflow files (CLAUDE.md, context/, design-brief)
- [ ] Android project initializes and builds (`./gradlew assembleDebug`)
- [ ] Gradle version catalog wired (`gradle/libs.versions.toml`)

### M1 — Foundation
Build the skeleton: dark theme, permissions, Hilt wiring.

- [ ] Material 3 dark theme: color tokens (amber, cyan, purple on dark surface)
- [ ] Audio permission request flow (`RECORD_AUDIO`)
- [ ] Hilt end-to-end wiring
- [ ] Single-screen shell: header bar, empty waveform placeholder, Big Button
- [ ] Settings screen (placeholder)

Deliverable: app launches with dark theme, requests mic permission, shows empty jam screen.

### M2 — Audio Loop Engine
The core recording and playback loop — no detection, no layers yet.

- [ ] `AudioRecorder` wrapper around `AudioRecord` (16-bit PCM, 44.1kHz)
- [ ] Record guitar audio to in-memory buffer + file
- [ ] Fixed-bar loop: user selects 4/8/16 bars, sets BPM manually for now
- [ ] Loop playback engine (sample-accurate looping)
- [ ] Waveform visualization composable (renders PCM amplitudes)
- [ ] Playhead animation synced to loop position
- [ ] Big Button state machine: Empty → Recording → Looping → Overdub
- [ ] Overdub: mix new recording into existing loop audio
- [ ] Beat marker overlay on waveform

Deliverable: record a guitar riff, hear it loop seamlessly, see the waveform with playhead.

### M3 — Tempo & Key Detection
Make the app smart about what you played.

- [ ] BPM auto-detection (onset detection + autocorrelation on recorded audio)
- [ ] Tap-tempo: tap Big Button rhythmically before recording to set BPM
- [ ] Manual BPM entry (tap BPM display → number input)
- [ ] Monophonic pitch detection via TarsosDSP (YIN algorithm)
- [ ] Key estimation from detected pitches (most likely key/scale)
- [ ] Guitar tab: display detected key, tempo, simplified note visualization
- [ ] Manual key override (dropdown)

Deliverable: record a riff, app detects ~correct BPM and key, displays in Guitar tab.

### M4 — Drum Step Sequencer
First half of the drum machine.

- [ ] Drum sample assets: kick, snare, hi-hat, clap × 3 styles (funk, lo-fi, rock)
- [ ] `SampleBank`: load WAV samples from assets into memory
- [ ] Step sequencer grid composable (instrument rows × beat columns)
- [ ] Toggle cells on tap (visual + audio preview of the hit)
- [ ] Drum playback engine: trigger samples at correct beat positions during loop
- [ ] Preset patterns: Funk, Lo-fi, Rock — load into grid
- [ ] Clear button
- [ ] Drums tab UI with sequencer/pads toggle (pads disabled until M5)

Deliverable: load a funk drum preset, see it in the grid, hear drums playing over your guitar loop.

### M5 — Drum Tap Pads
Second half — live performance input.

- [ ] 2×2 pad grid composable (KICK, SNARE, HI-HAT, CLAP)
- [ ] Tap pad → trigger sample immediately + record hit at current loop position
- [ ] Quantization: snap recorded hits to nearest 1/4, 1/8, or 1/16 grid
- [ ] Quantize off option (free timing)
- [ ] Visual feedback: pad flash (cyan) + haptic on tap
- [ ] Sequencer ↔ Pad toggle (both edit the same underlying DrumPattern)
- [ ] Undo last tap / Clear
- [ ] Hits recorded in pad mode appear in sequencer grid when switching back

Deliverable: tap pads in real-time while loop plays, hits are quantized and visible in sequencer.

### M6 — Bass Generator
Auto-generated bass lines that follow your key.

- [ ] Bass sample assets: single bass notes across 1–2 octaves × 3 styles
- [ ] Bass pattern engine: generate note sequences from key + pattern type + style
- [ ] Pattern types: Root, Root-Fifth, Octave, Walking, Funky
- [ ] Style variations: Funk, Lo-fi, Rock (different samples + rhythm feel)
- [ ] Bass tab UI: key display, pattern chips, style chips, mini note preview
- [ ] Bass playback: trigger bass samples at correct pitches during loop
- [ ] Changes apply on next loop cycle (not mid-loop)

Deliverable: select a bass pattern + style, hear a bass line matching your detected key.

### M7 — Mixer & Layering
Bring all three layers together with proper mixing.

- [ ] Mini mixer composable: 3 horizontal sliders (guitar/drums/bass)
- [ ] Per-layer volume control (0.0–1.0)
- [ ] Per-layer mute/unmute (tap icon to toggle)
- [ ] Real-time audio mixing of all active layers
- [ ] Visual: slider colors match layer accents (amber/cyan/purple)
- [ ] Visual: muted layers dim their entire section

Deliverable: adjust guitar/drums/bass volumes independently, mute individual layers.

### M8 — Export
Get the jam off the phone.

- [ ] Mix down all layers to single audio buffer
- [ ] Write WAV file to shared storage / MediaStore
- [ ] MP3 encoding via MediaCodec
- [ ] Share intent (share WAV/MP3 to any app)
- [ ] Export triggered from Big Button long-press menu or settings
- [ ] Progress indicator during mixdown

Deliverable: export a jam as WAV or MP3, share it.

### M9 — Session Persistence
Come back to a jam later.

- [ ] Room database: `JamSessionEntity` (serialize DrumPattern + BassConfig as JSON)
- [ ] Save current session (audio file path + all settings)
- [ ] Load session list screen
- [ ] Resume a saved session
- [ ] Delete saved sessions
- [ ] "New Session" clears state

Deliverable: save a jam, close the app, reopen and pick up where you left off.

### M10 — Polish & Release
Ship it.

- [ ] App icon + adaptive icon
- [ ] First-launch onboarding (1–2 screens: "Record → Layer → Jam")
- [ ] Accessibility audit (TalkBack, content descriptions, touch targets)
- [ ] Performance profiling (audio latency, UI jank)
- [ ] Release build config + ProGuard rules
- [ ] Play Store assets

---

## Open Questions

| Question | Options | Priority |
|---|---|---|
| Oboe vs AudioTrack for playback | Oboe (low-latency, complex) vs AudioTrack (simpler, higher latency) | High — decide in M2 |
| TarsosDSP vs ML Kit for pitch | TarsosDSP (proven) vs ML Kit Audio (newer) | Medium — evaluate in M3 |
| Polyphonic detection | Monophonic only vs basic chord detection | Low — mono for MVP, chords V2 |
| MIDI export | Add MIDI file export alongside audio | Low — audio-only for MVP |
| Additional drum instruments | 4 instruments vs expandable kit | Low — 4 for MVP |
| Time signature | 4/4 only vs configurable | Low — 4/4 only for MVP |
| Undo depth | Single undo vs multi-level undo stack | Medium — single for MVP |
