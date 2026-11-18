package com.nfcard.plus.protocol

/** ISO 7816 标签会话：封装 PBOC 读卡所需的基本命令。 */
class Iso7816Tag(private val transport: CardTransport) {

    private fun send(command: ApduCommand): ApduResponse =
        ApduResponse.from(transport.transceive(command.toBytes()))

    /** 按 DF 名称（AID）选择应用：00 A4 04 00 Lc <name>。 */
    fun selectByName(name: ByteArray): ApduResponse =
        send(ApduCommand(0x00, 0xA4, 0x04, 0x00, data = name))

    /** 按文件标识选择：00 A4 00 00 02 <id>。 */
    fun selectById(id: ByteArray): ApduResponse =
        send(ApduCommand(0x00, 0xA4, 0x00, 0x00, data = id))

    /** 读二进制文件：00 B0 (SFI|0x80) 00 00。 */
    fun readBinary(sfi: Int): ApduResponse =
        send(ApduCommand(0x00, 0xB0, sfi or 0x80, 0x00, le = 0))

    /** 读指定记录：00 B2 <record> (SFI<<3)|4 00。 */
    fun readRecord(sfi: Int, record: Int): ApduResponse =
        send(ApduCommand(0x00, 0xB2, record, (sfi shl 3) or 0x04, le = 0))

    /** 尝试一次读出全部记录（部分卡支持 P1=0）。 */
    fun readRecordAll(sfi: Int): ApduResponse = readRecord(sfi, 0)

    /** 查询余额：80 5C 00 <type> 04。type: 01 透支限额 / 02 已透支 / 03 余额（普通电子钱包用 02）。 */
    fun getBalance(type: Int = 0x02): ApduResponse =
        send(ApduCommand(0x80, 0x5C, 0x00, type, le = 4))

    fun close() = transport.close()
}
