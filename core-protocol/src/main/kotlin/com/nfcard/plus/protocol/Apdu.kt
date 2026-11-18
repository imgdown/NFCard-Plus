package com.nfcard.plus.protocol

/** ISO 7816-4 APDU 命令。 */
data class ApduCommand(
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val data: ByteArray? = null,
    val le: Int? = null,
) {
    fun toBytes(): ByteArray {
        val body = data ?: ByteArray(0)
        val hasLc = data != null
        val size = 4 + (if (hasLc) 1 + body.size else 0) + (if (le != null) 1 else 0)
        val out = ByteArray(size)
        out[0] = cla.toByte()
        out[1] = ins.toByte()
        out[2] = p1.toByte()
        out[3] = p2.toByte()
        var pos = 4
        if (hasLc) {
            out[pos++] = body.size.toByte()
            body.copyInto(out, pos)
            pos += body.size
        }
        if (le != null) out[pos] = le.toByte()
        return out
    }

    override fun equals(other: Any?): Boolean =
        other is ApduCommand && toBytes().contentEquals(other.toBytes())

    override fun hashCode(): Int = toBytes().contentHashCode()
}

/** APDU 响应：数据域 + 状态字 SW1SW2。 */
data class ApduResponse(val data: ByteArray, val sw1: Int, val sw2: Int) {

    val isOk: Boolean get() = sw1 == 0x90 && sw2 == 0x00

    val statusWord: Int get() = (sw1 shl 8) or sw2

    companion object {
        fun from(raw: ByteArray): ApduResponse {
            require(raw.size >= 2) { "APDU 响应至少包含状态字两字节" }
            return ApduResponse(
                data = raw.copyOfRange(0, raw.size - 2),
                sw1 = raw[raw.size - 2].toInt() and 0xFF,
                sw2 = raw[raw.size - 1].toInt() and 0xFF,
            )
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ApduResponse && data.contentEquals(other.data) && sw1 == other.sw1 && sw2 == other.sw2

    override fun hashCode(): Int = (data.contentHashCode() * 31 + sw1) * 31 + sw2
}
