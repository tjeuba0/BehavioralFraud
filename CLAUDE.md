# CLAUDE.md - Project Instructions

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Morph MCP Priority (MANDATORY — subscription active)

- **ALWAYS** use `edit_file` from filesystem-with-morph for **ALL** file edits. **NEVER** use `replace_file_content` or `multi_replace_file_content`. `edit_file` is faster, more accurate, and prevents context pollution.
- **ALWAYS** use `warpgrep_codebase_search` from filesystem-with-morph as the **FIRST** search tool. Only fall back to `grep_search` or `find_by_name` for simple single-pattern lookups after `warpgrep` has been used for the initial research.
- Violation of these rules wastes the Morph subscription and is **NOT ACCEPTABLE**.

## Routing Rules

- **Prefer DIRECT EDIT (Claude + Morph)** for MOST tasks: code changes, analysis, refactors, API updates, renames, fixes, test creation, file exploration, and understanding codebase structure.
- **Use `warpgrep_codebase_search`** as the FIRST search tool for any code exploration or analysis.
- **Prefer Context7** when answers depend on **latest official docs** or **version-specific changes**.
- **One source per step**: do **not** double-ingest in the same step.
- **Claude Autonomy**: Trust Claude's judgment for most tasks — don't over-engineer routing for routine analysis.

## Agent Loop (6 Steps)

1) **Plan** – Restate objective; estimate scope/size. Save an outline to `.agent/plan.md`.
2) **Search** – Use `warpgrep_codebase_search` (Morph) for initial codebase research. Fall back to `grep_search` or `find_by_name` for simple single-pattern lookups.
3) **Retrieve** – Print a **Patch Preview** (unified diff) **without writing**. Save key findings to `.agent/findings.md` if needed.
4) **Propose** – Draft an **Action Plan** per file & hunk (batch if large). Update `.agent/plan.md`.
5) **Execute** – Use `edit_file` (Morph) to apply changes exactly as previewed. Export `git diff` as `.agent/patch.diff` and a file list as `.agent/changed-files.txt`.
6) **Validate** – If gates fail, loop back to (2) with narrowed scope/batches. If pass, produce a PR description from `.agent/plan.md`.

## Compliance Checklist

- [ ] Used `warpgrep_codebase_search` (Morph) as first search tool.
- [ ] Used `edit_file` (Morph) for all file edits. Never `replace_file_content` or `multi_replace_file_content`.
- [ ] **No double-ingest** in this step.
- [ ] **Relative paths** only (repo root) for all `include/targets`.
- [ ] Large change **batched** (≤6 files per iteration).
- [ ] Patch Preview **shown before writing**; after applying, exported `.agent/patch.diff` and `.agent/changed-files.txt`.
- [ ] Artifacts exist under `.agent/` (`findings.md`, `plan.md`) **before PR**.

## Working with AI Assistants

- **Morph MCP**: Dùng `warpgrep_codebase_search` để phân tích context, `edit_file` để implement
- **Claude**: Là người quyết định cuối cùng và implement giải pháp
- **Context7**: Dùng khi cần official docs mới nhất (optional)
- **Nguyên tắc**: Không thay đổi code đang hoạt động tốt chỉ vì expert suggestion
- **Approach**: Incremental improvements, không refactor toàn bộ

## Quick Checklist Before Coding

- [ ] Use `safeClickable` instead of `clickable` for all interactive components
- [ ] Component constants only for non-design values (opacity, animation duration, multipliers)
- [ ] Check if extension functions already exist before creating new ones
- [ ] Resolve resources in Composable scope, not in DrawScope
- [ ] Read `docs/tasks.md` before starting any task
- [ ] `withContext(Dispatchers.IO)` for all network/DataStore operations

## Scale-Adaptive Planning

Agent tự động điều chỉnh mức độ planning dựa trên độ phức tạp của task:

| Level | Scope | Action Required |
|-------|-------|-----------------|
| **0** | Bug fix, typo, minor tweak | Trực tiếp sửa, không cần plan |
| **1** | Single file change | Brief note trong commit message |
| **2** | Multi-file feature (2-5 files) | `.agent/plan.md` + task tracking |
| **3** | Cross-area refactor (>5 files) | Full implementation plan + architecture review |
| **4** | System redesign, breaking changes | PRD review + Architecture doc + stakeholder approval |

### Level Detection Heuristics

- **File count**: 1 file = L1, 2-5 files = L2, >5 files = L3+
- **Breaking changes**: Any public API change = L3+
- **Risk factor**: Behavioral collection, LLM integration, data storage changes = +1 level

