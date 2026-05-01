# SRS — Bổ sung: Enhanced Behavioral Feature Extraction

> Append vào `SRS.md` sau FR-CL-04, trước Section 5 (Non-Functional Requirements).  
> Cập nhật section 6.1 (Data Model) và section 10.2 (Acceptance Criteria).  
> Reference: `plan_behavioral_features.md`, `research_behavioral_biometrics.md`

---

### FR-CL-05: Enhanced Feature Extraction — Phase 1

**Description:** Bổ sung feature extraction từ raw data đã có trong BehavioralCollector. Không thay đổi data collection — chỉ sửa `extractFeatures()` và thêm fields vào `BehavioralFeatures`.

**Scope:** Keystroke (Category 1), Touch (Category 2), Motion (Category 3), Navigation (Category 4).

**New features — Keystroke:**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-01 | `typingSpeedTrend` | double | Slope of per-window avg delay. Negative = speeding up (normal), ~0 = constant (scripted), positive = slowing down. Window size: 5 text events. |
| REQ-02 | `typingSpeedVariance` | double | Variance of per-window avg delays. High = irregular rhythm (duress). |
| REQ-03 | `memoryVsReferenceRatio` | double | delay(accountNumber) / delay(note). >1 = account field slower (reading from source). <1 = account field faster (memorized or paste). 0 if either field missing. |
| REQ-04 | `burstCount` | int | Count of bursts: >=3 consecutive text events with delay <50ms followed by pause >500ms. |
| REQ-05 | `avgBurstLength` | double | Average number of events per burst. 0 if no bursts. |

**New features — Touch:**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-06 | `touchCentroidX` | double | Mean x-coordinate of all ACTION_DOWN events. |
| REQ-07 | `touchCentroidY` | double | Mean y-coordinate of all ACTION_DOWN events. |
| REQ-08 | `touchSpreadX` | double | Std dev of x-coordinates. |
| REQ-09 | `touchSpreadY` | double | Std dev of y-coordinates. |
| REQ-10 | `dominantSwipeDirection` | string | Most frequent swipe direction: "UP" / "DOWN" / "LEFT" / "RIGHT" / "NONE". |
| REQ-11 | `avgSwipeLength` | double | Average Euclidean distance per swipe gesture (pixels). |
| REQ-12 | `touchDurationStdDev` | double | Std dev of touch durations (DOWN→UP, ms). |
| REQ-13 | `longPressRatio` | double | Ratio of touches with duration >500ms. |

**New features — Motion:**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-14 | `avgPitch` | double | Mean pitch angle (degrees). Calculated from accelerometer: atan2(y, z). |
| REQ-15 | `avgRoll` | double | Mean roll angle (degrees). Calculated from accelerometer: atan2(x, z). |
| REQ-16 | `pitchStdDev` | double | Std dev of pitch over session. |
| REQ-17 | `rollStdDev` | double | Std dev of roll over session. |
| REQ-18 | `orientationChangeRate` | int | Count of consecutive samples where pitch or roll changes >5°. |
| REQ-19 | `maxOrientationShift` | double | Maximum single-sample orientation change (degrees). |

**New features — Navigation:**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-20 | `inactivityGapCount` | int | Count of gaps >2000ms between any two consecutive events (touch or text). |
| REQ-21 | `maxInactivityGapMs` | long | Longest gap (ms). 0 if no gaps >2000ms. |
| REQ-22 | `totalInactivityMs` | long | Sum of all gaps >2000ms. |
| REQ-23 | `fieldRevisitCount` | int | Number of times a previously-focused field receives focus again. |
| REQ-24 | `hasBacktracking` | boolean | true if `fieldRevisitCount > 0`. |
| REQ-25 | `timePerField` | Map<String, Long> | Time spent on each field (ms), from focus to next field focus. |

**Constraints:**

- All new features computed in `extractFeatures()` from existing raw data
- BehavioralCollector data collection unchanged
- Default value for all new numeric features: 0.0 (or 0)
- Default for `dominantSwipeDirection`: "NONE"
- Default for `timePerField`: empty map
- All features serializable to JSON (for backend transmission)
- `touchCentroidX/Y` are absolute pixel values — backend should be aware of screen dimensions for normalization

**Affected files:**

- MODIFY: `data/model/BehavioralModels.kt` — add fields to `BehavioralFeatures` data class
- MODIFY: `data/collector/BehavioralCollector.kt` → `extractFeatures()` — add computation logic

**Acceptance criteria:**

- [ ] All 25 REQ-IDs have corresponding fields in `BehavioralFeatures`
- [ ] `extractFeatures()` computes all 25 features from existing raw data
- [ ] No new sensors registered, no new event listeners added
- [ ] Default values used when insufficient data (e.g., <2 text events → trend = 0)
- [ ] Existing features unchanged (regression-safe)
- [ ] JSON serialization includes all new fields
- [ ] App compiles and runs without crash

---

### FR-CL-06: Cognitive & Intent Signal Collection — Phase 2

**Description:** Thêm data collection mới trong BehavioralCollector để phát hiện dấu hiệu scam / social engineering. Thu thập các signals về trạng thái tinh thần và context của người dùng.

**Scope:** Cognitive / Intent (Category 6) + device context (Category 5 non-security subset) + minor Touch addition.

**New features — Cognitive / Intent:**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-01 | `isCallActiveDuringSession` | boolean | `AudioManager.getMode()` == `MODE_IN_CALL` or `MODE_IN_COMMUNICATION` at any check point during session. |
| REQ-02 | `callStartedDuringSession` | boolean | Call not active at session start but active at a later check point. |
| REQ-03 | `backgroundSwitchCount` | int | Number of times app went to background and returned to foreground during session. Captured via `onPause`/`onResume`. |
| REQ-04 | `totalBackgroundTimeMs` | long | Total time app spent in background during session (ms). |
| REQ-05 | `avgBackgroundDurationMs` | double | Average per-switch background duration. 0 if no switches. |
| REQ-06 | `preSubmitHesitationCategory` | string | "NORMAL" / "RUSHED" / "HESITANT" based on `timeFromLastInputToConfirm` vs enrollment baseline. RUSHED: < baseline - 2*std. HESITANT: > baseline + 2*std. Defaults to "UNKNOWN" if no baseline. |
| REQ-07 | `sessionHourOfDay` | int | Hour of day (0-23) when session started. |
| REQ-08 | `sessionDayOfWeek` | int | Day of week (1=Monday, 7=Sunday). |
| REQ-09 | `timeSinceLastSessionMs` | long | Time since previous session ended (ms). -1 if first session. Stored in SharedPreferences. |

**New features — Device context (non-security, for feature normalization):**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-10 | `deviceModel` | string | `Build.MODEL` — for context grouping, not security check |
| REQ-11 | `screenWidthPx` | int | Screen width in pixels — for touch coordinate normalization |
| REQ-12 | `screenHeightPx` | int | Screen height in pixels |
| REQ-13 | `screenDensity` | double | `DisplayMetrics.density` — for touch size normalization |
| REQ-14 | `batteryLevel` | int | Battery percentage (0-100). Context signal: charging + low gyro = on desk. |
| REQ-15 | `isCharging` | boolean | Device charging state |

**New features — Touch (minor addition):**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-16 | `multiTouchCount` | int | Count of MotionEvents with `pointerCount > 1`. |
| REQ-17 | `maxPointerCount` | int | Maximum `pointerCount` observed in session. |

**Implementation notes:**

- `AudioManager.getMode()`: no permission required. Check at `startSession()`, at `stopSession()`, and at each `onResume()`.
- `onPause`/`onResume`: BehavioralCollector needs lifecycle awareness. Either pass callbacks from Activity, or make collector implement `DefaultLifecycleObserver`.
- `timeSinceLastSessionMs`: store `lastSessionEndTimestamp` in SharedPreferences at session end.
- Device context features: collected once at session start, not repeatedly.
- `preSubmitHesitationCategory`: requires access to enrollment profile baseline. If no profile exists yet (enrollment phase), output "UNKNOWN".
- `multiTouchCount`: modify `onTouchEvent()` to read `event.pointerCount`.

**Constraints:**

- No new permissions required (AudioManager.getMode(), BatteryManager, Build, DisplayMetrics are all permission-free)
- Data collection passive — no UI interaction or user prompt
- Device context features are NOT security checks (Zimperium handles security). They are normalization context.

**Affected files:**

- MODIFY: `data/model/BehavioralModels.kt` — add fields to `BehavioralFeatures`
- MODIFY: `data/collector/BehavioralCollector.kt` — add lifecycle callbacks, AudioManager check, SharedPreferences, pointerCount collection
- MODIFY: Activity hoặc Fragment hosting transfer screen — pass lifecycle events to collector

