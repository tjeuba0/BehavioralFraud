package com.poc.behavioralfraud.ui.screens.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.poc.behavioralfraud.data.collector.BehavioralCollector
import com.poc.behavioralfraud.data.repository.ProfileRepository
import com.poc.behavioralfraud.ui.components.IPayButton
import com.poc.behavioralfraud.ui.components.IPayButtonSize
import com.poc.behavioralfraud.ui.components.IPayButtonVariant
import com.poc.behavioralfraud.ui.components.IPayCard
import com.poc.behavioralfraud.ui.components.IPayCardVariant
import com.poc.behavioralfraud.ui.components.IPayTopBar
import com.poc.behavioralfraud.ui.components.safeClickable
import com.poc.behavioralfraud.ui.theme.IPayPalette
import com.poc.behavioralfraud.ui.theme.IPayTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dev Menu — FR-CL-10 REQ-16..20.
 *
 * Hidden test harness UI accessible only via long-press iPay logo on Home
 * (1.5s). Surfaces all behavioral collection / verification artifacts that
 * MUST stay invisible to end users in production-feel screens.
 *
 * Entries (REQ-16):
 *   - Profile Inspector — view current behavioral profile (REQ-17)
 *   - Risk Score History — timeline of verification records (REQ-18)
 *   - Session Inspector — live view of current collector session (REQ-19)
 *   - Manual Override — reset profile / clear baseline / etc. (REQ-20)
 *   - Design System Preview — UI showcase
 */

@Composable
fun DevMenuScreen(
    onBack: () -> Unit,
    onProfileInspector: () -> Unit,
    onRiskHistory: () -> Unit,
    onSessionInspector: () -> Unit,
    onManualOverride: () -> Unit,
    onDesignSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Dev Menu (POC only)", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            DevMenuEntry(
                icon = Icons.Default.Person,
                title = "Profile Inspector",
                subtitle = "Xem behavioral profile hiện tại",
                onClick = onProfileInspector,
            )
            DevMenuEntry(
                icon = Icons.Default.History,
                title = "Risk Score History",
                subtitle = "Timeline verification records",
                onClick = onRiskHistory,
            )
            DevMenuEntry(
                icon = Icons.Default.Visibility,
                title = "Session Inspector",
                subtitle = "Live view collector session",
                onClick = onSessionInspector,
            )
            DevMenuEntry(
                icon = Icons.Default.Build,
                title = "Manual Override",
                subtitle = "Reset profile / clear baseline / force backend down",
                onClick = onManualOverride,
            )
            DevMenuEntry(
                icon = Icons.Default.Palette,
                title = "Design System Preview",
                subtitle = "Showcase tokens + 13 components",
                onClick = onDesignSystem,
            )

            Spacer(Modifier.height(IPayTheme.spacing.s16))

            Text(
                text = "Dev tools chỉ dùng trong POC. Không phơi ra production.",
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralTertiary,
            )
        }
    }
}

