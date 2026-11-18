package com.nfcard.plus.protocol

/** 卡片传输抽象：Android 侧由 IsoDep 实现，测试侧由模拟卡实现。 */
interface CardTransport {
    fun transceive(command: ByteArray): ByteArray
    fun close()
}