**Acceptance criteria:**

- [ ] All 17 REQ-IDs have corresponding fields in `BehavioralFeatures`
- [ ] `isCallActiveDuringSession` correctly detects active phone call
- [ ] `backgroundSwitchCount` increments on each pause→resume cycle
- [ ] `totalBackgroundTimeMs` accumulates background time accurately
- [ ] `timeSinceLastSessionMs` persists across app restarts via SharedPreferences
- [ ] Device context features populated correctly (deviceModel matches actual device)
- [ ] `multiTouchCount` counts events with >1 pointer
- [ ] No new permissions added to AndroidManifest.xml
- [ ] Existing features unchanged
- [ ] JSON serialization includes all new fields

---

### FR-CL-07: Advanced Motion & Pattern Analysis — Phase 3

**Description:** Feature extraction nâng cao cần tương quan dữ liệu giữa các nguồn (touch × sensor, text event sequences).

**Scope:** Motion (Category 3 advanced), Cognitive (Category 6 advanced).

**New features — Motion (advanced):**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-01 | `avgTapAccelSpike` | double | Average peak accel magnitude in 100ms window after each touch DOWN event, minus pre-tap baseline. Measures grasp resistance. |
| REQ-02 | `avgTapRecoveryMs` | double | Average time (ms) for accel magnitude to return to baseline after tap. |
| REQ-03 | `idleGyroRMS` | double | RMS of gyroscope magnitude during idle periods (no touch event for >500ms). Measures hand tremor. |
| REQ-04 | `idleAccelJitter` | double | High-frequency accel variation during idle periods. |

**New features — Cognitive (advanced):**

| REQ-ID | Feature | Type | Description |
|--------|---------|------|-------------|
| REQ-05 | `correctionSameCount` | int | Count of deletion(1-2 chars) immediately followed by insertion(1-2 chars) — typo fix pattern. Detected from `lengthDelta` sequence: -1/-2 then +1/+2 within 1000ms. |
| REQ-06 | `correctionDifferentCount` | int | Count of deletion(>=3 chars) followed by pause(>500ms) then insertion(>=3 chars) — content change pattern. |
| REQ-07 | `screenshotDuringInput` | boolean | Screenshot captured while session is active and user is inputting (before confirm). Android 14+: ScreenCaptureCallback. Below 14: ContentObserver on MediaStore.Images. |

**Implementation notes:**

- Grasp resistance (REQ-01/02): requires correlating touch DOWN timestamps with accelerometer data. For each DOWN event, find accel samples in [t, t+100ms] window. Compute magnitude spike = max(√(x²+y²+z²)) - baseline (avg magnitude in [-200ms, 0] window before tap).
- Idle periods (REQ-03/04): identify intervals >500ms with no touch events. Compute gyro/accel RMS in those intervals only.
- Correction patterns (REQ-05/06): analyze `textChangeEvents` sequence. Look at `lengthDelta` patterns without needing actual text content (privacy-preserving).
- Screenshot detection (REQ-07): `Activity.ScreenCaptureCallback` (API 34+) or `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` for older versions. Must register at session start, unregister at session end.

**Constraints:**

- Touch-sensor correlation requires timestamps in same clock base (SystemClock.elapsedRealtime or System.currentTimeMillis — must be consistent)
- Screenshot detection: `ContentObserver` may have timing delay — acceptable for fraud detection (not real-time blocking)
- Correction patterns: privacy-preserving — analyzes `lengthDelta` only, never reads actual text content
- If insufficient data for any feature (e.g., <3 tap events for grasp), default to 0

**Affected files:**

- MODIFY: `data/model/BehavioralModels.kt` — add fields
- MODIFY: `data/collector/BehavioralCollector.kt` — add screenshot observer, enhance sensor storage for windowed lookup
- MODIFY: `data/collector/BehavioralCollector.kt` → `extractFeatures()` — add cross-correlation logic

**Acceptance criteria:**

- [ ] All 7 REQ-IDs have corresponding fields in `BehavioralFeatures`
- [ ] `avgTapAccelSpike` correlates touch and sensor data correctly (non-zero when tapping while holding device)
- [ ] `idleGyroRMS` is near-zero when device on desk, non-zero when hand-held
- [ ] `correctionSameCount` detects typo-fix patterns
- [ ] `correctionDifferentCount` detects content-change patterns
- [ ] `screenshotDuringInput` detects screenshots on test device
- [ ] Screenshot observer properly registered/unregistered with session lifecycle
- [ ] No privacy violation — text content never read, only `lengthDelta`

---

## Update: Section 6.1 — BehavioralFeatures Data Model

> Replace existing section 6.1. Fields marked 🆕 are additions.

### 6.1 BehavioralFeatures

Output of on-device feature extraction, sent to backend as JSON. Backend needs corresponding Pydantic model.

**Existing fields (unchanged):**

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
| perFieldAvgDelay | Map | Per-field average typing delay |
| avgInterFieldPauseMs | double | Inter-field hesitation (ms) |
| deletionCount | int | Number of deletions |
| deletionRatio | double | Deletion ratio |
| fieldFocusSequence | string | Field focus order |
| timeToFirstInput | long | Time from screen open to first input (ms) |
| timeFromLastInputToConfirm | long | Time from last input to confirm (ms) |

**🆕 Phase 1 — FR-CL-05 (extracted from existing data):**

| Field | Type | Description |
|-------|------|-------------|
| typingSpeedTrend | double | Slope of windowed typing speed |
| typingSpeedVariance | double | Variance of windowed typing speed |
| memoryVsReferenceRatio | double | delay(accountNumber) / delay(note) |
| burstCount | int | Fast-typing burst count |
| avgBurstLength | double | Average burst length |
| touchCentroidX | double | Mean touch x-coordinate |
| touchCentroidY | double | Mean touch y-coordinate |
| touchSpreadX | double | Std dev of touch x |
| touchSpreadY | double | Std dev of touch y |
| dominantSwipeDirection | string | Most frequent swipe direction |
| avgSwipeLength | double | Average swipe distance (px) |
| touchDurationStdDev | double | Std dev of touch durations |
| longPressRatio | double | Ratio of long presses (>500ms) |
| avgPitch | double | Mean device pitch angle (°) |
| avgRoll | double | Mean device roll angle (°) |
| pitchStdDev | double | Std dev of pitch |
| rollStdDev | double | Std dev of roll |
| orientationChangeRate | int | Count of >5° orientation changes |
| maxOrientationShift | double | Max single-sample orientation change |
| inactivityGapCount | int | Count of gaps >2000ms |
| maxInactivityGapMs | long | Longest inactivity gap |
| totalInactivityMs | long | Sum of inactivity gaps |
| fieldRevisitCount | int | Field re-focus count |
| hasBacktracking | boolean | Any field revisited |
| timePerField | Map | Time per field (ms) |

**🆕 Phase 2 — FR-CL-06 (new data collection):**

| Field | Type | Description |
|-------|------|-------------|
| isCallActiveDuringSession | boolean | Phone call during session |
| callStartedDuringSession | boolean | Call started after session start |
| backgroundSwitchCount | int | App background/foreground switches |
| totalBackgroundTimeMs | long | Total background time |
| avgBackgroundDurationMs | double | Avg per-switch background time |
| preSubmitHesitationCategory | string | NORMAL / RUSHED / HESITANT / UNKNOWN |
| sessionHourOfDay | int | Hour (0-23) |
| sessionDayOfWeek | int | Day (1-7) |
| timeSinceLastSessionMs | long | Time since previous session |
| deviceModel | string | Build.MODEL |
| screenWidthPx | int | Screen width |
| screenHeightPx | int | Screen height |
| screenDensity | double | Display density |
| batteryLevel | int | Battery % |
| isCharging | boolean | Charging state |
| multiTouchCount | int | Multi-pointer events |
| maxPointerCount | int | Max simultaneous pointers |

**🆕 Phase 3 — FR-CL-07 (advanced analysis):**

| Field | Type | Description |
|-------|------|-------------|
| avgTapAccelSpike | double | Grasp resistance |
| avgTapRecoveryMs | double | Tap recovery time |
| idleGyroRMS | double | Hand tremor metric |
| idleAccelJitter | double | Idle accel variation |
| correctionSameCount | int | Typo-fix corrections |
| correctionDifferentCount | int | Content-change corrections |
| screenshotDuringInput | boolean | Screenshot while inputting |

**Total: 25 existing + 25 Phase 1 + 17 Phase 2 + 7 Phase 3 = 74 fields**

