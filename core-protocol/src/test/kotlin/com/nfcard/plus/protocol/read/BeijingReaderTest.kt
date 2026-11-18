package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeijingReaderTest {

    private fun routes(): Map<String, ByteArray> = mapOf(
        "00A40000023F00" to ScriptedTransport.OK,
        "00B0840000" to ScriptedTransport.ok(FakeCards.beijingInfo32()),
        "00B0850000" to ScriptedTransport.ok(Bytes05()),
        "00A40000021001" to ScriptedTransport.OK,
        "805C000204" to ScriptedTransport.ok(FakeCards.balance("00001388")),
        "00B200C400" to ScriptedTransport.ok(FakeCards.txn23() + FakeCards.txn23(seq = "000B")),
    )

    private fun Bytes05(): ByteArray = com.nfcard.plus.protocol.Bytes.fromHex("00" + "00000064")

    @Test
    fun `reads beijing card serial balance and logs`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(routes()))
        val app = BeijingReader().read(tag)!!
        assertEquals(CardKind.BEIJING, app.kind)
        assertEquals("1122334455667788", app.serial)
        assertEquals(5000L, app.balanceCents)
        assertEquals(2, app.transactions.size)
    }

    @Test
    fun `returns null when info file missing`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(routes() - "00B0840000"))
        assertNull(BeijingReader().read(tag))
    }
}
