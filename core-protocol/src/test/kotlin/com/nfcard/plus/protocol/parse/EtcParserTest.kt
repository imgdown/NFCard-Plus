package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EtcParserTest {

    private fun gb(text: String, len: Int): ByteArray =
        text.toByteArray(charset("GB2312")).copyOf(len)

    private fun issuance(newStandard: Boolean): ByteArray {
        val out = ByteArray(if (newStandard) 50 else 43)
        Bytes.fromHex("0102111111170809").copyInto(out, 0)
        out[8] = 0x01
        out[9] = if (newStandard) 0x41 else 0x21
        Bytes.fromHex("3101").copyInto(out, 10)
        Bytes.fromHex("1122334455667788").copyInto(out, 12)
        Bytes.fromHex("20200101").copyInto(out, 20)
        Bytes.fromHex("20301231").copyInto(out, 24)
        gb("豫A11115", 12).copyInto(out, 28)
        out[40] = 0x01
        if (newStandard) {
            out[41] = 0x04
            out[42] = 0x01
        }
        return out
    }

    private fun holder55(): ByteArray {
        val out = ByteArray(55)
        gb("张三", 20).copyInto(out, 2)
        "410101199911111234".toByteArray(Charsets.US_ASCII).copyOf(32).copyInto(out, 22)
        out[54] = 0x01
        return out
    }

    private fun passage43(): ByteArray {
        val out = ByteArray(43)
        out[0] = 0xAA.toByte()
        out[1] = 0x29
        out[2] = 0x00
        Bytes.fromHex("0001").copyInto(out, 3)
        Bytes.fromHex("0002").copyInto(out, 5)
        out[7] = 0x03
        Bytes.fromHex("6553F100").copyInto(out, 8) // 1700000000
        out[12] = 0x01
        out[13] = 0x01
        gb("豫A11115", 12).copyInto(out, 27)
        return out
    }

    @Test
    fun `issuance parses plate dates and card number`() {
        val info = EtcParser.issuance(issuance(true))!!
        assertEquals("豫A11115", info.plate)
        assertEquals("渐变绿", info.plateColor)
        assertEquals(1, info.vehicleType)
        assertTrue(info.newStandard)
        assertEquals("2020-01-01", info.issueDate)
        assertEquals("2030-12-31", info.expiryDate)
        assertEquals("1122334455667788", info.cardNo)
        assertEquals("3101", info.networkNo)
    }

    @Test
    fun `old standard issuance has no color or vehicle type`() {
        val info = EtcParser.issuance(issuance(false))!!
        assertFalse(info.newStandard)
        assertNull(info.plateColor)
        assertNull(info.vehicleType)
        assertEquals("豫A11115", info.plate)
    }

    @Test
    fun `short issuance is rejected`() {
        assertNull(EtcParser.issuance(ByteArray(42)))
    }

    @Test
    fun `holder parses name and id number`() {
        val (name, id, type) = EtcParser.holder(holder55())!!
        assertEquals("张三", name)
        assertEquals("410101199911111234", id)
        assertEquals(1, type)
    }

    @Test
    fun `passage record parses unix time and station`() {
        val p = EtcParser.passageRecord(passage43())!!
        assertEquals("0001", p.networkNo)
        assertEquals("0002", p.stationNo)
        assertEquals(3, p.lane)
        assertEquals("2023-11-15 06:13:20", p.time)
        assertEquals("豫A11115", p.plate)
    }

    @Test
    fun `passage with wrong marker is rejected`() {
        val bad = passage43().also { it[0] = 0xB1.toByte() }
        assertNull(EtcParser.passageRecord(bad))
        assertNull(EtcParser.passageRecord(ByteArray(20)))
    }

    @Test
    fun `plate color names`() {
        assertEquals("蓝色", EtcParser.plateColorName(0))
        assertEquals("黄色", EtcParser.plateColorName(1))
        assertEquals("黑色", EtcParser.plateColorName(2))
        assertEquals("白色", EtcParser.plateColorName(3))
        assertEquals("渐变绿", EtcParser.plateColorName(4))
        assertEquals("黄绿双拼", EtcParser.plateColorName(5))
        assertEquals("蓝白渐变", EtcParser.plateColorName(6))
        assertNull(EtcParser.plateColorName(99))
    }

    @Test
    fun `unix seconds format in beijing time`() {
        assertEquals("1970-01-01 08:00:00", EtcParser.formatUnixSeconds(0))
        assertNull(EtcParser.formatUnixSeconds(-1))
    }

    @Test
    fun `gb2312 decode trims padding`() {
        val padded = gb("豫A", 12)
        assertEquals("豫A", EtcParser.decodeGb2312(padded, 0, 12))
        val ffPadded = ByteArray(12) { 0xFF.toByte() }
        gb("京", 2).copyInto(ffPadded, 0)
        assertEquals("京", EtcParser.decodeGb2312(ffPadded, 0, 12))
    }
}
