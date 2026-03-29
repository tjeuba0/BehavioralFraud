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