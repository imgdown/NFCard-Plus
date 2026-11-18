package com.nfcard.plus.protocol

/** 诊断用传输包装：记录 APDU 命令/响应十六进制轨迹，供读卡失败时排查。仅本地保存。 */
class TracingTransport(
    private val delegate: CardTransport,
    private val maxEntries: Int = 300,
) : CardTransport {

    val entries: MutableList<String> = mutableListOf()

    override fun transceive(command: ByteArray): ByteArray {
        val response = delegate.transceive(command)
        if (entries.size < maxEntries) {
            entries.add("→ ${Bytes.hex(command)}\n← ${Bytes.hex(response)}")
        }
        return response
    }

    override fun close() = delegate.close()
}
