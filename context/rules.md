# Rules

## Tech Stack Constraints

- **Language:** Kotlin only. No Java.
- **UI:** Jetpack Compose + Material 3. No XML layouts.
- **Min SDK:** 26 (Android 8.0). No APIs below this.
- **DI:** Hilt only. No manual DI or other containers.
- **Async:** Kotlin Coroutines + StateFlow/SharedFlow. No RxJava, no LiveData.
- **Build:** Kotlin DSL (`build.gradle.kts`) + Version Catalog (`gradle/libs.versions.toml`).

## Architecture Rules

- Dependencies flow inward: `presentation` → `domain` ← `data`. Never sideways.
- Repository **interfaces** live in `domain`. Implementations live in `data`.
- ViewModels depend on use cases, not repositories directly.
- Domain models must be plain Kotlin classes — no Retrofit, Room, or Android annotations.
- Map DTOs/entities → domain models at the repository boundary.

## Kotlin

- Prefer `val` over `var`. Use `var` only when mutation is required.
- Use `data class` for domain models and UI state.
- Name parameters explicitly for constructors/functions with 3+ arguments.
- Use sealed classes for UI state: `Loading`, `Success(data: T)`, `Error(message: String)`.
- Avoid nullable types (`T?`) except at system boundaries. Unwrap immediately.

## Compose

- One screen-level composable per file. Small helpers can live in the same file.
- Pass navigation callbacks as lambdas. Never pass `NavController` into a composable.
- Hoist all mutable state to the ViewModel. Composables should be stateless.
- Provide `@Preview` with both light and dark variants.
- Use `remember { derivedStateOf { } }` for computed values to avoid redundant recompositions.

## ViewModel

- Expose UI state as `StateFlow<ScreenUiState>` (sealed class).
- Expose one-shot events (navigate, toast) as `SharedFlow<UiEvent>`.
- Never reference `Context`, `View`, or lifecycle objects inside a ViewModel.

## Testing

- Unit test ViewModels and use cases with JUnit 5 + MockK + fakes (not mocks) for repos.
- UI tests use the Compose testing API (`createComposeRule`).
- Shared fakes and builders go in `:core:testing`.
