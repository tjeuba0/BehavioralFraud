package com.poc.behavioralfraud.data.mock

/**
 * Mock data for POC iPay clone screens.
 *
 * Used by HomeIPayScreen / TransferTypeScreen / RecipientScreen / etc. to render
 * realistic content without backend integration. NO real banking data.
 *
 * Will move/extend at backend integration phase (POST-POC).
 */

/**
 * Bank entry for inter-bank transfer (Napas) selection.
 *
 * @param code Short identifier (vd "VCB", "TCB"). Used as STK lookup hint.
 * @param shortName Display name (vd "Vietcombank", "Techcombank").
 * @param fullName Legal name for confirmation display.
 */
data class MockBank(
    val code: String,
    val shortName: String,
    val fullName: String,
)

/** ~20 most common Vietnamese banks for the recipient bank list (Napas members). */
object MockBanks {
    val list: List<MockBank> = listOf(
        MockBank("VCB", "Vietcombank", "Ngân hàng TMCP Ngoại thương Việt Nam"),
        MockBank("TCB", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam"),
        MockBank("MBB", "MB Bank", "Ngân hàng TMCP Quân Đội"),
        MockBank("ACB", "ACB", "Ngân hàng TMCP Á Châu"),
        MockBank("BIDV", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam"),
        MockBank("CTG", "VietinBank", "Ngân hàng TMCP Công thương Việt Nam"),
        MockBank("VPB", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng"),
        MockBank("STB", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín"),
        MockBank("HDB", "HDBank", "Ngân hàng TMCP Phát triển TP.HCM"),
        MockBank("TPB", "TPBank", "Ngân hàng TMCP Tiên Phong"),
        MockBank("SHB", "SHB", "Ngân hàng TMCP Sài Gòn - Hà Nội"),
        MockBank("EIB", "Eximbank", "Ngân hàng TMCP Xuất nhập khẩu Việt Nam"),
        MockBank("OCB", "OCB", "Ngân hàng TMCP Phương Đông"),
        MockBank("VIB", "VIB", "Ngân hàng TMCP Quốc tế Việt Nam"),
        MockBank("MSB", "MSB", "Ngân hàng TMCP Hàng Hải Việt Nam"),
        MockBank("AGR", "Agribank", "Ngân hàng Nông nghiệp và Phát triển Nông thôn"),
        MockBank("SEA", "SeABank", "Ngân hàng TMCP Đông Nam Á"),
        MockBank("LPB", "LienVietPostBank", "Ngân hàng TMCP Bưu điện Liên Việt"),
        MockBank("NAB", "NAB", "Ngân hàng TMCP Nam Á"),
        MockBank("VAB", "VietABank", "Ngân hàng TMCP Việt Á"),
    )
}

/**
 * Recipient suggestion shown in "Gần đây" (recent) row of RecipientScreen.
 */
data class MockRecipient(
    val accountNumber: String,
    val name: String,
    val bankCode: String, // matches MockBank.code
)

/** Mock recent recipients for the "Gần đây" chip row. */
object MockRecipients {
    val list: List<MockRecipient> = listOf(
        MockRecipient("0123456789", "Nguyễn Văn An", "VCB"),
        MockRecipient("9876543210", "Trần Thị Bình", "TCB"),
        MockRecipient("5555666677", "Phạm Quốc Cường", "MBB"),
    )
}

/**
 * Quick balance shown on HomeIPayScreen — single mock account (POC).
 */
object MockAccount {
    const val BALANCE_VND: Long = 12_345_678L
    const val ACCOUNT_NUMBER_MASKED: String = "9999 1111"
    const val ACCOUNT_TYPE_LABEL: String = "Tài khoản thanh toán"
}

/**
 * Napas inter-bank limit (POC). Triggers OverNapasLimitBottomSheet at TASK-020
 * when user enters amount above this threshold.
 */
object MockTransferLimits {
    /** 10 million VND — common Napas single-tx limit for retail accounts. */
    const val NAPAS_DAILY_LIMIT_VND: Long = 10_000_000L
}
