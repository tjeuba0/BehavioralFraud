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
| TASK-008 | Phase 2: Cognitive & intent signals | FR-CL-06 | P1 | done | TASK-007 |
| TASK-009 | Phase 3: Advanced motion & pattern analysis | FR-CL-07 | P2 | done | TASK-008 |

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

---

## iPay Visual Clone — TASK-010..025

> Reference SRS: FR-CL-08 (Design System), FR-CL-09 (Login + Touch ID), FR-CL-10 (Transfer Flow E2E + Silent Behavioral Pipeline + Dev Menu).
> Figma: `5JXePCuiqKQFdrjNHNQVbw` node `1:15393`.

### Task Index — TASK-010..025

| ID | Feature | SRS | Priority | Status | Dependencies |
|----|---------|-----|----------|--------|--------------|
| TASK-010 | Design tokens (color/typography/spacing/shape/stroke/elevation) | FR-CL-08 (REQ-01..08) | P0 | planned | none |
| TASK-011 | Foundation components (Button/TextField/TopBar/Card) | FR-CL-08 (REQ-09..13) | P0 | planned | TASK-010 |
| TASK-012 | Foundation components (Chip/AIChip/Alert/BottomSheet) | FR-CL-08 (REQ-14..17) | P0 | planned | TASK-010 |
| TASK-013 | Foundation components (Toggle/Selection/Tabs/Badges) | FR-CL-08 (REQ-18..22) | P1 | planned | TASK-010 |
| TASK-014 | Design System Preview screen | FR-CL-08 (REQ-23) | P1 | planned | TASK-011, 012, 013 |
| TASK-015 | NavHost migration (replace `when (currentScreen)` switch-case in MainActivity:30) | FR-CL-10 (REQ-09) | P0 | planned | TASK-011 |
| TASK-016 | ~~Login screen + PinDots + NumericKeypad + biometric~~ — **DROPPED** (no Figma reference, login not in behavioral session, doesn't add demo value) | FR-CL-09 | — | dropped | — |
| TASK-017 | HomeIPayScreen rewrite — strip enrollment UI + dev affordance | FR-CL-10 (REQ-01) | P0 | planned | TASK-013, 015 |
| TASK-018 | TransferTypeScreen + RecipientScreen | FR-CL-10 (REQ-02, 03) | P0 | planned | TASK-011, 017 |
| TASK-019 | TransferFormScreen rewrite + TransferOrchestratorViewModel | FR-CL-10 (REQ-04, 08) | P0 | planned | TASK-018 |
| TASK-020 | OverNapasLimitBottomSheet + decisionTimeOverLimitMs | FR-CL-10 (REQ-05, 13) | P0 | planned | TASK-019, TASK-012 |
| TASK-021 | OtpScreen + otpPasted detection | FR-CL-10 (REQ-06, 14) | P0 | planned | TASK-020 |
| TASK-022 | TransferSuccessScreen — production-feel, no risk display | FR-CL-10 (REQ-07) | P0 | planned | TASK-021 |
| TASK-023 | Silent behavioral pipeline (session lifecycle in VM + silent verification + history persist) | FR-CL-10 (REQ-10..12, 15) | P0 | planned | TASK-016..022 |
| TASK-024 | Dev Menu (DevMenu/Profile/RiskHistory/Session/ManualOverride) | FR-CL-10 (REQ-16..20) | P0 | planned | TASK-023 |
| TASK-025 | E2E manual smoke + Figma visual diff + production-feel audit | FR-CL-08, 09, 10 acceptance | P1 | planned | TASK-024 |
| TASK-026 | mockLocationDetected — GPS spoofing fraud signal | FR-CL-11 (REQ-01) | P1 | done | none |
| TASK-027 | Extended sensor coverage (magnetometer + light + proximity + linear-accel + rotation-vector) | FR-CL-12 (REQ-01..12) | P1 | done | none |
| TASK-028 | Touch micro-biometrics (tap precision + inter-tap velocity + hand dominance + tap jitter) | FR-CL-13 (REQ-01..06) | P1 | review | none |

---

### TASK-010: Design tokens + theme architecture

- **SRS section:** FR-CL-08 (REQ-01..08, REQ-24..27)
- **Branch:** `feat/ipay-tokens`
- **Dependencies:** none
- **Status:** planned

**Goal:** Setup design tokens 3-layer (palette → semantic → spec) + theme provider hỗ trợ runtime switching. Mọi component sau này tiêu thụ token qua `IPayTheme.colors/.typography/...` (object accessor đọc CompositionLocal).

**Architecture overview:**
```
Layer 1 — Palette (primitives):    IPayPalette.vietinDarkBlue50 = Color(0xFF005BAA)
                                       ↓ (consume)
Layer 2 — Semantic tokens:         IPayColors(brandPrimary = ..., textPrimary = ..., ...)
                                   IPayTypography, IPaySpacing, IPayShapes, IPayStroke, IPayElevation
                                       ↓ (bundle)
Layer 3a — Spec (immutable):       IPayThemeSpec(colors, typography, spacing, shapes, stroke, elevation)
Layer 3b — Registry:               IPayThemes.Default / .Dark / .Demo
                                       ↓ (provide)
Provider Composable:               IPayTheme(spec) { CompositionLocalProvider(...) { MaterialTheme(...) { content } } }
                                       ↓ (consume via accessor)
Component code:                    Box(Modifier.background(IPayTheme.colors.brandPrimary))
```

**Done when:**

*Token files (REQ-01..07):*
- [ ] 7 file token compile (Color, Spacing, Shape, Stroke, Elevation, Typography, Theme)
- [ ] `IPayPalette` object — primitives, hard-coded color values, KHÔNG dùng trực tiếp trong UI
- [ ] `IPayColors` data class — semantic tokens consume palette
- [ ] `IPayTypography`, `IPaySpacing`, `IPayShapes`, `IPayStroke`, `IPayElevation` data class
- [ ] Font fallback `FontFamily.SansSerif` + comment chỗ swap khi có SVN-Gilroy ttf

*Theme architecture (REQ-24..27):*
- [ ] `IPayThemeSpec` data class bundle 6 token types
- [ ] `IPayThemes` object expose 3 variant: `Default` (full iPay light), `Dark` (stub — có thể mirror Default), `Demo` (alternative palette — vd brand đỏ thay xanh để test switchability)
- [ ] `IPayTheme(spec: IPayThemeSpec = IPayThemes.Default, content: @Composable () -> Unit)` provider Composable
- [ ] 6 `CompositionLocal<*>` (1 per token type) provided từ spec bên trong provider
- [ ] `IPayTheme` object accessor — properties `@Composable get()` đọc từ CompositionLocal
- [ ] `MaterialTheme(colorScheme, typography, shapes)` map từ spec, wrap content

*Switchability proof:*
- [ ] **Demo runtime switch**: temp button trong app (hoặc test fixture) cycle Default→Dark→Demo→Default, mọi consumer recompose trong 1 frame không crash
- [ ] **Nested override**: bọc subtree với `IPayTheme(spec = IPayThemes.Demo) { ... }` → subtree dùng Demo tokens, ngoài subtree giữ Default — verify bằng 2 Box màu khác nhau

*Compatibility:*
- [ ] `BehavioralFraudTheme` alias còn tồn tại để MainActivity hiện tại không vỡ (proxy về `IPayTheme()` với default spec)
- [ ] Material3 colorScheme/typography/shapes mapped từ IPay tokens — Material widget (Snackbar/Dialog) tự lấy đúng màu

*Lint:*
- [ ] Component (sau này) đọc token chỉ qua `IPayTheme.*` — KHÔNG nhận token qua param, KHÔNG cache trong remember (rule này enforce ở TASK-011..013)

---

### TASK-011: Foundation components — Button / TextField / TopBar / Card

- **SRS section:** FR-CL-08 (REQ-09..13)
- **Branch:** `feat/ipay-foundation-1`
- **Dependencies:** TASK-010
- **Status:** planned

**Goal:** Build 5 base components + `safeClickable` modifier.

**Done when:**
- [ ] `IPayButton` 4 variant × 3 size hiển thị đúng trong preview
- [ ] `IPayIconButton` round 40dp
- [ ] `IPayTextField` default/active/error/disabled + prefix/suffix
- [ ] `IPayTopBar` standard + transparent
- [ ] `IPayCard` plain/elevated/outlined
- [ ] `safeClickable` debounce 350ms

---

### TASK-012: Foundation components — Chip / AIChip / Alert / BottomSheet

- **SRS section:** FR-CL-08 (REQ-14..17)
- **Branch:** `feat/ipay-foundation-2`
- **Dependencies:** TASK-010
- **Status:** planned

**Done when:**
- [ ] `IPayChip` default + selected
- [ ] `IPayAIChip` gradient border 5 stops + label gradient 3 stops
- [ ] `IPayAlertBanner` info/warning/success
- [ ] `IPayBottomSheet` skin lại Material3 ModalBottomSheet (handle 40×4dp, radius top r24)

---

### TASK-013: Foundation components — Toggle / Selection / Tabs / Badges

- **SRS section:** FR-CL-08 (REQ-18..22)
- **Branch:** `feat/ipay-foundation-3`
- **Dependencies:** TASK-010
- **Status:** planned

**Done when:**
- [ ] `IPayToggle` animate handle on/off
- [ ] `IPaySelection` radio-card + checkbox-card, selected state border + bg brand secondary
- [ ] `IPayHorizontalTabs` underline indicator (Vietin red 60)
- [ ] `IPayStatusBadge` 5 variant
- [ ] `IPayNotificationBadge` dot + count với gradient red

---

### TASK-014: Design System Preview screen

- **SRS section:** FR-CL-08 (REQ-23)
- **Branch:** `feat/ipay-preview-screen`
- **Dependencies:** TASK-011, 012, 013
- **Status:** planned

**Goal:** Showcase tokens + 13 components — accessed via Dev Menu (long-press logo Home 1.5s → Dev Menu → Design System Preview).

**Done when:**
- [ ] Screen showcase đầy đủ 13 components + token sections
- [ ] Truy cập qua Dev Menu (sau TASK-024) — KHÔNG truy cập trực tiếp từ Home

---

### TASK-015: NavHost migration

- **SRS section:** FR-CL-10 (REQ-09)
- **Branch:** `feat/ipay-navhost`
- **Dependencies:** TASK-011
- **Status:** planned

**Goal:** Replace `when (currentScreen)` switch-case trong `MainActivity.kt` (line 30) bằng `NavHost` với 12+ routes.

**Note:** Verified — code hiện tại dùng switch-case, KHÔNG phải NavHost. CLAUDE.md mô tả sai.

**Routes:** `login`, `home`, `transfer/type`, `transfer/recipient`, `transfer/form`, `transfer/otp`, `transfer/success`, `dev`, `dev/profile`, `dev/risk-history`, `dev/session`, `dev/manual-override`, `dev/design-system`.

**Done when:**
- [ ] `NavController` quản lý mọi navigation
- [ ] `androidx.navigation.compose` đã có trong dependencies (verify build.gradle)
- [ ] Các màn hiện có (Home/Transfer/Profile) chạy được trên NavHost (giai đoạn migration)
- [ ] Existing TransferViewModelTest còn pass

---

### TASK-016: LoginScreen + Touch ID

- **SRS section:** FR-CL-09 (REQ-01..05)
- **Branch:** `feat/ipay-login`
- **Dependencies:** TASK-011, TASK-015
- **Status:** planned

**Goal:** Màn login PIN 6 số + biometric. Mock pass mọi PIN. Login KHÔNG mở behavioral session (transfer session bắt đầu ở Home).

**Done when:**
- [ ] App start vào LoginScreen
- [ ] 6 PIN dots + numeric keypad render đúng
- [ ] `BiometricManager.canAuthenticate()` decide hide/show button (handle NO_HARDWARE / NONE_ENROLLED / SECURITY_UPDATE_REQUIRED)
- [ ] Nhập 6 số → vào Home; biometric success → vào Home
- [ ] BehavioralCollector idle (KHÔNG có session active) sau khi login → Home
- [ ] biometric library bumped lên 1.2.0-alpha05+ trong build.gradle.kts

---

### TASK-017: HomeIPayScreen rewrite — strip enrollment UI

- **SRS section:** FR-CL-10 (REQ-01)
- **Branch:** `feat/ipay-home`
- **Dependencies:** TASK-013, TASK-015
- **Status:** planned

**Goal:** Skin Home theo iPay + **strip toàn bộ enrollment/verification UI** khỏi production view. Long-press logo affordance vào Dev Menu.

**Note:** HomeScreen.kt hiện tại (verified line 91-105, 122-134, 153, 161-171) hiển thị:
- "Enrollment: 0/3" counter
- "Chế độ: ENROLLMENT/VERIFICATION" toggle
- "Cách hoạt động" guide với "Enrollment", "Verification", "Demo Fraud"
- Button label "Chuyển khoản (Enrollment 1/3)" / "Chuyển khoản (Verification)"
- "Xem Profile hành vi" button
- "Xóa tất cả dữ liệu" button

→ **Tất cả phải bỏ khỏi HomeIPayScreen production**. Logic chuyển vào DevMenuScreen (TASK-024).

**Done when:**
- [ ] Header gradient + greeting + avatar + notification badge
- [ ] Quick balance card mock
- [ ] Action grid 4×N với IPayCard icon + label (action "Chuyển tiền trong nước" trigger startSession + navigate)
- [ ] Promotions row với card thường (KHÔNG dùng IPayAIChip)
- [ ] **KHÔNG có chữ** "Enrollment", "Verification", "Profile", "Behavior", "POC" trên Home
- [ ] Long-press logo 1.5s → DevMenuScreen (placeholder cho đến TASK-024)

---

### TASK-018: TransferTypeScreen + RecipientScreen

- **SRS section:** FR-CL-10 (REQ-02, 03)
- **Branch:** `feat/ipay-recipient`
- **Dependencies:** TASK-011, TASK-017
- **Status:** planned

**Done when:**
- [ ] TransferType: 2 IPaySelection card (nội bộ / Napas)
- [ ] Recipient: STK input (focus đầu tiên) + bank list scroll + recent chips
- [ ] Sticky button "Tiếp tục" disabled cho đến khi đủ data
- [ ] Mock data trong `data/mock/MockData.kt`

---

### TASK-019: TransferFormScreen + TransferOrchestratorViewModel

- **SRS section:** FR-CL-10 (REQ-04, 08)
- **Branch:** `feat/ipay-transfer-form`
- **Dependencies:** TASK-018
- **Status:** planned

**Done when:**
- [ ] Form số tiền (thousand-separator) + ghi chú + nguồn tiền
- [ ] AlertBanner info hạn mức Napas
- [ ] TransferOrchestratorViewModel quản lý toàn flow
- [ ] Channel<TransferEvent> cho navigation
- [ ] Update existing TransferViewModelTest hoặc viết test mới cho TransferOrchestratorViewModel
- [ ] Refactor TransferViewModel cũ — tách enrollment/verification logic ra khỏi production VM

---

### TASK-020: OverNapasLimitBottomSheet + decisionTimeOverLimitMs

- **SRS section:** FR-CL-10 (REQ-05, 13)
- **Branch:** `feat/ipay-napas-sheet`
- **Dependencies:** TASK-019, TASK-012
- **Status:** planned

**Done when:**
- [ ] Bottom sheet trigger khi amount > hạn mức mock
- [ ] 2 button: chuyển kênh thường / huỷ
- [ ] `decisionTimeOverLimitMs: Long = 0` field thêm vào BehavioralFeatures (default 0 cho backward-compat)
- [ ] Backend Pydantic model mirror update
- [ ] Đo được hesitation từ lúc sheet show đến lúc tap

---

### TASK-021: OtpScreen + otpPasted detection

- **SRS section:** FR-CL-10 (REQ-06, 14)
- **Branch:** `feat/ipay-otp`
- **Dependencies:** TASK-020
- **Status:** planned

**Done when:**
- [ ] 6 ô OTP (reuse IPayPinDots ở mode "show digit") + numeric keypad
- [ ] Counter "Gửi lại sau 60s"
- [ ] Auto-submit khi đủ 6 số
- [ ] `otpPasted: Boolean = false` field thêm vào BehavioralFeatures
- [ ] Khi `onTextChanged` lengthDelta >= 3 trên field OTP → set otpPasted=true

---

### TASK-022: TransferSuccessScreen — production-feel

- **SRS section:** FR-CL-10 (REQ-07)
- **Branch:** `feat/ipay-success`
- **Dependencies:** TASK-021
- **Status:** planned

**Goal:** Success screen iPay thật — KHÔNG hiển thị risk score / verification result. Result lưu silent vào DataStore (TASK-023).

**Done when:**
- [ ] Full screen success skin theo Figma
- [ ] Card chi tiết giao dịch (người nhận, STK, ngân hàng, nội dung, mã GD, thời gian)
- [ ] Button "Về trang chủ" + "Lưu biên lai"
- [ ] **KHÔNG hiển thị** "Phân tích rủi ro" / risk score / verification result / "POC" gì cả
- [ ] Code review: grep "risk", "score", "verification", "phân tích" trong file → 0 match (trừ comment giải thích)

---

### TASK-023: Silent behavioral pipeline

- **SRS section:** FR-CL-10 (REQ-10, 11, 12, 15)
- **Branch:** `feat/ipay-silent-pipeline`
- **Dependencies:** TASK-016..022
- **Status:** planned

**Goal:** Wire collector lifecycle vào TransferOrchestratorViewModel + silent verification + persist verification history.

**Done when:**
- [ ] `collector.startSession()` gọi trong `TransferOrchestratorViewModel.init` (sau khi user tap "Chuyển tiền" Home)
- [ ] `collector.stopSession()` gọi ở Success path + `onCleared()` (cover abort)
- [ ] App backgrounded > 30s → end session với reason "backgrounded"
- [ ] LoginScreen + HomeScreen browsing KHÔNG có session active
- [ ] Mọi screen trong transfer flow (TransferType/Recipient/Form/OTP/Success) wrap pointerInteropFilter
- [ ] Field focus sequence ghi đúng `account_number → bank_select → amount → note → otp` (KHÔNG có pin_pad)
- [ ] sessionDurationMs chỉ cover transfer flow
- [ ] OTP submit (trước Success navigate): extract features → silent enroll/verify
- [ ] Baseline candidates lưu vào DataStore key `baseline_candidates` (list features); đủ N=3 → backend build profile → lưu DataStore
- [ ] Profile có sẵn → `verifyTransaction()` → kết quả (timestamp, riskScore, reasoning, txSummary) append vào DataStore key `verification_history`
- [ ] **KHÔNG hiển thị result trên Success screen**
- [ ] LocalScorer fallback khi backend down
- [ ] Pipeline chạy `withContext(Dispatchers.IO)` không block UI navigate Success
- [ ] Test rotation device: không duplicate verify call

---

### TASK-024: Dev Menu

- **SRS section:** FR-CL-10 (REQ-16..20)
- **Branch:** `feat/ipay-dev-menu`
- **Dependencies:** TASK-023
- **Status:** planned

**Goal:** Test harness ẩn — Profile/RiskHistory/Session/ManualOverride/DesignPreview accessible via long-press logo Home.

**Done when:**
- [ ] DevMenuScreen list 6 entries với navigate đúng route
- [ ] ProfileInspectorScreen show profile JSON + baseline features list
- [ ] RiskHistoryScreen timeline verification_history (newest first) + clear button với confirm
- [ ] SessionInspectorScreen live view session active (refresh 500ms) + empty state
- [ ] ManualOverrideScreen: Reset profile / Clear baseline / Show next risk score toggle / Force backend down toggle — mỗi action có confirmation dialog
- [ ] Design System Preview accessible từ menu này
- [ ] Existing ProfileScreen.kt logic migrate vào ProfileInspectorScreen
- [ ] Existing "Chế độ ENROLLMENT/VERIFICATION" + "Xóa data" UI từ HomeScreen migrate vào ManualOverrideScreen

---

### TASK-025: E2E smoke + Figma visual diff + production-feel audit

- **SRS section:** FR-CL-08, 09, 10 acceptance
- **Branch:** `feat/ipay-smoke`
- **Dependencies:** TASK-024
- **Status:** planned

**Done when:**
- [ ] Smoke test Login → Home → Transfer flow → Success không crash
- [ ] Visual diff manual ≥ 80% match Figma frame `1:15393`: chụp Figma export PNG cho từng screen + screenshot device → side-by-side trong PR description
- [ ] Lint clean (`./gradlew lint`)
- [ ] **Production-feel audit**: grep toàn codebase trừ `ui/screens/dev/` cho strings: "enrollment", "verification", "fraud", "risk", "behavior", "POC", "Phân tích rủi ro" → 0 match user-facing
- [ ] Behavioral payload gửi backend chứa đầy đủ field mới (decisionTimeOverLimitMs, otpPasted)
- [ ] Test scenario fraud: Tester A enroll 3 lần silent → Tester B chuyển 1 lần → Dev Menu > Risk History thấy score Tester B cao hơn baseline rõ rệt
- [ ] Backend Pydantic backward-compat: client cũ thiếu field mới không 422

---

### TASK-026: mockLocationDetected — GPS spoofing fraud signal

- **SRS section:** FR-CL-11 REQ-01
- **Branch:** `feat/task-026-mock-location`
- **Dependencies:** none
- **Status:** planned

**Goal:** Detect fake-GPS / fraud-farm spoofing qua `Location.isFromMockProvider()` flag trên last-known location của GPS + NETWORK provider.

**Files thay đổi:**

- MODIFY: `app/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/collector/BehavioralCollector.kt` — add private helper `isMockLocationActive()` + wire trong `extractFeatures()`
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/model/BehavioralModels.kt` — add `mockLocationDetected: Boolean = false` field

**Implementation note:**

```kotlin
private fun isMockLocationActive(): Boolean {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return false
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return try {
        sequenceOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> lm.getLastKnownLocation(provider) }
            .any { it.isFromMockProvider }
    } catch (_: SecurityException) { false }
}
```

**Constraints:**

- Read-only — KHÔNG `requestLocationUpdates()`. Zero battery impact.
- Graceful degrade nếu permission không grant → return `false`. Không crash, không bother user.
- KHÔNG runtime permission prompt UI trong scope POC. Van/tester có thể bật qua Settings > App permissions.

**Done when:**

- [ ] Manifest có entry `ACCESS_COARSE_LOCATION`
- [ ] `mockLocationDetected: Boolean` field xuất hiện trong `BehavioralFeatures`
- [ ] `extractFeatures()` populate field qua helper
- [ ] Build green, lint clean
- [ ] Optional manual test: cài fake-GPS app + grant permission → `mockLocationDetected = true` trong Dev Menu Session Inspector

---

### TASK-027: Extended sensor coverage

- **SRS section:** FR-CL-12 REQ-01..12
- **Branch:** `feat/task-027-extended-sensors`
- **Dependencies:** none
- **Status:** planned

**Goal:** Mở rộng sensor coverage từ 2 (accel + gyro) lên 7 sensor để khai thác hết hardware behavioral signal có trên smartphone. Output 12 features mới.

**Files thay đổi:**

- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/model/BehavioralModels.kt`
  - Extend `SensorEvent.type` documentation với 5 type mới: `"magnetometer"`, `"light"`, `"proximity"`, `"linear_acceleration"`, `"rotation_vector"`
  - Add 12 fields vào `BehavioralFeatures`: `magnetometerStabilityX/Y/Z`, `magnetometerMagnitudeAvg`, `lightAvgLux`, `lightStdDevLux`, `proximityNearRatio`, `linearAccelStabilityX/Y/Z`, `rotationVectorPitchStdDev`, `rotationVectorRollStdDev`
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/collector/BehavioralCollector.kt`
  - `startSession()`: register 5 sensor mới qua `sensorManager.registerListener()` cùng accel/gyro existing
  - `sensorListener.onSensorChanged()`: extend switch-case route theo `event.sensor.type` cho 5 type mới. Map quaternion → pitch/roll qua `SensorManager.getOrientation(rotMatrix, orientationOut)`
  - Wire 12 features mới trong `extractFeatures()`
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/scorer/FeatureComputation.kt` — thêm computation helpers cho magnetometer magnitude + proximity ratio + rotation vector orientation

