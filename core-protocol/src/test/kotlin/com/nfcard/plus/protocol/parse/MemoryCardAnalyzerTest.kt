package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCardAnalyzerTest {

    private fun page(hex: String): ByteArray = Bytes.fromHex(hex)

    @Test
    fun `uid with cascade marker uses seven bytes`() {
        val pages = listOf(page("88010203"), page("04050607"), page("08090000"))
        val expected = "010203" + "04050607"
        assertEquals(expected, MemoryCardAnalyzer.uidFromPages(pages))
    }

    @Test
    fun `four byte uid reads first page`() {
        val pages = listOf(page("AABBCCDD"), page("00000000"))
        assertEquals("AABBCCDD", MemoryCardAnalyzer.uidFromPages(pages))
    }

    @Test
    fun `empty pages return null`() {
        assertNull(MemoryCardAnalyzer.uidFromPages(emptyList()))
    }

    @Test
    fun `dump groups four pages per line with page offset`() {
        val pages = (0 until 8).map { page("00010203") }
        val dump = MemoryCardAnalyzer.formatDump(pages)
        val lines = dump.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("0000:"))
        assertTrue(lines[1].startsWith("0004:"))
        assertTrue(lines[0].contains("00010203"))
    }
}
