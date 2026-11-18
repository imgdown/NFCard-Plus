package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AutoReaderTest {

    /** 多合一模拟卡：同时含交通联合与城市互联两个应用，按当前选中 AID 返回对应文件。 */
    private fun multiAppTransport(brokenTUnionBalance: Boolean = false): ScriptedTransport {
        var current = ""
        val tUnionAid = "A000000632010105"
        val cityUnionAid = "A00000000386980701"
        return ScriptedTransport { cmd ->
            val hex = Bytes.hex(cmd)
            when {
                hex == "00A40000023F00" -> ScriptedTransport.OK
                hex.startsWith("00A40400") && hex.contains(tUnionAid) -> {
                    current = "tunion"; ScriptedTransport.OK
                }
                hex.startsWith("00A40400") && hex.contains(cityUnionAid) -> {
                    current = "cityunion"; ScriptedTransport.OK
                }
                hex == "00B0950000" && current == "tunion" ->
                    ScriptedTransport.ok(FakeCards.info30())
                hex == "00B0950000" && current == "cityunion" ->
                    ScriptedTransport.ok(FakeCards.info30(issuer = "A000000003869807", serial = "98765432109876543210"))
                hex == "805C000304" && current == "tunion" ->
                    if (brokenTUnionBalance) throw IOException("模拟通信故障")
                    else ScriptedTransport.ok(FakeCards.balance("00002710"))
                hex == "805C000204" && current == "cityunion" ->
                    ScriptedTransport.ok(FakeCards.balance("00000BB8"))
                hex == "805C000204" && current == "tunion" ->
                    ScriptedTransport.ok(FakeCards.balance("00000000"))
                hex == "805C000104" -> ScriptedTransport.ok(FakeCards.balance("00000000"))
                hex == "00B0970000" && current == "tunion" ->
                    ScriptedTransport.ok(FakeCards.mgmt60())
                hex == "00B201C400" -> ScriptedTransport.ok(FakeCards.txn23())
                hex == "00B201D400" && current == "tunion" ->
                    ScriptedTransport.ok(FakeCards.metro128())
                hex == "00B201F400" && current == "tunion" ->
                    ScriptedTransport.ok(FakeCards.process48())
                else -> ScriptedTransport.NOT_FOUND
            }
        }
    }

    @Test
    fun `multi application card yields both apps`() {
        val report = AutoReader.read(multiAppTransport())
        assertEquals(2, report.apps.size)
        assertTrue(report.apps.any { it.kind == CardKind.T_UNION })
        assertTrue(report.apps.any { it.kind == CardKind.CITY_UNION })
        assertTrue(report.failures.isEmpty())
    }

    @Test
    fun `one broken app does not block the other`() {
        val report = AutoReader.read(multiAppTransport(brokenTUnionBalance = true))
        assertEquals(1, report.apps.size)
        assertEquals(CardKind.CITY_UNION, report.apps[0].kind)
        assertEquals(1, report.failures.size)
        assertEquals(CardKind.T_UNION, report.failures[0].kind)
    }

    @Test
    fun `unknown card yields empty report`() {
        val report = AutoReader.read(ScriptedTransport { ScriptedTransport.NOT_FOUND })
        assertTrue(report.apps.isEmpty())
    }

    @Test
    fun `manual reader reads only requested kind`() {
        val report = ManualReader.read(multiAppTransport(), CardKind.CITY_UNION)
        assertEquals(1, report.apps.size)
        assertEquals(CardKind.CITY_UNION, report.apps[0].kind)
    }

    @Test
    fun `manual reader reports failure for absent kind`() {
        val report = ManualReader.read(
            ScriptedTransport { ScriptedTransport.NOT_FOUND },
            CardKind.T_UNION,
        )
        assertTrue(report.apps.isEmpty())
    }
}