**Implementation note:**

- Sensor delays: light + proximity dùng `SENSOR_DELAY_NORMAL` (~5 Hz đủ). Magnetometer + linear-accel + rotation-vector dùng `SENSOR_DELAY_GAME` (~50 Hz) consistent với accel/gyro.
- Listener single — 1 SensorEventListener route 7 sensor type. Existing `sensorManager.unregisterListener(sensorListener)` đã cover all.
- Rotation vector pitch/roll: dùng `SensorManager.getRotationMatrixFromVector()` + `SensorManager.getOrientation()` — output [azimuth, pitch, roll] in radians.
- Memory budget: 50 Hz × 60s × 5 sensors × 4 bytes × 3 axes = ~180 KB/session. Acceptable.

**Constraints:**

- KHÔNG cần permission gì — sensors free trên Android.
- Backward compat: nếu device không có sensor (vd no proximity) thì `getDefaultSensor()` return null → skip register (existing pattern dùng `?.let`).

**Done when:**

- [ ] 12 fields mới trong `BehavioralFeatures` data class
- [ ] 5 sensors mới registered trong `startSession()`
- [ ] `extractFeatures()` populate đủ 12 features
- [ ] Build green
- [ ] Manual test: chạy app trên device thật → Session Inspector hiển thị giá trị plausible (lightAvgLux > 0 indoor, magnetometer non-zero, etc)

