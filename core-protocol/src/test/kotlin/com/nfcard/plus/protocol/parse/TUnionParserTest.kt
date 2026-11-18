package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.MetroStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TUnionParserTest {

    private fun metroHex(entryTime: String, exitTime: String): String =
        "2701" + "7D" + "01" + "01" + "00" + "01" +
            "1122334455667788" +
            "4910" + "0000" +
            "0100000000000001" + "0000000000000000" +
            "0100000000000101" + "0000000000000000" +
            "0100000000000011" + "0000000000000000" +
            entryTime + exitTime + "000000C8" +
            "0000" + "000000000000" + "0000" + "00" + "00".repeat(32)

    @Test
    fun `metro record with entry only means entered not exited`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("20260101080000", "00000000000000")),
        )!!
        assertEquals(MetroStatus.ENTERED_NOT_EXITED, rec.status)
        assertEquals("4910", rec.entryCityCode)
        assertEquals("0100000000000101", rec.entryStation)
        assertEquals("2026-01-01 08:00:00", rec.entryTime)
        assertNull(rec.exitTime)
        assertEquals(200L, rec.maxFareCents)
    }

    @Test
    fun `metro record with both times means exited`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("20260101080000", "20260101083000")),
        )!!
        assertEquals(MetroStatus.EXITED, rec.status)
        assertEquals("2026-01-01 08:30:00", rec.exitTime)
    }

    @Test
    fun `metro record with no times means no record`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("00000000000000", "00000000000000")),
        )!!
        assertEquals(MetroStatus.NO_RECORD, rec.status)
    }

    @Test
    fun `record with unknown id is rejected`() {
        val wrong = "27FF" + metroHex("20260101080000", "00000000000000").substring(4)
        assertNull(TUnionParser.metroRecord(Bytes.fromHex(wrong)))
    }

    @Test
    fun `short record is rejected`() {
        assertNull(TUnionParser.metroRecord(ByteArray(64)))
    }

    @Test
    fun `process record parses real-world layout`() {
        val raw = Bytes.fromHex(
            "03" + "4910" + "00000000000011" + "00000101" +
                "00000000" + "000000" + "00002710" + "20260101080000" +
                "4910" + "0100000000000001" + "00".repeat(6),
        )
        val rec = TUnionParser.processRecord(raw)!!
        assertEquals(3, rec.typeCode)
        assertEquals("00000000000011", rec.terminalId)
        assertEquals("00000101", rec.stationCode)
        assertEquals(0L, rec.amountCents)
        assertEquals(10000L, rec.balanceAfterCents)
        assertEquals("2026-01-01 08:00:00", rec.dateTime)
        assertEquals("4910", rec.cityCode)
        assertEquals("0100000000000001", rec.agencyId)
    }

    @Test
    fun `all zero process record is empty slot`() {
        assertNull(TUnionParser.processRecord(ByteArray(48)))
    }

    @Test
    fun `management info extracts city code`() {
        val raw = Bytes.fromHex("00000156" + "0049" + "4910" + "0001" + "01" + "00".repeat(49))
        val info = TUnionParser.managementInfo(raw)!!
        assertEquals("4910", info.cityCode)
        assertEquals("0049", info.provinceCode)
    }
}
