# Tasks — BehavioralFraud Android Client

> Managed by: Van (Vandz)  
> SRS: `docs/SRS.md`  
> Status: `planned` | `in-progress` | `review` | `done`

---

## Task Index

| ID | Feature | SRS | Priority | Status | Dependencies |
|----|---------|-----|----------|--------|--------------|
| TASK-001 | BackendClient.kt (thay OpenRouterClient.kt) | FR-CL-01 | P0 | done | BE-API-ready |
| TASK-002 | Sửa TransferViewModel.kt | FR-CL-02 | P0 | done | TASK-001 |
| TASK-003 | Xóa API key + xóa OpenRouterClient.kt | FR-CL-03 | P0 | done | TASK-001 |
| TASK-004 | Sửa bug ProfileScreen.kt | FR-CL-04 | P1 | done | none |
| TASK-005 | Integration test end-to-end | Section 10.3 | P1 | done | TASK-002, TASK-003, BE-deployed |
| TASK-006 | DEBUG-level request/response logging in BackendClient | — | P1 | done | TASK-001 |
| TASK-007 | Phase 1: Enhanced feature extraction | FR-CL-05 | P1 | done | none |
| TASK-008 | Phase 2: Cognitive & intent signals | FR-CL-06 | P1 | planned | TASK-007 |
| TASK-009 | Phase 3: Advanced motion & pattern analysis | FR-CL-07 | P2 | planned | TASK-008 |

> **BE-API-ready**: Backend đã có OpenAPI spec hoặc ít nhất POST /risk/score và POST /profile/enroll chạy được.  
> **BE-deployed**: Backend chạy được (localhost hoặc remote) để Android gọi thật.

---

## Task Details

### TASK-001: BackendClient.kt

- **SRS section:** FR-CL-01
- **Branch:** `feat/backend-client`
- **Dependencies:** BE-API-ready
- **Status:** done

**Goal:** Tạo file mới `BackendClient.kt` thay thế `OpenRouterClient.kt`. POST feature JSON lên FastAPI backend thay vì gọi trực tiếp OpenRouter.

**File thay đổi:**
- NEW: `app/src/main/java/com/poc/behavioralfraud/network/BackendClient.kt`

**Methods cần implement:**

```kotlin
suspend fun enrollSession(
    userId: String,
    features: BehavioralFeatures
): EnrollResponse

suspend fun verifyTransaction(
    userId: String,
    features: BehavioralFeatures,
    profile: BehavioralProfile
): FraudAnalysisResult

suspend fun getProfile(
    userId: String
): BehavioralProfile?
```

**Request/Response mapping:**

| Method | HTTP | Endpoint | Request body | Response |
|--------|------|----------|-------------|----------|
| `enrollSession()` | POST | `/profile/enroll` | `{ user_id, session_features }` | `EnrollResponse` (status, enrollment_count, remaining, profile?) |
| `verifyTransaction()` | POST | `/risk/score` | `{ user_id, current_session, profile }` | `FraudAnalysisResult` (risk_score, risk_level, anomalies, explanation, recommendation, trace_id, model, latency_ms) |
| `getProfile()` | GET | `/profile/{user_id}` | — | `BehavioralProfile` hoặc null (404) |

**Constraints:**
- Base URL đọc từ `BuildConfig.BACKEND_BASE_URL`, không hard-code
- Timeout: 60 giây (giữ nguyên như hiện tại)
- HTTP client: OkHttp (đã có trong dependencies)
- Không chứa prompt string
- Không biết về model hay OpenRouter API key
- Không parse LLM response — chỉ parse JSON từ backend

**Done when:**
- [ ] File tạo tại `network/BackendClient.kt`
- [ ] 3 methods implement đúng
- [ ] Không có reference tới OpenRouter API key hoặc prompt string trong file
- [ ] Base URL từ BuildConfig
- [ ] Trả về parsed response models khớp backend response schema
- [ ] Throw exception có ý nghĩa khi lỗi network/parse

---

### TASK-002: Sửa TransferViewModel.kt

- **SRS section:** FR-CL-02
- **Branch:** `feat/update-viewmodel`
- **Dependencies:** TASK-001
- **Status:** done

**Goal:** Thay thế các chỗ gọi `llmClient` bằng `backendClient` trong `submitTransfer()`.

