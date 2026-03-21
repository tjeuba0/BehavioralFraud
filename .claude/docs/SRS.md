# SRS — Behavioral Fraud Detection — Backend Migration

> **Version:** 1.0  
> **Date:** 2026-03  
> **Author:** Van (Vandz)  
> **Status:** Draft  

---

## 1. Introduction

### 1.1 Purpose

This document specifies requirements for adding a backend layer to the Behavioral Fraud Detection project. The core change: move LLM calling from Android client to a FastAPI backend using DSPy.

### 1.2 Scope

**In scope:**

- New backend: FastAPI server with DSPy programs, calling LLM via OpenRouter
- Android client: replace `OpenRouterClient.kt` with `BackendClient.kt`, update `TransferViewModel.kt`

**Out of scope (DO NOT MODIFY):**

- `BehavioralCollector.kt` — behavioral data collection
- `extractFeatures()` — feature extraction logic
- `BehavioralModels.kt` — data models
- `LocalScorer.kt` — offline fallback scorer
- UI/UX of screens (except bug fix in `ProfileScreen.kt`)
- `HomeScreen.kt`, `TransferScreen.kt`, `Theme.kt`, `MainActivity.kt`

### 1.3 Definitions

| Term | Definition |
|------|-----------|
| DSPy | Stanford NLP framework: define AI tasks via Signature (input/output contract) instead of prompt strings |
| Signature | DSPy input/output declaration, similar to a typed function signature |
| dspy.LM() | DSPy's LLM connection config, supports OpenAI-compatible endpoints |
| OpenRouter | API gateway for multiple LLMs, provides OpenAI-compatible endpoint |
| Enrollment | Phase where legitimate user performs 3 transfers to build behavioral baseline |
| Verification | Phase where current session is compared against enrolled profile |
| Feature JSON | Extracted behavioral metrics (BehavioralFeatures), sent from Android to backend |
| Decision policy | Threshold logic: 0-30 APPROVE, 31-70 STEP_UP_AUTH, 71-100 BLOCK |

### 1.4 References

- Architecture document: Behavioral Fraud Detection — Architecture v2
- DSPy Documentation: https://dspy.ai
- OpenRouter API Documentation: https://openrouter.ai/docs
- FastAPI Documentation: https://fastapi.tiangolo.com
- Current project README.md

---

## 2. System Overview

### 2.1 Architecture — As-Is

```
Android App
  → BehavioralCollector (touch/sensor/text/navigation)
  → FeatureExtractor (20+ features on-device)
  → OpenRouterClient.kt (LLM call with prompt string)
  → OpenRouter API (Gemini 2.0 Flash / GLM-5)
  → Parse response → Display result
```

Problems:
- API key in BuildConfig (decompilable)
- Prompt logic hardcoded in Android — changing prompt requires app rebuild
- Changing model requires code change + release
- No observability (no latency/token/score logging)
- Decision policy split between prompt and LocalScorer

### 2.2 Architecture — To-Be

```
Android App
  → BehavioralCollector (unchanged)
  → FeatureExtractor (unchanged)
  → BackendClient.kt (POST feature JSON to FastAPI)

FastAPI Backend
  → routes_risk.py (receive request, return JSON)
  → RiskService (orchestration + decision policy)
  → DSPy Program (ProfileBuilder / RiskScorer)
  → dspy.LM() → OpenRouter API
```

Design principles:
- Android only collects behavioral data and sends extracted features. It knows nothing about prompts, models, or API keys.
- FastAPI is the HTTP layer only. No business logic.
- RiskService contains orchestration and decision policy (APPROVE/STEP_UP/BLOCK thresholds).
- DSPy contains AI logic: input/output contracts for each task.
- dspy.LM() is the model adapter. Phase 1 uses it directly. Phase 2 adds abstraction if needed.

### 2.3 Phasing

| Phase | Content | Profile storage |
|-------|---------|-----------------|
| 1A | FastAPI + DSPy + dspy.LM(OpenRouter). Android sends both profile and session in request. | Client-side (SharedPreferences, as current) |
| 1B | Move profile to server. Android sends only user_id + current session. | Server-side (SQLite or JSON file) |
| 2 | Add provider abstraction only when real need arises (different auth, retry policy, CLIProxy, etc.) | No impact |

---

## 3. Functional Requirements — Backend

### FR-BE-01: POST /risk/score

**Description:** Receive current session features and behavioral profile, return risk analysis result.

