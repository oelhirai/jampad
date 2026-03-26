# Learnings

Project-specific insights discovered during development of this app.
For cross-project Android knowledge, see `../../shared/learnings.md`.

---

## How to add a learning

When something surprises you, causes a bug, or takes more than one attempt — write it here.
If it would help any future Android project, **also add it to `../../shared/learnings.md`**.

```
## [Short title]
**Context:** What were you trying to do?
**Problem:** What went wrong or was surprising?
**Insight:** What you now understand.
**Code:** (optional) minimal example.
```

---

<!-- Learnings go below this line as the project progresses. Start empty. -->

## Compose drag gestures capture stale state
**Context:** Building a trim overlay with drag handles inside a Canvas.
**Problem:** Lambda passed to `pointerInput(Unit)` captures state at composition time. During a drag, the state values were always the initial values, causing handles to snap back.
**Insight:** Use a local `mutableFloatStateOf` for the drag value, sync external state changes via `LaunchedEffect`, and report changes via `onValueChange` callback during drag. The `pointerInput` key should be `Unit` (stable) — reactivity comes from the mutable state, not recomposition.
**Code:**
```kotlin
var localOffset by remember { mutableFloatStateOf(externalOffset) }
LaunchedEffect(externalOffset) { localOffset = externalOffset }
Canvas(modifier = Modifier.pointerInput(Unit) {
    detectHorizontalDragGestures { _, dragAmount ->
        localOffset = (localOffset + delta).coerceIn(0f, 1f)
        onOffsetChanged(localOffset)
    }
}) { /* draw using localOffset */ }
```

## Align-to-Grid beats trim for BPM-locked loops
**Context:** Needed to remove dead space from recordings and make loops match BPM exactly.
**Problem:** Trim handles (even with snap-to-beat) can't guarantee the resulting loop length equals exactly N bars at the detected BPM — it will always have rounding error that causes drift.
**Insight:** Instead of trimming, show a fixed-size window of exactly N bars (computed from BPM) and let the user scroll the full recording behind it. The loop is BPM-locked by construction since the window size is always `barCount * 4 * 60/bpm * sampleRate` samples.
