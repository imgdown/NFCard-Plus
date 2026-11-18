package com.nfcard.plus.protocol.model

/** 卡种。 */
enum class CardKind(val label: String) {
    T_UNION("交通联合"),
    CITY_UNION("城市互联互通"),
    BEIJING("北京一卡通"),
    WUHAN("武汉通"),
    SHENZHEN("深圳通"),
    PBOC_EP("标准电子钱包"),
}

/** 电子钱包交易明细（0x18 文件，23 字节记录）。 */
data class TxnRecord(
    val seq: Int,
    val amountCents: Long,
    val typeCode: Int,
    val terminalId: String,
    val date: String,
    val time: String,
    val overdrawCents: Long,
) {
    /** 0x06 消费 / 0x09 复合消费为扣款，其余（圈存等）为入账。 */
    val isDebit: Boolean get() = typeCode == 0x06 || typeCode == 0x09
}

/** 地铁进出站状态（依据进出闸时间推导，原始状态码一并保留）。 */
enum class MetroStatus { ENTERED_NOT_EXITED, EXITED, NO_RECORD }

/** 地铁应用信息记录（0x1A 文件，记录 ID 0x2704，128 字节）。 */
data class MetroInfo(
    val stateCode: Int,
    val entryCityCode: String,
    val exitCityCode: String,
    val entryAgency: String,
    val exitAgency: String,
    val entryStation: String,
    val exitStation: String,
    val entryTerminal: String,
    val exitTerminal: String,
    val entryTime: String?,
    val exitTime: String?,
    val maxFareCents: Long,
) {
    val status: MetroStatus
        get() = when {
            entryTime != null && exitTime == null -> MetroStatus.ENTERED_NOT_EXITED
            entryTime != null && exitTime != null -> MetroStatus.EXITED
            else -> MetroStatus.NO_RECORD
        }
}

/** 互联互通过程记录（0x1E 文件，48 字节记录）。 */
data class ProcessRecord(
    val typeCode: Int,
    val terminalId: String,
    val seq: String,
    val amountCents: Long,
    val balanceAfterCents: Long,
    val dateTime: String,
    val cityCode: String,
    val agencyId: String,
)

/** 电子钱包公共应用信息（0x15 文件，30 字节）。 */
data class EpInfo(
    val issuer: String,
    val appType: Int,
    val version: String?,
    val serial: String,
    val validFrom: String,
    val validTo: String,
)

/** 交通联合管理信息（0x17 文件）。 */
data class ManagementInfo(
    val countryCode: String,
    val provinceCode: String,
    val cityCode: String,
    val cardType: String,
)

/** 单个卡片应用的读取结果。 */
data class CardApp(
    val kind: CardKind,
    val displayName: String,
    val balanceCents: Long? = null,
    val overdrawUsedCents: Long? = null,
    val overdrawLimitCents: Long? = null,
    val serial: String? = null,
    val version: String? = null,
    val validFrom: String? = null,
    val validTo: String? = null,
    val cityCode: String? = null,
    val cityName: String? = null,
    val transactions: List<TxnRecord> = emptyList(),
    val metro: MetroInfo? = null,
    val processRecords: List<ProcessRecord> = emptyList(),
)

/** 单个读取器失败信息。 */
data class ReadFailure(val kind: CardKind, val reason: String)

/** 整卡读取报告：多合一卡包含多个应用。 */
data class CardReport(val apps: List<CardApp>, val failures: List<ReadFailure> = emptyList())
