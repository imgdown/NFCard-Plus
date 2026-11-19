package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.EtcInfo
import com.nfcard.plus.protocol.model.EtcPassage
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/** ETC 用户卡数据解析（联网收费用户卡文件结构公开规范）。 */
object EtcParser {

    /** 0015 卡片发行基本数据文件（新标准 50 字节 / 旧标准 43 字节）。长度不足返回 null。 */
    fun issuance(data: ByteArray): EtcInfo? {
        if (data.size < 43) return null
        val version = data[9].toInt() and 0xFF
        val newStandard = (version ushr 4) >= 4
        return EtcInfo(
            issuer = Bytes.hex(data, 0, 8),
            cardType = data[8].toInt() and 0xFF,
            version = version,
            newStandard = newStandard,
            networkNo = Bytes.hex(data, 10, 2),
            cardNo = Bytes.hex(data, 12, 8),
            issueDate = bcdDate(data, 20),
            expiryDate = bcdDate(data, 24),
            plate = decodeGb2312(data, 28, 12),
            plateColor = if (newStandard && data.size >= 42) plateColorName(data[41].toInt() and 0xFF) else null,
            vehicleType = if (newStandard && data.size >= 43) data[42].toInt() and 0xFF else null,
            userType = data[40].toInt() and 0xFF,
            holderName = null,
            holderIdNumber = null,
            holderIdType = null,
        )
    }

    /** 0016 持卡人基本数据文件（55 字节）：姓名 GB2312、证件号码。长度不足返回 null。 */
    fun holder(data: ByteArray): Triple<String?, String?, Int?>? {
        if (data.size < 55) return null
        val name = decodeGb2312(data, 2, 20)
        val id = asciiTrim(data, 22, 32)
        val type = data[54].toInt() and 0xFF
        return Triple(name, id, type)
    }

    /** 0019 记录一：ETC 专用通行记录（43 字节，标记 AA 29）。不符返回 null。 */
    fun passageRecord(data: ByteArray): EtcPassage? {
        if (data.size < 43) return null
        if (data[0] != 0xAA.toByte() || data[1] != 0x29.toByte()) return null
        return EtcPassage(
            networkNo = Bytes.hex(data, 3, 2),
            stationNo = Bytes.hex(data, 5, 2),
            lane = data[7].toInt() and 0xFF,
            time = formatUnixSeconds(Bytes.intBE(data, 8, 4)),
            vehicleType = data[12].toInt() and 0xFF,
            ioState = data[13].toInt() and 0xFF,
            plate = decodeGb2312(data, 27, 12),
        )
    }

    /** 车牌颜色代码 → 名称（含新能源渐变绿等扩展色）。 */
    fun plateColorName(code: Int): String? = when (code) {
        0x00 -> "蓝色"
        0x01 -> "黄色"
        0x02 -> "黑色"
        0x03 -> "白色"
        0x04 -> "渐变绿"
        0x05 -> "黄绿双拼"
        0x06 -> "蓝白渐变"
        else -> null
    }

    /** GB2312 字节解码为文本，去除尾部填充。 */
    fun decodeGb2312(data: ByteArray, offset: Int, len: Int): String? {
        if (offset + len > data.size) return null
        var end = offset + len
        while (end > offset && (data[end - 1] == 0x00.toByte() || data[end - 1] == 0xFF.toByte() || data[end - 1] == 0x20.toByte())) {
            end--
        }
        if (end == offset) return null
        return String(data, offset, end - offset, charset("GB2312")).trim().ifEmpty { null }
    }

    /** UNIX 秒 → 北京时间文本（收费公路统一使用东八区）。 */
    fun formatUnixSeconds(seconds: Long): String? {
        if (seconds < 0) return null
        val cal = GregorianCalendar(TimeZone.getTimeZone("GMT+8"), Locale.CHINA)
        cal.timeInMillis = seconds * 1000
        return "%04d-%02d-%02d %02d:%02d:%02d".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
        )
    }

    private fun bcdDate(data: ByteArray, offset: Int): String? {
        val digits = Bytes.bcd(data, offset, 4)
        return if (digits.all { it == '0' }) null else PbocParser.formatDate(digits)
    }

    private fun asciiTrim(data: ByteArray, offset: Int, len: Int): String? {
        if (offset + len > data.size) return null
        var end = offset + len
        while (end > offset && (data[end - 1] == 0x00.toByte() || data[end - 1] == 0xFF.toByte() || data[end - 1] == 0x20.toByte())) {
            end--
        }
        if (end == offset) return null
        return String(data, offset, end - offset, Charsets.US_ASCII).trim().ifEmpty { null }
    }
}