**File thay đổi:**
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/ui/screens/TransferViewModel.kt`

**Thay đổi cụ thể:**

| Dòng hiện tại | Thay bằng |
|---------------|-----------|
| `private val llmClient = OpenRouterClient()` | `private val backendClient = BackendClient()` |
| `llmClient.enrollProfile(allFeatures)` | `backendClient.enrollSession(userId, features)` |
| `llmClient.verifyTransaction(features, profile)` | `backendClient.verifyTransaction(userId, features, profile)` |

**Giữ nguyên (KHÔNG đổi):**
- State machine (`sealed class TransferUiState`)
- `LocalScorer` fallback trong try/catch — khi backend không phản hồi, LocalScorer vẫn chạy
- `collector` (BehavioralCollector)
- Tất cả properties và methods khác

**Lưu ý về enrollment flow:**
- Phase 1A: client vẫn đếm enrollment count phía client, gửi từng session lên backend. Backend cũng đếm.
- Khi backend trả `status: "completed"`, client nhận profile từ response thay vì tự build.

**Done when:**
- [ ] `llmClient` thay bằng `backendClient`
- [ ] `OpenRouterClient` không còn import hay instantiate
- [ ] State machine không đổi
- [ ] LocalScorer fallback vẫn hoạt động khi backend unreachable
- [ ] Enrollment flow hoạt động (3 sessions → profile)

---

### TASK-003: Xóa API key + xóa OpenRouterClient.kt

- **SRS section:** FR-CL-03
- **Branch:** `feat/remove-api-key`
- **Dependencies:** TASK-001
- **Status:** done

**Goal:** Loại bỏ OPENROUTER_API_KEY khỏi `app/build.gradle.kts` và BuildConfig. Xóa `OpenRouterClient.kt`. Thay bằng BACKEND_BASE_URL.

**File thay đổi:**
- MODIFY: `app/build.gradle.kts`
- DELETE: `app/src/main/java/com/poc/behavioralfraud/network/OpenRouterClient.kt`

**Thay đổi cụ thể trong `app/build.gradle.kts`:**

Xóa:
```kotlin
buildConfigField(
    "String",
    "OPENROUTER_API_KEY",
    "\"${localProperties.getProperty("OPENROUTER_API_KEY", "YOUR_API_KEY_HERE")}\""
)
```

Thêm:
```kotlin
buildConfigField(
    "String",
    "BACKEND_BASE_URL",
    "\"${localProperties.getProperty("BACKEND_BASE_URL", "http://10.0.2.2:8000")}\""
)
```

> `10.0.2.2` là localhost của host machine khi chạy trên Android emulator.

**Done when:**
- [ ] Không có `OPENROUTER_API_KEY` trong `app/build.gradle.kts`
- [ ] Không có `BuildConfig.OPENROUTER_API_KEY` reference trong toàn bộ codebase
- [ ] `BACKEND_BASE_URL` có trong BuildConfig
- [ ] `OpenRouterClient.kt` đã xóa
- [ ] App compile không lỗi

---

### TASK-004: Sửa bug ProfileScreen.kt

- **SRS section:** FR-CL-04
- **Branch:** `feat/fix-profile-screen`
- **Dependencies:** none
- **Status:** done

**Goal:** Sửa lỗi tham chiếu thuộc tính không tồn tại trên BehavioralProfile.

**File thay đổi:**
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/ui/screens/ProfileScreen.kt`

**Bug hiện tại:**

```kotlin
// KHÔNG TỒN TẠI trên BehavioralProfile
profile.avgGyroStability
profile.avgAccelStability
```

**BehavioralProfile chỉ có per-axis fields:**
- `avgGyroStabilityX`, `avgGyroStabilityY`, `avgGyroStabilityZ`
- `avgAccelStabilityX`, `avgAccelStabilityY`, `avgAccelStabilityZ`

**Cách sửa (chọn 1):**

Option A — Hiển thị per-axis:
```kotlin
ProfileMetric("Gyro stability X", String.format("%.6f", profile.avgGyroStabilityX), "...")
ProfileMetric("Gyro stability Y", String.format("%.6f", profile.avgGyroStabilityY), "...")
ProfileMetric("Gyro stability Z", String.format("%.6f", profile.avgGyroStabilityZ), "...")
```

Option B — Tính trung bình tại chỗ hiển thị:
```kotlin
val avgGyro = (profile.avgGyroStabilityX + profile.avgGyroStabilityY + profile.avgGyroStabilityZ) / 3
ProfileMetric("Gyro stability", String.format("%.6f", avgGyro), "Trung bình 3 trục")
```

**Lưu ý:** Task này không phụ thuộc task nào khác. Có thể làm bất cứ lúc nào.

**Done when:**
- [ ] Không có compile error trong ProfileScreen.kt
- [ ] Gyro và accel stability hiển thị đúng giá trị
- [ ] Không crash khi mở profile screen

