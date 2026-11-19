package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtcReaderTest {

    private fun gb(text: String, len: Int): ByteArray =
        text.toByteArray(charset("GB2312")).copyOf(len)

    private fun issuance50(): ByteArray {
        val out = ByteArray(50)
        out[9] = 0x41
        Bytes.fromHex("20200101").copyInto(out, 20)
        Bytes.fromHex("20301231").copyInto(out, 24)
        gb("豫A11115", 12).copyInto(out, 28)
        out[41] = 0x00
        out[42] = 0x01
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
        Bytes.fromHex("6553F100").copyInto(out, 8)
        gb("豫A11115", 12).copyInto(out, 27)
        return out
    }

    private fun routes(): Map<String, ByteArray> = mapOf(
        "00A40000023F00" to ScriptedTransport.OK,
        "00B0960000" to ScriptedTransport.ok(holder55()),
        "00A40000021001" to ScriptedTransport.OK,
        "00B0950000" to ScriptedTransport.ok(issuance50()),
        "805C000204" to ScriptedTransport.ok(FakeCards.balance("00002710")),
        "00B201CC00" to ScriptedTransport.ok(passage43()),
        // 交易记录受 PIN 保护：安全状态不满足
        "00B200C400" to Bytes.fromHex("6982"),
        "00B201C400" to Bytes.fromHex("6982"),
    )

    @Test
    fun `reads etc card plate holder balance and passages`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(routes()))
        val app = EtcReader().read(tag)!!
        assertEquals(CardKind.ETC, app.kind)
        assertEquals(10000L, app.balanceCents)
        assertEquals("豫A11115", app.etc!!.plate)
        assertEquals("张三", app.etc!!.holderName)
        assertEquals("410101199911111234", app.etc!!.holderIdNumber)
        assertEquals(1, app.etc!!.passages.size)
        assertEquals("2023-11-15 06:13:20", app.etc!!.passages[0].time)
        assertEquals(0, app.transactions.size)
    }

    @Test
    fun `transit sized issuance file is not etc`() {
        val short = routes().toMutableMap()
        short["00B0950000"] = ScriptedTransport.ok(FakeCards.info30())
        val tag = Iso7816Tag(ScriptedTransport.routing(short))
        assertNull(EtcReader().read(tag))
    }

    @Test
    fun `standard ep reader rejects etc sized info file`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(routes()))
        assertNull(PbocEpReader().read(tag))
    }
}
