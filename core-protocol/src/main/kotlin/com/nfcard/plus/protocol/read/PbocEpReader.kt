package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.CardReader
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.EpInfo
import com.nfcard.plus.protocol.model.TxnRecord
import com.nfcard.plus.protocol.parse.PbocParser

/** 标准 PBOC 电子钱包读取器，亦作为各城市卡读取器的基类。 */
open class PbocEpReader(
    override val kind: CardKind = CardKind.PBOC_EP,
    private val displayName: String = kind.label,
) : CardReader {

    protected val mf: ByteArray = Bytes.fromHex("3F00")
    protected val epDf: ByteArray = Bytes.fromHex("1001")
    protected val pse: ByteArray = "1PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)
    protected val ppse: ByteArray = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    /** 尽力而为的复位：真实卡片可能拒绝全部复位指令，不能作为读取前提。 */
    protected open fun resetTag(tag: Iso7816Tag) {
        if (tag.selectById(mf).isOk) return
        if (tag.selectByName(pse).isOk) return
        tag.selectByName(ppse)
    }

    protected open fun selectApp(tag: Iso7816Tag): Boolean = tag.selectById(epDf).isOk

    override fun read(tag: Iso7816Tag): CardApp? {
        resetTag(tag)
        if (!selectApp(tag)) return null
        val info = tag.readBinary(0x15).let { if (it.isOk) PbocParser.epInfo(it.data) else null }
        val balance = tag.getBalance(0x02).let { if (it.isOk) PbocParser.balance(it.data) else null }
        return baseApp(balance, info, readTransactions(tag))
    }

    /** 读交易明细：优先一次读全部，失败则逐条读 1..10。 */
    protected fun readTransactions(tag: Iso7816Tag, sfi: Int = 0x18): List<TxnRecord> {
        val all = tag.readRecordAll(sfi)
        val records = if (all.isOk) {
            PbocParser.splitRecords(all.data, 23)
        } else {
            val list = mutableListOf<ByteArray>()
            for (i in 1..10) {
                val r = tag.readRecord(sfi, i)
                if (!r.isOk || r.data.size < 23) break
                list.addAll(PbocParser.splitRecords(r.data, 23))
            }
            list
        }
        return records.mapNotNull { PbocParser.txnRecord(it) }
    }

    protected fun baseApp(balanceCents: Long?, info: EpInfo?, transactions: List<TxnRecord>): CardApp =
        CardApp(
            kind = kind,
            displayName = displayName,
            balanceCents = balanceCents,
            serial = info?.serial,
            version = info?.version,
            validFrom = info?.validFrom,
            validTo = info?.validTo,
            transactions = transactions,
        )
}
