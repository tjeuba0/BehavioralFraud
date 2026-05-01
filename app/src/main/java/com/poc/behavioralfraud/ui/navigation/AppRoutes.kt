package com.poc.behavioralfraud.ui.navigation

/**
 * Route table — FR-CL-10 REQ-09.
 *
 * Single source of truth for all NavHost destinations. Tasks adding new screens
 * register their route as a constant here so every navigate/popBackStack call
 * uses a typed reference (no string literals scattered across screens).
 *
 * Group conventions:
 *  - flat string for top-level routes (`home`, `login`)
 *  - slash-separated for grouped flows (`transfer/form`, `dev/profile`)
 */
object AppRoutes {
    // ── Top-level (production-feel) ───────────────────────────────────
    const val HOME = "home"

    // ── Transfer flow (TASK-018..022) ──────────────────────────────────
    const val TRANSFER_TYPE = "transfer/type"
    const val TRANSFER_RECIPIENT = "transfer/recipient"
    const val TRANSFER_FORM = "transfer/form"
    const val TRANSFER_OTP = "transfer/otp"
    const val TRANSFER_SUCCESS = "transfer/success"

    // ── Legacy single transfer route (existing TransferScreen) ─────────
    // Kept for current POC TransferScreen until TASK-019 splits it into
    // form/otp/success. Will be removed at TASK-019.
    const val TRANSFER_LEGACY = "transfer"

    // ── Dev menu (TASK-024) ────────────────────────────────────────────
    const val DEV = "dev"
    const val DEV_PROFILE = "dev/profile"
    const val DEV_RISK_HISTORY = "dev/risk-history"
    const val DEV_SESSION = "dev/session"
    const val DEV_MANUAL_OVERRIDE = "dev/manual-override"
    const val DEV_DESIGN_SYSTEM = "dev/design-system"

    // ── Legacy POC routes — folded into Dev Menu at TASK-024 ──────────
    /** Existing ProfileScreen — moves under DEV_PROFILE at TASK-024. */
    const val PROFILE_LEGACY = "profile"

    /** DesignSystemPreviewScreen — moves under DEV_DESIGN_SYSTEM at TASK-024. */
    const val DESIGN_SYSTEM_LEGACY = "design-system"
}
