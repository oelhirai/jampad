# CLAUDE.md

Guidance for Claude Code when working in this project.

## Session Start

The workspace `../CLAUDE.md` handles shared context automatically.
Read these project-specific files at the start of every session:

1. `context/rules.md` — this project's coding standards
2. `context/project-state.md` — current progress and open decisions
3. `context/skills.md` — project-specific templates
4. `context/learnings.md` — project-specific insights

> `context/design-brief.md` exists but is **not read every session** — consult it
> when making UX or data-model decisions, not as routine context.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Test
./gradlew test
./gradlew :app:test --tests "com.jampad.SomeTest"
./gradlew :app:test --tests "com.jampad.SomeTest.methodName"
./gradlew connectedAndroidTest

# Quality
./gradlew lint
./gradlew check
```

Fix common setup issues:
```bash
gradle wrapper --gradle-version 8.10
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Architecture

JamPad is a single-screen loop station for jamming. Record a guitar riff, layer drums
and bass, mix and export — all from one screen with an always-running loop.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Clean Architecture (MVVM) · Hilt · Coroutines + Flow · Oboe · TarsosDSP

**Layer flow:** `presentation` → `domain` ← `data`

| Layer | Package | Responsibility |
|---|---|---|
| `presentation` | `com.jampad.presentation` | ViewModels, Compose screens, UI state |
| `domain` | `com.jampad.domain` | Use cases, domain models, repository interfaces |
| `data` | `com.jampad.data` | Audio engine, sample bank, pitch detection, repository impls |

Dependencies point inward only. `data` and `presentation` depend on `domain`; never on each other.

## Feature Addition Workflow

1. Define domain model (plain Kotlin `data class`)
2. Define repository interface in `domain`
3. Implement repository in `data` (map entities/DTOs → domain at the boundary)
4. Write use case(s)
5. Wire Hilt bindings (`@Binds` or `@Provides`)
6. Create ViewModel: `StateFlow<UiState>` + `SharedFlow<UiEvent>`
7. Build Compose screen (stateful wrapper + stateless content composable)
8. Register navigation destination
9. Write unit tests for use case and ViewModel

## Audio-specific Notes

- All audio work happens on dedicated threads (never main/UI thread)
- Use `Dispatchers.IO` for file I/O, dedicated audio thread for recording/playback
- Sample rate: 44100 Hz, 16-bit PCM, mono for recording
- Drum/bass samples stored as WAV assets in `assets/samples/`
- Loop timing must be sample-accurate — calculate exact sample counts from BPM + bar count

## When Marking a Milestone ✅ Done

Do not start the next milestone until these steps are complete:

1. Update the milestone `Status` to ✅ Done in `context/project-state.md`
2. Run the harvest (see below), then mark `Harvest` ✅ in the same row
3. Update any resolved decisions or new open questions in `context/project-state.md`

### Harvest Protocol

Scan `context/learnings.md` and `context/skills.md` for anything generalizable:

- **Each learning:** would this prevent a mistake or save time on *any* future Android project?
  If yes → add it to `../shared/learnings.md` (include `**App discovered in:**` attribution)
- **Each skill:** is this pattern not already covered in `../shared/skills.md`?
  If yes → add it there under the appropriate section heading

Be decisive. The bar for shared/ is "useful to any Android project", not "specific to this app."
When in doubt, promote — a shared file that's slightly too full is better than knowledge that gets lost.
