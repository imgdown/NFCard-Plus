package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.MemoryCardInfo
import com.nfcard.plus.protocol.model.TicketSummary
import com.nfcard.plus.protocol.model.ValueBlock

/** 存储型票卡（Mifare Ultralight / Classic 等）原始数据分析。 */
object MemoryCardAnalyzer {

    /** 从数据页提取 UID：页 0 首字节 0x88 为级联标记（7 字节 UID），否则 4 字节 UID。 */
    fun uidFromPages(pages: List<ByteArray>): String? {
        val p0 = pages.getOrNull(0) ?: return null
        if (p0.size < 4) return null
        return if (p0[0] == 0x88.toByte()) {
            val p1 = pages.getOrNull(1) ?: return null
            if (p1.size < 4) return null
            Bytes.hex(p0, 1, 3) + Bytes.hex(p1, 0, 4)
        } else {
            Bytes.hex(p0, 0, 4)
        }
    }

    /** 数据页转储文本：每行 4 页，行首为起始页号。 */
    fun formatDump(pages: List<ByteArray>): String =
        pages.chunked(4).mapIndexed { index, group ->
            val offset = index * 4
            "%04X: ".format(offset) + group.joinToString(" ") { Bytes.hex(it) }
        }.joinToString("\n")

    /** Classic 块转储文本：每行一个 16 字节块，行首为块号。 */
    fun formatBlockDump(blocks: List<ByteArray>): String =
        blocks.mapIndexed { index, b -> "%02d: ".format(index) + Bytes.hex(b) }.joinToString("\n")

    /** 标准 Mifare 值块校验：值+取反+值+地址校验格式（值以小端存储）。符合返回有符号值，否则 null。 */
    fun valueOf(block: ByteArray): Long? {
        if (block.size < 16) return null
        val v = intLE(block, 0)
        val inv = intLE(block, 4)
        val v2 = intLE(block, 8)
        if (inv != (v xor 0xFFFFFFFFL) || v2 != v) return null
        val a = block[12].toInt() and 0xFF
        val na = block[13].toInt() and 0xFF
        if (block[14].toInt() and 0xFF != a || block[15].toInt() and 0xFF != na) return null
        if (na != (a xor 0xFF)) return null
        return v.toInt().toLong()
    }

    private fun intLE(data: ByteArray, offset: Int): Long {
        var v = 0L
        for (i in offset + 3 downTo offset) {
            v = (v shl 8) or (data[i].toLong() and 0xFF)
        }
        return v
    }

    /** 地铁票卡系统密钥特征（公开字典密钥，用于识别票卡体系）。 */
    private const val TICKET_KEY = "441015110246"

    /** 乘车记录固定标记（实测）。 */
    private val RIDE_MARKER = Bytes.fromHex("00018C0000")

    /**
     * 地铁票卡识别与摘要：扇区密钥特征 + 城市代码 + 值块 + 乘车记录。
     * 不符合特征返回 null。
     */
    fun ticketSummary(blocks: List<ByteArray>): TicketSummary? {
        if (blocks.size != 64) return null
        var keySectors = 0
        for (s in 0 until 16) {
            val trailer = blocks[s * 4 + 3]
            if (trailer.size >= 6 && Bytes.hex(trailer, 0, 6) == TICKET_KEY) keySectors++
        }
        if (keySectors < 4) return null

        val b2 = blocks[2]
        val cityCode = if (b2.size >= 2) "%04d".format(Bytes.intBE(b2, 0, 2).let {
            ((it and 0xFF) shl 8) or ((it shr 8) and 0xFF)
        }) else null
        val cityName = cityCode?.let { com.nfcard.plus.protocol.CityUnionCities.city(it) }

        val values = mutableListOf<ValueBlock>()
        val rides = mutableListOf<ByteArray>()
        for (i in 1 until 64) {
            if (i % 4 == 3) continue
            val b = blocks[i]
            if (b.size < 16) continue
            valueOf(b)?.let { values.add(ValueBlock(i, it)) }
            if (b.copyOfRange(6, 11).contentEquals(RIDE_MARKER) && !b.all { it == 0.toByte() }) {
                rides.add(b)
            }
        }
        return TicketSummary(cityCode, cityName, values, rides)
    }
}
