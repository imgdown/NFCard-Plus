package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PbocParserTest {

    @Test
    fun `balance decodes positive cents`() {
        assertEquals(100000L, PbocParser.balance(Bytes.fromHex("000186A0")))
    }

    @Test
    fun `balance decodes negative overdraft as signed`() {
        assertEquals(-100L, PbocParser.balance(Bytes.fromHex("FFFFFF9C")))
    }

    @Test
    fun `balance shorter than four bytes is null`() {
        assertNull(PbocParser.balance(Bytes.fromHex("0001")))
    }

    @Test
    fun `epInfo parses serial version and validity`() {
        val raw = Bytes.fromHex(
            "A000000632010105" + "02" + "01" +
                "12345678901234567890" + "20200101" + "20301231" + "0000",
        )
        val info = PbocParser.epInfo(raw)!!
        assertEquals("12345678901234567890", info.serial)
        assertEquals("1", info.version)
        assertEquals("2020-01-01", info.validFrom)
        assertEquals("2030-12-31", info.validTo)
        assertEquals("A000000632010105", info.issuer)
    }

    @Test
    fun `epInfo shorter than 30 bytes is null`() {
        assertNull(PbocParser.epInfo(ByteArray(29)))
    }

    @Test
    fun `txnRecord parses composite consumption`() {
        val raw = Bytes.fromHex(
            "000A" + "000000" + "000001F4" + "09" +
                "000000000001" + "20261118" + "083015",
        )
        val txn = PbocParser.txnRecord(raw)!!
        assertEquals(10, txn.seq)
        assertEquals(500L, txn.amountCents)
        assertTrue(txn.isDebit)
        assertEquals("000000000001", txn.terminalId)
        assertEquals("2026-11-18", txn.date)
        assertEquals("08:30:15", txn.time)
        assertEquals(0L, txn.overdrawCents)
    }

    @Test
    fun `txnRecord load type is not debit`() {
        val raw = Bytes.fromHex(
            "000B" + "000000" + "00002710" + "02" +
                "000000000002" + "20261119" + "120000",
        )
        val txn = PbocParser.txnRecord(raw)!!
        assertFalse(txn.isDebit)
        assertEquals(10000L, txn.amountCents)
    }

    @Test
    fun `txnRecord zero amount is empty slot`() {
        val raw = Bytes.fromHex(
            "0000" + "000000" + "00000000" + "00" +
                "000000000000" + "00000000" + "000000",
        )
        assertNull(PbocParser.txnRecord(raw))
    }

    @Test
    fun `splitRecords splits concatenated records and drops remainder`() {
        val one = "000A" + "000000" + "000001F4" + "09" + "000000000001" + "20261118" + "083015"
        val raw = Bytes.fromHex(one + one + "FFFF")
        val parts = PbocParser.splitRecords(raw, 23)
        assertEquals(2, parts.size)
        assertEquals(23, parts[0].size)
    }
}
