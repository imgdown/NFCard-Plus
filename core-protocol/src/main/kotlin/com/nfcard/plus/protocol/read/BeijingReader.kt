package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.parse.PbocParser

/** 北京市政一卡通读取器（MF 下 0x04/0x05 文件 + EP 应用）。 */
class BeijingReader : PbocEpReader(CardKind.BEIJING) {

    override fun read(tag: Iso7816Tag): CardApp? {
        if (!tag.selectById(mf).isOk) return null
        val infoRsp = tag.readBinary(0x04)
        if (!infoRsp.isOk || infoRsp.data.size < 32) return null
        tag.readBinary(0x05)
        if (!tag.selectById(epDf).isOk) return null

        val balance = tag.getBalance(0x02).let { if (it.isOk) PbocParser.balance(it.data) else null }
        val d = infoRsp.data
        return CardApp(
            kind = kind,
            displayName = kind.label,
            balanceCents = balance,
            serial = Bytes.hex(d, 0, 8),
            version = "%02X.%02X%02X".format(d[8], d[9], d[10]),
            validFrom = PbocParser.formatDate(Bytes.bcd(d, 24, 4)),
            validTo = PbocParser.formatDate(Bytes.bcd(d, 28, 4)),
            transactions = readTransactions(tag, 0x18),
        )
    }
}
