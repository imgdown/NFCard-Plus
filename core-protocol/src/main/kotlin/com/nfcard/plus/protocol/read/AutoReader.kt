package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.CardReader
import com.nfcard.plus.protocol.CardTransport
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.CardReport
import com.nfcard.plus.protocol.model.ReadFailure

/** 自动识别：依次尝试全部读取器，多合一卡收集所有应用，单个失败不影响其他。 */
object AutoReader {

    internal fun readers(): List<CardReader> = listOf(
        BeijingReader(),
        WuhanReader(),
        CityUnionReader(),
        TUnionReader(),
        ShenzhenTongReader(),
        PbocEpReader(),
    )

    fun read(transport: CardTransport): CardReport = runReaders(Iso7816Tag(transport), readers())

    internal fun runReaders(tag: Iso7816Tag, readers: List<CardReader>): CardReport {
        val apps = mutableListOf<CardApp>()
        val failures = mutableListOf<ReadFailure>()
        for (reader in readers) {
            try {
                val app = reader.read(tag) ?: continue
                // 同一底层应用可能被多个读取器命中（如深圳通与标准电子钱包），按卡号去重
                if (app.serial != null && apps.any { it.serial == app.serial }) continue
                apps.add(app)
            } catch (e: Exception) {
                failures.add(ReadFailure(reader.kind, e.message ?: e.javaClass.simpleName))
            }
        }
        return CardReport(apps, failures)
    }
}

/** 手动指定卡种读取。 */
object ManualReader {

    fun read(transport: CardTransport, kind: CardKind): CardReport {
        // 存储型票卡不走 ISO-DEP 读取器，由应用层专用通道处理
        val reader = AutoReader.readers().firstOrNull { it.kind == kind }
            ?: return CardReport(emptyList())
        return AutoReader.runReaders(Iso7816Tag(transport), listOf(reader))
    }
}
