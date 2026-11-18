package com.nfcard.plus.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TracingTransportTest {

    @Test
    fun `records command and response hex for each exchange`() {
        val delegate = ScriptedTransport { ScriptedTransport.ok(Bytes.fromHex("000186A0")) }
        val tracing = TracingTransport(delegate)
        tracing.transceive(Bytes.fromHex("805C000304"))
        assertEquals(1, tracing.entries.size)
        val entry = tracing.entries[0]
        assertTrue(entry.contains("805C000304"))
        assertTrue(entry.contains("000186A09000"))
    }

    @Test
    fun `caps entries to avoid unbounded growth`() {
        val delegate = ScriptedTransport { ScriptedTransport.OK }
        val tracing = TracingTransport(delegate, maxEntries = 2)
        repeat(5) { tracing.transceive(Bytes.fromHex("00B0950000")) }
        assertEquals(2, tracing.entries.size)
    }

    @Test
    fun `close delegates to underlying transport`() {
        val delegate = ScriptedTransport { ScriptedTransport.OK }
        TracingTransport(delegate).close()
        assertTrue(delegate.closed)
    }
}