@Composable
private fun DevMenuEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    IPayCard(
        variant = IPayCardVariant.Outlined,
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onSafeClick = onClick, role = Role.Button),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IPayTheme.colors.bgBrandSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IPayTheme.colors.iconBrandPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = IPayTheme.typography.bodyEmphasizedLarge,
                    color = IPayTheme.colors.textNeutralPrimary,
                )
                Text(
                    text = subtitle,
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = IPayTheme.colors.iconNeutralTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Inspector — REQ-17
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileInspectorScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { ProfileRepository(context) }
    val profile = remember { repo.getProfile() }
    val baselineCount = remember { repo.getEnrollmentCount() }
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Profile Inspector", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            if (profile == null) {
                IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chưa có profile",
                        style = IPayTheme.typography.titleMedium,
                        color = IPayTheme.colors.textNeutralPrimary,
                    )
                    Spacer(Modifier.height(IPayTheme.spacing.s4))
                    Text(
                        text = "Baseline đã thu: $baselineCount/3 giao dịch.\n" +
                            "Hoàn thành thêm ${(3 - baselineCount).coerceAtLeast(0)} giao dịch nữa để build profile.",
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralSecondary,
                    )
                }
            } else {
                IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8)) {
                        Text(
                            text = "Profile JSON",
                            style = IPayTheme.typography.titleMedium,
                            color = IPayTheme.colors.textNeutralPrimary,
                        )
                        Text(
                            text = gson.toJson(profile),
                            style = IPayTheme.typography.bodySmall,
                            color = IPayTheme.colors.textNeutralSecondary,
                        )
                    }
                }
            }

            IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
                    Text(
                        text = "Baseline features ($baselineCount)",
                        style = IPayTheme.typography.titleMedium,
                        color = IPayTheme.colors.textNeutralPrimary,
                    )
                    if (baselineCount == 0) {
                        Text(
                            text = "Chưa có giao dịch baseline nào.",
                            style = IPayTheme.typography.bodyMedium,
                            color = IPayTheme.colors.textNeutralTertiary,
                        )
                    } else {
                        repo.getEnrollmentFeaturesList().forEachIndexed { idx, features ->
                            Text(
                                text = "#${idx + 1}: duration=${features.sessionDurationMs}ms, " +
                                    "touch=${features.totalTouchEvents}, paste=${features.pasteCount}",
                                style = IPayTheme.typography.bodySmall,
                                color = IPayTheme.colors.textNeutralSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Risk Score History — REQ-18
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RiskHistoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { ProfileRepository(context) }
    var records by remember { mutableStateOf(repo.getVerificationHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(
            title = "Risk Score History",
            onBack = onBack,
            trailing = {
                if (records.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = IPayTheme.typography.bodyEmphasizedMedium,
                        color = IPayPalette.VietinRed60,
                        modifier = Modifier.safeClickable(
                            onSafeClick = { showClearDialog = true },
                            role = Role.Button,
                        ),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s8),
        ) {
            if (records.isEmpty()) {
                IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Chưa có verification record nào.",
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                    Spacer(Modifier.height(IPayTheme.spacing.s4))
                    Text(
                        text = "Hoàn thành 1 giao dịch để xem dữ liệu hiện ở đây.",
                        style = IPayTheme.typography.bodySmall,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                }
            } else {
                records.forEach { record ->
                    RiskHistoryRow(record = record, dateFormatter = dateFormatter)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Xoá toàn bộ history?") },
            text = { Text("Hành động này không thể undo. Profile + baseline KHÔNG bị xoá.") },
            confirmButton = {
                TextButton(onClick = {
                    // SharedPrefs only stores history under one key; we don't have a
                    // dedicated clear-history method. Use a small workaround: write empty list.
                    // Future: add ProfileRepository.clearVerificationHistory().
                    val ctx = context.applicationContext
                    val sp = ctx.getSharedPreferences("behavioral_profiles", android.content.Context.MODE_PRIVATE)
                    sp.edit().remove("verification_history").apply()
                    records = emptyList()
                    showClearDialog = false
                }) {
                    Text("Xoá", color = IPayPalette.VietinRed60)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Huỷ") }
            },
        )
    }
}

@Composable
private fun RiskHistoryRow(
    record: ProfileRepository.VerificationRecord,
    dateFormatter: SimpleDateFormat,
) {
    val (chipBg, chipFg) = when {
        record.riskScore >= 70 -> IPayPalette.VietinRed60.copy(alpha = 0.12f) to IPayPalette.VietinRed60
        record.riskScore >= 40 -> IPayPalette.Orange60.copy(alpha = 0.12f) to IPayPalette.Orange60
        record.source == "enrollment" -> IPayPalette.Ink20 to IPayPalette.Ink70
        else -> IPayPalette.Green10 to IPayPalette.Green70
    }
    IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(chipBg)
                        .padding(horizontal = IPayTheme.spacing.s8, vertical = IPayTheme.spacing.s2),
                ) {
                    Text(
                        text = if (record.source == "enrollment") "ENROLL" else "RISK ${record.riskScore}",
                        style = IPayTheme.typography.labelSmall,
                        color = chipFg,
                    )
                }
                Text(
                    text = dateFormatter.format(Date(record.timestampMs)),
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
            }
            Text(
                text = record.txSummary,
                style = IPayTheme.typography.bodyEmphasizedMedium,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Text(
                text = record.reasoning,
                style = IPayTheme.typography.bodySmall,
                color = IPayTheme.colors.textNeutralSecondary,
            )
            Text(
                text = "source: ${record.source}",
                style = IPayTheme.typography.labelSmall,
                color = IPayTheme.colors.textNeutralTertiary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session Inspector — REQ-19
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SessionInspectorScreen(
    collector: BehavioralCollector,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var counts by remember { mutableStateOf(collector.getEventCounts()) }
    var features by remember {
        mutableStateOf<com.poc.behavioralfraud.data.model.BehavioralFeatures?>(null)
    }
    var tickCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            counts = collector.getEventCounts()
            features = runCatching { collector.extractFeatures() }.getOrNull()
            tickCount += 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Session Inspector", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
                    Text(
                        text = "Live event counts",
                        style = IPayTheme.typography.titleMedium,
                        color = IPayTheme.colors.textNeutralPrimary,
                    )
                    Text(
                        text = "Refreshes every 500ms",
                        style = IPayTheme.typography.bodySmall,
                        color = IPayTheme.colors.textNeutralTertiary,
                    )
                    Spacer(Modifier.height(IPayTheme.spacing.s8))
                    EventCountRow(label = "Touch events", value = counts["touch"] ?: 0)
                    EventCountRow(label = "Text changes", value = counts["textChange"] ?: 0)
                    EventCountRow(label = "Sensor readings", value = counts["sensor"] ?: 0)
                    EventCountRow(label = "Navigation events", value = counts["navigation"] ?: 0)
                }
            }

            // FR-CL-14 — 21 new features grouped into 6 sections.
            features?.let { f ->
                FeatureSection(
                    title = "Hesitation (FR-CL-10)",
                    rows = listOf(
                        "decisionTimeOverLimitMs" to "${f.decisionTimeOverLimitMs} ms",
                        "otpPasted" to f.otpPasted.toString(),
                    ),
                )
                ThreatIndicatorsSection(mockLocationDetected = f.mockLocationDetected)
                FeatureSection(
                    title = "Magnetometer (FR-CL-12)",
                    rows = listOf(
                        "magnetometerStabilityX" to format3(f.magnetometerStabilityX),
                        "magnetometerStabilityY" to format3(f.magnetometerStabilityY),
                        "magnetometerStabilityZ" to format3(f.magnetometerStabilityZ),
                        "magnetometerMagnitudeAvg" to format3(f.magnetometerMagnitudeAvg),
                    ),
                )
                FeatureSection(
                    title = "Light + Proximity (FR-CL-12)",
                    rows = listOf(
                        "lightAvgLux" to format3(f.lightAvgLux),
                        "lightStdDevLux" to format3(f.lightStdDevLux),
                        "proximityNearRatio" to format3(f.proximityNearRatio),
                    ),
                )
                FeatureSection(
                    title = "Linear-accel + Rotation-vector (FR-CL-12)",
                    rows = listOf(
                        "linearAccelStabilityX" to format3(f.linearAccelStabilityX),
                        "linearAccelStabilityY" to format3(f.linearAccelStabilityY),
                        "linearAccelStabilityZ" to format3(f.linearAccelStabilityZ),
                        "rotationVectorPitchStdDev" to format3(f.rotationVectorPitchStdDev),
                        "rotationVectorRollStdDev" to format3(f.rotationVectorRollStdDev),
                    ),
                )
                TouchMicroBiometricsSection(features = f)
            }

            IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tick: $tickCount",
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralTertiary,
                )
                Text(
                    text = "Note: collector session active during transfer flow only " +
                        "(Home tap \"Chuyển tiền\" → Success/abort). Outside transfer, " +
                        "counts may be stale from last session.",
                    style = IPayTheme.typography.bodySmall,
                    color = IPayTheme.colors.textNeutralSecondary,
                )
            }
        }
    }
}

@Composable
private fun EventCountRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = IPayTheme.typography.bodyMedium,
            color = IPayTheme.colors.textNeutralSecondary,
        )
        Text(
            text = value.toString(),
            style = IPayTheme.typography.bodyEmphasizedMedium,
            color = IPayTheme.colors.textNeutralPrimary,
        )
    }
}

