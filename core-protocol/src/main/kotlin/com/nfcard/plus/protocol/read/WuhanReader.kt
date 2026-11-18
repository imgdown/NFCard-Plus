package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.parse.PbocParser

/** 武汉通读取器（MF 下 0x05/0x0A 文件 + AP1.WHCTC 应用）。 */
class WuhanReader : PbocEpReader(CardKind.WUHAN) {

    private val aid = "AP1.WHCTC".toByteArray(Charsets.US_ASCII)

    override fun read(tag: Iso7816Tag): CardApp? {
        if (!tag.selectById(mf).isOk) return null
        val serialRsp = tag.readBinary(0x0A)
        if (!serialRsp.isOk || serialRsp.data.size < 5) return null
        val infoRsp = tag.readBinary(0x05)
        if (!infoRsp.isOk || infoRsp.data.size < 27) return null

        var balanceRsp = tag.getBalance(0x02)
        if (!tag.selectByName(aid).isOk) return null
        if (!balanceRsp.isOk) balanceRsp = tag.getBalance(0x02)
        val balance = if (balanceRsp.isOk) PbocParser.balance(balanceRsp.data) else null

        val d = infoRsp.data
        return CardApp(
            kind = kind,
            displayName = kind.label,
            balanceCents = balance,
            serial = Bytes.hex(serialRsp.data, 0, 5),
            version = (d[24].toInt() and 0xFF).toString(),
            validFrom = PbocParser.formatDate(Bytes.bcd(d, 20, 4)),
            validTo = PbocParser.formatDate(Bytes.bcd(d, 16, 4)),
            transactions = readTransactions(tag, 0x18),
        )
    }
}
