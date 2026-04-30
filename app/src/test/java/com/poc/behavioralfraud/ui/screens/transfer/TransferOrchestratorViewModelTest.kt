package com.poc.behavioralfraud.ui.screens.transfer

import com.poc.behavioralfraud.data.mock.MockBanks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferOrchestratorViewModelTest {

    @Test
    fun `initial state is empty`() {
        val vm = TransferOrchestratorViewModel()
        val s = vm.state.value

        assertNull(s.transferType)
        assertEquals("", s.recipientAccount)
        assertNull(s.recipientBank)
        assertEquals("", s.amountRaw)
        assertEquals(0L, s.amountVnd)
        assertEquals("", s.note)
        assertEquals(TransferSource.PaymentAccount, s.source)
        assertEquals(TransferChannel.Default, s.transferChannel)
        assertFalse(s.overLimit)
        assertEquals(TransferTxStatus.Idle, s.txStatus)
        assertNull(s.riskResult)
    }

    @Test
    fun `setTransferType updates state`() {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Napas)
        assertEquals(TransferType.Napas, vm.state.value.transferType)
    }

    @Test
    fun `setRecipient updates account and bank`() {
        val vm = TransferOrchestratorViewModel()
        val bank = MockBanks.list.first { it.code == "VCB" }
        vm.setRecipient("0123456789", bank)
        val s = vm.state.value
        assertEquals("0123456789", s.recipientAccount)
        assertEquals("VCB", s.recipientBank?.code)
    }

    @Test
    fun `setAmount sanitizes non-digits`() {
        val vm = TransferOrchestratorViewModel()
        vm.setAmount("1,234abc567")
        val s = vm.state.value
        assertEquals("1234567", s.amountRaw)
        assertEquals(1_234_567L, s.amountVnd)
    }

    @Test
    fun `setAmount truncates to MAX_AMOUNT_DIGITS`() {
        val vm = TransferOrchestratorViewModel()
        vm.setAmount("999999999999999")  // 15 digits
        assertEquals(12, vm.state.value.amountRaw.length)
    }

    @Test
    fun `setAmount over Napas limit flags overLimit when type is Napas`() {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Napas)
        vm.setAmount("11000000")  // 11M VND > 10M Napas limit

        assertTrue(vm.state.value.overLimit)
    }

    @Test
    fun `setAmount over Napas limit does not flag when type is Internal`() {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Internal)
        vm.setAmount("100000000")  // 100M VND, but internal transfer

        assertFalse(vm.state.value.overLimit)
    }

    @Test
    fun `setAmount under Napas limit does not flag overLimit`() {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Napas)
        vm.setAmount("9999999")  // ~10M boundary

        assertFalse(vm.state.value.overLimit)
    }

    @Test
    fun `setNote truncates to MAX_NOTE_LENGTH`() {
        val vm = TransferOrchestratorViewModel()
        val long = "a".repeat(150)
        vm.setNote(long)
        assertEquals(100, vm.state.value.note.length)
    }

    @Test
    fun `setSource updates state`() {
        val vm = TransferOrchestratorViewModel()
        vm.setSource(TransferSource.Savings)
        assertEquals(TransferSource.Savings, vm.state.value.source)
    }

    @Test
    fun `reset clears all state`() {
        val vm = TransferOrchestratorViewModel()
        val bank = MockBanks.list.first()
        vm.setTransferType(TransferType.Napas)
        vm.setRecipient("0123", bank)
        vm.setAmount("1000000")
        vm.setNote("hello")

        vm.reset()

        val s = vm.state.value
        assertNull(s.transferType)
        assertEquals("", s.recipientAccount)
        assertEquals(0L, s.amountVnd)
        assertEquals("", s.note)
    }

    @Test
    fun `onFormContinue emits NavigateToOtp when not over limit`() = runTest {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Internal)
        vm.setAmount("1000000")

        vm.onFormContinue()
        val event = vm.events.first()

        assertEquals(TransferEvent.NavigateToOtp, event)
    }

    @Test
    fun `onFormContinue emits ShowOverLimitSheet when over Napas limit`() = runTest {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Napas)
        vm.setAmount("11000000")  // > 10M

        vm.onFormContinue()
        val event = vm.events.first()

        assertEquals(TransferEvent.ShowOverLimitSheet, event)
    }

    @Test
    fun `onOverLimitProceed sets channel to Regular and navigates to OTP`() = runTest {
        val vm = TransferOrchestratorViewModel()
        vm.setTransferType(TransferType.Napas)

        vm.onOverLimitProceed()
        val event = vm.events.first()

        assertEquals(TransferChannel.Regular, vm.state.value.transferChannel)
        assertEquals(TransferEvent.NavigateToOtp, event)
    }

    @Test
    fun `onOtpComplete emits NavigateToSuccess`() = runTest {
        val vm = TransferOrchestratorViewModel()
        vm.onOtpComplete()
        val event = vm.events.first()
        assertEquals(TransferEvent.NavigateToSuccess, event)
    }
}
