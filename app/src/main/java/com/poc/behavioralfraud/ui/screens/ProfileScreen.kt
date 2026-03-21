package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.behavioralfraud.data.model.BehavioralProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: BehavioralProfile?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Behavioral Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Chưa có profile. Hãy enrollment trước.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Behavioral Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Enrollment: ${profile.enrollmentCount} sessions",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chỉ số trung bình", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileMetric(
                            "Thời gian phiên giao dịch",
                            "${String.format("%.0f", profile.avgSessionDuration)}ms",
                            "Thời gian trung bình để hoàn thành 1 giao dịch"
                        )
                        ProfileMetric(
                            "Nhịp nhập liệu",
                            "${String.format("%.1f", profile.avgInterCharDelay)}ms",
                            "Khoảng cách trung bình giữa các lần text change"
                        )
                        ProfileMetric(
                            "Độ lệch nhịp nhập",
                            "${String.format("%.1f", profile.stdInterCharDelay)}ms",
                            "Độ ổn định của nhịp nhập liệu"
                        )
                        ProfileMetric(
                            "Kích thước chạm",
                            String.format("%.4f", profile.avgTouchSize),
                            "Kích thước trung bình vùng chạm (phụ thuộc ngón tay)"
                        )
                        ProfileMetric(
                            "Thời gian chạm",
                            "${String.format("%.1f", profile.avgTouchDuration)}ms",
                            "Thời gian trung bình mỗi lần chạm"
                        )
                        ProfileMetric(
                            "Gyro stability X",
                            String.format("%.6f", profile.avgGyroStabilityX),
                            "Độ ổn định con quay hồi chuyển trục X"
                        )
                        ProfileMetric(
                            "Gyro stability Y",
                            String.format("%.6f", profile.avgGyroStabilityY),
                            "Độ ổn định con quay hồi chuyển trục Y"
                        )
                        ProfileMetric(
                            "Gyro stability Z",
                            String.format("%.6f", profile.avgGyroStabilityZ),
                            "Độ ổn định con quay hồi chuyển trục Z"
                        )
                        ProfileMetric(
                            "Accel stability X",
                            String.format("%.4f", profile.avgAccelStabilityX),
                            "Độ rung gia tốc kế trục X"
                        )
                        ProfileMetric(
                            "Accel stability Y",
                            String.format("%.4f", profile.avgAccelStabilityY),
                            "Độ rung gia tốc kế trục Y"
                        )
                        ProfileMetric(
                            "Accel stability Z",
                            String.format("%.4f", profile.avgAccelStabilityZ),
                            "Độ rung gia tốc kế trục Z"
                        )
                        ProfileMetric(
                            "Paste count",
                            String.format("%.1f", profile.avgPasteCount),
                            "Số lần paste trung bình mỗi phiên"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LLM Summary
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "AI Profile Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.profileSummary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Text(description, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}