> Backend `BehavioralFeatures` Pydantic model phải cập nhật tương ứng. Tất cả fields mới PHẢI có default value để backward-compatible (client cũ gửi lên thiếu field → backend không lỗi).

---

## Update: Section 10.2 — Acceptance Criteria — Android Client

> Append thêm vào danh sách hiện tại.

7. Phase 1 features: `extractFeatures()` trả về tất cả 25 features mới với giá trị hợp lệ (không phải tất cả 0 khi có input)
8. Phase 2 features: `isCallActiveDuringSession` phát hiện đúng cuộc gọi đang diễn ra; `backgroundSwitchCount` tăng đúng khi chuyển app
9. Phase 3 features: `avgTapAccelSpike` khác 0 khi tap trên thiết bị cầm tay; `idleGyroRMS` gần 0 trên bàn
10. Tất cả features mới serialize thành JSON và backend Pydantic model parse thành công
11. Backend `BehavioralFeatures` model backward-compatible — client cũ không có fields mới → default values, không lỗi 422

---

# SRS — Bổ sung: iPay Visual Clone for Internal Behavioral Data Collection

> Append vào `SRS.md` sau Section 10.2.
> Reference Figma: `https://www.figma.com/design/5JXePCuiqKQFdrjNHNQVbw/Transfer--hieunt9-` — frame chính `1:15393` (375×812 iOS layout).
> Mục tiêu: clone giao diện iPay (VietinBank) đủ giống thật để team nội bộ tin tưởng dùng → thu thập behavioral data trên môi trường gần production.

> **Design principle (CRITICAL):** Mọi UI người dùng thấy phải giống iPay 100% — KHÔNG được lộ bất kỳ dấu vết test harness nào (enrollment label, verification toggle, risk score display, debug info). Profile build & verification chạy ngầm; team test xem kết quả qua Dev Menu ẩn (long-press logo Home 1.5s).

> **Behavioral session boundary (CRITICAL):** 1 giao dịch = 1 session độc lập. `collector.startSession()` gọi khi user tap "Chuyển tiền trong nước" ở HomeIPayScreen action grid; `collector.stopSession()` gọi khi đến TransferSuccessScreen HOẶC khi user abort (back giữa flow / VM cleared / app backgrounded > X giây). LoginScreen + HomeScreen browsing KHÔNG nằm trong session.

---

### FR-CL-08: Design System Foundation

**Description:** Xây dựng design system iPay đầy đủ — tokens (color/typography/spacing/shape/stroke/elevation) + foundation components — làm nền cho FR-CL-09 và FR-CL-10. Tất cả screens sau này phải tiêu thụ token qua `IPayTheme.*`, KHÔNG hard-code màu, font, dp.

**Scope:** Theme tokens + 13 base Composables + 1 preview screen (truy cập qua Dev Menu).

**New tokens — Theme:**

| REQ-ID | Token | Type | Description |
|--------|-------|------|-------------|
| REQ-01 | `IPayPalette` | object (primitives) | Brand `vietinDarkBlue/10..95`, `vietinRed/40..80`, neutral `Ink/5..95`, semantic `green/orange/blue`, AI gradient stops, Purple. Map 1-1 từ Figma vars. |
| REQ-02 | `IPayColors` | data class (semantic) | text/icon/background/border/button/input/chip/tab/toggle/badge/alert tokens. Tên trùng Figma var (vd `textNeutralPrimary`, `borderBrandPrimary`). |
| REQ-03 | `IPayTypography` | data class | 16 text styles: heading L/S/XS, title L/M/S, body L/M/S, body emphasized XL/L/M/S, label L/M/S/XS. Font fallback `FontFamily.SansSerif` cho POC; document chỗ swap khi có SVN-Gilroy ttf (chưa có trong res/font/ — confirmed). |
| REQ-04 | `IPaySpacing` | data class | s0/s1/s2/s4/s6/s8/s10/s12/s16/s20/s24/s32/s40 + s1.5. |
| REQ-05 | `IPayShapes` | data class | none/xsmall(8)/small(16)/medium(20)/large(24)/full(9999) + r4. |
| REQ-06 | `IPayStroke` | data class | xs(1)/s(1.5)/md(1.75)/lg(2)/xl(4). |
| REQ-07 | `IPayElevation` | data class | small (offset 0,-2 / blur 12) + large (offset 0,4 / blur 16) — match Figma `dropShadow/Small` & `Shadow/Large`. |
| REQ-08 | `IPayTheme` | object + Composable | Public access (`IPayTheme.colors`, `.typography`, …). Wraps `MaterialTheme` để Material widgets vẫn nhận token. Composable provider chấp nhận `spec: IPayThemeSpec` param (xem REQ-24..27 — theme architecture). POC: chỉ implement light mode đầy đủ; Dark mode stub spec để chứng minh API support. |

**New foundation components — under `ui/components/`:**

| REQ-ID | Component | Variants | Description |
|--------|-----------|----------|-------------|
| REQ-09 | `IPayButton` | Primary / Secondary / Ghost / Tertiary × Large / Medium / Small | Primary = horizontal gradient `#005993 → #007DD2`, full radius. Loading state hiển thị `CircularProgressIndicator`. Hỗ trợ leading/trailing icon. Dùng `safeClickable` (debounce 350ms). |
| REQ-10 | `IPayIconButton` | Primary / Secondary / Ghost | Round (radius full) icon button, default size 40dp. |
| REQ-11 | `IPayTextField` | Default / Active / Error / Disabled, prefix/suffix slot | Border `inputBorderDefault → inputBorderActive` khi focus. Caret = `inputCaret`. Hỗ trợ helper text + error text. Underlying = `BasicTextField` để toàn quyền control style. |
| REQ-12 | `IPayTopBar` | Standard / Transparent | Back icon (left), title (center), trailing slot (right). Background `bgNeutralPrimary` hoặc transparent. |
| REQ-13 | `IPayCard` | Plain / Elevated / Outlined | Container radius `r16`. Elevated dùng `IPayElevation.large`. Outlined dùng `borderNeutralPrimary`. Slot content. |
| REQ-14 | `IPayChip` | Default / Selected | Pill (radius full). Border `chipBorder`, label `chipLabel`. Click = `safeClickable`. |
| REQ-15 | `IPayAIChip` | — | Pill với gradient border 5 stops (`#76CFFF → #5F59FB → #F286FF → #FFA0B2 → #FFFFFF80`) + label gradient (`#005BAA → #797DFF → #FD3664`). Indicator AI (sparkle icon). Dành riêng cho AI features (KHÔNG dùng cho promotions). |
| REQ-16 | `IPayAlertBanner` | Info / Warning / Success | Container radius `r16`, leading icon, body text, optional close. Color theo variant (info=`alertInfoBg/Border/Icon`, warning=`warning*`, success=`green*`). |
| REQ-17 | `IPayBottomSheet` | Standard | Modal sheet, top handle 40×4dp `Ink30`, radius top `r24`. Header slot + content slot + footer slot (button row). Dùng `ModalBottomSheet` của Material3 nhưng skin lại theo iPay. |
| REQ-18 | `IPayToggle` | On / Off / Disabled | Track 32×20dp, handle 16×16dp. Off track = `toggleBgDefault`. On track = brand gradient. Animate handle. |
| REQ-19 | `IPaySelection` | RadioCard / Checkbox-card | Card chứa label + description + radio/checkbox phải. Selected → border `borderBrandPrimary` + bg `bgBrandSecondary`. |
| REQ-20 | `IPayHorizontalTabs` | Underline indicator | Row of tabs, active tab `tabLabelActive` + indicator `tabIndicatorActive` (Vietin red 60). Default indicator `tabIndicatorDefault`. |
| REQ-21 | `IPayStatusBadge` | Success / Warning / Error / Info / Neutral | Pill + small dot icon + label. |
| REQ-22 | `IPayNotificationBadge` | Dot / Count | Gradient red `#D71249 → #FFA0B2`, white outside border, label white. Auto co theo content. |

**New preview screen:**

| REQ-ID | Item | Description |
|--------|------|-------------|
| REQ-23 | `DesignSystemPreviewScreen` | Showcase mọi token + component để dev/QA review. Sections: Color palette, Typography scale, Buttons, Inputs, Cards, Chips, Alerts, Bottom sheet trigger, Toggles, Selection cards, Tabs, Badges. **Truy cập qua Dev Menu** (long-press logo Home 1.5s → Dev Menu → Design System Preview). KHÔNG truy cập trực tiếp từ Home. |

**Theme architecture for runtime switching (CRITICAL — 3-layer design):**

