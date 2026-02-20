package com.poc.behavioralfraud.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.behavioralfraud.data.model.BehavioralProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    enrollmentCount: Int,
    profile: BehavioralProfile?,
    onNavigateToTransfer: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onClearData: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Behavioral Fraud POC") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Behavioral Fraud Detection",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "POC - Mobile Banking Security",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (profile != null)
                        Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (profile != null) "Profile đã sẵn sàng"
                        else "Chưa có Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (profile != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enrollment: $enrollmentCount/${TransferViewModel.MIN_ENROLLMENT}",
                        fontSize = 14.sp
                    )
                    if (profile != null) {
                        Text(
                            text = "Chế độ: VERIFICATION (so sánh hành vi)",
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "Chế độ: ENROLLMENT (thu thập hành vi baseline)",
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // How it works
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cách hoạt động",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Enrollment (${TransferViewModel.MIN_ENROLLMENT} lần): Thực hiện chuyển khoản giả lập để app học hành vi của bạn",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Verification: Các lần chuyển khoản sau sẽ được so sánh với profile hành vi đã học",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. Demo Fraud: Đưa điện thoại cho người khác thao tác → Hệ thống sẽ detect hành vi khác biệt",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = onNavigateToTransfer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (profile != null) "Chuyển khoản (Verification)"
                    else "Chuyển khoản (Enrollment ${enrollmentCount + 1}/${TransferViewModel.MIN_ENROLLMENT})",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (profile != null) {
                OutlinedButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xem Profile hành vi")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xóa tất cả dữ liệu", color = Color(0xFFD32F2F))
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Xác nhận") },
            text = { Text("Xóa tất cả dữ liệu enrollment và profile? Bạn sẽ phải enrollment lại từ đầu.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearData()
                    showClearDialog = false
                }) {
                    Text("Xóa", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
