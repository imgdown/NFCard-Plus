package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.EtcPassage
import com.nfcard.plus.protocol.parse.EtcParser
import com.nfcard.plus.protocol.parse.PbocParser

/**
 * ETC 用户卡读取器（联网收费应用目录 1001）。
 * 以发行文件 0015 长度（43/50 字节）区别于公交电子钱包（30 字节）。
 * 交易记录文件 0018 受 PIN 保护：不尝试 PIN 校验（避免旧标准卡 PIN 锁卡风险），读不到则跳过。
 */
class EtcReader : PbocEpReader(CardKind.ETC) {

    override fun read(tag: Iso7816Tag): CardApp? {
        if (!tag.selectById(mf).isOk) return null
        val holderRsp = tag.readBinary(0x16)
        val holder = if (holderRsp.isOk) EtcParser.holder(holderRsp.data) else null

        if (!tag.selectById(epDf).isOk) return null
        val issRsp = tag.readBinary(0x15)
        if (!issRsp.isOk || issRsp.data.size < 43) return null
        val issuance = EtcParser.issuance(issRsp.data) ?: return null

        val balance = tag.getBalance(0x02).let { if (it.isOk) PbocParser.balance(it.data) else null }

        val passages = mutableListOf<EtcPassage>()
        for (i in 1..10) {
            val r = tag.readRecord(0x19, i)
            if (!r.isOk) break
            EtcParser.passageRecord(r.data)?.let { passages.add(it) }
        }

        val etc = issuance.copy(
            holderName = holder?.first,
            holderIdNumber = holder?.second,
            holderIdType = holder?.third,
            passages = passages,
        )
        return CardApp(
            kind = kind,
            displayName = kind.label,
            balanceCents = balance,
            serial = issuance.cardNo,
            etc = etc,
            transactions = readTransactions(tag, 0x18),
        )
    }
}