> Design system phải tách bạch 3 tầng để hỗ trợ swap theme nhanh chóng (apply theme khác chỉ cần thay 1 spec, không sửa code component nào):
> **Layer 1 — Palette** (primitives, hard-coded color values, KHÔNG dùng trực tiếp trong UI).
> **Layer 2 — Semantic tokens** (data classes — colors/typography/spacing/etc., consume palette, dùng trực tiếp trong component).
> **Layer 3 — Theme spec + registry** (bundle semantic tokens thành 1 spec, registry chứa nhiều variants, runtime switchable).

| REQ-ID | Item | Description |
|--------|------|-------------|
| REQ-24 | `IPayThemeSpec` | Immutable data class bundle 6 token types: `colors: IPayColors`, `typography: IPayTypography`, `spacing: IPaySpacing`, `shapes: IPayShapes`, `stroke: IPayStroke`, `elevation: IPayElevation`. Định nghĩa "1 theme = 1 IPayThemeSpec instance". |
| REQ-25 | `IPayThemes` | Singleton `object` registry chứa các theme variant định danh: tối thiểu `IPayThemes.Default` (light, full implementation theo Figma iPay) + `IPayThemes.Dark` (stub — có thể mirror Default cho POC, mục đích chứng minh API support multi-theme) + 1 demo variant `IPayThemes.Demo` (token khác hẳn Default — vd brand đỏ thay xanh — để test switchability). Adding new theme = thêm 1 entry mới vào object này, KHÔNG sửa code khác. |
| REQ-26 | `IPayTheme(spec, content)` | `@Composable` provider, signature: `fun IPayTheme(spec: IPayThemeSpec = IPayThemes.Default, content: @Composable () -> Unit)`. Bên trong: tạo 6 `CompositionLocal<*>` (1 per token type) provided từ `spec`, đồng thời map sang `MaterialTheme(colorScheme, typography, shapes)` để Material widget tự động pick up khi spec đổi. Bao bọc `content` trong `CompositionLocalProvider(...)` rồi `MaterialTheme(...)`. Spec param có default → app top-level chỉ cần `IPayTheme { content }`. |
| REQ-27 | `IPayTheme` (object accessor) | Singleton `object IPayTheme` expose properties đọc CompositionLocal: `IPayTheme.colors` `@Composable get()`, `IPayTheme.typography` `@Composable get()`, etc. Mọi component **chỉ** đọc token qua object này — KHÔNG nhận spec làm param, KHÔNG cache token. Khi spec ở provider đổi → mọi consumer recompose tự động. Hỗ trợ nested override: bọc subtree với `IPayTheme(spec = IPayThemes.Demo) { ... }` → subtree dùng tokens Demo, ngoài subtree giữ Default. |

**Constraints:**

- KHÔNG hard-code color, font, dp trong code feature — phải đi qua `IPayTheme.*`
- KHÔNG dùng `clickable` trực tiếp — luôn `safeClickable` (CLAUDE.md mandatory)
- Material3 vẫn là nền (cho `Scaffold`, `ModalBottomSheet`, `Snackbar`, …) — `IPayTheme` override `colorScheme`/`typography`/`shapes` cho Material widget không bị override
- Dark mode: implement đầy đủ ở mức API (REQ-25 stub Dark spec) nhưng skip filling Dark colors thật — POC luôn dùng Default. Khi cần ship Dark mode thật chỉ cần điền colors vào `IPayThemes.Dark`, không sửa code khác.
- Không thêm dependency mới ngoài Compose BOM + Material3 đã có
- File path: `app/src/main/java/com/poc/behavioralfraud/ui/theme/` (Color/Spacing/Shape/Typography/Stroke/Elevation/Theme) và `ui/components/` (mỗi component 1 file)
- Mỗi component KHÔNG quá 250 dòng — tách helper nếu cần
- Tất cả component public phải có `Modifier` parameter mặc định `Modifier`
- **Theme switchability rules (mandatory):**
  - Component KHÔNG được nhận token làm param — luôn đọc qua `IPayTheme.colors/.typography/...`
  - Component KHÔNG được cache token (vd `val color = remember { IPayTheme.colors.brandPrimary }`) — recomposition phải re-read
  - Spec phải immutable — đổi theme = swap reference toàn bộ spec, không mutate field
  - Adding new theme = thêm 1 entry vào `IPayThemes` object, ZERO thay đổi component code

**Affected files:**

- NEW: `ui/theme/Color.kt`
- NEW: `ui/theme/Spacing.kt`
- NEW: `ui/theme/Shape.kt`
- NEW: `ui/theme/Stroke.kt` (gộp với Spacing nếu nhỏ)
- NEW: `ui/theme/Elevation.kt`
- NEW: `ui/theme/Typography.kt`
- MODIFY: `ui/theme/Theme.kt` — replace MaterialTheme-only với `IPayTheme` (giữ alias `BehavioralFraudTheme` cho `MainActivity`)
- NEW: `ui/components/SafeClickable.kt`
- NEW: `ui/components/IPayButton.kt` (gộp `IPayIconButton`)
- NEW: `ui/components/IPayTextField.kt`
- NEW: `ui/components/IPayTopBar.kt`
- NEW: `ui/components/IPayCard.kt`
- NEW: `ui/components/IPayChip.kt` (gộp `IPayAIChip`)
- NEW: `ui/components/IPayAlertBanner.kt`
- NEW: `ui/components/IPayBottomSheet.kt`
- NEW: `ui/components/IPayToggle.kt`
- NEW: `ui/components/IPaySelection.kt`
- NEW: `ui/components/IPayHorizontalTabs.kt`
- NEW: `ui/components/IPayBadge.kt` (gộp `IPayStatusBadge` + `IPayNotificationBadge`)
- NEW: `ui/screens/DesignSystemPreviewScreen.kt` (accessed via Dev Menu)

**Acceptance criteria:**

- [ ] Tất cả 8 token files compile, không có TODO/FIXME chưa giải quyết
- [ ] `IPayTheme.colors.brandPrimary` == `#005BAA` (Figma `text/textBrandPrimary`)
- [ ] 13 components (REQ-09..22) build được, render đúng trong `DesignSystemPreviewScreen`
- [ ] `safeClickable` debounce 350ms — double-tap test không gây 2 lần navigation
- [ ] Không screen nào trong codebase còn `Color(0xFF…)` literal hoặc `.dp` hard-code spacing (trừ token file)
- [ ] App compile + run + mở preview qua Dev Menu không crash
- [ ] Mọi component có `Modifier` param đầu tiên không phải `text`/`onClick` (Compose convention)
- [ ] Lint sạch (`./gradlew lint` không add warning mới)
- [ ] **Theme architecture (REQ-24..27):**
  - [ ] `IPayThemeSpec` data class tồn tại, bundle 6 token types
  - [ ] `IPayThemes` object expose `Default` (full) + `Dark` (stub) + `Demo` (alternative palette)
  - [ ] `IPayTheme(spec, content)` Composable accept spec param, default = `IPayThemes.Default`
  - [ ] `IPayTheme` accessor object đọc 6 token types qua `CompositionLocal`
  - [ ] **Runtime swap test**: `DesignSystemPreviewScreen` có button "Switch theme" cycle qua Default → Dark → Demo → Default; tất cả token + component update trong 1 frame, KHÔNG crash, KHÔNG cần reload screen
  - [ ] **Nested override test**: bọc 1 subtree với `IPayTheme(spec = IPayThemes.Demo) { ... }` → subtree đó dùng tokens Demo, ngoài subtree giữ Default
  - [ ] **Adding new theme test**: tạo 1 entry mới `IPayThemes.Test = IPayThemeSpec(...)` ở local branch — chứng minh KHÔNG cần sửa file component nào, chỉ cần tham chiếu spec mới ở provider
  - [ ] Material3 mapping (`MaterialTheme(colorScheme/typography/shapes = ...)`) tự update khi spec đổi — verify bằng `Snackbar`/`AlertDialog` Material3 đổi màu theo spec mới

---

### FR-CL-09: Authentication Flow — Login + Touch ID

**Description:** Màn login mở app, mô phỏng iPay: chào người dùng + nhập PIN 6 số + Touch ID (biometric). Login KHÔNG nằm trong behavioral fraud session — chỉ là authentication. Tuy nhiên có thể thu thập login signal riêng (debug only).

**Scope:** 1 screen + biometric integration + navigation tới Home. KHÔNG có behavioral session active ở screen này.

**New screens:**

| REQ-ID | Screen | Description |
|--------|--------|-------------|
| REQ-01 | `LoginScreen` | Top: logo VietinBank (placeholder asset OK) + greeting "Xin chào, [Username]". Middle: 6 PIN dots + numeric keypad (3×4). Bottom: "Dùng Touch ID" button + "Quên mật khẩu?" link. Background `bgNeutralPrimary` hoặc subtle gradient brand light. |