/** FR-CL-14: generic section card with feature label + value rows. */
@Composable
private fun FeatureSection(title: String, rows: List<Pair<String, String>>) {
    IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
            Text(
                text = title,
                style = IPayTheme.typography.titleSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            rows.forEach { (label, value) ->
                EventCountRowText(label = label, value = value)
            }
        }
    }
}

@Composable
private fun EventCountRowText(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = IPayTheme.typography.bodyMedium,
            color = IPayTheme.colors.textNeutralSecondary,
        )
        Text(
            text = value,
            style = IPayTheme.typography.bodyEmphasizedMedium,
            color = IPayTheme.colors.textNeutralPrimary,
        )
    }
}

/** FR-CL-14 REQ-02 — threat indicators with RED highlight when triggered. */
@Composable
private fun ThreatIndicatorsSection(mockLocationDetected: Boolean) {
    val triggered = mockLocationDetected
    IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
            Text(
                text = "Threat indicators (FR-CL-11)",
                style = IPayTheme.typography.titleSmall,
                color = if (triggered) {
                    IPayTheme.colors.iconWarning
                } else {
                    IPayTheme.colors.textNeutralPrimary
                },
            )
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "mockLocationDetected",
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.textNeutralSecondary,
                )
                Text(
                    text = mockLocationDetected.toString(),
                    style = IPayTheme.typography.bodyEmphasizedMedium,
                    color = if (mockLocationDetected) {
                        IPayTheme.colors.iconWarning
                    } else {
                        IPayTheme.colors.textNeutralPrimary
                    },
                )
            }
        }
    }
}

