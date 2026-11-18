package com.nfcard.plus.protocol

/** 测试卡片数据构造工具（依据公开规范与实测字节布局合成，全部为虚构数据）。 */
object FakeCards {

    fun info30(
        issuer: String = "A000000632010105",
        serial: String = "12345678901234567890",
    ): ByteArray = Bytes.fromHex(issuer + "02" + "01" + serial + "20200101" + "20301231" + "0000")

    fun txn23(seq: String = "000A", amount: String = "000001F4", type: String = "09"): ByteArray =
        Bytes.fromHex(seq + "000000" + amount + type + "000000000001" + "20261118" + "083015")

    /** 地铁应用信息记录：实测紧凑布局（PAN 8 字节，无前置流水号/状态字段）。 */
    fun metro128(entryTime: String = "20260101080000", exitTime: String = "00000000000000"): ByteArray =
        Bytes.fromHex(
            "2701" + "7D" + "01" + "01" + "00" + "01" +
                "1122334455667788" +
                "4910" + "0000" +
                "0100000000000001" + "0000000000000000" +
                "0100000000000101" + "0000000000000000" +
                "0100000000000011" + "0000000000000000" +
                entryTime + exitTime + "000000C8" +
                "0000" + "000000000000" + "0000" + "00" + "00".repeat(32),
        )

    /** 互联互通过程记录：实测布局（类型1+城市2+终端7+站点4+金额4+透支3+余额4+时间7+城市2+机构8+保留6）。 */
    fun process48(): ByteArray = Bytes.fromHex(
        "03" + "4910" + "00000000000011" + "00000101" +
            "00000000" + "000000" + "00002710" + "20260101080000" +
            "4910" + "0100000000000001" + "00".repeat(6),
    )

    fun mgmt60(city: String = "4910"): ByteArray =
        Bytes.fromHex("00000156" + "0049" + city + "0001" + "01" + "00".repeat(49))

    fun beijingInfo32(): ByteArray =
        Bytes.fromHex("1122334455667788" + "010001" + "00".repeat(13) + "20180101" + "20281231")

    fun balance(centsHex: String): ByteArray = Bytes.fromHex(centsHex)
}
