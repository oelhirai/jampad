# JamPad — Design Brief

## The Idea

JamPad answers one question: *"What would this riff sound like with a full band?"*

You record a guitar riff on your phone, the app detects the tempo and key, and you
layer digital drums and bass on top — all without leaving one screen. It's a loop pedal
that grew a drum machine and a bass player. The goal is jamming, not producing.

---

## Core Mechanic: The Always-Running Loop

The loop is the heartbeat of the app. Once you record, it plays continuously. Drums,
bass, re-recording — everything happens while the loop is rolling. There are no separate
"record" and "edit" modes. You're always in **jam mode**.

The Big Button at the bottom drives the primary state machine:
- **Empty** → Tap to start recording
- **Recording** → Tap to set the loop point and start looping
- **Looping** → Tap to overdub another guitar layer
- **Overdubbing** → Tap to stop overdub and return to looping

The loop never stops once it starts. You interact with different zones of the screen
(waveform, drums, bass, mixer) while the music keeps playing.

---

## UX Principles

1. **One screen, always jamming** — no mode-switching, no navigation. The loop is the center. Everything revolves around it.
2. **Guitar-first** — recording is the entry point. Drums and bass appear only after you have a loop, because now they're relevant.
3. **Instant gratification** — tap a drum preset, hear it immediately. Pick a bass pattern, it plays on the next bar. Zero delay between intention and result.
4. **Simple controls, real musicianship** — step sequencer for precision, tap pads for feel. Both serve different creative impulses.

---

## Visual Design Direction

### Palette

Dark theme — standard for music apps, reduces eye strain during creative sessions,
makes accent colors pop.

| Role | Hex | Usage |
|---|---|---|
| Background | `#121212` | Main canvas |
| Surface | `#1E1E1E` | Cards, panels |
| Surface variant | `#2A2A2A` | Elevated panels, active cells |
| Guitar accent | `#FFB74D` | Amber — guitar waveform, guitar layer controls |
| Drums accent | `#26C6DA` | Cyan — drum pads, drum grid active cells |
| Bass accent | `#AB47BC` | Purple — bass controls, bass pattern highlights |
| Primary | `#26C6DA` | Primary actions (uses drums cyan for energy) |
| On-surface text | `#E0E0E0` | Body copy |
| Muted text | `#757575` | Labels, captions |
| Record red | `#EF5350` | Record button active state |

### Typography

- **Display / BPM / bar count:** System monospace or bold sans — utilitarian, control-panel feel
- **Body:** System default sans (Roboto on Android)
- Sizes follow Material 3 type scale

### Shape & Elevation

- Pad buttons: 12dp rounded corners, tactile feel
- Panels/cards: 16dp rounded corners
- Big Button: circular, 72dp diameter
- Elevation: minimal — use surface color differentiation, not shadows. Dark themes look better flat.

### Motion

- Playhead animates smoothly across the waveform strip
- Drum pad cells flash on beat hit
- Big Button pulses gently when recording (red glow)
- Layer controls slide in when first loop is set (drums/bass appear)
- No gratuitous animation — every motion has rhythmic or functional purpose

---

## Screens

### Screen 1: Loop Station (Main — and only — screen)

**Purpose:** The entire jam experience on one screen.

```
┌─────────────────────────────────────┐
│  JamPad          120 BPM   4 bars ⚙ │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │  [waveform / loop visualizer] │  │
│  │  ▶─────────●──────────────    │  │
│  └───────────────────────────────┘  │
│                                     │
│  [🎸 Guitar]  [🥁 Drums]  [🎸 Bass] │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  [contextual control area]    │  │
│  └───────────────────────────────┘  │
│                                     │
│  🔊━━━●━━━ 🔊━━●━━━━ 🔊━━━━●━━━  │
│  Guitar    Drums     Bass         │
│                                     │
│           [ ● REC ]                 │
└─────────────────────────────────────┘
```

**Key elements:**
- Header bar: BPM (tap to edit via tap-tempo or manual), bar count chips (4/8/16), settings gear
- Waveform strip: guitar loop visualization with sweeping playhead and beat markers
- Layer tabs: Guitar / Drums / Bass — swap the contextual control area
- Mini mixer: always-visible volume sliders + mute per layer
- Big Button: context-aware primary action (REC / STOP / OVERDUB)

**Primary action:** Big Button → Record / Loop / Overdub

---

### Screen 1a: Guitar Tab (contextual area)

**Purpose:** Show detected audio info, provide re-record/overdub controls.

**Key elements:**
- Detected key and tempo display: "Key: Am · 120 BPM"
- Simplified MIDI note visualization (detected pitches)
- Re-record (replace) / Overdub buttons
- Undo last overdub

---

### Screen 1b: Drums Tab — Step Sequencer (contextual area)

**Purpose:** Build drum patterns cell by cell.

```
┌─────────────────────────────────────┐
│  [Sequencer]  |  Pads               │
├─────────────────────────────────────┤
│        1  2  3  4  5  6  7  8  ...  │
│  Kick  ●  ·  ·  ·  ●  ·  ·  ·     │
│  Snare ·  ·  ·  ·  ●  ·  ·  ·     │
│  HiHat ●  ·  ●  ·  ●  ·  ●  ·     │
│  Clap  ·  ·  ·  ·  ·  ·  ·  ·     │
├─────────────────────────────────────┤
│  Preset: [Funk] [Lo-fi] [Rock]     │
│  [Clear]                            │
└─────────────────────────────────────┘
```

**Key elements:**
- Toggle cells (filled = hit, empty = silent)
- Grid resolution: 1/8th notes default (8 cells per bar)
- Preset patterns: Funk, Lo-fi, Rock — load into grid, then customize
- Clear button to reset
- Horizontal scroll for bars beyond screen width

