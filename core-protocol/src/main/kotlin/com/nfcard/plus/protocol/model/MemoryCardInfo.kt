package com.nfcard.plus.protocol.model

/** Mifare Classic 值块（余额/计数）。 */
data class ValueBlock(val blockIndex: Int, val value: Long)

/** 票卡摘要（地铁票卡等存储卡的结构化信息）。 */
data class TicketSummary(
    val cityCode: String?,
    val cityName: String?,
    val valueBlocks: List<ValueBlock>,
    val rideRecords: List<ByteArray>,
)

/** 存储型票卡（非 ISO-DEP）读取结果：技术类型、UID、原始数据。 */
data class MemoryCardInfo(
    val techs: List<String>,
    val uid: String,
    val pages: List<ByteArray> = emptyList(),
    val blocks: List<ByteArray> = emptyList(),
    val ticket: TicketSummary? = null,
    val note: String? = null,
)