---

### TASK-028: Touch micro-biometrics

- **SRS section:** FR-CL-13 REQ-01..06
- **Branch:** `feat/task-028-touch-microbiometrics`
- **Dependencies:** none (pure extractor — không touch collector / sensor)
- **Status:** planned

**Goal:** Khai thác thêm signal từ raw `TouchEvent` đã có. Output 6 features: tap precision (2), inter-tap velocity (2), hand dominance (1), tap jitter (1).

**Files thay đổi:**

- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/model/BehavioralModels.kt` — add 6 fields vào `BehavioralFeatures`: `avgTapPrecisionOffsetPx`, `tapPrecisionStdDev`, `avgInterTapVelocityPxPerMs`, `interTapVelocityStdDev`, `dominantHandSide`, `tapJitterPostDownMs`
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/scorer/FeatureComputation.kt` — add `computeTouchMicroBiometrics(touchEvents, displayMetrics)` helper
- MODIFY: `app/src/main/java/com/poc/behavioralfraud/data/collector/BehavioralCollector.kt` — wire trong `extractFeatures()` (tương tự pattern `motionAdv`, `cognitiveAdv`)

**Implementation note (per REQ):**

- **REQ-01/02 (tap precision):** option B (approximate) — không cần Compose layout hook. Pseudo:
  ```
  for each ACTION_DOWN at (x, y):
    cell_size_px = 50.dp.toPx()
    nearest_center_x = round(x / cell_size_px) * cell_size_px + cell_size_px / 2
    nearest_center_y = round(y / cell_size_px) * cell_size_px + cell_size_px / 2
    offset = sqrt((x - nearest_center_x)^2 + (y - nearest_center_y)^2)
  avg + std dev qua all DOWN events
  ```
