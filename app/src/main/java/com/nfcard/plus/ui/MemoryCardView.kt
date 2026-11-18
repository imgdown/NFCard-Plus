package com.nfcard.plus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nfcard.plus.protocol.Bytes
import com.nfcard.plus.protocol.model.MemoryCardInfo
import com.nfcard.plus.protocol.parse.MemoryCardAnalyzer

@Composable
fun MemoryCardView(info: MemoryCardInfo) {
    val clipboard = LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (info.ticket != null) "地铁票卡（存储型）" else "存储型票卡",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "此卡为非 ISO-DEP 存储卡（常见于地铁单程票、计时畅游票等）。",
                style = MaterialTheme.typography.bodySmall,
            )
            InfoLine("卡片技术", info.techs.joinToString(" / "))
            InfoLine("UID", info.uid)

            info.ticket?.let { ticket ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                ticket.cityName?.let { InfoLine("发卡城市", it) }
                    ?: ticket.cityCode?.let { InfoLine("城市代码", it) }
                if (ticket.valueBlocks.isNotEmpty()) {
                    Text("值块（余额/计数）", style = MaterialTheme.typography.titleSmall)
                    ticket.valueBlocks.forEach { v ->
                        InfoLine("块 ${v.blockIndex}", v.value.toString())
                    }
                }
                if (ticket.rideRecords.isNotEmpty()) {
                    Text(
                        "乘车记录（${ticket.rideRecords.size} 条）",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ticket.rideRecords.forEachIndexed { i, r ->
                        Text(
                            "${i + 1}. ${Bytes.hex(r)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        "记录的时间/站点编码校准中，可复制原始数据反馈给开发者。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            val dumpText = when {
                info.blocks.isNotEmpty() -> MemoryCardAnalyzer.formatBlockDump(info.blocks)
                info.pages.isNotEmpty() -> MemoryCardAnalyzer.formatDump(info.pages)
                else -> null
            }
            if (dumpText != null) {
                val expandedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("原始数据", style = MaterialTheme.typography.titleSmall)
                    Row {
                        TextButton(onClick = { expandedState.value = !expandedState.value }) {
                            Text(if (expandedState.value) "收起" else "展开")
                        }
                        TextButton(onClick = {
                            val text = buildString {
                                append("技术: ${info.techs.joinToString("/")}\n")
                                append("UID: ${info.uid}\n")
                                append(dumpText)
                            }
                            clipboard.setText(AnnotatedString(text))
                        }) {
                            Text("复制")
                        }
                    }
                }
                if (expandedState.value) {
                    Text(
                        dumpText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            info.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
