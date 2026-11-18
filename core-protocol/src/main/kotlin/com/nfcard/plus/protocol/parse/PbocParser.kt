package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.EpInfo
import com.nfcard.plus.protocol.model.TxnRecord

/** PBOC 电子钱包通用数据解析。金额单位：分。 */
object PbocParser {

    /** GET BALANCE 返回的 4 字节有符号余额（分）。数据不足返回 null。 */
    fun balance(data: ByteArray): Long? {
        if (data.size < 4) return null
        val v = Bytes.intBE(data, 0, 4)
        return if (v >= 0x80000000L) v - 0x100000000L else v
    }

    /** 0x15 公共应用信息文件（30 字节）。 */
    fun epInfo(data: ByteArray): EpInfo? {
        if (data.size < 30) return null
        val version = data[9].toInt() and 0xFF
        return EpInfo(
            issuer = Bytes.hex(data, 0, 8),
            appType = data[8].toInt() and 0xFF,
            version = if (version != 0) version.toString() else null,
            serial = Bytes.bcd(data, 10, 10),
            validFrom = formatDate(Bytes.bcd(data, 20, 4)),
            validTo = formatDate(Bytes.bcd(data, 24, 4)),
        )
    }

    /** 0x18 交易明细单条记录（23 字节）。全零空槽返回 null；零金额复合消费为有效进站记录。 */
    fun txnRecord(data: ByteArray): TxnRecord? {
        if (data.size < 23) return null
        val amount = Bytes.intBE(data, 5, 4)
        val type = data[9].toInt() and 0xFF
        val dateDigits = Bytes.bcd(data, 16, 4)
        val isEmptySlot = amount == 0L && (type == 0 || isZeroDigits(dateDigits))
        if (isEmptySlot) return null
        return TxnRecord(
            seq = Bytes.intBE(data, 0, 2).toInt(),
            amountCents = amount,
            typeCode = type,
            terminalId = Bytes.bcd(data, 10, 6),
            date = formatDate(dateDigits),
            time = formatTime(Bytes.bcd(data, 20, 3)),
            overdrawCents = Bytes.intBE(data, 2, 3),
        )
    }

    /** 将一次读出的多条定长记录切分。 */
    fun splitRecords(raw: ByteArray, recordLen: Int): List<ByteArray> {
        require(recordLen > 0)
        val count = raw.size / recordLen
        return (0 until count).map { raw.copyOfRange(it * recordLen, (it + 1) * recordLen) }
    }

    internal fun formatDate(yyyymmdd: String): String =
        "${yyyymmdd.substring(0, 4)}-${yyyymmdd.substring(4, 6)}-${yyyymmdd.substring(6, 8)}"

    internal fun formatTime(hhmmss: String): String =
        "${hhmmss.substring(0, 2)}:${hhmmss.substring(2, 4)}:${hhmmss.substring(4, 6)}"

    internal fun formatDateTime(yyyymmddhhmmss: String): String =
        formatDate(yyyymmddhhmmss.substring(0, 8)) + " " + formatTime(yyyymmddhhmmss.substring(8, 14))

    internal fun isZeroDigits(digits: String): Boolean = digits.all { it == '0' }
}