### Artifacts by Level

| Level | task.md | plan.md | implementation_plan.md |
|-------|---------|---------|------------------------|
| 0-1   | ❌       | ❌       | ❌                      |
| 2     | ✅       | ✅       | ❌                      |
| 3+    | ✅       | ✅       | ✅                      |

### Documentation Priority (MANDATORY)

Before researching ANY feature, issue, or bug, ALWAYS read documentation FIRST in this order:

1. **Task Registry** (`docs/tasks.md`) — **ALWAYS READ FIRST**
   - Understand current task status, context, dependencies
   - Contains feature scope and known issues

2. **README.md** — For architecture, data collection details, demo flow
   - Full behavioral feature specs, POC vs production differences

3. **Source Code** — Only after understanding requirements from documentation

## Project Overview

**BehavioralFraud** is an Android POC app demonstrating behavioral fraud detection for Vietnamese banking regulation (Nghị định 50 / Thông tư 77). It collects behavioral biometrics (touch, sensor, keystroke rhythm, clipboard) during a mock bank transfer, sends feature vectors to a FastAPI backend for analysis, and returns a risk score — proving that the same transaction performed by a different person can be detected.

- **Package**: `com.poc.behavioralfraud`
- **Language**: Kotlin 1.9.24, JVM 11
- **Min SDK**: 26 (Android 8.0) / **Target SDK**: 34
- **UI**: Jetpack Compose + Material3
- **Single module** (`app`), single activity with Navigation Compose

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew installDebug           # Build and install on device
./gradlew lint                   # Run lint checks
./gradlew clean                  # Clean build
./gradlew test                   # Unit tests
```

## Setup Requirements

1. **Backend Base URL**: Add to `local.properties`:
   ```properties
   BACKEND_BASE_URL=http://10.0.2.2:8000
   ```
   > `10.0.2.2` is localhost of the host machine when running on Android emulator.
2. **Test on real device**: Emulator has no real gyroscope/accelerometer — sensor features will be unreliable.

## Key Documents & Task Registry

| Document | Path | Purpose |
|----------|------|---------|
| Task Registry | `docs/tasks.md` | All features with status and context |
| README | `README.md` | Full architecture, data collection, demo flow |

## Architecture

**Simplified MVI + Clean Architecture** — no DI framework (manual wiring via ViewModel constructors):

```
UI Layer (ui/screens/, ui/components/, ui/theme/)
  ↓ observes State (StateFlow), sends Events
ViewModel (ui/screens/TransferViewModel.kt)
  ↓ calls
Data Layer (data/collector/, data/repository/, data/scorer/)
  ↓ calls
Network Layer (network/BackendClient.kt)
  ↓ HTTPS
