package com.nfcard.plus.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class BytesTest {

    @Test
    fun `hex formats uppercase without separators`() {
        assertEquals("00A40400", Bytes.hex(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)))
    }

    @Test
    fun `hex honors offset and length`() {
        val data = Bytes.fromHex("1122334455")
        assertEquals("2233", Bytes.hex(data, 1, 2))
    }

    @Test
    fun `fromHex parses odd-safe even string`() {
        assertEquals(3, Bytes.fromHex("A00000").size)
        assertEquals(0xA0.toByte(), Bytes.fromHex("A00000")[0])
    }

    @Test
    fun `intBE reads big endian unsigned`() {
        val data = Bytes.fromHex("000186A0")
        assertEquals(100000L, Bytes.intBE(data, 0, 4))
    }

    @Test
    fun `intBE reads partial field`() {
        val data = Bytes.fromHex("FF000064FF")
        assertEquals(100L, Bytes.intBE(data, 1, 3))
    }

    @Test
    fun `bcd decodes digit pairs`() {
        val data = Bytes.fromHex("20261119")
        assertEquals("20261119", Bytes.bcd(data, 0, 4))
    }
}
