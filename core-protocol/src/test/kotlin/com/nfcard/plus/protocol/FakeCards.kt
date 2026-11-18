package com.nfcard.plus.protocol

/** 测试卡片数据构造工具（依据公开规范字节布局合成，非真实卡片数据）。 */
object FakeCards {

    fun info30(
        issuer: String = "A000000632010105",
        serial: String = "12345678901234567890",
    ): ByteArray = Bytes.fromHex(issuer + "02" + "01" + serial + "20200101" + "20301231" + "0000")

    fun txn23(seq: String = "000A", amount: String = "000001F4", type: String = "09"): ByteArray =
        Bytes.fromHex(seq + "000000" + amount + type + "000000000001" + "20261118" + "083015")

    fun metro128(entryTime: String = "20261119083000", exitTime: String = "00000000000000"): ByteArray =
        Bytes.fromHex(
            "2704" + "7D" + "01" + "01" + "00" + "01" +
                "12345678901234567890" + "0000000000000001" + "01" +
                "4910" + "0000" +
                "0000000000000001" + "0000000000000000" +
                "0000000001010203" + "0000000000000000" +
                "0000000000000001" + "0000000000000000" +
                entryTime + exitTime + "000003E8" + "00".repeat(32),
        )

    fun process48(): ByteArray = Bytes.fromHex(
        "01" + "0000000000000001" + "0000000000000002" +
            "00000000" + "00002710" + "20261119083000" +
            "4910" + "0000000000000001" + "00".repeat(6),
    )

    fun mgmt60(city: String = "4910"): ByteArray =
        Bytes.fromHex("00000156" + "0049" + city + "0001" + "01" + "00".repeat(49))

    fun beijingInfo32(): ByteArray =
        Bytes.fromHex("1122334455667788" + "010001" + "00".repeat(13) + "20180101" + "20281231")

    fun balance(centsHex: String): ByteArray = Bytes.fromHex(centsHex)
}
