package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CityUnionReaderTest {

    private fun routes(issuer: String): Map<String, ByteArray> = mapOf(
        "00A40000023F00" to ScriptedTransport.OK,
        "00A4040009A00000000386980701" to ScriptedTransport.OK,
        "00B0950000" to ScriptedTransport.ok(FakeCards.info30(issuer = issuer)),
        "805C000204" to ScriptedTransport.ok(FakeCards.balance("00000BB8")),
        "00B201C400" to ScriptedTransport.ok(FakeCards.txn23()),
    )

    @Test
    fun `nanjing card is named jinling tong with city`() {
        // 信息文件第 3-4 字节为城市代码 2100（南京）
        val tag = Iso7816Tag(ScriptedTransport.routing(routes("A000210000000000")))
        val app = CityUnionReader().read(tag)!!
        assertEquals(CardKind.CITY_UNION, app.kind)
        assertEquals("南京", app.cityName)
        assertEquals("金陵通", app.displayName)
        assertEquals(3000L, app.balanceCents)
    }

    @Test
    fun `unknown city falls back to generic name`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(routes("A000999900000000")))
        val app = CityUnionReader().read(tag)!!
        assertEquals("城市互联互通", app.displayName)
        assertEquals("9999", app.cityCode)
    }
}