**New components — supporting Login:**

| REQ-ID | Component | Description |
|--------|-----------|-------------|
| REQ-02 | `IPayPinDots` | Hiển thị 6 ô PIN, ô đã nhập = filled circle `brandPrimary`, ô chưa nhập = empty `borderNeutralPrimary`. Animation: filled bounce nhẹ khi nhập. Reuse được cho OTP screen ở chế độ "show digit". |
| REQ-03 | `IPayNumericKeypad` | 3×4 grid: 1-9, 0, biometric icon (trái), backspace (phải). Mỗi nút round 64dp, ripple + `safeClickable`. Phát audio click khi tap (optional, theo iPay). |

**Authentication logic:**

| REQ-ID | Behavior | Description |
|--------|----------|-------------|
| REQ-04 | Touch ID launch | Click "Dùng Touch ID" → `BiometricPrompt.authenticate()` (androidx.biometric). Sử dụng `BiometricManager.canAuthenticate()` để check: `BIOMETRIC_SUCCESS` → show button; `NO_HARDWARE` / `NONE_ENROLLED` / `SECURITY_UPDATE_REQUIRED` → ẩn button. Nếu success → vào Home. Fail → toast hoặc inline error. |
| REQ-05 | PIN auth (mock) | Nhập đủ 6 số → auto-submit. POC mock: PIN nào cũng pass → vào Home. Gate behind `BuildConfig.DEBUG` hoặc log warning rõ "POC PIN mock — KHÔNG dùng cho production". |

**Behavioral coverage (KHÔNG share với transfer session):**

| REQ-ID | Hook | Description |
|--------|------|-------------|
| REQ-06 | Login signal riêng (optional, debug) | LoginScreen có thể dùng 1 collector instance riêng (key DataStore `login_behavioral_session`) để capture PIN keystroke rhythm — chỉ phục vụ debug/inspect qua Dev Menu, KHÔNG share session id với transfer flow, KHÔNG gửi backend. Implementation có thể skip nếu không cần debug login behavior. |

**Constraints:**

- Dùng `androidx.biometric:biometric:1.2.0-alpha05` hoặc latest stable (1.1.0 đã outdated)
- KHÔNG persist PIN — POC mock pass mọi giá trị
- Login KHÔNG mở `transfer behavioral session` — session thật bắt đầu ở Home tap "Chuyển tiền"
- Greeting username hard-code "Vandz" (POC) — sau có thể đọc từ profile

**Affected files:**

- NEW: `ui/screens/login/LoginScreen.kt`
- NEW: `ui/screens/login/LoginViewModel.kt` (state + biometric launcher)
- NEW: `ui/components/IPayPinDots.kt`
- NEW: `ui/components/IPayNumericKeypad.kt`
- MODIFY: `MainActivity.kt` — `LoginScreen` là start destination, sau auth → Home (qua NavHost FR-CL-10 REQ-09)
- MODIFY: `app/build.gradle.kts` — thêm `androidx.biometric:biometric:1.2.0-alpha05`

**Acceptance criteria:**

- [ ] App khởi động → vào `LoginScreen`, không còn vào Home trực tiếp
- [ ] Nhập 6 số PIN → tự động navigate sang Home
- [ ] `BiometricManager.canAuthenticate()` không trả `BIOMETRIC_SUCCESS` → button Touch ID ẩn
- [ ] Trên device có biometric → tap button → biometric prompt hiện → success vào Home
- [ ] BehavioralCollector KHÔNG có session active sau khi login (vào Home rồi vẫn idle)
- [ ] Quay lại Login (vd back button từ Home) reset PIN dots về empty

---

### FR-CL-10: Transfer Flow E2E (iPay Skin) + Silent Behavioral Pipeline

**Description:** Skin/làm lại 7 màn của flow chuyển tiền iPay — từ Home đến Thành công, qua decision point "vượt hạn mức Napas". Mục tiêu visual fidelity 80%+ vs Figma `1:15393` để team nội bộ dùng như app thật. Behavioral collection chạy ngầm trong scope transfer flow; profile build + verification ẩn hoàn toàn khỏi UI thật.

**Scope:** 7 màn (5 mới + 2 skin lại) + state machine + silent behavioral pipeline + Dev Menu.

**New / updated screens:**

| REQ-ID | Screen | Action | Description |
|--------|--------|--------|-------------|
| REQ-01 | `HomeIPayScreen` | REPLACE `HomeScreen` | Top: header gradient brand + greeting + avatar + IPayNotificationBadge. Quick balance card (số dư mock). Action grid 4×N (Chuyển tiền trong nước, Nạp tiền, Thanh toán, …) — mỗi item là `IPayCard` icon + label. Promotions row dùng card thường (KHÔNG dùng IPayAIChip — AIChip dành cho AI features). **KHÔNG hiển thị**: enrollment count, "Chế độ ENROLLMENT/VERIFICATION" toggle, profile status, "Cách hoạt động" guide, "Xóa dữ liệu" button — toàn bộ chuyển vào Dev Menu. **Hidden affordance**: long-press logo top-left 1.5s → navigate `DevMenuScreen`. Tap "Chuyển tiền trong nước" action → trigger `collector.startSession()` rồi navigate `TransferTypeScreen`. |
| REQ-02 | `TransferTypeScreen` | NEW | Top bar "Chuyển tiền". Body: 2 `IPaySelection` cards — "Chuyển trong VietinBank" / "Chuyển liên ngân hàng (Napas)" — kèm description + icon. Tap → navigate sang `RecipientScreen` với arg `transferType`. |
| REQ-03 | `RecipientScreen` | NEW | Top bar "Người nhận". Section "Nhập số tài khoản" với `IPayTextField` (keyboard `Number`) — focus đầu tiên (account_number trước bank_select). Section "Chọn ngân hàng" — list scrollable các bank (mock data ~20 banks, mỗi item `IPayCard` flat icon + tên). Section "Gần đây" — row `IPayChip` các STK gần đây (mock 3 items). Button "Tiếp tục" sticky bottom, disabled cho đến khi đủ STK + bank. |
| REQ-04 | `TransferFormScreen` | REPLACE `TransferScreen` | Top bar "Khởi tạo chuyển tiền". Sticky top: card hiển thị thông tin người nhận (đã chọn ở màn trước). Form: `IPayTextField` Số tiền (Number, format thousand-separator) + `IPayTextField` Nội dung (Text, 100 ký tự). Source selector: `IPaySelection` chọn nguồn tiền (mặc định "Tài khoản thanh toán"). `IPayAlertBanner` info hiển thị hạn mức Napas còn lại (mock). Button "Tiếp tục" sticky bottom. Khi user nhập số tiền > hạn mức Napas → set state `state.overLimit = true`. |
| REQ-05 | `OverNapasLimitBottomSheet` | NEW | `IPayBottomSheet` trigger khi `overLimit && tap "Tiếp tục"`. Header icon warning + tiêu đề "Vượt hạn mức Napas". Body text giải thích + so sánh hạn mức. Footer 2 button: Primary "Chuyển bằng kênh thường" + Ghost "Huỷ". Tap primary → đóng sheet → set `state.transferChannel = "regular"` → navigate `OtpScreen`. |
| REQ-06 | `OtpScreen` | NEW | Top bar "Xác thực OTP". Body: tiêu đề "Nhập mã OTP" + body subtitle "Mã đã gửi tới ****1234". 6 ô OTP (reuse `IPayPinDots` ở mode "show digit") + numeric keypad. Counter "Gửi lại sau 60s" + button "Gửi lại" sau khi count xong. Nhập đủ 6 số → auto-submit → loading 1.5s (mock) → navigate `TransferSuccessScreen`. |
| REQ-07 | `TransferSuccessScreen` | REPLACE `TransferResultViews` | Full screen success **production-feel**. Top icon `IPayStatusBadge` Success large. Tiêu đề "Chuyển tiền thành công" + số tiền lớn. Card chi tiết giao dịch (người nhận, STK, ngân hàng, nội dung, mã GD, thời gian). Button row: Primary "Về trang chủ" + Secondary "Lưu biên lai". **KHÔNG hiển thị** "Phân tích rủi ro" / risk score / verification result — toàn bộ chuyển vào Dev Menu > Risk History. UI trông như iPay thật, không có dấu vết POC. |

**State machine — TransferOrchestratorViewModel:**

