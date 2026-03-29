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
import androidx.compose.ui.platform.LocalLifecycleOwner
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

    // Register collector as lifecycle observer for background tracking (Phase 2)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel.collector)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel.collector)
        }
    }

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
                        eventTime = event.eventTime,
                        pointerCount = event.pointerCount
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

// Result views extracted to TransferResultViews.kt