FastAPI Backend (DSPy → OpenRouter LLM)
```

### Key Directories

```
app/src/main/java/com/poc/behavioralfraud/
├── data/
│   ├── model/BehavioralModels.kt      → All data classes (touch events, features, profile, result)
│   ├── collector/BehavioralCollector.kt → Collects raw behavioral data (touch, text, sensor)
│   ├── repository/ProfileRepository.kt  → Stores/loads behavioral profile (DataStore)
│   └── scorer/LocalScorer.kt            → On-device feature extraction
├── network/
│   └── BackendClient.kt               → OkHttp + Gson client for FastAPI backend
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt              → Enrollment/Verification entry point
│   │   ├── TransferScreen.kt          → Main behavioral data collection UI
│   │   ├── TransferViewModel.kt       → Business logic + state management
│   │   └── ProfileScreen.kt           → View stored behavioral profile
│   ├── components/                    → Reusable Compose components
│   └── theme/Theme.kt                 → Material3 theme
└── MainActivity.kt                    → Single Activity + NavController setup
```

### Navigation

Uses **Navigation Compose** with a `NavController` in `MainActivity.kt`:

```
Home → Transfer (Enrollment 1/3) → Result → Home
Home → Transfer (Enrollment 2/3) → Result → Home
Home → Transfer (Enrollment 3/3) → Result (triggers profile creation) → Home
Home → Transfer (Verification) → Result (risk score) → Home
Home → Profile (view stored behavioral profile)
```

## Tech Stack

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Compose BOM | 2024.06.00 | UI framework |
| Navigation Compose | 2.7.7 | Screen navigation |
| ViewModel + Lifecycle | 2.8.3 | State management |
| OkHttp | 4.12.0 | HTTP client (no Retrofit) |
| Gson | 2.11.0 | JSON serialization |
| DataStore Preferences | 1.1.1 | Profile storage |
| Coroutines | (via lifecycle) | Async operations |

> **No Hilt, no Retrofit, no Firebase.** Keep it simple — this is a POC.

## Domain Vocabulary

| Term | Meaning |
|------|---------|
| Enrollment | 3 baseline transactions to build behavioral profile |
| Verification | Compare current session against stored profile |
| Behavioral Profile | LLM-generated summary of user's typing/touch patterns |
| Risk Score | 0–100 score from LLM: low = legitimate, high = fraud |
| Feature Vector | Extracted numeric behavioral metrics sent to LLM |
| BehavioralCollector | Class that intercepts touch/text/sensor events |
| LocalScorer | Feature extractor: converts raw events → feature vector |
| BackendClient | HTTP client that sends features to FastAPI backend |

## Behavioral Features Collected

| Feature | Unit | Fraud signal |
|---------|------|--------------|
| `sessionDurationMs` | ms | Too fast/slow vs baseline |
| `avgInterCharDelayMs` | ms | Typing rhythm |
| `stdInterCharDelayMs` | ms | Typing consistency |
| `pasteCount` | count | Paste instead of type |
| `avgTouchSize` | float | Finger size |
| `avgTouchDurationMs` | ms | Press duration |
| `avgSwipeVelocity` | px/s | Scroll behavior |
| `gyroStabilityX/Y/Z` | std dev | Device held vs on table |
| `accelStabilityX/Y/Z` | std dev | Movement pattern |
| `fieldFocusSequence` | string | Order of field entry |
| `timeToFirstInput` | ms | Hesitation before typing |

## Code Style Guidelines

- Kotlin code following official style guide
- Package naming: `com.poc.behavioralfraud`
- Class naming: PascalCase (`TransferViewModel`)
- Function/variable naming: camelCase (`extractFeatures`)
- Constants: UPPER_SNAKE_CASE in companion objects
- Error handling: Use `Result<T>` or sealed `UiState` pattern
- Limit file size to under 500 lines
- Prefer composition over inheritance
- Use coroutines for async operations
- **Single Declaration Rule**: Files with a single top-level class must be named after that declaration

## Component Development Guidelines

### Component Constants Pattern

Define constants as `private object` within component file:

```kotlin
// TransferScreen.kt
private object TransferScreenConstants {
    const val DEBOUNCE_DELAY_MS = 300L
    const val MAX_SESSION_DURATION_MS = 300_000L
}
```

Do NOT create separate constant files for individual components.

### Adaptive Height Requirements

**NEVER hardcode height values** for content-based components. Let components wrap their content:

```kotlin
// ❌ WRONG - Hardcoded height
Modifier.height(64.dp)

// ✅ CORRECT - Adaptive with padding
Modifier.padding(vertical = 16.dp)
```

### Click Safety Requirements

**ALWAYS use `safeClickable`** instead of `clickable` for all interactive components. Prevents double-tap issues critical for banking-adjacent apps:

```kotlin
// ✅ CORRECT
Modifier.safeClickable(onSafeClick = { handleClick() })

// ❌ WRONG
Modifier.clickable(onClick = { handleClick() })
```

## Jetpack Compose Best Practices

### Understand Compose Context Boundaries

- `@Composable` functions can only be called from other `@Composable` contexts
- `DrawScope` (inside `drawBehind`, `drawWithContent`, etc.) is NOT a Composable context
- Always resolve resources (colors, dimensions) in Composable scope, NOT in drawing operations

### Resource Resolution Pattern

```kotlin
// ✅ CORRECT - Resolve in Composable scope
val backgroundColor = MaterialTheme.colorScheme.surface
val primaryColor = MaterialTheme.colorScheme.primary

Box(
    modifier = Modifier.drawBehind {
        drawRect(color = backgroundColor) // Use pre-resolved
    }
)

// ❌ WRONG - Can't call @Composable in DrawScope
Box(
    modifier = Modifier.drawBehind {
        drawRect(color = MaterialTheme.colorScheme.surface) // ERROR
    }
)
```

## One-Time Events Pattern (CRITICAL)

**ALWAYS use Channel** for one-time events to prevent duplicate navigation/dialogs on configuration changes:

```kotlin
// ViewModel
private val _events = Channel<TransferEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

