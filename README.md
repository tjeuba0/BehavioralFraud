# Behavioral Fraud Detection POC

## Mục đích

POC app Android giả lập luồng chuyển khoản ngân hàng, thu thập behavioral data (touch, sensor, text input, clipboard) từ client, gửi lên LLM (OpenRouter) để phân tích và trả về risk score.

**Mục tiêu demo:** Cùng 1 app, cùng 1 giao dịch, nhưng **người khác thao tác thì bị phát hiện**.

---

## Kiến trúc

```
┌─────────────────────────────────────────────┐
│              Android App (Client)            │
│                                              │
│  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Transfer UI  │  │ BehavioralCollector   │  │
│  │ (Compose)    │──│                       │  │
│  │              │  │ • MotionEvent (touch) │  │
│  │ STK: ____    │  │ • TextWatcher (input) │  │
│  │ Tiền: ____   │  │ • SensorManager       │  │
│  │ ND: ____     │  │   (accel + gyro)      │  │
│  │              │  │ • Clipboard detect    │  │
│  │ [Xác nhận]   │  │ • Navigation tracking │  │
│  └──────┬───────┘  └──────────┬───────────┘  │
│         │                     │              │
│         │    ┌────────────────┘              │
│         ▼    ▼                               │
│  ┌──────────────────┐                        │
│  │ Feature Extractor │                       │
│  │ (on-device)       │                       │
│  └────────┬─────────┘                        │
│           │ JSON                             │
│           ▼                                  │
│  ┌──────────────────┐                        │
│  │ OpenRouterClient  │── HTTPS ──►           │
│  └──────────────────┘            │           │
└──────────────────────────────────┼───────────┘
                                   │
                    ┌──────────────▼──────────┐
                    │   OpenRouter API         │
                    │   (Gemini 2.0 Flash)     │
                    │                          │
                    │   Enrollment:            │
                    │   features → profile     │
                    │                          │
                    │   Verification:          │
                    │   features vs profile    │
                    │   → risk score           │
                    └──────────────────────────┘
```

---

## Cấu trúc Project

```
BehavioralFraudPOC/
├── app/src/main/java/com/poc/behavioralfraud/
│   ├── MainActivity.kt                    # Entry point + Navigation
│   ├── data/
│   │   ├── model/BehavioralModels.kt      # Data classes
│   │   ├── collector/BehavioralCollector.kt # Thu thập behavioral data
│   │   └── repository/ProfileRepository.kt # Lưu trữ profile (SharedPrefs)
│   ├── network/
│   │   └── OpenRouterClient.kt            # Gọi LLM API
│   └── ui/
│       ├── theme/Theme.kt                 # Material3 Theme
│       └── screens/
│           ├── HomeScreen.kt              # Màn hình chính
│           ├── TransferScreen.kt          # Màn hình chuyển khoản
│           ├── TransferViewModel.kt       # Business logic
│           └── ProfileScreen.kt           # Xem behavioral profile
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

---

## Cài đặt

### 1. Mở project trong Android Studio

```bash
# Clone hoặc copy thư mục BehavioralFraudPOC vào máy
# Mở Android Studio → File → Open → chọn thư mục BehavioralFraudPOC
```

### 2. Cấu hình OpenRouter API Key

**Cách 1: Trong `gradle.properties` (khuyến nghị)**

Mở file `gradle.properties` và thêm:

```properties
OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Cách 2: Trong `local.properties`**