---

### Screen 1c: Drums Tab — Tap Pad Mode (contextual area)

**Purpose:** Record drum hits in real-time by tapping pads while the loop plays.

```
┌─────────────────────────────────────┐
│  Sequencer  |  [Pads]               │
├─────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐        │
│  │   KICK   │  │  SNARE   │        │
│  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐        │
│  │  HI-HAT  │  │   CLAP   │        │
│  └──────────┘  └──────────┘        │
│                                     │
│  Quantize: [Off] [1/4] [1/8] [1/16]│
│  [Clear] [Undo]                     │
└─────────────────────────────────────┘
```

**Key elements:**
- Large tactile pads (min 72dp touch targets)
- Tap while loop plays → hit recorded at current beat position
- Quantize snaps taps to nearest grid division
- Pads flash cyan + haptic feedback on tap
- Clear / Undo controls

---

### Screen 1d: Bass Tab (contextual area)

**Purpose:** Configure bass line generation.

```
┌─────────────────────────────────────┐
│  Key: Am  (auto-detected)     [edit]│
├─────────────────────────────────────┤
│  Pattern:                           │
│  [Root]  [Root-Fifth]  [Octave]     │
│  [Walking]  [Funky]                 │
├─────────────────────────────────────┤
│  Style: [Funk] [Lo-fi] [Rock]      │
│                                     │
│  Bass line preview (mini note view) │
└─────────────────────────────────────┘
```

**Key elements:**
- Auto-detected key (manually overridable)
- Pattern type chips: Root, Root-Fifth, Octave, Walking, Funky
- Style chips: Funk, Lo-fi, Rock
- Bass line preview as simplified horizontal note blocks (purple)
- Changes take effect on next loop cycle

---

### Screen 2: Settings

**Purpose:** Audio and export configuration.

**Key elements:**
- Input source selection
- Monitor toggle (hear yourself while recording)
- Metronome click toggle
- Default export format (WAV / MP3)
- About / version

---

## Data Model

### Core entities

```kotlin
data class JamSession(
    val id: Long = 0,
    val bpm: Int,
    val barCount: Int,                   // 4, 8, or 16
    val detectedKey: MusicalKey?,
    val guitarAudioPath: String?,
    val drumPattern: DrumPattern,
    val bassConfig: BassConfig,
    val mixState: MixState,
    val createdAt: Long = System.currentTimeMillis()
)

data class DrumPattern(
    val hits: Map<DrumInstrument, BooleanArray>,
    val stepsPerBar: Int = 8,            // 8 or 16
)

enum class DrumInstrument { KICK, SNARE, HI_HAT, CLAP }

data class BassConfig(
    val key: MusicalKey,
    val pattern: BassPatternType,
    val style: MusicStyle,
)

enum class BassPatternType { ROOT, ROOT_FIFTH, OCTAVE, WALKING, FUNKY }
enum class MusicStyle { FUNK, LO_FI, ROCK }

enum class MusicalKey {
    C, C_SHARP, D, D_SHARP, E, F,
    F_SHARP, G, G_SHARP, A, A_SHARP, B
}

data class MixState(
    val guitarVolume: Float = 0.8f,
    val drumsVolume: Float = 0.7f,
    val bassVolume: Float = 0.7f,
    val guitarMuted: Boolean = false,
    val drumsMuted: Boolean = false,
    val bassMuted: Boolean = false,
)
```

---

## Technical Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Audio recording | `AudioRecord` API | Direct PCM access for pitch detection and mixing |
| Audio playback | Oboe (C++ via JNI) | Low-latency multi-track mixing, Google-recommended |
| Pitch detection | TarsosDSP (YIN algorithm) | Proven monophonic detection, Java, Android-compatible |
| Drum/bass sounds | Pre-recorded WAV samples | Simpler than synthesis, genre-authentic, sounds great |
| Audio mixing | Oboe mixer | Real-time PCM stream mixing at native sample rate |
| Export | WAV writer / MediaCodec (MP3) | WAV is trivial; MP3 via MediaCodec for sharing |
| Loop timing | Sample-accurate Oboe callback | Glitch-free looping requires sample-level precision |
| Tempo detection | Onset detection + autocorrelation | Standard BPM detection approach for audio |

---

## Phase Plan

| Phase | Goal | Key deliverable |
|---|---|---|
| 0 | Scaffold & AI workflow | Project builds, context files written |
| 1 | Foundation | Dark theme, Hilt, audio permissions, project skeleton |
| 2 | Audio loop engine | Record guitar, fixed-bar loop playback, waveform display |
| 3 | Tempo & key detection | Auto BPM + tap-tempo, pitch detection, manual override |
| 4 | Drum step sequencer | Grid UI, sample playback, preset patterns (funk/lo-fi/rock) |
| 5 | Drum tap pads | MPC-style pads, quantization, sequencer ↔ pad toggle |
| 6 | Bass generator | Pattern styles, key-aware generation, sample playback |
| 7 | Mixer & layering | 3-channel real-time mix, volume sliders, mute/unmute |
| 8 | Export | Mixdown to WAV/MP3, share intent |
| 9 | Session persistence | Save/load sessions via Room |
| 10 | Polish & release | Icons, onboarding, accessibility, Play Store |

---

## Tone & Copy

Utilitarian and musical. The app should feel like a piece of gear, not software.
Labels are short: "REC", "BPM", "KICK". No long explanations.
Empty states are inviting: "Record a riff to get started."
The vibe is a jam room, not a studio — loose, creative, fun.