/** FR-CL-14 REQ-06 — touch micro-biometrics with hand-side chip color-coded. */
@Composable
private fun TouchMicroBiometricsSection(
    features: com.poc.behavioralfraud.data.model.BehavioralFeatures,
) {
    IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s4)) {
            Text(
                text = "Touch micro-biometrics (FR-CL-13)",
                style = IPayTheme.typography.titleSmall,
                color = IPayTheme.colors.textNeutralPrimary,
            )
            Spacer(Modifier.height(IPayTheme.spacing.s4))
            EventCountRowText("avgTapPrecisionOffsetPx", format3(features.avgTapPrecisionOffsetPx))
            EventCountRowText("tapPrecisionStdDev", format3(features.tapPrecisionStdDev))
            EventCountRowText(
                "avgInterTapVelocityPxPerMs",
                format3(features.avgInterTapVelocityPxPerMs),
            )
            EventCountRowText("interTapVelocityStdDev", format3(features.interTapVelocityStdDev))
            EventCountRowText("tapJitterPostDownMs", format3(features.tapJitterPostDownMs))
            // dominantHandSide as color-coded chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "dominantHandSide",
                    style = IPayTheme.typography.bodyMedium,
                    color = IPayTheme.colors.textNeutralSecondary,
                )
                HandSideChip(side = features.dominantHandSide)
            }
        }
    }
}