| REQ-ID | State | Description |
|--------|-------|-------------|
| REQ-08 | Single VM bao flow | `TransferOrchestratorViewModel` quản lý: `transferType`, `recipient`, `amount`, `note`, `source`, `transferChannel`, `otp`, `txStatus`. State tích hợp `riskResult` (lưu silent, KHÔNG bind UI) chỉ để Dev Menu đọc. Dùng `Channel<TransferEvent>` cho navigation events (CLAUDE.md mandatory cho banking). |

**Navigation:**

| REQ-ID | Item | Description |
|--------|------|-------------|
| REQ-09 | NavController + NavHost | **Replace `MainActivity.kt:30` switch-case `when (currentScreen)` bằng `NavHost`** (verified: code hiện dùng switch-case, KHÔNG phải NavHost — CLAUDE.md mô tả sai). Routes: `login`, `home`, `transfer/type`, `transfer/recipient`, `transfer/form`, `transfer/otp`, `transfer/success`, `dev`, `dev/profile`, `dev/risk-history`, `dev/session`, `dev/design-system`. Pass arg qua `SavedStateHandle` hoặc nav arg, không dùng global state. |

**Silent behavioral pipeline:**

| REQ-ID | Hook | Description |
|--------|------|-------------|
| REQ-10 | Session lifecycle gắn với transfer flow | `TransferOrchestratorViewModel.init { collector.startSession() }`. `onCleared() { if (!sessionEnded) collector.stopSession() }` để cover abort cases. Success path explicit gọi `collector.stopSession()` + persist BehavioralSession history. Background > 30s → end session với reason "backgrounded". 1 giao dịch = 1 session id độc lập. **LoginScreen + HomeScreen browsing KHÔNG nằm trong session.** |
| REQ-11 | Touch interceptor | Mỗi màn trong transfer flow (TransferType, Recipient, Form, OtpScreen, Success) wrap với `pointerInteropFilter` gọi `collector.onTouchEvent()`. `IPayBottomSheet` cũng phải wrap. **Home + Login KHÔNG wrap.** |
| REQ-12 | Field focus sequence | Mỗi `IPayTextField` (STK, amount, note, otp) nối field name vào `collector.onFieldFocus(fieldName)` (đúng tên hàm hiện tại trong BehavioralCollector). Sequence cuối session = `account_number → bank_select → amount → note → otp` (KHÔNG có `pin_pad` vì PIN entry ở Login không nằm trong session). |
| REQ-13 | Decision time over-limit | `OverNapasLimitBottomSheet` ghi `bottomSheetShownTimestamp` khi mở, `bottomSheetDecisionMs` khi user tap primary/cancel — feature mới `decisionTimeOverLimitMs` (long, default 0) thêm vào `BehavioralFeatures`. |
| REQ-14 | Paste detection trên OTP | Field OTP có hook detect paste — dùng `lengthDelta >= 3` của `onTextChanged` hiện tại. Set flag riêng `otpPasted: boolean` trong `BehavioralFeatures` (default false). |
| REQ-15 | Silent baseline accumulation + verification | Khi tap "Tiếp tục" ở OtpScreen (trước navigate Success): extract features → đọc profile từ DataStore. Nếu chưa đủ N=3 baseline sessions → `BackendClient.enrollSession()` lưu baseline; đủ N → backend tự build profile, lưu DataStore. Nếu đã có profile → `BackendClient.verifyTransaction()` → response `riskScore + reasoning`. **Lưu silent vào `verification_history` DataStore với timestamp**, KHÔNG hiển thị trên Success screen. LocalScorer fallback khi backend down. Toàn bộ pipeline chạy trong `withContext(Dispatchers.IO)`, KHÔNG block UI navigate sang Success. |

**Dev Menu (test harness, ẩn khỏi user):**

| REQ-ID | Screen | Description |
|--------|--------|-------------|
| REQ-16 | `DevMenuScreen` | Truy cập qua long-press logo Home 1.5s. Top bar "Dev Menu (POC only)". List entries: Profile Inspector / Risk Score History / Session Inspector / Manual Override / Design System Preview / Clear All Data. Mỗi entry navigate sang sub-screen riêng. |
| REQ-17 | `ProfileInspectorScreen` | Show profile JSON hiện tại trong DataStore (LLM-generated summary + baseline features). Nếu chưa có profile → "Chưa đủ baseline (X/3 transactions)". Button "Xem features baseline" mở list raw features đã capture. |
| REQ-18 | `RiskHistoryScreen` | Timeline list các verification trong DataStore: timestamp + riskScore (0-100) + reasoning + transaction summary. Sort newest first. Empty state "Chưa có verification". Button "Clear history" với confirmation. |
| REQ-19 | `SessionInspectorScreen` | Live view session hiện tại đang collect (nếu có): touch count, sensor std, current focused field, elapsed ms. Refresh mỗi 500ms. Empty state "Không có session active" khi ở ngoài transfer flow. |
| REQ-20 | `ManualOverrideScreen` | Test scenario controls: [Reset profile] (xoá profile để build lại), [Clear baseline candidates], [Show next risk score on Success] toggle (cho phép hiển thị 1 lần next time để demo), [Force backend down] toggle (test LocalScorer fallback). Mỗi action có confirmation dialog. |

**Constraints:**

- Visual fidelity ≥ 80% vs Figma — kiểm bằng visual diff manual: chụp Figma export PNG cho từng frame + screenshot device tương ứng, attach side-by-side trong PR description
- KHÔNG dùng `clickable`, KHÔNG hard-code màu/font/dp (CLAUDE.md mandatory)
- Mọi navigation đi qua Channel event (one-time event), KHÔNG StateFlow (tránh duplicate trigger sau rotation)
- File mỗi screen ≤ 500 dòng — tách composable phụ nếu cần
- Mock data (banks, balance, recipient suggestions) sống trong `data/mock/` package mới, KHÔNG hard-code trong screen
- KHÔNG tạo `strings.xml` nội bộ — strings tiếng Việt inline trong screen vì là POC; sau migrate khi scale
- **Production-feel rule**: HomeIPayScreen + TransferSuccessScreen KHÔNG được lộ bất kỳ string/UI nào liên quan tới "enrollment", "verification", "risk", "behavior", "fraud", "POC" — toàn bộ chuyển sang Dev Menu

**Affected files:**

- NEW: `ui/screens/login/LoginScreen.kt` (đã ở FR-CL-09)
- MODIFY: `ui/screens/HomeScreen.kt` → `HomeIPayScreen` (full rewrite, **strip enrollment UI**)
- NEW: `ui/screens/transfer/TransferTypeScreen.kt`
- NEW: `ui/screens/transfer/RecipientScreen.kt`
- MODIFY: `ui/screens/TransferScreen.kt` → `TransferFormScreen.kt` (rename + rewrite, giữ behavioral hook)
- NEW: `ui/screens/transfer/OverNapasLimitBottomSheet.kt`
- NEW: `ui/screens/transfer/OtpScreen.kt`
- MODIFY: `ui/screens/TransferResultViews.kt` → `TransferSuccessScreen.kt` (rewrite, **strip risk score UI**)
- NEW: `ui/screens/transfer/TransferOrchestratorViewModel.kt`
- NEW: `ui/screens/dev/DevMenuScreen.kt`
- NEW: `ui/screens/dev/ProfileInspectorScreen.kt`
- NEW: `ui/screens/dev/RiskHistoryScreen.kt`
- NEW: `ui/screens/dev/SessionInspectorScreen.kt`
- NEW: `ui/screens/dev/ManualOverrideScreen.kt`
- MODIFY: `ui/screens/ProfileScreen.kt` → MOVE to `ui/screens/dev/ProfileInspectorScreen.kt` (rewrite based on existing logic)
- NEW: `data/mock/MockData.kt` — banks list, recipients, balance
- MODIFY: `data/model/BehavioralModels.kt` — thêm `decisionTimeOverLimitMs: Long = 0`, `otpPasted: Boolean = false`
- MODIFY: `data/repository/ProfileRepository.kt` — thêm methods cho `verification_history` DataStore key (list of {timestamp, riskScore, reasoning, txSummary})
- MODIFY: `data/collector/BehavioralCollector.kt` — log decision time hook (REQ-13). API hiện có (`startSession/stopSession/onFieldFocus/onTextChanged/onTouchEvent`) đủ dùng, KHÔNG cần rename.
- MODIFY: `MainActivity.kt:30` — replace `when (currentScreen)` switch-case bằng `NavHost`
- MODIFY: `ui/screens/TransferViewModel.kt` — refactor: tách phần "POC test concept" (enrollment count, mode toggle UI state) ra khỏi production VM, chuyển logic vào `TransferOrchestratorViewModel` mới + Dev Menu VMs

**Acceptance criteria:**