**Request body (Phase 1A):**

```json
{
  "user_id": "string",
  "current_session": { /* BehavioralFeatures */ },
  "profile": { /* BehavioralProfile */ }
}
```

**Request body (Phase 1B):**

```json
{
  "user_id": "string",
  "current_session": { /* BehavioralFeatures */ }
}
```

**Response body:**

```json
{
  "risk_score": 0-100,
  "risk_level": "LOW | MEDIUM | HIGH",
  "anomalies": ["string"],
  "explanation": "string",
  "recommendation": "APPROVE | STEP_UP_AUTH | BLOCK",
  "trace_id": "string",
  "model": "string",
  "latency_ms": 0
}
```

**Processing rules:**

1. Validate request schema
2. Call `RiskService.score()`
3. RiskService calls DSPy `RiskScoringProgram` via `dspy.LM()`
4. RiskService applies decision policy on DSPy output
5. Return JSON response with `trace_id`, `model`, `latency_ms`

**Acceptance criteria:**

- [ ] Returns valid JSON matching response schema
- [ ] `risk_score` is integer 0–100
- [ ] `risk_level` and `recommendation` are derived by decision policy in RiskService, NOT by LLM
- [ ] `trace_id` is unique UUID per request
- [ ] `latency_ms` reflects actual processing time
- [ ] Returns 422 on invalid request body
- [ ] Returns 500 with `trace_id` on internal error

---

### FR-BE-02: POST /profile/enroll

**Description:** Receive one enrollment session's features. When 3 sessions collected, call DSPy ProfileBuilderProgram to create profile.

**Request body:**

```json
{
  "user_id": "string",
  "session_features": { /* BehavioralFeatures */ }
}
```

**Response body (pending — fewer than 3 sessions):**

```json
{
  "status": "pending",
  "enrollment_count": 1 | 2,
  "remaining": 2 | 1
}
```

**Response body (completed — 3 sessions, profile created):**

```json
{
  "status": "completed",
  "enrollment_count": 3,
  "profile": { /* BehavioralProfile */ },
  "profile_summary": "string"
}
```

**Processing rules:**

1. Store session features in enrollment buffer (in-memory or DB)
2. If fewer than 3: return `status: "pending"`
3. If 3: call `ProfileBuilderProgram`, create and store profile, return `status: "completed"`

**Acceptance criteria:**

- [ ] First 2 calls return `status: "pending"` with correct `enrollment_count` and `remaining`
- [ ] Third call returns `status: "completed"` with profile and summary
- [ ] Profile contains averaged metrics from all 3 sessions
- [ ] `profile_summary` is generated by DSPy ProfileBuilderProgram
- [ ] Returns 422 on invalid request body
- [ ] Enrollment buffer is per-user (user_id)

---

### FR-BE-03: GET /profile/{user_id}

**Description:** Return behavioral profile for a user. For debug and demo purposes.

**Response body:**

```json
{
  "user_id": "string",
  "enrollment_count": 3,
  "profile": { /* BehavioralProfile */ },
  "profile_summary": "string"
}
```

**Acceptance criteria:**

- [ ] Returns profile when it exists
- [ ] Returns 404 when no profile exists for user_id
- [ ] Response matches BehavioralProfile schema

---

### FR-BE-04: DSPy RiskScoringProgram

**Description:** DSPy Signature defining the contract for comparing a session against a profile.

| Direction | Field | Description |
|-----------|-------|-------------|
| Input | `profile` | BehavioralProfile JSON (enrolled) |
| Input | `current_session` | BehavioralFeatures JSON (current) |
| Output | `risk_score` | Integer 0–100 |
| Output | `top_reasons` | List of anomaly reasons (Vietnamese) |
| Output | `confidence` | Float 0.0–1.0 |

**Implementation:**

- Phase 1: single `dspy.Predict` call. No ChainOfThought, no multi-hop.
- Signature must include description of what each output means.
- Paste detection threshold (lengthDelta >= 3) should be mentioned in signature description for model context.

**Acceptance criteria:**

- [ ] Signature class defined with typed InputField and OutputField
- [ ] Calling the program with valid profile + session returns all 3 output fields
- [ ] `risk_score` is parseable as integer 0–100
- [ ] `top_reasons` is parseable as list of strings
- [ ] `confidence` is parseable as float 0.0–1.0
- [ ] Works with `dspy.Predict` (single LLM call)

---

