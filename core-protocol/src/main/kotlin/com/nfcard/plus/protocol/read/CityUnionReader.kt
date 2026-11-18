package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.parse.PbocParser

/** 城市互联互通（住建部标准）电子钱包读取器。 */
class CityUnionReader : PbocEpReader(CardKind.CITY_UNION) {

    private val aid = Bytes.fromHex("A00000000386980701")

    override fun selectApp(tag: Iso7816Tag): Boolean = tag.selectByName(aid).isOk

    override fun read(tag: Iso7816Tag): CardApp? {
        resetTag(tag)
        if (!selectApp(tag)) return null

        val infoRsp = tag.readBinary(0x15)
        val info = if (infoRsp.isOk) PbocParser.epInfo(infoRsp.data) else null
        val cityCode = if (infoRsp.isOk && infoRsp.data.size >= 4) Bytes.hex(infoRsp.data, 2, 2) else null
        val cityName = when (cityCode) {
            "2000" -> "上海"
            "7100" -> "西安（长安通）"
            else -> null
        }
        val balance = tag.getBalance(0x02).let { if (it.isOk) PbocParser.balance(it.data) else null }
        return baseApp(balance, info, readTransactions(tag)).copy(
            cityCode = cityCode,
            cityName = cityName,
        )
    }
}
