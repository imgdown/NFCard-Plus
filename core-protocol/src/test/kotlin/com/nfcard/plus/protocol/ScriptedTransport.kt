package com.nfcard.plus.protocol

/** 测试用脚本化模拟卡：按命令前缀匹配返回预设响应，并记录全部收到的命令。 */
class ScriptedTransport(private val handler: (ByteArray) -> ByteArray) : CardTransport {
    val received = mutableListOf<ByteArray>()
    var closed = false

    override fun transceive(command: ByteArray): ByteArray {
        received.add(command.copyOf())
        return handler(command)
    }

    override fun close() {
        closed = true
    }

    companion object {
        val OK: ByteArray = byteArrayOf(0x90.toByte(), 0x00)
        val NOT_FOUND: ByteArray = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        fun ok(data: ByteArray): ByteArray = data + OK

        /** 按命令 hex 前缀路由的模拟卡；未匹配返回 6A82。 */
        fun routing(routes: Map<String, ByteArray>): ScriptedTransport =
            ScriptedTransport { cmd ->
                val hex = Bytes.hex(cmd)
                routes.entries.firstOrNull { hex.startsWith(it.key) }?.value ?: NOT_FOUND
            }
    }
}
