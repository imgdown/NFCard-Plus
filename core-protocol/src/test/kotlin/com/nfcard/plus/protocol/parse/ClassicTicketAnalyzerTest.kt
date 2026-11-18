package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicTicketAnalyzerTest {

    private fun block(hex: String): ByteArray = Bytes.fromHex(hex)

    /** 合成 64 块转储：密钥特征 + 城市代码 + 值块 + 乘车记录（全部虚构数据）。 */
    private fun syntheticDump(): List<ByteArray> {
        val blocks = MutableList(64) { ByteArray(16) }
        // 扇区尾块：密钥 A = 441015110246（系统密钥特征）
        for (s in 1 until 16) {
            blocks[s * 4 + 3] = block("441015110246" + "78778869" + "441015110246")
        }
        blocks[3] = block("A0A1A2A3A4A5" + "78778869" + "000000000000")
        // 块 2：城市代码 2100（小端 0x0834）
        blocks[2] = block("3408" + "00".repeat(14))
        // 值块：99，块 8/9 互为备份
        val value99 = "63000000" + "9CFFFFFF" + "63000000" + "09F609F6"
        blocks[8] = block(value99)
        blocks[9] = block(value99)
        // 零值计数块
        val value0 = "00000000" + "FFFFFFFF" + "00000000" + "0CF30CF3"
        blocks[12] = block(value0)
        // 乘车记录：固定标记 00018C0000 位于偏移 6
        blocks[32] = block("112233445566" + "00018C0000" + "778899AABB")
        blocks[33] = block("223344556677" + "00018C0000" + "8899AABBCC")
        return blocks
    }

    @Test
    fun `value block decodes positive value`() {
        assertEquals(99L, MemoryCardAnalyzer.valueOf(block("630000009CFFFFFF6300000009F609F6")))
    }

    @Test
    fun `value block decodes negative value`() {
        assertEquals(-1L, MemoryCardAnalyzer.valueOf(block("FFFFFFFF00000000FFFFFFFF0CF30CF3")))
    }

    @Test
    fun `non value block returns null`() {
        assertNull(MemoryCardAnalyzer.valueOf(block("0001021111160708090A0B0C0D0E0F10")))
        assertNull(MemoryCardAnalyzer.valueOf(ByteArray(8)))
    }

    @Test
    fun `ticket summary extracts city values and ride records`() {
        val summary = MemoryCardAnalyzer.ticketSummary(syntheticDump())!!
        assertEquals("2100", summary.cityCode)
        assertEquals("南京", summary.cityName)
        assertTrue(summary.valueBlocks.any { it.blockIndex == 8 && it.value == 99L })
        assertTrue(summary.valueBlocks.any { it.blockIndex == 12 && it.value == 0L })
        assertEquals(2, summary.rideRecords.size)
    }

    @Test
    fun `dump without ticket features returns null`() {
        assertNull(MemoryCardAnalyzer.ticketSummary(List(64) { ByteArray(16) }))
    }

    @Test
    fun `block dump labels each block`() {
        val dump = MemoryCardAnalyzer.formatBlockDump(List(4) { block("0001021111160708090A0B0C0D0E0F10") })
        val lines = dump.lines()
        assertEquals(4, lines.size)
        assertTrue(lines[0].startsWith("00:"))
        assertTrue(lines[3].startsWith("03:"))
    }
}
