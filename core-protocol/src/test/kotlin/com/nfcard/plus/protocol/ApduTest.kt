package com.nfcard.plus.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApduTest {

    @Test
    fun `command with data serializes Lc and payload`() {
        val cmd = ApduCommand(0x00, 0xA4, 0x04, 0x00, data = Bytes.fromHex("A000"))
        assertArrayEquals(Bytes.fromHex("00A4040002A000"), cmd.toBytes())
    }

    @Test
    fun `command with Le appends expected length`() {
        val cmd = ApduCommand(0x80, 0x5C, 0x00, 0x02, le = 4)
        assertArrayEquals(Bytes.fromHex("805C000204"), cmd.toBytes())
    }

    @Test
    fun `header only command has four bytes`() {
        val cmd = ApduCommand(0x00, 0xB0, 0x95, 0x00, le = 0)
        assertArrayEquals(Bytes.fromHex("00B0950000"), cmd.toBytes())
    }

    @Test
    fun `response splits data and status word`() {
        val rsp = ApduResponse.from(Bytes.fromHex("000186A09000"))
        assertTrue(rsp.isOk)
        assertArrayEquals(Bytes.fromHex("000186A0"), rsp.data)
        assertEquals(0x9000, rsp.statusWord)
    }

    @Test
    fun `error response has empty data and not ok`() {
        val rsp = ApduResponse.from(Bytes.fromHex("6A82"))
        assertFalse(rsp.isOk)
        assertEquals(0, rsp.data.size)
        assertEquals(0x6A82, rsp.statusWord)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `response shorter than two bytes is rejected`() {
        ApduResponse.from(byteArrayOf(0x90.toByte()))
    }
}
