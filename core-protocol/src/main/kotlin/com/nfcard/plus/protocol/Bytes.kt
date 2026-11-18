package com.nfcard.plus.protocol

/** 字节工具：十六进制、大端整数、BCD 解码。 */
object Bytes {

    private const val HEX = "0123456789ABCDEF"

    fun hex(data: ByteArray, offset: Int = 0, len: Int = data.size - offset): String {
        require(offset >= 0 && len >= 0 && offset + len <= data.size) { "范围越界" }
        val sb = StringBuilder(len * 2)
        for (i in offset until offset + len) {
            val v = data[i].toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    fun fromHex(text: String): ByteArray {
        val s = text.replace(" ", "")
        require(s.length % 2 == 0) { "十六进制长度必须为偶数" }
        return ByteArray(s.length / 2) { i ->
            ((Character.digit(s[i * 2], 16) shl 4) or Character.digit(s[i * 2 + 1], 16)).toByte()
        }
    }

    /** 大端无符号整数，最多 8 字节。 */
    fun intBE(data: ByteArray, offset: Int, len: Int): Long {
        require(len in 1..8 && offset + len <= data.size) { "范围越界" }
        var v = 0L
        for (i in offset until offset + len) {
            v = (v shl 8) or (data[i].toLong() and 0xFF)
        }
        return v
    }

    /** BCD 数字串（每字节两位十进制）。 */
    fun bcd(data: ByteArray, offset: Int, len: Int): String {
        val sb = StringBuilder(len * 2)
        for (i in offset until offset + len) {
            val v = data[i].toInt() and 0xFF
            sb.append(v ushr 4).append(v and 0x0F)
        }
        return sb.toString()
    }
}
