package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.FakeCards
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.ScriptedTransport
import com.nfcard.plus.protocol.model.CardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：部分真实卡片会拒绝 MF/PSE 复位指令，
 * 但直接选择交通联合 AID 仍可成功。复位必须是尽力而为，不能作为读取前提。
 */
class TUnionResetRegressionTest {

    private fun cardRoutes(mfOk: Boolean, pse1Ok: Boolean, pse2Ok: Boolean): Map<String, ByteArray> {
        val routes = mutableMapOf(
            "00A4040008A000000632010105" to ScriptedTransport.OK,
            "00B0950000" to ScriptedTransport.ok(FakeCards.info30()),
            "805C000304" to ScriptedTransport.ok(FakeCards.balance("00002710")),
            "805C000204" to ScriptedTransport.ok(FakeCards.balance("00000000")),
            "805C000104" to ScriptedTransport.ok(FakeCards.balance("00000000")),
            "00B0970000" to ScriptedTransport.ok(FakeCards.mgmt60()),
            "00B201C400" to ScriptedTransport.ok(FakeCards.txn23()),
            "00B201D400" to ScriptedTransport.ok(FakeCards.metro128()),
            "00B201F400" to ScriptedTransport.ok(FakeCards.process48()),
        )
        if (mfOk) routes["00A40000023F00"] = ScriptedTransport.OK
        if (pse1Ok) routes["00A404000E3150"] = ScriptedTransport.OK
        if (pse2Ok) routes["00A404000E3250"] = ScriptedTransport.OK
        return routes
    }

    @Test
    fun `card refusing MF and PSE still reads via direct AID select`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(cardRoutes(mfOk = false, pse1Ok = false, pse2Ok = false)))
        val app = TUnionReader().read(tag)
        assertEquals(CardKind.T_UNION, app?.kind)
        assertEquals(10000L, app?.balanceCents)
    }

    @Test
    fun `card requiring PPSE 2PAY before AID select reads successfully`() {
        val tag = Iso7816Tag(ScriptedTransport.routing(cardRoutes(mfOk = false, pse1Ok = false, pse2Ok = true)))
        val app = TUnionReader().read(tag)
        assertEquals(CardKind.T_UNION, app?.kind)
    }

    @Test
    fun `auto reader finds T-Union on card refusing MF and PSE`() {
        val report = AutoReader.read(
            ScriptedTransport.routing(cardRoutes(mfOk = false, pse1Ok = false, pse2Ok = false)),
        )
        assertTrue(report.apps.any { it.kind == CardKind.T_UNION })
    }
}