### FR-BE-05: DSPy ProfileBuilderProgram

**Description:** DSPy Signature defining the contract for building a profile from enrollment data.

| Direction | Field | Description |
|-----------|-------|-------------|
| Input | `sessions` | List of 3 BehavioralFeatures JSON |
| Output | `profile_summary` | Description of user behavior with specific numbers |
| Output | `expected_ranges` | Normal value ranges for each feature |
| Output | `behavioral_markers` | List of distinctive characteristics of this user |

**Implementation:**

- Phase 1: single `dspy.Predict` call.

**Acceptance criteria:**

- [ ] Signature class defined with typed InputField and OutputField
- [ ] Calling the program with 3 valid sessions returns all 3 output fields
- [ ] `profile_summary` contains specific numeric values (not vague descriptions)
- [ ] Works with `dspy.Predict` (single LLM call)

---

### FR-BE-06: Decision Policy

**Description:** Threshold logic in RiskService (Python code). NOT in DSPy program. NOT in LLM prompt.

| risk_score | risk_level | recommendation | Meaning |
|------------|-----------|----------------|---------|
| 0 – 30 | LOW | APPROVE | Behavior matches profile |
| 31 – 70 | MEDIUM | STEP_UP_AUTH | Some anomalies |
| 71 – 100 | HIGH | BLOCK | Significant deviation |

DSPy program returns only `risk_score` (integer) and `top_reasons`. RiskService applies thresholds and assigns `risk_level` + `recommendation`.

**Acceptance criteria:**

- [ ] Policy is deterministic Python code in RiskService
- [ ] Score 0 → APPROVE, LOW
- [ ] Score 30 → APPROVE, LOW
- [ ] Score 31 → STEP_UP_AUTH, MEDIUM
- [ ] Score 70 → STEP_UP_AUTH, MEDIUM
- [ ] Score 71 → BLOCK, HIGH
- [ ] Score 100 → BLOCK, HIGH
- [ ] Policy is NOT inside any DSPy Signature or prompt

---

### FR-BE-07: dspy.LM() Configuration with OpenRouter

**Description:** Connect DSPy to OpenRouter via OpenAI-compatible endpoint.

```python
import dspy

lm = dspy.LM(
    "openai/<model-name>",
    api_base="https://openrouter.ai/api/v1",
    api_key=OPENROUTER_API_KEY,
)
dspy.configure(lm=lm)
```

Configuration values (`model`, `api_base`, `api_key`) read from environment variables or `.env` file. Never hard-coded.

**Acceptance criteria:**

- [ ] `dspy.LM()` configured with OpenRouter base URL
- [ ] Model name, API key, base URL read from environment / .env
- [ ] No hard-coded credentials in source code
- [ ] A test call (spike test) succeeds: send a trivial Signature, get valid response
- [ ] Changing `LLM_MODEL` in `.env` and restarting changes the model used (no code change)

---

### FR-BE-08: Logging and Observability

**Description:** Every request must produce a structured log entry.

| Field | Type | Purpose |
|-------|------|---------|
| `trace_id` | UUID | Request identifier for debugging |
| `user_id` | string | User identifier |
| `model` | string | Model used (e.g., google/gemini-2.0-flash-001) |
| `risk_score` | int | Scoring result |
| `latency_ms` | int | Total processing time including LLM call |
| `llm_latency_ms` | int | LLM call time only |
| `token_usage` | object | prompt_tokens, completion_tokens (if OpenRouter returns it) |
| `recommendation` | string | APPROVE / STEP_UP_AUTH / BLOCK |

**Acceptance criteria:**

- [ ] Every `/risk/score` request produces log entry with all fields above
- [ ] Every `/profile/enroll` request (3rd call) produces log entry with profile build info
- [ ] Logs are structured (JSON format)
- [ ] `trace_id` is included in both log and response

---

## 4. Functional Requirements — Android Client

### FR-CL-01: BackendClient.kt

**Description:** New file replacing `OpenRouterClient.kt`. POSTs feature JSON to FastAPI backend instead of calling OpenRouter directly.

**Methods:**

- `enrollSession(userId: String, features: BehavioralFeatures)` → calls `POST /profile/enroll`
- `verifyTransaction(userId: String, features: BehavioralFeatures, profile: BehavioralProfile)` → calls `POST /risk/score`
- `getProfile(userId: String)` → calls `GET /profile/{user_id}`

**Constraints:**

