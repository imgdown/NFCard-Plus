package com.nfcard.plus.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.MemoryCardInfo
import com.nfcard.plus.protocol.parse.MemoryCardAnalyzer
import java.io.IOException

/**
 * 存储型票卡读取：技术识别 + UID + 原始数据。只读，绝不写卡。
 * 调用方负责在 IO 线程使用。
 */
object MemoryCardReader {

    private const val MAX_PAGES = 60

    /** 公开字典密钥（出厂默认与常见公共交通系统密钥），仅用于读取认证。 */
    private val KNOWN_KEYS = listOf(
        "FFFFFFFFFFFF",
        "A0A1A2A3A4A5",
        "441015110246",
        "D3F7D3F7D3F7",
        "000000000000",
    ).map { Bytes.fromHex(it) }

    fun read(tag: Tag): MemoryCardInfo {
        val techs = tag.techList.map { it.substringAfterLast('.') }
        val uid = Bytes.hex(tag.id)

        val classic = MifareClassic.get(tag)
        if (classic != null) return readClassic(classic, techs, uid)

        val ultralight = MifareUltralight.get(tag) ?: return MemoryCardInfo(
            techs = techs,
            uid = uid,
            note = "该卡类型暂不支持内容读取，仅显示基础信息",
        )
        return readUltralight(ultralight, techs, uid)
    }

    private fun readClassic(classic: MifareClassic, techs: List<String>, uid: String): MemoryCardInfo {
        val blocks = MutableList(64) { ByteArray(16) }
        var readable = 0
        try {
            classic.connect()
            val sectorCount = minOf(classic.sectorCount, 16)
            for (sector in 0 until sectorCount) {
                val authed = KNOWN_KEYS.any { key ->
                    try {
                        classic.authenticateSectorWithKeyA(sector, key)
                    } catch (e: IOException) {
                        false
                    }
                }
                if (!authed) continue
                readable++
                val base = classic.sectorToBlock(sector)
                for (b in 0 until 4) {
                    val idx = base + b
                    if (idx >= 64) break
                    blocks[idx] = try {
                        classic.readBlock(idx)
                    } catch (e: IOException) {
                        ByteArray(16)
                    }
                }
            }
        } catch (e: IOException) {
            // 连接中断时保留已读部分
        } finally {
            try {
                classic.close()
            } catch (_: IOException) {
            }
        }
        val ticket = MemoryCardAnalyzer.ticketSummary(blocks)
        return MemoryCardInfo(
            techs = techs,
            uid = uid,
            blocks = blocks,
            ticket = ticket,
            note = when {
                readable == 0 -> "未能通过密钥认证，无法读取内容"
                ticket == null -> "已读取 $readable 个扇区，未识别出票卡结构"
                else -> null
            },
        )
    }

    private fun readUltralight(ultralight: MifareUltralight, techs: List<String>, uid: String): MemoryCardInfo {
        val pages = mutableListOf<ByteArray>()
        try {
            ultralight.connect()
            var offset = 0
            while (offset < MAX_PAGES) {
                val block = try {
                    ultralight.readPages(offset)
                } catch (e: IOException) {
                    break
                }
                if (block == null || block.size < 16) break
                for (i in 0 until 4) {
                    pages.add(block.copyOfRange(i * 4, i * 4 + 4))
                }
                offset += 4
            }
        } catch (e: IOException) {
            // 连接失败时保留已读到的部分
        } finally {
            try {
                ultralight.close()
            } catch (_: IOException) {
            }
        }
        return MemoryCardInfo(
            techs = techs,
            uid = MemoryCardAnalyzer.uidFromPages(pages) ?: uid,
            pages = pages,
            note = if (pages.isEmpty()) "未能读出数据页，请重新贴卡重试" else null,
        )
    }
}
