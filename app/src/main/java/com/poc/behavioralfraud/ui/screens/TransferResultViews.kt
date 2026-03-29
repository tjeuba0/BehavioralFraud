package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AnalyzingView() {
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
internal fun EnrollmentResultView(
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
internal fun VerificationResultView(
    result: com.poc.behavioralfraud.data.model.FraudAnalysisResult,
    features: com.poc.behavioralfraud.data.model.BehavioralFeatures,
    onBack: () -> Unit
) {
    val riskColor = when {
        result.riskScore <= 30 -> Color(0xFF2E7D32)
        result.riskScore <= 70 -> Color(0xFFE65100)
        else -> Color(0xFFD32F2F)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

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
                Text(text = "Risk Score", fontSize = 14.sp, color = Color.Gray)
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

        if (result.anomalies.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bất thường phát hiện:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    result.anomalies.forEach { anomaly ->
                        Text("* $anomaly", fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Giải thích chi tiết:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(result.explanation, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Về Home")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun FeatureRow(label: String, value: String) {
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
internal fun ErrorView(
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
