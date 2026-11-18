package com.nfcard.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nfcard.plus.BuildConfig
import com.nfcard.plus.ReadMode
import com.nfcard.plus.ReaderUiState
import com.nfcard.plus.protocol.model.CardKind

private const val REPO_URL = "https://github.com/imgdown/NFCard-Plus"
private const val RELEASE_URL = "$REPO_URL/releases"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: ReaderUiState, nfcAvailable: Boolean, onModeChange: (ReadMode) -> Unit) {
    val tab = when (state.mode) {
        is ReadMode.Auto -> 0
        is ReadMode.Manual -> 1
    }
    // “关于”页用独立标记：保持当前读取模式不变
    var aboutSelected = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("NFCard+") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = !aboutSelected.value && tab == 0,
                    onClick = {
                        aboutSelected.value = false
                        if (state.mode !is ReadMode.Auto) onModeChange(ReadMode.Auto)
                    },
                    label = { Text("自动读取") },
                    icon = {},
                )
                NavigationBarItem(
                    selected = !aboutSelected.value && tab == 1,
                    onClick = {
                        aboutSelected.value = false
                        if (state.mode !is ReadMode.Manual) onModeChange(ReadMode.Manual(CardKind.T_UNION))
                    },
                    label = { Text("手动读取") },
                    icon = {},
                )
                NavigationBarItem(
                    selected = aboutSelected.value,
                    onClick = { aboutSelected.value = true },
                    label = { Text("关于") },
                    icon = {},
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                aboutSelected.value -> AboutContent()
                !nfcAvailable -> Text("本机不支持 NFC，无法读卡。请在具备 NFC 功能的设备上使用。")
                else -> ReadContent(state, onModeChange)
            }
        }
    }
}

@Composable
private fun ReadContent(state: ReaderUiState, onModeChange: (ReadMode) -> Unit) {
    if (state.mode is ReadMode.Manual) {
        Text("选择卡类与卡种后贴卡，将强制按所选卡种读取：", style = MaterialTheme.typography.bodyMedium)
        com.nfcard.plus.protocol.model.CardCategory.entries.forEach { category ->
            CategorySection(
                category = category,
                selected = state.mode.kind,
                onSelect = { onModeChange(ReadMode.Manual(it)) },
            )
        }
        Text(
            "银行卡、ETC 等更多卡类将在后续版本加入，按大类分组展示。",
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        Text(
            "将公交卡贴近手机背面 NFC 感应区，自动识别并读取全部应用。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "多合一卡（公交/社保/银联）会逐项读取；若自动识别不全，请到“手动读取”指定卡种。",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (state.busy) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text("正在读取，请保持卡片贴紧…", modifier = Modifier.padding(top = 8.dp))
        }
    }

    state.message?.let { msg ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(msg, modifier = Modifier.padding(12.dp))
        }
    }

    state.report?.let { CardReportView(it) }

    state.memory?.let { MemoryCardView(it) }

    if (state.trace.isNotEmpty()) {
        DiagnosticCard(state.trace)
    }
}

@Composable
private fun DiagnosticCard(trace: List<String>) {
    val expandedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("读卡诊断", style = MaterialTheme.typography.titleSmall)
                Row {
                    androidx.compose.material3.TextButton(onClick = {
                        expandedState.value = !expandedState.value
                    }) {
                        Text(if (expandedState.value) "收起" else "展开")
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(trace.joinToString("\n")))
                    }) {
                        Text("复制")
                    }
                }
            }
            if (expandedState.value) {
                Text(
                    trace.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            } else {
                Text("读卡异常时，点“复制”把诊断内容发给开发者即可定位问题。", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: com.nfcard.plus.protocol.model.CardCategory,
    selected: CardKind,
    onSelect: (CardKind) -> Unit,
) {
    val kinds = CardKind.entries.filter { it.category == category }
    val expandedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedState.value = !expandedState.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(category.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${kinds.size} 种 · ${if (expandedState.value) "收起" else "展开"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (expandedState.value) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    kinds.forEach { kind ->
                        FilterChip(
                            selected = selected == kind,
                            onClick = { onSelect(kind) },
                            label = { Text(kind.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutContent() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("NFCard+", style = MaterialTheme.typography.headlineSmall)
            Text("版本：${BuildConfig.VERSION_NAME}（构建号 ${BuildConfig.VERSION_CODE}）")
            Text("项目地址：$REPO_URL")
            Text("下载地址：$RELEASE_URL")
            Text("许可证：Apache-2.0（全新重写实现，非 GPL 衍生作品）")
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当前版本支持读取", style = MaterialTheme.typography.titleMedium)
            Text("· 交通联合卡（全国交通一卡通）：余额、卡号、有效期、发卡城市、交易明细、地铁进出站状态与时间、进出站线路站点")
            Text("· 城市互联互通卡（住建部标准）：自动识别发卡城市，覆盖上海公共交通卡、南京金陵通、西安长安通等 300 余个城市代码")
            Text("· 北京市政一卡通")
            Text("· 武汉通")
            Text("· 深圳通")
            Text("· 标准 PBOC 电子钱包公交卡")
            Text("· 存储型票卡（Mifare Classic/Ultralight，地铁单程票、计时畅游票等）：自动密钥认证、城市识别、值块与乘车记录读取")
            Text("后续版本计划：ETC 卡、银联电子现金、地方旅游票卡适配、更多城市卡种、鸿蒙 NEXT 版")
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("隐私说明", style = MaterialTheme.typography.titleMedium)
            Text("本应用仅在本地通过 NFC 读取卡片公开信息，不联网上传任何卡片数据，不收集个人信息。")
        }
    }
}