- No prompt strings
- No knowledge of model or OpenRouter API key
- No LLM response parsing — only parses JSON from backend
- Base URL read from `BuildConfig.BACKEND_BASE_URL`, not hard-coded
- Timeout: 60 seconds (same as current, LLM call can take 2–8 seconds)
- HTTP client: OkHttp (already in dependencies)

**Acceptance criteria:**

- [ ] File created at `network/BackendClient.kt`
- [ ] All 3 methods implemented
- [ ] No reference to OpenRouter API key or prompt strings anywhere in file
- [ ] Base URL from BuildConfig
- [ ] Returns parsed response models matching backend response schemas
- [ ] Throws meaningful exceptions on network/parse errors

---

### FR-CL-02: Update TransferViewModel.kt

**Description:** Replace LLM client calls with backend client calls in `submitTransfer()`.

**Changes:**

- Replace `llmClient.enrollProfile(allFeatures)` → `backendClient.enrollSession(userId, features)`
- Replace `llmClient.verifyTransaction(features, profile)` → `backendClient.verifyTransaction(userId, features, profile)`
- Enrollment counting: keep client-side in Phase 1A; backend handles it in Phase 1B

**Keep unchanged:**

- State machine (`sealed class TransferUiState`) — no changes
- `LocalScorer` fallback when backend unreachable — keep existing try/catch logic
- `collector` (BehavioralCollector) — no changes
- All other properties and methods

**Acceptance criteria:**

- [ ] `llmClient` replaced with `backendClient`
- [ ] `OpenRouterClient` no longer imported or instantiated
- [ ] State machine unchanged
- [ ] LocalScorer fallback still works when backend is unreachable
- [ ] Enrollment flow still works (3 sessions → profile)

---

### FR-CL-03: Remove API Key from Client

**Description:** Remove OPENROUTER_API_KEY from `app/build.gradle.kts` and BuildConfig.

**Changes:**

- Remove `buildConfigField("String", "OPENROUTER_API_KEY", ...)` from `app/build.gradle.kts`
- Remove all references to `BuildConfig.OPENROUTER_API_KEY`
- Add `buildConfigField("String", "BACKEND_BASE_URL", ...)` with configurable value
- Default: `http://10.0.2.2:8000` for emulator, or actual server URL

**Acceptance criteria:**

- [ ] No `OPENROUTER_API_KEY` in `app/build.gradle.kts`
- [ ] No `BuildConfig.OPENROUTER_API_KEY` referenced anywhere in codebase
- [ ] `BACKEND_BASE_URL` available in BuildConfig
- [ ] App compiles without error

---

### FR-CL-04: Fix ProfileScreen.kt Bug

**Description:** Fix references to non-existent properties on BehavioralProfile.

**Current (broken):**

```kotlin
profile.avgGyroStability   // DOES NOT EXIST on BehavioralProfile
profile.avgAccelStability  // DOES NOT EXIST on BehavioralProfile
```

**BehavioralProfile has per-axis fields:**

- `avgGyroStabilityX`, `avgGyroStabilityY`, `avgGyroStabilityZ`
- `avgAccelStabilityX`, `avgAccelStabilityY`, `avgAccelStabilityZ`

**Fix options (pick one):**

- Display per-axis values (X, Y, Z separately)
- Compute average of 3 axes at display time: `(X + Y + Z) / 3`

**Acceptance criteria:**

- [ ] No compile error in ProfileScreen.kt
- [ ] Gyro and accel stability values display correctly
- [ ] No crash when viewing profile screen

---

## 5. Non-Functional Requirements

### NFR-01: Performance

- Backend processing time (excluding LLM call): < 100ms
- LLM call time: depends on OpenRouter and model, typically 2–8 seconds. Backend must not add significant latency.
- Android client timeout: 60 seconds

### NFR-02: Security

- OpenRouter API key exists ONLY on backend (environment variable). Never appears on client.
- Android → Backend connection via HTTPS (except local dev).
- Backend → OpenRouter connection via HTTPS (OpenRouter default).

### NFR-03: Extensibility

- Change model: only change `LLM_MODEL` in `.env`, no code changes.
- Change provider (Phase 2): add new provider without modifying DSPy programs or Android code.
- Add new DSPy program: does not affect existing programs.

### NFR-04: Operability

- Backend starts with single command: `uvicorn app.main:app`
- Configuration via single `.env` file
- Health check endpoint: `GET /health`
- Structured logs (JSON) with `trace_id` per request

