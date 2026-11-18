package com.nfcard.plus.nfc

import android.nfc.tech.IsoDep
import com.nfcard.plus.protocol.CardTransport
import java.io.IOException

/** IsoDep 传输实现：所有 APDU 经此收发。调用方负责在 IO 线程使用。 */
class IsoDepTransport(private val isoDep: IsoDep) : CardTransport {

    init {
        isoDep.timeout = 3000
        if (!isoDep.isConnected) isoDep.connect()
    }

    @Synchronized
    override fun transceive(command: ByteArray): ByteArray = isoDep.transceive(command)

    override fun close() {
        try {
            isoDep.close()
        } catch (_: IOException) {
            // 卡片已移开时关闭失败可忽略
        }
    }
}
