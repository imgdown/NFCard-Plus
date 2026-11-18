package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.MetroStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TUnionReaderTest {

    private fun tUnionRoutes(): Map<String, ByteArray> = mapOf(
        "00A40000023F00" to ScriptedTransport.OK,
        "00A4040008A000000632010105" to ScriptedTransport.OK,
        "00B0950000" to ScriptedTransport.ok(FakeCards.info30()),
        "805C000304" to ScriptedTransport.ok(FakeCards.balance("00002710")),
        "805C000204" to ScriptedTransport.ok(FakeCards.balance("00000000")),
        "805C000104" to ScriptedTransport.ok(FakeCards.balance("00000000")),
        "00B0970000" to ScriptedTransport.ok(FakeCards.mgmt60()),
        "00B200C400" to ScriptedTransport.NOT_FOUND,
        "00B201C400" to ScriptedTransport.ok(FakeCards.txn23()),
        "00B201D400" to ScriptedTransport.ok(FakeCards.metro128()),
        "00B201F400" to ScriptedTransport.ok(FakeCards.process48()),
    )

    @Test
    fun `reads balance serial city transactions metro and process records`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(tUnionRoutes()))
        val app = TUnionReader().read(tag)!!
        assertEquals(CardKind.T_UNION, app.kind)
        assertEquals(10000L, app.balanceCents)
        assertEquals("12345678901234567890", app.serial)
        assertEquals("4910", app.cityCode)
        assertEquals("郑州", app.cityName)
        assertEquals(1, app.transactions.size)
        assertEquals(500L, app.transactions[0].amountCents)
        assertEquals(MetroStatus.ENTERED_NOT_EXITED, app.metro!!.status)
        assertEquals(1, app.processRecords.size)
        assertEquals(10000L, app.processRecords[0].balanceAfterCents)
    }

    @Test
    fun `returns null when aid select fails`() {
        val routes = tUnionRoutes() - "00A4040008A000000632010105"
        val tag = Iso7816Tag(ScriptedTransport.routing(routes))
        assertNull(TUnionReader().read(tag))
    }

    @Test
    fun `missing optional files do not abort reading`() {
        val routes = tUnionRoutes() - "00B0970000" - "00B201D400" - "00B201F400"
        val tag = Iso7816Tag(ScriptedTransport.routing(routes))
        val app = TUnionReader().read(tag)!!
        assertEquals(10000L, app.balanceCents)
        assertNull(app.metro)
        assertNull(app.cityCode)
        assertEquals(0, app.processRecords.size)
    }
}