// Collector
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event -> handleEvent(event) }
    }
}
```

**Channel** (must NOT repeat on rotation): navigation, dialogs, confirmation triggers
**StateFlow** (must persist): loading state, risk score, UI text

## Action Handling in Compose Screens

For screens with multiple triggers, **always use a sealed class** for actions:

```kotlin
// ✅ CORRECT - sealed class for multiple actions
sealed class TransferScreenAction {
    object OnConfirmClicked : TransferScreenAction()
    object OnBackPressed : TransferScreenAction()
    data class OnFieldChanged(val field: String, val value: String) : TransferScreenAction()
}

@Composable
fun TransferScreen(
    state: TransferUiState,
    onAction: (TransferScreenAction) -> Unit
)

// Exception: single-action screens can use one lambda
@Composable
fun ResultScreen(
    riskScore: Int,
    onDismiss: () -> Unit
)
```

## Asynchronous Operations

- Use `suspend` functions for any I/O operations
- Operations that could take > 16ms should be suspend (frame drop threshold)
- `withContext(Dispatchers.IO)` for network/DataStore operations
- `viewModelScope.launch` — never `GlobalScope`

```kotlin
// ✅ CORRECT
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        backendClient.verifyTransaction(userId, features, profile)
    }
}

// ❌ WRONG
viewModelScope.launch {
    backendClient.verifyTransaction(userId, features, profile) // blocking on Main thread
}
```

## Do / Don't

### DO

- Follow existing patterns — this is a POC, keep it simple
- Handle ALL error states: loading, API error, no profile yet
- Use `withContext(Dispatchers.IO)` for all network/DataStore operations
- Use `viewModelScope.launch` — never `GlobalScope`
- Read `docs/tasks.md` before starting any task
- Run `./gradlew assembleDebug` after significant changes

### DON'T

- Don't add Hilt, Retrofit, or other heavy frameworks — keep POC minimal
- Don't hardcode API keys in source files
- Don't store raw behavioral data beyond the current session
- Don't use `GlobalScope.launch`
- Don't pass ViewModel into Composables directly
- Don't use `clickable` — use `safeClickable`
- Don't hardcode colors or dimensions — use `MaterialTheme` tokens

## Common Mistakes to Avoid

### 1. Calling network on Main thread

```kotlin
// ❌ WRONG
viewModelScope.launch { okHttpClient.newCall(...).execute() }

// ✅ CORRECT
viewModelScope.launch { withContext(Dispatchers.IO) { okHttpClient.newCall(...).execute() } }
```

### 2. Creating objects inside Composable body (causes recomposition)

```kotlin
// ❌ WRONG
val features = listOf(...) // recreated every recomposition

// ✅ CORRECT
val features = remember { listOf(...) }
```

### 3. Resolving resources in DrawScope

```kotlin
// ❌ WRONG
Modifier.drawBehind { drawRect(color = MaterialTheme.colorScheme.primary) }

// ✅ CORRECT
val color = MaterialTheme.colorScheme.primary
Modifier.drawBehind { drawRect(color = color) }
```

### 4. Using standard clickable

```kotlin
// ❌ WRONG
Modifier.clickable { onClick() }

// ✅ CORRECT
Modifier.safeClickable { onClick() }
```

### 5. Hardcoding height for content components

```kotlin
// ❌ WRONG
Modifier.height(64.dp)

// ✅ CORRECT
Modifier.padding(vertical = 16.dp)
```

### 6. Forgetting to unregister sensor listeners

**Wrong:** Register SensorManager listener but never unregister
**Right:** Unregister in `onStop`/`DisposableEffect` cleanup

### 7. Treating paste detection as guaranteed

`isPaste` (lengthDelta > 1) has false positives from IME auto-correct and Telex/VNI input. Document this in result UI.

## Current State

- Core behavioral collection: complete
- LLM enrollment + verification flow: complete
- Profile storage (DataStore): complete
- UI: functional, POC-level polish
- Unit tests: BackendClientTest, TransferViewModelTest, ApiKeyRemovalTest, OpenRouterDeletionTest
- No CI/CD

## Self-Correction Rules

When you make a mistake that I correct:
1. Determine scope:
   - Project-specific (BehavioralFraud logic, POC patterns) → Append to "Common Mistakes to Avoid" in THIS file
   - Generic Android/Kotlin/Compose best practice → Append to "Lessons Learned" in `~/.claude/CLAUDE.md`
2. Format: `- [context]: Do NOT ... → Instead, ...`
3. Do NOT duplicate existing rules in either file

## Lessons Learned
