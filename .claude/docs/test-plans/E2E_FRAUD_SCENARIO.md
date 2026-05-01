# E2E Fraud Detection — Acceptance Test Plan

> **TASK-031 / FR-CL-16** — Verify the full pipeline (mobile collector → BE Pydantic → DSPy LLM + RiskRuleEngine → mobile result UI) actually detects fraud on real devices.
>
> **Prerequisites merged:** Mobile TASK-013..030, Backend TASK-013..015.
> **Backend live:** `https://api.khoivan.dev/fraud` (per `docs/tasks.md` deployment section).

---

## Setup

### Devices

- **Device A** — primary (Tester A enrolls here, Tester B then verifies on the same physical device).
- **Device B** *(optional)* — only used in Scenario A2 if you want to verify "different device" behaviour separately.

Real Android phones, not emulators (sensor data on emulator is too clean and won't represent realistic baselines for the rule thresholds).

### App build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point to the live backend:

```properties
BACKEND_BASE_URL=https://api.khoivan.dev/fraud
```

### Reset state before each scenario

Open Dev Menu (long-press iPay logo on Home, ≥1.5s) → Manual Override → **Reset profile** + **Clear baseline** + **Clear history**. Confirm there's no stale enrollment.

---

## Scenario A1 — Identity fraud (same device, different person)

**Goal:** Tester B impersonating Tester A on the same device should score significantly higher than Tester A's own baseline.

### Steps

1. **Tester A enrolls (3 sessions):**
   - Open app → Home → tap **Chuyển tiền trong nước**.
   - Recipient screen → enter the same fake recipient (e.g. `9704322345` Vietcombank) → Tiếp tục.
   - Form screen → enter `1,000,000` VND → enter same memo (e.g. `Tester A enroll #1`) → Tiếp tục.
   - OTP screen → tap **Xác nhận & Hoàn tất**.
   - Repeat 2 more times with the same flow (Tester A's natural typing/touch rhythm). Memo can be `#2`, `#3`.

2. **Verify enrollment built:** Dev Menu → Profile Inspector → `enrollmentCount = 3` and a `profileSummary` text is present.

3. **Tester B verifies:** Hand the same physical device to Tester B (different person, different finger size, typing rhythm, hand dominance). Tester B does ONE transfer with the same recipient + amount + memo. Tester B types at their natural speed — do NOT instruct them to "type slowly to look fraudulent".

4. **Inspect result:**
   - Dev Menu → Risk History → Tester B's entry is at top.
   - Note: `riskScore`, `riskLevel`, `reasoning`, `source` (should be `backend` — if it's `local-fallback`, the test failed because backend was unreachable; retry).

### Expected

- [ ] Tester B `riskScore` ≥ Tester A enrollment-baseline + **20 points** (e.g. enrollment averaged ~10 → Tester B ≥ 30).
- [ ] `reasoning` non-empty, references at least one behavioural signal (typing rhythm, touch size, hand dominance, etc.).
- [ ] `source = "backend"`.
- [ ] No app crash. No visible error toast/snackbar on the production-feel Success screen.

### Record

```
Tester A baseline avg riskScore over 3 enrollments: ____
Tester B verification riskScore:                      ____
Gap:                                                  ____   (must be ≥ 20)
Reasons (from Risk History entry): ________________________________
Source: backend / local-fallback                      ____
Pass? Y / N
```

---

## Scenario A2 — Identity fraud (cross-device profile sharing) *(optional)*

If you have Device B, repeat Scenario A1 but Tester B uses Device B (with the same `userId` — currently `default_user` per the POC mock). Backend should still flag because behavioral profile is per-user, not per-device. Useful for catching backend dropping per-device buffers.

### Expected

Same as A1. Plus: device context fields (`deviceModel`, `screenWidthPx`, etc.) differ between enrollment and verification — this is informational, not a fraud signal by itself.

---

## Scenario B — GPS spoofing (deterministic rule fires)

**Goal:** With `mockLocationDetected = true`, the deterministic `gps_spoofing` rule (TASK-015 / TASK-030) should add ≥ 30 to the risk score regardless of behavioural similarity.

### Steps

1. **Install fake-GPS app** on the test device (Play Store: "Fake GPS Location" by Lexa, or any equivalent; F-Droid: "FakeTraveler" works too).

2. **Grant location permission to BehavioralFraud:**
   - Settings → Apps → Behavioral Fraud POC → Permissions → Location → **Allow only while using the app**.
   - Settings → System → Developer options → **Select mock location app** → choose the fake-GPS app.

3. **Activate the fake location** in the fake-GPS app (set any country/city different from your real location — e.g. New York).

4. **Reset profile** (Dev Menu → Manual Override) so the previous scenario's data doesn't muddy the result.

5. **Tester A enrolls 3 sessions** as in Scenario A1 — but **with mock location active**. Note: if `mockLocationDetected = true` flagged enrollment too, that's fine — backend still builds a profile; the score is what we check on verification.

6. **Tester A verifies once** (the same Tester A who enrolled — biometric similar, so the only fraud signal is mock location).

7. **Inspect:**
   - Dev Menu → Session Inspector → "Threat indicators" section: `mockLocationDetected = true` rendered in **RED**.
   - Dev Menu → Risk History → reasoning includes `Phát hiện giả mạo GPS`.

### Expected

- [ ] `mockLocationDetected = true` in Session Inspector (RED highlight).
- [ ] Verification `riskScore ≥ 30` (rule weight baseline) even though Tester A is verifying their own profile.
- [ ] Reasoning array contains `"Phát hiện giả mạo GPS"`.
- [ ] `source = "backend"` (rule fires on backend; mobile side LocalScorer would also fire it offline — this is online path).

### Record

```
mockLocationDetected (Session Inspector): true / false   ____
Verification riskScore:                                  ____   (must be ≥ 30)
"Phát hiện giả mạo GPS" in reasons: Y / N                ____
Source: backend / local-fallback                         ____
Pass? Y / N
```

---

## Scenario C — Backend offline fallback (LocalScorer rules fire)

**Goal:** When backend is unreachable, mobile `LocalScoreRules` (TASK-030) should fire the same rule set as the backend, producing a non-zero score with rule reasons.

### Steps

1. **Reset profile** (Manual Override) so we start clean.

2. **Enroll 3 sessions ONLINE** so we have a baseline profile saved locally (mobile caches the profile after the 3rd backend response).

3. **Disable network on the device:**
   - Pull down quick-settings → Airplane mode ON.
   - Verify in Settings → Network: WiFi OFF, Mobile data OFF.

4. **(Optional) Trigger one or more rules** by combining:
   - Keep mock location active from Scenario B → `gps_spoofing` rule.
   - Run the transfer at 3 AM device clock (or change device system time temporarily) + cover the light sensor with a finger → `dark_anomaly` rule (lightAvgLux < 10 AND hour ∈ [0..5]).
   - Or just verify the offline path runs at all without anomalies.

5. **Tester A runs 1 transfer** — same flow as Scenario A1 step 3.

6. **Inspect:**
   - Dev Menu → Risk History → newest entry's `source = "local-fallback"` (NOT `backend`).
   - If you triggered rules, the `reasoning` array contains the same Vietnamese strings as backend would produce: `"Phát hiện giả mạo GPS"`, `"Môi trường tối + thời gian bất thường"`, etc.

### Expected

- [ ] Backend call fails gracefully (no app crash, no UI error visible to Tester A on production-feel screens).
- [ ] Risk History entry source = `"local-fallback"`.
- [ ] If GPS spoof or dark anomaly triggered, `reasoning` contains the matching Vietnamese strings (must match backend strings exactly per FR-CL-15).
- [ ] Risk score reflects max(rule_score, z_score against enrollment baseline).

### Record

```
Backend unreachable confirmed (airplane mode):           Y / N   ____
Risk History source = "local-fallback":                  Y / N   ____
Rules fired (which Vietnamese strings, comma-separated): ________________________________
Risk score:                                              ____
App stayed crash-free across the whole flow:             Y / N   ____
Pass? Y / N
```

---

## Backend log spot-check (Scenario A1 + B only, optional)

If you have SSH to the VPS (`139.99.104.250`), tail the backend log during the test:

```bash
ssh root@139.99.104.250
journalctl -u behavioralfraud.service -f
```

For each verify call (Scenario A1 + B), the JSON log line for `risk_score_completed` should include:

- `rule_score` (integer, 0..100) — TASK-015 RuleEngine output
- `llm_score` (integer, 0..100) — DSPy raw output
- `rules_fired` (list of Vietnamese strings) — empty list if no rule fired
- `risk_score` = `max(rule_score, llm_score)`

Confirm those fields are present and the math is right.

---

## Pass/fail summary

The acceptance suite passes only if **all three scenarios pass**:

- [ ] Scenario A1 — score gap ≥ 20
- [ ] Scenario B — `mockLocationDetected=true` + score ≥ 30 + Vietnamese reason matches
- [ ] Scenario C — local-fallback source + reasons match BE strings + no crash

If any scenario fails, file a bug referencing the failing TASK-013..030 and re-run after fix.

---

## Cleanup after testing

- Disable mock-location app and revoke location permission.
- Re-enable network.
- Reset profile / history (Manual Override) to clean state for next demo.
- Uninstall fake-GPS app if not needed.

---

## Result log template (paste filled-out version into the TASK-031 PR description)

```
Tester(s):           ___________________
Date:                ___________________
Devices:             A=____________  B=____________
App build SHA:       ___________________
Backend version:     https://api.khoivan.dev/fraud (commit: __________)

Scenario A1: PASS / FAIL    notes: __________
Scenario A2: PASS / FAIL / SKIP    notes: __________
Scenario B:  PASS / FAIL    notes: __________
Scenario C:  PASS / FAIL    notes: __________

Overall: ACCEPTED / NEEDS FIX
```