@Composable
private fun HandSideChip(side: String) {
    val (chipBg, chipText) = when (side) {
        "LEFT" -> IPayTheme.colors.bgBrandSecondary to IPayTheme.colors.textBrandPrimary
        "RIGHT" -> com.poc.behavioralfraud.ui.theme.IPayPalette.Green10 to
            IPayTheme.colors.iconSuccess
        else -> IPayTheme.colors.bgNeutralSecondary to IPayTheme.colors.textNeutralTertiary
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
            .background(chipBg)
            .padding(
                horizontal = IPayTheme.spacing.s12,
                vertical = IPayTheme.spacing.s4,
            ),
    ) {
        Text(
            text = side,
            style = IPayTheme.typography.bodyEmphasizedSmall,
            color = chipText,
        )
    }
}

/** Format float with 3 decimal places. */
private fun format3(value: Double): String = "%.3f".format(value)

// ─────────────────────────────────────────────────────────────────────────────
// Manual Override — REQ-20
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ManualOverrideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { ProfileRepository(context) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IPayTheme.colors.bgNeutralSecondary),
    ) {
        IPayTopBar(title = "Manual Override", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(IPayTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(IPayTheme.spacing.s12),
        ) {
            IPayButton(
                text = "Reset profile",
                onClick = { pendingAction = PendingAction.ResetProfile },
                variant = IPayButtonVariant.Secondary,
                size = IPayButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
            IPayButton(
                text = "Clear baseline candidates",
                onClick = { pendingAction = PendingAction.ClearBaseline },
                variant = IPayButtonVariant.Secondary,
                size = IPayButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
            IPayButton(
                text = "Clear ALL behavioral data",
                onClick = { pendingAction = PendingAction.ClearAll },
                variant = IPayButtonVariant.Secondary,
                size = IPayButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )
            statusMessage?.let { msg ->
                IPayCard(variant = IPayCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = msg,
                        style = IPayTheme.typography.bodyMedium,
                        color = IPayTheme.colors.textSuccess,
                    )
                }
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.confirmTitle) },
            text = { Text(action.confirmBody) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        PendingAction.ResetProfile -> {
                            val sp = context.applicationContext.getSharedPreferences(
                                "behavioral_profiles",
                                android.content.Context.MODE_PRIVATE,
                            )
                            sp.edit().remove("user_profile").apply()
                            statusMessage = "Profile reset. Lần giao dịch tiếp theo sẽ build lại baseline."
                        }
                        PendingAction.ClearBaseline -> {
                            val sp = context.applicationContext.getSharedPreferences(
                                "behavioral_profiles",
                                android.content.Context.MODE_PRIVATE,
                            )
                            sp.edit().remove("enrollment_features").apply()
                            statusMessage = "Baseline candidates cleared."
                        }
                        PendingAction.ClearAll -> {
                            repo.clearAll()
                            val sp = context.applicationContext.getSharedPreferences(
                                "behavioral_profiles",
                                android.content.Context.MODE_PRIVATE,
                            )
                            sp.edit().remove("verification_history").apply()
                            statusMessage = "Tất cả behavioral data đã xoá (profile + baseline + history)."
                        }
                    }
                    pendingAction = null
                }) {
                    Text("Xác nhận", color = IPayPalette.VietinRed60)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Huỷ") }
            },
        )
    }
}

private enum class PendingAction(val confirmTitle: String, val confirmBody: String) {
    ResetProfile(
        confirmTitle = "Reset profile?",
        confirmBody = "Profile hiện tại sẽ bị xoá. Baseline + history giữ nguyên.",
    ),
    ClearBaseline(
        confirmTitle = "Clear baseline candidates?",
        confirmBody = "Các baseline candidates sẽ bị xoá. Profile (nếu đã build) + history giữ nguyên.",
    ),
    ClearAll(
        confirmTitle = "Clear ALL behavioral data?",
        confirmBody = "Profile + baseline + history đều xoá. App quay về trạng thái fresh install.",
    ),
}