- **REQ-03/04 (inter-tap velocity):** giữa 2 ACTION_DOWN liên tiếp (sort by timestamp): `velocity = sqrt(dx² + dy²) / dt` (px/ms). Avg + std dev.
- **REQ-05 (hand dominance):** dùng `touchCentroidX` (đã có FR-CL-05 REQ-06). Compute ratio = centroid / screenWidth. Threshold:
  - ratio > 0.55 → `"RIGHT"` (right thumb dominant)
  - ratio < 0.45 → `"LEFT"`
  - else → `"AMBIGUOUS"`
- **REQ-06 (tap jitter):** với mỗi ACTION_DOWN, scan forward tìm ACTION_MOVE đầu tiên có cùng pointer (approximate via timestamp gap < 500ms + same x±20px). Compute `Δt = MOVE_timestamp - DOWN_timestamp`. Avg qua tất cả DOWN có MOVE follow-up. 0 nếu không có.

**Constraints:**

- KHÔNG touch `BehavioralCollector` — pure extractor logic.
- KHÔNG cần permission, không cần sensor mới.
- Grid cell size hard-code 50dp constant trong `FeatureComputation.kt`.
- Backward compat: nếu `touchEvents` empty → return all zeros (gracefully).

**Done when:**

- [ ] 6 fields mới trong `BehavioralFeatures`
- [ ] `computeTouchMicroBiometrics()` helper trong `FeatureComputation.kt`
- [ ] `extractFeatures()` wire 6 features
- [ ] Build green
- [ ] Manual test: chạy 1 transfer → Session Inspector cho thấy 6 features có giá trị plausible (avgTapPrecisionOffsetPx > 0, dominantHandSide ∈ {LEFT, RIGHT, AMBIGUOUS})