```properties
OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> **Lưu ý:** Không commit API key lên git. Thêm `local.properties` vào `.gitignore`.

### 3. Lấy OpenRouter API Key

1. Đăng ký tại https://openrouter.ai/
2. Vào Settings → API Keys → Create Key
3. Nạp credit (khoảng $5 là đủ cho POC)
4. Model sử dụng: `z-ai/glm-5`

### 4. Build & Run

- Chọn device/emulator (Android 8.0+ / API 26+)
- Click Run ▶️
- **Lưu ý:** Nên test trên thiết bị thật để có sensor data chính xác (emulator không có gyroscope thật)

---

## Cách sử dụng (Demo cho sếp)

### Bước 1: Enrollment (3 lần)

1. Mở app → Nhấn **"Chuyển khoản (Enrollment 1/3)"**
2. Nhập thông tin chuyển khoản:
   - STK: `1234567890` (gõ bằng tay, KHÔNG paste)
   - Số tiền: `5000000`
   - Nội dung: `chuyen tien`
3. Nhấn **"Xác nhận chuyển khoản"**
4. Đợi AI phân tích → Nhấn "Về Home"
5. **Lặp lại 3 lần** (cùng thông tin, cùng cách gõ tự nhiên)

> Sau 3 lần, app sẽ gọi LLM để tạo behavioral profile từ data đã thu thập.

### Bước 2: Verification - Bạn thao tác (Normal)

1. Nhấn **"Chuyển khoản (Verification)"**
2. Nhập cùng thông tin như enrollment
3. Nhấn xác nhận
4. **Kết quả mong đợi:** Risk Score thấp (0-30), màu xanh, "APPROVE"

### Bước 3: Verification - Người khác thao tác (Fraud)

1. **Đưa điện thoại cho đồng nghiệp**
2. Nhờ họ nhập cùng thông tin chuyển khoản
3. Nhấn xác nhận
4. **Kết quả mong đợi:** Risk Score cao (70-100), màu đỏ, "BLOCK"

### Bước 4 (Bonus): Đặt điện thoại trên bàn

1. Đặt điện thoại nằm yên trên bàn
2. Gõ bằng ngón trỏ (thay vì cầm tay gõ ngón cái)
3. **Kết quả mong đợi:** Gyroscope quá ổn định → phát hiện bất thường

---

## Data thu thập chi tiết

### 1. Touch Events (`MotionEvent`)

| Field | API | Ý nghĩa |
|-------|-----|---------|
| `x, y` | `getX()`, `getY()` | Tọa độ chạm |
| `size` | `getSize()` | Kích thước vùng chạm (phụ thuộc ngón tay) |
| `touchMajor` | `getTouchMajor()` | Chiều dài trục chính vùng chạm |
| `action` | `getAction()` | DOWN(0), UP(1), MOVE(2) |
| `downTime` | `getDownTime()` | Thời điểm nhấn xuống |
| `eventTime` | `getEventTime()` | Thời điểm event |

### 2. Text Change Events (`TextWatcher` equivalent)

| Field | Ý nghĩa |
|-------|---------|
| `timestamp` | Thời điểm text thay đổi |
| `fieldName` | Tên field (accountNumber, amount, note) |
| `lengthDelta` | Số ký tự thay đổi |
| `isPaste` | `true` nếu delta > 1 (có thể là paste hoặc IME commit) |

> **Lưu ý:** `TextWatcher` ghi nhận timestamp của text change, không phải từng phím bấm. IME có thể commit nhiều ký tự cùng lúc (auto-correct, gợi ý từ, Telex/VNI).

### 3. Sensor Events (`SensorManager`)

| Sensor | Delay | Data | Ý nghĩa |
|--------|-------|------|---------|
| Accelerometer | NORMAL (200ms) | x, y, z | Gia tốc 3 trục → cách cầm và di chuyển |
| Gyroscope | NORMAL (200ms) | x, y, z | Tốc độ xoay → góc nghiêng và độ rung |

### 4. Navigation Events

| Event | Ý nghĩa |
|-------|---------|
| `screen_enter` | Mở màn hình chuyển khoản |
| `field_focus` | Focus vào field nào |
| `confirm_tap` | Nhấn nút xác nhận |

---

## Features được extract

Từ raw data, app tính toán các features sau gửi cho LLM:

| Feature | Đơn vị | Ý nghĩa fraud detection |
|---------|--------|-------------------------|
| `sessionDurationMs` | ms | Thời gian hoàn thành giao dịch |
| `avgInterCharDelayMs` | ms | Nhịp nhập liệu trung bình |
| `stdInterCharDelayMs` | ms | Độ ổn định nhịp nhập |
| `pasteCount` | count | Số lần paste (delta > 1) |
| `avgTouchSize` | float | Kích thước chạm trung bình |
| `avgTouchDurationMs` | ms | Thời gian chạm trung bình |
| `avgSwipeVelocity` | px/s | Tốc độ vuốt trung bình |
| `gyroStabilityX/Y/Z` | std dev | Độ rung thiết bị (thấp = trên bàn/RAT) |
| `accelStabilityX/Y/Z` | std dev | Độ rung gia tốc |
| `fieldFocusSequence` | string | Thứ tự focus các field |
| `timeToFirstInput` | ms | Thời gian từ mở app đến nhập liệu đầu tiên |

---

## Lưu ý quan trọng

### Về kỹ thuật
- **Soft keyboard không fire `KeyEvent`**: Android không đảm bảo `KeyEvent` từ bàn phím ảo. App dùng `TextWatcher` (onValueChange) để ước lượng nhịp nhập, không phải keystroke-level chính xác.
- **Pressure không đáng tin**: Nhiều thiết bị trả về `getPressure() = 1.0` cố định. App không dùng pressure làm feature.
- **Sensor trên emulator**: Emulator không có gyroscope/accelerometer thật. Nên test trên thiết bị thật.
- **IME composition**: Bàn phím Telex/VNI, auto-correct có thể commit nhiều ký tự cùng lúc → `isPaste` có thể false positive.

### Về bảo mật (POC only)
- API key lưu trong `BuildConfig` → **KHÔNG an toàn cho production**. Production cần server-side proxy.
- Data truyền qua HTTPS nhưng không được mã hóa thêm.
- Profile lưu trong SharedPreferences → **KHÔNG an toàn cho production**. Production cần encrypted storage.

### Về privacy
- Dữ liệu chỉ phát sinh trong app, không thu thập ngoài app.
- Dữ liệu được feature hóa (giảm độ chi tiết) trước khi gửi lên server.
- Production cần thêm: consent dialog, privacy policy, data retention policy.

---

## Khác biệt POC vs Production

| Aspect | POC | Production |
|--------|-----|-----------|
| AI Engine | LLM (OpenRouter) | ML Model (TensorFlow/PyTorch) |
| Profile Storage | SharedPreferences | Encrypted DB + Server |
| API Key | BuildConfig | Server-side proxy |
| Feature Extraction | On-device (basic) | On-device + Server (advanced) |
| Model Training | LLM prompt | Supervised/Unsupervised ML |
| Accuracy | Demo-level | 99%+ với đủ data |
| Privacy | Basic | Full compliance (GDPR, PDPA) |