---

### TASK-005: Integration test end-to-end

- **SRS section:** Section 10.3 (Integration acceptance criteria)
- **Branch:** `feat/integration-test`
- **Dependencies:** TASK-002, TASK-003, BE-deployed
- **Status:** done

**Goal:** Chạy test end-to-end: Android gọi backend thật, xác nhận toàn bộ flow hoạt động.

**Test scenarios:**

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Enrollment session 1 | Backend trả `status: "pending"`, `enrollment_count: 1` |
| 2 | Enrollment session 2 | Backend trả `status: "pending"`, `enrollment_count: 2` |
| 3 | Enrollment session 3 | Backend trả `status: "completed"` với profile |
| 4 | Verification — cùng người | `risk_score` thấp (0-30), `recommendation: "APPROVE"` |
| 5 | Verification — người khác | `risk_score` cao (71-100), `recommendation: "BLOCK"` |
| 6 | Backend unreachable | LocalScorer fallback trả kết quả, app không crash |

**Prerequisites:**
- Backend running (localhost hoặc ngrok)
- `BACKEND_BASE_URL` đúng trong `local.properties`
- `OPENROUTER_API_KEY` configured trên backend

**Done when:**
- [ ] Scenario 1-3: enrollment flow hoàn chỉnh qua backend
- [ ] Scenario 4: cùng người → score thấp
- [ ] Scenario 5: người khác → score cao
- [ ] Scenario 6: backend down → LocalScorer fallback hoạt động
- [ ] Latency không tăng đáng kể so với kiến trúc cũ (cùng model, cùng OpenRouter)

---

## File Change Summary

| File | Action | Task |
|------|--------|------|
| `network/BackendClient.kt` | NEW | TASK-001 |
| `network/OpenRouterClient.kt` | DELETE | TASK-003 |
| `ui/screens/TransferViewModel.kt` | MODIFY | TASK-002 |
| `app/build.gradle.kts` | MODIFY | TASK-003 |
| `ui/screens/ProfileScreen.kt` | MODIFY | TASK-004 |
| `data/collector/BehavioralCollector.kt` | MODIFY | TASK-007, 008, 009 |
| `data/model/BehavioralModels.kt` | MODIFY | TASK-007, 008, 009 |
| `data/scorer/LocalScorer.kt` | KEEP | — |
| `data/repository/ProfileRepository.kt` | KEEP | — |
| `ui/screens/HomeScreen.kt` | KEEP | — |
| `ui/screens/TransferScreen.kt` | KEEP | — |
| `ui/theme/Theme.kt` | KEEP | — |
| `MainActivity.kt` | KEEP | — |

**Tổng: 1 file mới, 3 file sửa, 1 file xóa, 8 file giữ nguyên.**

---

### TASK-006: DEBUG-level request/response logging in BackendClient

- **Branch:** `feat/client-debug-logging`
- **Dependencies:** TASK-001
- **Status:** done

**Goal:** Log request gửi lên backend và response nhận về tại BackendClient, ở DEBUG level. Giúp trace khi gặp lỗi từ phía mobile.

**Scope:**
- Log URL + request body JSON trước khi gửi HTTP request
- Log HTTP status code + response body JSON sau khi nhận response
- Log error detail khi request fail
- Dùng `android.util.Log.d()` (DEBUG level) — không xuất hiện trong release build

**Files to modify:**
- `app/src/main/java/com/poc/behavioralfraud/network/BackendClient.kt`

**Done when:**
- [ ] Mỗi call `enrollSession()`, `verifyTransaction()`, `getProfile()` log request + response
- [ ] Log ở DEBUG level (không leak trong release)
- [ ] Không log sensitive data ở INFO/WARN/ERROR
- [ ] trace_id từ response được log cùng

---

### TASK-007: Phase 1 — Enhanced Feature Extraction

- **SRS section:** FR-CL-05
- **Branch:** `feat/enhanced-features-p1`
- **Dependencies:** none
- **Status:** done

**Goal:** Thêm 25 features mới vào `BehavioralFeatures` và implement extraction từ raw data đã có. Không thay đổi data collection.

**File thay đổi:**
- MODIFY: `data/model/BehavioralModels.kt` — thêm 25 fields vào data class
- MODIFY: `data/collector/BehavioralCollector.kt` → `extractFeatures()` — thêm computation

**Done when:**
- [x] 25 fields mới trong `BehavioralFeatures` data class
- [x] `extractFeatures()` compute tất cả 25
- [x] Existing features giữ nguyên
- [x] JSON serialize/deserialize OK
- [x] App compile + chạy không crash
