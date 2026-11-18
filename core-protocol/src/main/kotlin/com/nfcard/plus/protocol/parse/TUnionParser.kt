package com.nfcard.plus.protocol.parse

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.ManagementInfo
import com.nfcard.plus.protocol.model.MetroInfo
import com.nfcard.plus.protocol.model.ProcessRecord

/** 交通联合（交通部互联互通规范）专用数据解析。布局经真实卡片实测核对。 */
object TUnionParser {

    /** 实测地铁应用信息记录 ID。 */
    const val METRO_RECORD_ID = 0x2701

    /** 兼容部分规范文档中的地铁记录 ID。 */
    const val METRO_RECORD_ID_ALT = 0x2704

    const val BUS_RECORD_ID = 0x2702

    /** 0x1A 地铁应用信息记录（128 字节紧凑布局）。ID 不符或长度不足返回 null。 */
    fun metroRecord(data: ByteArray): MetroInfo? {
        if (data.size < 85) return null
        val id = Bytes.intBE(data, 0, 2).toInt()
        if (id != METRO_RECORD_ID && id != METRO_RECORD_ID_ALT) return null
        val entryDigits = Bytes.bcd(data, 67, 7)
        val exitDigits = Bytes.bcd(data, 74, 7)
        return MetroInfo(
            entryCityCode = Bytes.bcd(data, 15, 2),
            exitCityCode = Bytes.bcd(data, 17, 2),
            entryAgency = Bytes.bcd(data, 19, 8),
            exitAgency = Bytes.bcd(data, 27, 8),
            entryStation = Bytes.bcd(data, 35, 8),
            exitStation = Bytes.bcd(data, 43, 8),
            entryTerminal = Bytes.bcd(data, 51, 8),
            exitTerminal = Bytes.bcd(data, 59, 8),
            entryTime = if (PbocParser.isZeroDigits(entryDigits)) null else PbocParser.formatDateTime(entryDigits),
            exitTime = if (PbocParser.isZeroDigits(exitDigits)) null else PbocParser.formatDateTime(exitDigits),
            maxFareCents = Bytes.intBE(data, 81, 4),
        )
    }

    /** 0x1E 互联互通循环记录（48 字节，实测布局）。全零记录视为空返回 null。 */
    fun processRecord(data: ByteArray): ProcessRecord? {
        if (data.size < 48) return null
        if (data.all { it == 0.toByte() }) return null
        return ProcessRecord(
            typeCode = data[0].toInt() and 0xFF,
            terminalId = Bytes.hex(data, 3, 7),
            stationCode = Bytes.hex(data, 10, 4),
            amountCents = Bytes.intBE(data, 14, 4),
            balanceAfterCents = Bytes.intBE(data, 21, 4),
            dateTime = PbocParser.formatDateTime(Bytes.bcd(data, 25, 7)),
            cityCode = Bytes.bcd(data, 32, 2),
            agencyId = Bytes.bcd(data, 34, 8),
        )
    }

    /** 0x17 管理信息文件（60 字节），城市代码为银联地区码。 */
    fun managementInfo(data: ByteArray): ManagementInfo? {
        if (data.size < 11) return null
        return ManagementInfo(
            countryCode = Bytes.hex(data, 0, 4),
            provinceCode = Bytes.hex(data, 4, 2),
            cityCode = Bytes.hex(data, 6, 2),
            cardType = Bytes.hex(data, 8, 2),
        )
    }
}
