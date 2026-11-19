package com.nfcard.plus.protocol.model

/** ETC 入口/出口通行记录（0019 文件记录一，43 字节，标记 0xAA）。 */
data class EtcPassage(
    val networkNo: String,
    val stationNo: String,
    val lane: Int,
    val time: String?,
    val vehicleType: Int,
    val ioState: Int,
    val plate: String?,
)

/** ETC 用户卡信息（依据联网收费用户卡文件结构公开规范）。 */
data class EtcInfo(
    val issuer: String,
    val cardType: Int,
    val version: Int,
    val newStandard: Boolean,
    val networkNo: String,
    val cardNo: String,
    val issueDate: String?,
    val expiryDate: String?,
    val plate: String?,
    val plateColor: String?,
    val vehicleType: Int?,
    val userType: Int?,
    val holderName: String?,
    val holderIdNumber: String?,
    val holderIdType: Int?,
    val passages: List<EtcPassage> = emptyList(),
)
