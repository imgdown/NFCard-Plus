package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.CityCodes
import com.nfcard.plus.protocol.Iso7816Tag
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.MetroInfo
import com.nfcard.plus.protocol.model.ProcessRecord
import com.nfcard.plus.protocol.parse.PbocParser
import com.nfcard.plus.protocol.parse.TUnionParser

/** 交通联合（交通部标准）电子钱包读取器，含地铁进出站与过程记录。 */
class TUnionReader : PbocEpReader(CardKind.T_UNION) {

    private val aid = Bytes.fromHex("A000000632010105")

    override fun selectApp(tag: Iso7816Tag): Boolean = tag.selectByName(aid).isOk

    override fun read(tag: Iso7816Tag): CardApp? {
        resetTag(tag)
        if (!selectApp(tag)) return null

        val info = tag.readBinary(0x15).let { if (it.isOk) PbocParser.epInfo(it.data) else null }
        val balance = tag.getBalance(0x03).let { if (it.isOk) PbocParser.balance(it.data) else null }
        val overUsed = tag.getBalance(0x02).let { if (it.isOk) PbocParser.balance(it.data) else null }
        val overLimit = tag.getBalance(0x01).let { if (it.isOk) PbocParser.balance(it.data) else null }
        val mgmt = tag.readBinary(0x17).let { if (it.isOk) TUnionParser.managementInfo(it.data) else null }
        val transactions = readTransactions(tag, 0x18)
        val metro = readMetro(tag)
        val process = readProcessRecords(tag)

        return baseApp(balance, info, transactions).copy(
            overdrawUsedCents = overUsed,
            overdrawLimitCents = overLimit,
            cityCode = mgmt?.cityCode,
            cityName = mgmt?.cityCode?.let { CityCodes.name(it) },
            metro = metro,
            processRecords = process,
        )
    }

    /** 0x1A 变长记录文件：按记录内 ID 标识识别地铁记录，不依赖记录号顺序。 */
    private fun readMetro(tag: Iso7816Tag): MetroInfo? {
        for (i in 1..14) {
            val r = tag.readRecord(0x1A, i)
            if (!r.isOk) break
            TUnionParser.metroRecord(r.data)?.let { return it }
        }
        return null
    }

    private fun readProcessRecords(tag: Iso7816Tag): List<ProcessRecord> {
        val list = mutableListOf<ProcessRecord>()
        for (i in 1..10) {
            val r = tag.readRecord(0x1E, i)
            if (!r.isOk || r.data.size < 48) break
            TUnionParser.processRecord(r.data)?.let { list.add(it) }
        }
        return list
    }
}