- [ ] App khởi động: Login → Home iPay (production-feel) → Transfer flow → Success
- [ ] HomeIPayScreen KHÔNG có chữ "Enrollment", "Verification", "Profile", "Behavior" hiển thị
- [ ] TransferSuccessScreen KHÔNG hiển thị risk score / "Phân tích rủi ro" card
- [ ] 7 màn render đúng theo Figma — không còn dấu vết Material default (vd `TopAppBar` Material xanh đậm cũ)
- [ ] Bottom sheet vượt hạn mức trigger đúng khi amount > hạn mức mock (vd 10,000,000 VND)
- [ ] Behavioral session bắt đầu khi tap "Chuyển tiền trong nước" Home; kết thúc ở Success/abort
- [ ] `sessionDurationMs` chỉ cover từ Home tap đến Success, KHÔNG bao gồm Login + Home browsing
- [ ] Field focus sequence ghi đúng `account_number → bank_select → amount → note → otp` (KHÔNG có pin_pad)
- [ ] User abort giữa flow (back từ Form/OTP) → session vẫn end đúng cách (VM cleared → stopSession called)
- [ ] OTP screen: paste 6 số từ clipboard → flag `otpPasted = true`
- [ ] `decisionTimeOverLimitMs` > 0 khi user thấy bottom sheet và tap → đo được hesitation
- [ ] Backend `verifyTransaction()` hoặc `enrollSession()` được gọi 1 lần ở OTP submit, không gọi nhiều lần
- [ ] Verification result lưu vào `verification_history` DataStore, **không hiển thị Success screen**
- [ ] Long-press logo Home 1.5s → vào DevMenuScreen
- [ ] Dev Menu > Risk History hiển thị verification history với timestamp + score + reasoning
- [ ] Dev Menu > Manual Override > Reset profile → xoá profile + baseline, lần giao dịch tiếp theo build lại baseline
- [ ] LocalScorer fallback hoạt động khi backend down (giống FR-CL-02)
- [ ] Rotation device trên mọi màn input → không duplicate navigation, không mất dữ liệu form
---

### FR-CL-11: Threat Indicator Signals — Mobile Anti-Fraud

**Description:** Bổ sung threat-indicator signal trực tiếp từ Android system cho fraud detection. Hiện tại Zimperium SDK upstream đã cover accessibility services + USB debugging + screen overlay → KHÔNG duplicate ở đây. Section này chỉ thêm các signal Zimperium chưa có hoặc orthogonal.

**Scope:** 1 feature mới trong `BehavioralFeatures`. Read-only — không request location updates, chỉ đọc OS-cached state.

**New features — extend BehavioralFeatures:**

| REQ-ID | Field | Type | Default | Description |
|--------|-------|------|---------|-------------|
| REQ-01 | `mockLocationDetected` | boolean | `false` | True nếu `Location.isFromMockProvider()` flag được set trên last-known location của ít nhất 1 trong 2 provider (GPS, NETWORK). Detect fraud farm / fake-GPS app spoofing vị trí (vd hiển thị "đang ở VN" trong khi remote). Read-only — đọc OS cache, KHÔNG request location updates. Permission `ACCESS_COARSE_LOCATION` cần được declare trong manifest; nếu user chưa grant → silently return `false` (graceful degrade — feature disabled, không crash, mọi feature khác vẫn collect). |

**Permission impact:**

- NEW manifest entry: `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`
- Runtime grant prompt: KHÔNG cần xin trong POC — collector tự handle ungranted state. Để bật signal Van/tester có thể grant qua Settings.

**Out of scope (Zimperium SDK đã cover):**

- Accessibility services enabled
- USB debugging / Developer options
- Screen overlay permission active

---

### FR-CL-12: Extended Sensor Coverage

**Description:** Mở rộng sensor coverage từ 2 (Accelerometer + Gyroscope) lên 7 sensor để khai thác hết hardware behavioral signal có trên smartphone. Mỗi sensor mới cung cấp 1 axis/dimension orthogonal cho fraud detection.

**Scope:** 5 sensor mới registered trong `startSession()` cùng với accel/gyro existing. Output 12 features mới trong `BehavioralFeatures`. Memory cost: ~60 KB/session (acceptable).

**New features — extend BehavioralFeatures:**

| REQ-ID | Field | Type | Default | Sensor | Description |
|--------|-------|------|---------|--------|-------------|
| REQ-01 | `magnetometerStabilityX` | double | `0.0` | `TYPE_MAGNETIC_FIELD` | Std dev of X-axis magnetic field (μT) over session. Environmental fingerprint — user thật có pattern lặp (cùng bàn làm, cùng nhà → magnetic pattern stable). Đột ngột thay đổi giữa session = di chuyển bất thường. |
| REQ-02 | `magnetometerStabilityY` | double | `0.0` | `TYPE_MAGNETIC_FIELD` | Y-axis std dev — same. |
| REQ-03 | `magnetometerStabilityZ` | double | `0.0` | `TYPE_MAGNETIC_FIELD` | Z-axis std dev — same. |
| REQ-04 | `magnetometerMagnitudeAvg` | double | `0.0` | `TYPE_MAGNETIC_FIELD` | Avg of √(x²+y²+z²) — magnitude. Indoor (kim loại / cáp) vs outdoor có magnitude khác baseline. Anomaly nếu lệch >30% baseline. |
| REQ-05 | `lightAvgLux` | double | `0.0` | `TYPE_LIGHT` | Average ambient light (lux) over session. Tối thui (<10 lux) lúc 3am = sus; outdoor sáng (>1000 lux) trong giờ ngủ = anomaly. Sensor delay: NORMAL (~5 Hz đủ). |
| REQ-06 | `lightStdDevLux` | double | `0.0` | `TYPE_LIGHT` | Std dev of light. Đột biến = di chuyển in/outdoor, hoặc user che màn hình → cố ý phòng quay lén = sus. |
| REQ-07 | `proximityNearRatio` | double | `0.0` | `TYPE_PROXIMITY` | Tỉ lệ thời gian (0..1) phone "near" (đa số device là binary near/far). Fraud farm phone đặt bàn (always far). User thật cầm tay đôi khi near (pre-call gesture, áp tai). |
| REQ-08 | `linearAccelStabilityX` | double | `0.0` | `TYPE_LINEAR_ACCELERATION` | Std dev of linear acceleration X-axis (gravity-removed). Cleaner motion signal — accelerometer raw bị gravity offset 9.8 m/s² ở axis Z khi đặt bàn; linear-accel trừ luôn → tap micro-jolt rõ hơn. Sensor delay GAME (~50 Hz). |
| REQ-09 | `linearAccelStabilityY` | double | `0.0` | `TYPE_LINEAR_ACCELERATION` | Y-axis std dev. |
| REQ-10 | `linearAccelStabilityZ` | double | `0.0` | `TYPE_LINEAR_ACCELERATION` | Z-axis std dev. |
| REQ-11 | `rotationVectorPitchStdDev` | double | `0.0` | `TYPE_ROTATION_VECTOR` | Std dev of pitch derived from rotation vector (quaternion fused from 3 sensors → real device orientation). Pitch std dev = mức độ rung trong giờ thao tác. |
| REQ-12 | `rotationVectorRollStdDev` | double | `0.0` | `TYPE_ROTATION_VECTOR` | Std dev of roll. Bổ sung pitch — orthogonal axis. |

**Implementation note:**

- `BehavioralCollector.startSession()`: register 5 sensor mới qua `SensorManager.registerListener()` cùng accel/gyro existing. Light + proximity dùng `SENSOR_DELAY_NORMAL`; magnetometer + linear-accel + rotation-vector dùng `SENSOR_DELAY_GAME` (consistent với accel/gyro).
- `SensorEventListener.onSensorChanged()`: extend existing switch-case để route theo `event.sensor.type`. Map quaternion → pitch/roll qua `SensorManager.getOrientation()`.
- `BehavioralCollector.stopSession()`: existing `unregisterListener(sensorListener)` đã cover (1 listener, multiple sensors).
- `SensorEvent` data class cần extend `type` enum: hiện có `"accelerometer"`, `"gyroscope"` → thêm `"magnetometer"`, `"light"`, `"proximity"`, `"linear_acceleration"`, `"rotation_vector"`.
- Compute features trong `extractFeatures()` hoặc helper trong `FeatureComputation.kt`.

**Permission impact:** None. Sensors free trên Android — không cần runtime permission.

**Memory budget:** 50 Hz × 60s × 5 sensors × 4 bytes/sample × 3 axes = ~180 KB/session. Acceptable cho POC; có thể downsample magnetometer + linear-accel xuống 25 Hz nếu cần optimize.