### NFR-05: Testability

- DSPy Signatures testable independently with mock LLM
- RiskService testable with mock DSPy program
- API routes testable with FastAPI TestClient
- Android BackendClient testable with MockWebServer

---

## 6. Data Models

### 6.1 BehavioralFeatures (unchanged from Android)

No changes. This is the output of on-device feature extraction, sent to backend as JSON. Backend needs a corresponding Pydantic model.

| Field | Type | Description |
|-------|------|-------------|
| sessionDurationMs | long | Session duration (ms) |
| avgInterCharDelayMs | double | Average inter-character delay (ms) |
| stdInterCharDelayMs | double | Std dev of inter-character delay |
| maxInterCharDelayMs | long | Max delay between text changes |
| minInterCharDelayMs | long | Min delay |
| totalTextChanges | int | Total text change events |
| pasteCount | int | Paste detections (lengthDelta >= 3) |
| totalTouchEvents | int | Total touch events |
| avgTouchSize | double | Average touch size |
| avgTouchDurationMs | double | Average touch duration (ms) |
| avgSwipeVelocity | double | Average swipe velocity (px/s) |
| gyroStabilityX | double | Gyroscope stability X-axis (std dev) |
| gyroStabilityY | double | Gyroscope stability Y-axis (std dev) |
| gyroStabilityZ | double | Gyroscope stability Z-axis (std dev) |
| accelStabilityX | double | Accelerometer stability X-axis |
| accelStabilityY | double | Accelerometer stability Y-axis |
| accelStabilityZ | double | Accelerometer stability Z-axis |
| avgTouchPressure | double | Average touch pressure (touchMajor) |
| perFieldAvgDelay | Map<String, Double> | Per-field average typing delay |
| avgInterFieldPauseMs | double | Inter-field hesitation (ms) |
| deletionCount | int | Number of deletions |
| deletionRatio | double | Deletion ratio (deletions / total text changes) |
| fieldFocusSequence | string | Field focus order |
| timeToFirstInput | long | Time from screen open to first input (ms) |
| timeFromLastInputToConfirm | long | Time from last input to confirm (ms) |

### 6.2 BehavioralProfile (unchanged from Android)

No structural changes. In Phase 1A, Android sends full profile in request body. In Phase 1B, backend loads from storage. Backend needs a corresponding Pydantic model matching `BehavioralProfile.kt`.

### 6.3 FraudAnalysisResult (extended)

Same structure as current `FraudAnalysisResult.kt`, plus additional fields from backend:

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| riskScore | int | DSPy → RiskService | 0–100 |
| riskLevel | string | RiskService (decision policy) | LOW / MEDIUM / HIGH |
| anomalies | List<String> | DSPy | Detected anomalies |
| explanation | string | DSPy | Detailed explanation |
| recommendation | string | RiskService (decision policy) | APPROVE / STEP_UP_AUTH / BLOCK |
| trace_id | string | Backend (new) | Request trace identifier |
| model | string | Backend (new) | LLM model used |
| latency_ms | int | Backend (new) | Processing time |

---

## 7. Backend Directory Structure

```
backend/
├── app/
│   ├── main.py                    # FastAPI app entry point
│   ├── api/
│   │   └── routes_risk.py         # 3 routes: enroll, score, get profile
│   ├── domain/
│   │   └── schemas.py             # Pydantic request/response models
│   ├── services/
│   │   └── risk_service.py        # Orchestration + decision policy
│   ├── dspy_programs/
│   │   ├── signatures.py          # DSPy Signatures (2 Signatures)
│   │   └── programs.py            # DSPy Programs (2 Programs)
│   └── core/
│       ├── config.py              # Settings from .env
│       └── logging.py             # Structured logging setup
├── .env                           # LLM_MODEL, OPENROUTER_API_KEY, ...
├── requirements.txt
└── README.md
```

Note: No `llm/` directory in Phase 1. Model calling goes through `dspy.LM()` configured in `config.py`. Phase 2 adds abstraction layer only if needed.

---

## 8. Team Assignment

### 8.1 Backend Team

