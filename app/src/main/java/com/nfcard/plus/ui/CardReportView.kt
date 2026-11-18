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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcard.plus.protocol.CityCodes
import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardReport
import com.nfcard.plus.protocol.model.MetroStatus
import com.nfcard.plus.protocol.model.TxnRecord

/** 金额：分 → 元字符串。 */
internal fun money(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    return "$sign¥${abs / 100}.${"%02d".format(abs % 100)}"
}

/** 城市代码后附城市名（可解析时）。 */
internal fun cityLabel(code: String): String =
    CityCodes.name(code)?.let { "$code（$it）" } ?: code

@Composable
fun CardReportView(report: CardReport) {
    report.apps.forEach { app -> CardAppView(app) }
    if (report.failures.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("以下应用读取失败", style = MaterialTheme.typography.titleSmall)
                report.failures.forEach { f ->
                    Text("· ${f.kind.label}：${f.reason}", style = MaterialTheme.typography.bodySmall)
                }
                Text("可尝试在“手动读取”中单独指定该卡种重试。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CardAppView(app: CardApp) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(app.displayName, style = MaterialTheme.typography.titleLarge)
            app.balanceCents?.let {
                Text("余额 ${money(it)}", style = MaterialTheme.typography.headlineSmall)
            }
            app.overdrawUsedCents?.let { used ->
                if (used > 0) {
                    val limit = app.overdrawLimitCents
                    Text("已透支 ${money(used)}" + (limit?.let { "（限额 ${money(it)}）" } ?: ""))
                }
            }
            app.serial?.let { InfoRow("卡号", it) }
            app.cityName?.let { InfoRow("发卡城市", it) }
                ?: app.cityCode?.let { InfoRow("城市代码", cityLabel(it)) }
            app.version?.let { InfoRow("应用版本", it) }
            if (app.validFrom != null && app.validTo != null) {
                InfoRow("有效期", "${app.validFrom} 至 ${app.validTo}")
            }

            app.metro?.let { metro ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("地铁进出站", style = MaterialTheme.typography.titleMedium)
                val statusText = when (metro.status) {
                    MetroStatus.ENTERED_NOT_EXITED -> "已进站，未出站"
                    MetroStatus.EXITED -> "已出站"
                    MetroStatus.NO_RECORD -> "无进出站记录"
                }
                InfoRow("当前状态", statusText)
                metro.entryTime?.let { InfoRow("进站时间", it) }
                metro.exitTime?.let { InfoRow("出站时间", it) }
                val entryLabel = com.nfcard.plus.protocol.StationFormatter.format(metro.entryStation, metro.entryCityCode)
                if (entryLabel != null) {
                    InfoRow("进站站点", entryLabel)
                } else if (metro.entryStation.any { c -> c != '0' }) {
                    InfoRow("进站站点代码", metro.entryStation)
                }
                val exitLabel = com.nfcard.plus.protocol.StationFormatter.format(metro.exitStation, metro.exitCityCode)
                if (exitLabel != null) {
                    InfoRow("出站站点", exitLabel)
                } else if (metro.exitStation.any { c -> c != '0' }) {
                    InfoRow("出站站点代码", metro.exitStation)
                }
                if (metro.maxFareCents > 0) InfoRow("本次票价上限", money(metro.maxFareCents))
                Text(
                    "状态依据卡内进出闸时间推导；票价上限为本次行程最高扣费，实际扣费以交易明细为准（可能含当地优惠）。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (app.transactions.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("交易明细（最近 ${app.transactions.size} 笔）", style = MaterialTheme.typography.titleMedium)
                app.transactions.forEach { txn -> TxnRow(txn) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TxnRow(txn: TxnRecord) {
    val isEntry = txn.amountCents == 0L && txn.typeCode == 0x09
    val typeText = when {
        isEntry -> "复合消费（进站）"
        txn.typeCode == 0x06 -> "消费"
        txn.typeCode == 0x09 -> "复合消费"
        txn.typeCode == 0x02 -> "圈存"
        else -> "交易(类型%02X)".format(txn.typeCode)
    }
    val amountText = if (isEntry) "¥0.00" else (if (txn.isDebit) "-" else "+") + money(txn.amountCents)
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("${txn.date} ${txn.time}  $typeText  $amountText", style = MaterialTheme.typography.bodyMedium)
        Text("序号 ${txn.seq} · 终端 ${txn.terminalId}", style = MaterialTheme.typography.bodySmall)
    }
}