---

### FR-CL-13: Touch Micro-Biometrics

**Description:** Khai thác thêm signal từ raw `TouchEvent` đã có (KHÔNG thay đổi collector). Output 6 features mới qua extractor logic mới trong `FeatureComputation.kt`.

**Scope:** Pure extractor — KHÔNG touch BehavioralCollector. KHÔNG thêm sensor / permission. Chỉ derive thêm signal từ `touchEvents: List<TouchEvent>` đã có sẵn.

**New features — extend BehavioralFeatures:**

| REQ-ID | Field | Type | Default | Description |
|--------|-------|------|---------|-------------|
| REQ-01 | `avgTapPrecisionOffsetPx` | double | `0.0` | Mỗi `ACTION_DOWN` event, tính khoảng cách từ tap point (x, y) đến center của 50dp×50dp grid cell gần nhất (option B — approximate, không cần Compose layout hook). Avg qua tất cả tap. Bot/script tap dead-center (offset ~0). Người tap miss center ±5-15px tự nhiên. |
| REQ-02 | `tapPrecisionStdDev` | double | `0.0` | Std dev của offset trên. Bot offset rất consistent (low std). Người high std do hand jitter. |
| REQ-03 | `avgInterTapVelocityPxPerMs` | double | `0.0` | Giữa 2 `ACTION_DOWN` liên tiếp (cùng field hoặc cross-field): velocity = √(Δx² + Δy²) / Δt (px/ms). Personal biometric mạnh hơn delay đơn thuần — tốc độ ngón tay di chuyển giữa các tap là chữ ký riêng. |
| REQ-04 | `interTapVelocityStdDev` | double | `0.0` | Std dev của velocity. Variance nhỏ = motor pattern ổn định; variance lớn = đang căng thẳng/duress hoặc bị duress hold. |
| REQ-05 | `dominantHandSide` | string enum | `"AMBIGUOUS"` | `"LEFT"` / `"RIGHT"` / `"AMBIGUOUS"`. Dùng `touchCentroidX` đã có (FR-CL-05): nếu centroid >55% screen width consistent qua session = right-hand thumb dominant; <45% = left-hand thumb; ở giữa = AMBIGUOUS. Hand dominance là personal trait stable. Đột nhiên đổi side giữa các session = device bị share hoặc fraud. |
| REQ-06 | `tapJitterPostDownMs` | double | `0.0` | Avg khoảng thời gian (ms) từ `ACTION_DOWN` đến `ACTION_MOVE` đầu tiên trong cùng tap (nếu user tap-and-hold-vibrate). 0 nếu tap clean (no MOVE follow-up). Người có micro-jitter tự nhiên trên skin (small MOVE events ngay sau DOWN). Bot synthetic không có. |

**Implementation note:**

- All logic trong `data/scorer/FeatureComputation.kt` (hoặc tạo `TouchMicroBiometrics.kt` nếu muốn tách file).
- Input: existing `touchEvents: List<TouchEvent>` từ `BehavioralCollector.touchEvents`.
- Output: thêm 6 fields vào `BehavioralFeatures` data class.
- Wire trong `extractFeatures()` — tương tự pattern Phase 3 (motionAdv, cognitiveAdv).
- Grid cell size: 50dp default (configurable via const). Dùng `displayMetrics.density` để convert dp→px.

**Permission impact:** None. Pure extractor.

---

## Update: Section 4 — Mobile Detection Pipeline (mirror Backend FR-BE-RULES)

> Bổ sung sau backend đã ship TASK-013/014/015 (PR #16/#17/#18). Mobile cần align để (a) inspect 21 features mới trong Dev Menu, (b) khi backend down LocalScorer fallback có rules đồng nhất với BE, (c) E2E acceptance test verify pipeline thực sự work.

### FR-CL-14: Dev Menu surface 21 new features

**Description:** SessionInspectorScreen hiện chỉ hiển thị baseline 49 fields từ Phase 1-3. Sau khi TASK-026/027/028 merge thêm 19 fields + 2 fields cũ FR-CL-10, Van cần Dev Menu surface để verify trên device thật rằng giá trị plausible (vd lightAvgLux > 0 indoor, dominantHandSide đúng).

**Scope:** UI-only update trong `ui/screens/dev/SessionInspectorScreen.kt`. Không touch collector / scorer.

**New requirements:**

| REQ-ID | Section | Fields |
|--------|---------|--------|
| REQ-01 | "Hesitation" | decisionTimeOverLimitMs, otpPasted |
| REQ-02 | "Threat indicators" | mockLocationDetected |
| REQ-03 | "Extended sensors — Magnetometer" | magnetometerStabilityX/Y/Z, magnetometerMagnitudeAvg |
| REQ-04 | "Extended sensors — Light + Proximity" | lightAvgLux, lightStdDevLux, proximityNearRatio |
| REQ-05 | "Extended sensors — Linear acceleration + Rotation vector" | linearAccelStabilityX/Y/Z, rotationVectorPitchStdDev/RollStdDev |
| REQ-06 | "Touch micro-biometrics" | avgTapPrecisionOffsetPx, tapPrecisionStdDev, avgInterTapVelocityPxPerMs, interTapVelocityStdDev, dominantHandSide, tapJitterPostDownMs |

Acceptance: 21 fields visible trên device sau 1 transfer session, formatted với decimal precision phù hợp (3 chữ số sau dấu phẩy cho float, integer raw cho count, boolean True/False).

---

### FR-CL-15: LocalScorer fallback rules (mirror Backend FR-BE-RULES)

**Description:** Khi backend unreachable, `LocalScorer.kt` chạy on-device để cung cấp fallback risk score. Hiện tại scorer chỉ dùng features từ FR-CL-05/06/07. Sau khi BE ship TASK-015 với 5 deterministic rules, mobile cần port các rules này sang Kotlin để fallback path cho consistent verdict.

**Scope:** Pure logic in `data/scorer/LocalScorer.kt`. Mirror exactly 5 rules + weights + score cap từ `app/services/risk_rule_engine.py` (BE TASK-015).

**5 rules (must match BE exactly):**

| Rule | Trigger | Weight |
|---|---|---|
| GPS spoofing | `mockLocationDetected = true` | +30 |
| Bot tap precision | `avgTapPrecisionOffsetPx < 2.0 AND tapPrecisionStdDev < 1.0 AND totalTouchEvents >= 5` | +25 |
| Synthetic velocity | `interTapVelocityStdDev < 0.005 AND totalTouchEvents >= 5` | +20 |
| OTP paste violation | `otpPasted = true` | +20 |
| Dark anomaly | `lightAvgLux < 10 AND sessionHourOfDay in [0,5]` | +15 |

Score capped at 100 (`SCORE_CAP`).

**Reasons (Vietnamese, must match BE strings):**
- "Phát hiện giả mạo GPS"
- "Mẫu tap dead-center bất thường (nghi ngờ bot)"
- "Tốc độ giữa các tap đồng đều bất thường"
- "OTP nhập bằng paste — vi phạm UX (Soft OTP DISPLAYED only)"
- "Môi trường tối + thời gian bất thường"

**Integration:** When backend call fails, `LocalScorer.computeRisk()` runs rule engine on `BehavioralFeatures` and returns `LocalScoreResult(score, riskLevel, reasons)`. Existing fallback path in `TransferOrchestratorViewModel.runVerification()` continues to use this.

---

### FR-CL-16: E2E acceptance test — fraud scenario

**Description:** Verify pipeline TASK-013..015 + TASK-026..028 + TASK-029/030 thực sự detect được fraud trong real-device test.

**Scope:** Manual test plan — không tự động hoá vì cần 2 device.

**Test scenario:**

1. Tester A enroll 3 lần (3 transfer sessions) trên device A → backend build profile
2. Tester B sử dụng device A của Tester A để chuyển 1 lần (mô phỏng fraud — ai khác chiếm device)
3. Verify trong Dev Menu > Risk History:
   - Tester B verification có `riskScore` >> mức trung bình của 3 enrollment baseline
   - Reasons mention specific FR-CL-10..13 features (nếu có rule fired) hoặc LLM hint
4. Optional: Cài fake-GPS app + grant LOCATION permission → run 1 session → verify `mockLocationDetected = true` trong Session Inspector + risk score ≥ 30 baseline (rule weight)

**Acceptance:**

- [ ] Score gap Tester A baseline → Tester B verification ≥ 20 points
- [ ] Reasons array non-empty cho fraud session
- [ ] Backend log có `rule_score`, `llm_score`, `rules_fired` field (per BE TASK-015)
- [ ] Dev Menu Risk History persist fraud entry, không mất sau app restart
