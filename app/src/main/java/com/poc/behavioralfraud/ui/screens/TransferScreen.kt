package com.poc.behavioralfraud.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventCounts by viewModel.eventCounts.collectAsState()

    var accountNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Start collection when screen opens
    LaunchedEffect(Unit) {
        viewModel.startCollection()
    }

    // Periodically refresh event counts for live display
    LaunchedEffect(uiState) {
        if (uiState is TransferUiState.Collecting) {
            while (true) {
                viewModel.refreshEventCounts()
                delay(500)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển khoản") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        // Wrap entire content with touch interceptor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInteropFilter { event ->
                    viewModel.collector.onTouchEvent(
                        action = event.action and MotionEvent.ACTION_MASK,
                        x = event.x,
                        y = event.y,
                        size = event.size,
                        touchMajor = event.touchMajor,
                        downTime = event.downTime,
                        eventTime = event.eventTime
                    )
                    false // Don't consume the event
                }
        ) {
            when (val state = uiState) {
                is TransferUiState.Idle,
                is TransferUiState.Collecting -> {
                    TransferForm(
                        accountNumber = accountNumber,
                        amount = amount,
                        note = note,
                        eventCounts = eventCounts,
                        onAccountNumberChange = { newValue ->
                            viewModel.collector.onTextChanged(
                                fieldName = "accountNumber",
                                previousLength = accountNumber.length,
                                newLength = newValue.length
                            )
                            accountNumber = newValue
                        },
                        onAmountChange = { newValue ->
                            viewModel.collector.onTextChanged(
                                fieldName = "amount",
                                previousLength = amount.length,
                                newLength = newValue.length
                            )
                            amount = newValue
                        },
                        onNoteChange = { newValue ->
                            viewModel.collector.onTextChanged(
                                fieldName = "note",
                                previousLength = note.length,
                                newLength = newValue.length
                            )
                            note = newValue
                        },
                        onFieldFocus = { fieldName ->
                            viewModel.collector.onFieldFocus(fieldName)
                        },
                        onSubmit = {
                            viewModel.submitTransfer(accountNumber, amount, note)
                        }
                    )
                }

                is TransferUiState.Analyzing -> {
                    AnalyzingView()
                }

                is TransferUiState.EnrollmentComplete -> {
                    EnrollmentResultView(
                        count = state.count,
                        message = state.message,
                        onBack = {
                            viewModel.resetState()
                            accountNumber = ""
                            amount = ""
                            note = ""
                            onBack()
                        }
                    )
                }

                is TransferUiState.VerificationResult -> {
                    VerificationResultView(
                        result = state.result,
                        features = state.features,
                        onBack = {
                            viewModel.resetState()
                            accountNumber = ""
                            amount = ""
                            note = ""
                            onBack()
                        }
                    )
                }

                is TransferUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = {
                            viewModel.resetState()
                        },
                        onBack = {
                            viewModel.resetState()
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferForm(
    accountNumber: String,
    amount: String,
    note: String,
    eventCounts: Map<String, Int>,
    onAccountNumberChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onFieldFocus: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Live data collection indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Thu thập hành vi...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1565C0)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Touch: ${eventCounts["touch"] ?: 0}", fontSize = 11.sp, color = Color.Gray)
                    Text("Text: ${eventCounts["textChange"] ?: 0}", fontSize = 11.sp, color = Color.Gray)
                    Text("Sensor: ${eventCounts["sensor"] ?: 0}", fontSize = 11.sp, color = Color.Gray)
                    Text("Nav: ${eventCounts["navigation"] ?: 0}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Transfer form
        Text("Thông tin chuyển khoản", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = accountNumber,
            onValueChange = onAccountNumberChange,
            label = { Text("Số tài khoản nhận") },
            placeholder = { Text("Nhập số tài khoản") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocus("accountNumber") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Số tiền (VNĐ)") },
            placeholder = { Text("Nhập số tiền") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocus("amount") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Nội dung chuyển khoản") },
            placeholder = { Text("Nhập nội dung") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocus("note") },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = accountNumber.isNotBlank() && amount.isNotBlank()
        ) {
            Text("Xác nhận chuyển khoản", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "* Đây là giao dịch giả lập, không chuyển tiền thật",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnalyzingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang phân tích hành vi...", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gửi dữ liệu tới AI để đánh giá", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun EnrollmentResultView(
    count: Int,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("Enrollment", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (count >= TransferViewModel.MIN_ENROLLMENT)
                    Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (count >= TransferViewModel.MIN_ENROLLMENT)
                    "Về Home (Sẵn sàng Verification)"
                else "Về Home (Tiếp tục Enrollment)"
            )
        }
    }
}

@Composable
private fun VerificationResultView(
    result: com.poc.behavioralfraud.data.model.FraudAnalysisResult,
    features: com.poc.behavioralfraud.data.model.BehavioralFeatures,
    onBack: () -> Unit
) {
    val riskColor = when {
        result.riskScore <= 30 -> Color(0xFF2E7D32)  // Green
        result.riskScore <= 70 -> Color(0xFFE65100)   // Orange
        else -> Color(0xFFD32F2F)                      // Red
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Risk Score
        Text("Kết quả phân tích", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${result.riskScore}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
                Text(
                    text = "Risk Score",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.riskLevel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Khuyến nghị: ${result.recommendation}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = riskColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Anomalies
        if (result.anomalies.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bất thường phát hiện:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    result.anomalies.forEach { anomaly ->
                        Text("• $anomaly", fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Explanation
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Giải thích chi tiết:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(result.explanation, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Raw features (for demo)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Raw Features (Debug):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow("Session duration", "${features.sessionDurationMs}ms")
                FeatureRow("Avg inter-char delay", "${String.format("%.1f", features.avgInterCharDelayMs)}ms")
                FeatureRow("Std inter-char delay", "${String.format("%.1f", features.stdInterCharDelayMs)}ms")
                FeatureRow("Total text changes", "${features.totalTextChanges}")
                FeatureRow("Paste count", "${features.pasteCount}")
                FeatureRow("Total touch events", "${features.totalTouchEvents}")
                FeatureRow("Avg touch size", String.format("%.4f", features.avgTouchSize))
                FeatureRow("Avg touch duration", "${String.format("%.1f", features.avgTouchDurationMs)}ms")
                FeatureRow("Avg touch pressure", String.format("%.4f", features.avgTouchPressure))
                FeatureRow("Avg swipe velocity", "${String.format("%.1f", features.avgSwipeVelocity)} px/s")
                FeatureRow("Gyro stability (X)", String.format("%.6f", features.gyroStabilityX))
                FeatureRow("Gyro stability (Y)", String.format("%.6f", features.gyroStabilityY))
                FeatureRow("Gyro stability (Z)", String.format("%.6f", features.gyroStabilityZ))
                FeatureRow("Inter-field pause", "${String.format("%.0f", features.avgInterFieldPauseMs)}ms")
                FeatureRow("Deletion count", "${features.deletionCount}")
                FeatureRow("Deletion ratio", String.format("%.2f", features.deletionRatio))
                FeatureRow("Field focus", features.fieldFocusSequence)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Về Home")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Lỗi", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFD32F2F))
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Thử lại")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Về Home")
        }
    }
}