| # | REQ ID | Task | Effort | Priority |
|---|--------|------|--------|----------|
| 1 | FR-BE-07 | Spike test dspy.LM() + OpenRouter | 0.5 day | P0 |
| 2 | FR-BE-01 | POST /risk/score (route + RiskService + RiskScoringProgram) | 4–5 days | P0 |
| 3 | FR-BE-06 | Decision policy in RiskService | Included in #2 | P0 |
| 4 | FR-BE-02 | POST /profile/enroll (route + ProfileBuilderProgram) | 3–4 days | P0 |
| 5 | FR-BE-03 | GET /profile/{user_id} | 0.5 day | P1 |
| 6 | FR-BE-08 | Logging and observability | 1 day | P1 |
| 7 | FR-BE-04/05 | Unit tests for DSPy programs and RiskService | 1–2 days | P1 |

### 8.2 Client Team (Android)

| # | REQ ID | Task | Effort | Priority |
|---|--------|------|--------|----------|
| 1 | FR-CL-01 | Create BackendClient.kt (replace OpenRouterClient.kt) | 1 day | P0 |
| 2 | FR-CL-02 | Update TransferViewModel.kt (replace llmClient with backendClient) | 0.5 day | P0 |
| 3 | FR-CL-03 | Remove API key from build.gradle.kts and BuildConfig | 0.5 day | P0 |
| 4 | FR-CL-04 | Fix ProfileScreen.kt bug (avgGyroStability/avgAccelStability) | 0.5 day | P1 |
| 5 | — | Integration test with backend (end-to-end) | 1–2 days | P1 |

### 8.3 Dependencies

- Backend and client can work in parallel for most tasks.
- Client needs running backend API for integration test. Backend should provide OpenAPI spec early so client can code against contract.
- Client can use MockWebServer for independent testing while waiting for backend.
- Backend task #1 (spike test) must complete before all other backend tasks.

---

## 9. Android File Change Checklist

| File | Action | Detail |
|------|--------|--------|
| `network/OpenRouterClient.kt` | DELETE | Replace with BackendClient.kt. Keep old file in separate branch for reference. |
| `network/BackendClient.kt` | NEW | 3 methods: enrollSession(), verifyTransaction(), getProfile(). POST/GET to FastAPI. |
| `ui/screens/TransferViewModel.kt` | MODIFY | Replace llmClient with backendClient. Keep state machine, keep LocalScorer fallback. |
| `app/build.gradle.kts` | MODIFY | Remove OPENROUTER_API_KEY. Add BACKEND_BASE_URL to BuildConfig. |
| `ui/screens/ProfileScreen.kt` | MODIFY | Fix reference to avgGyroStability / avgAccelStability → per-axis fields. |
| `data/collector/BehavioralCollector.kt` | KEEP | No changes. |
| `data/model/BehavioralModels.kt` | KEEP | No changes. |
| `data/scorer/LocalScorer.kt` | KEEP | No changes. Still serves as offline fallback. |
| `data/repository/ProfileRepository.kt` | KEEP | No changes in Phase 1A. Modify if moving to Phase 1B. |
| `ui/screens/HomeScreen.kt` | KEEP | No changes. |
| `ui/screens/TransferScreen.kt` | KEEP | No changes. |
| `ui/theme/Theme.kt` | KEEP | No changes. |
| `MainActivity.kt` | KEEP | No changes. |

**Summary: 1 new, 3 modify, 1 delete, 8 keep.**

---

## 10. Acceptance Criteria

### 10.1 Backend

1. Spike test: `dspy.LM()` calls OpenRouter and returns valid response
2. `POST /risk/score` returns JSON matching response schema with risk_score, risk_level, recommendation, trace_id
3. Decision policy correct: score 25 → APPROVE, score 50 → STEP_UP_AUTH, score 80 → BLOCK
4. `POST /profile/enroll`: first 2 calls return pending, 3rd returns completed with profile
5. `GET /profile/{user_id}` returns profile or 404
6. Every request has structured log with trace_id, latency_ms, model, risk_score
7. Changing `LLM_MODEL` in `.env` and restarting changes the model used (no code change)

### 10.2 Android Client

1. App calls backend instead of OpenRouter directly
2. No API key in APK (grep BuildConfig shows no OPENROUTER_API_KEY)
3. Enrollment 3 times → profile created successfully (via backend)
4. Verification returns results of same quality as old architecture (same input, same model)
5. When backend unreachable, LocalScorer still works (fallback)
6. ProfileScreen displays correctly (no compile error, no crash)

### 10.3 Integration

1. End-to-end: Android enrollment 3x → backend creates profile → Android verification → backend returns score
2. Demo scenario: same person → low score; different person → high score
3. Overall latency not significantly increased compared to old architecture (same model, same OpenRouter)
