package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.MetroStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TUnionParserTest {

    private fun metroHex(entryTime: String, exitTime: String): String =
        "2704" + "7D" + "01" + "01" + "00" + "01" +
            "12345678901234567890" + "0000000000000001" + "01" +
            "4910" + "0000" +
            "0000000000000001" + "0000000000000000" +
            "0000000001010203" + "0000000000000000" +
            "0000000000000001" + "0000000000000000" +
            entryTime + exitTime + "000003E8" + "00".repeat(32)

    @Test
    fun `metro record with entry only means entered not exited`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("20261119083000", "00000000000000")),
        )!!
        assertEquals(MetroStatus.ENTERED_NOT_EXITED, rec.status)
        assertEquals("4910", rec.entryCityCode)
        assertEquals("0000000001010203", rec.entryStation)
        assertEquals("2026-11-19 08:30:00", rec.entryTime)
        assertNull(rec.exitTime)
        assertEquals(1000L, rec.maxFareCents)
        assertEquals(1, rec.stateCode)
    }

    @Test
    fun `metro record with both times means exited`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("20261119083000", "20261119091500")),
        )!!
        assertEquals(MetroStatus.EXITED, rec.status)
        assertEquals("2026-11-19 09:15:00", rec.exitTime)
    }

    @Test
    fun `metro record with no times means no record`() {
        val rec = TUnionParser.metroRecord(
            Bytes.fromHex(metroHex("00000000000000", "00000000000000")),
        )!!
        assertEquals(MetroStatus.NO_RECORD, rec.status)
    }

    @Test
    fun `record with wrong id is rejected`() {
        val wrong = "2703" + metroHex("20261119083000", "00000000000000").substring(4)
        assertNull(TUnionParser.metroRecord(Bytes.fromHex(wrong)))
    }

    @Test
    fun `short record is rejected`() {
        assertNull(TUnionParser.metroRecord(ByteArray(64)))
    }

    @Test
    fun `process record parses all fields`() {
        val raw = Bytes.fromHex(
            "01" + "0000000000000001" + "0000000000000002" +
                "00000000" + "00002710" + "20261119083000" +
                "4910" + "0000000000000001" + "00".repeat(6),
        )
        val rec = TUnionParser.processRecord(raw)!!
        assertEquals(1, rec.typeCode)
        assertEquals("0000000000000001", rec.terminalId)
        assertEquals("0000000000000002", rec.seq)
        assertEquals(0L, rec.amountCents)
        assertEquals(10000L, rec.balanceAfterCents)
        assertEquals("2026-11-19 08:30:00", rec.dateTime)
        assertEquals("4910", rec.cityCode)
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
