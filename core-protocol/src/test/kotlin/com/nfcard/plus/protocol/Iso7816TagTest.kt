package com.nfcard.plus.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Iso7816TagTest {

    @Test
    fun `selectByName sends 00A40400 with Lc and name`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        val tag = Iso7816Tag(transport)
        val rsp = tag.selectByName(Bytes.fromHex("A000000632010105"))
        assertTrue(rsp.isOk)
        assertArrayEquals(
            Bytes.fromHex("00A4040008A000000632010105"),
            transport.received.single(),
        )
    }

    @Test
    fun `selectById sends 00A40000 with two byte id`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).selectById(Bytes.fromHex("3F00"))
        assertArrayEquals(Bytes.fromHex("00A40000023F00"), transport.received.single())
    }

    @Test
    fun `readBinary sets high bit of SFI in P1`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).readBinary(0x15)
        assertArrayEquals(Bytes.fromHex("00B0950000"), transport.received.single())
    }

    @Test
    fun `readRecord encodes SFI shifted into P2 with mode 4`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).readRecord(0x18, 1)
        // P2 = (0x18 << 3) | 4 = 0xC4
        assertArrayEquals(Bytes.fromHex("00B201C400"), transport.received.single())
    }

    @Test
    fun `readRecordAll uses record number zero`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).readRecordAll(0x18)
        assertArrayEquals(Bytes.fromHex("00B200C400"), transport.received.single())
    }

    @Test
    fun `getBalance sends 805C with type and Le 4`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).getBalance(0x03)
        assertArrayEquals(Bytes.fromHex("805C000304"), transport.received.single())
    }

    @Test
    fun `close delegates to transport`() {
        val transport = ScriptedTransport { ScriptedTransport.OK }
        Iso7816Tag(transport).close()
        assertTrue(transport.closed)
    }
}
