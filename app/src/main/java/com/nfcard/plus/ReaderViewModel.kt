package com.nfcard.plus

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcard.plus.nfc.IsoDepTransport
import com.nfcard.plus.protocol.TracingTransport
import com.nfcard.plus.protocol.model.CardKind
import com.nfcard.plus.protocol.model.CardReport
import com.nfcard.plus.protocol.read.AutoReader
import com.nfcard.plus.protocol.read.ManualReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 读取模式：自动识别，或手动指定卡种。 */
sealed interface ReadMode {
    data object Auto : ReadMode
    data class Manual(val kind: CardKind) : ReadMode
}

data class ReaderUiState(
    val mode: ReadMode = ReadMode.Auto,
    val busy: Boolean = false,
    val report: CardReport? = null,
    val message: String? = null,
    val trace: List<String> = emptyList(),
)

class ReaderViewModel : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state

    fun setMode(mode: ReadMode) {
        _state.value = ReaderUiState(mode = mode)
    }

    fun onTagDiscovered(tag: Tag) {
        if (_state.value.busy) return
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            _state.value = _state.value.copy(message = "该卡片不支持 ISO-DEP 协议，无法读取")
            return
        }
        val mode = _state.value.mode
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            val isoTransport = try {
                IsoDepTransport(isoDep)
            } catch (e: Exception) {
                null
            }
            if (isoTransport == null) {
                _state.value = _state.value.copy(
                    busy = false,
                    message = "连接卡片失败，请重新贴卡",
                )
                return@launch
            }
            val tracing = TracingTransport(isoTransport)
            val result = try {
                when (mode) {
                    is ReadMode.Auto -> AutoReader.read(tracing)
                    is ReadMode.Manual -> ManualReader.read(tracing, mode.kind)
                }
            } catch (e: Exception) {
                null
            } finally {
                tracing.close()
            }
            val trace = tracing.entries.toList()
            _state.value = when {
                result == null -> _state.value.copy(
                    busy = false,
                    trace = trace,
                    message = "读取失败：卡片移开过快或通信中断，请重新贴卡",
                )
                result.apps.isEmpty() -> _state.value.copy(
                    busy = false,
                    report = result,
                    trace = trace,
                    message = if (mode is ReadMode.Manual) {
                        "未识别到所选卡种。可展开下方“读卡诊断”复制内容反馈给开发者排查。"
                    } else {
                        "未识别到支持的卡片应用。可在“手动读取”指定卡种重试；若仍失败，复制下方“读卡诊断”反馈给开发者。"
                    },
                )
                else -> _state.value.copy(busy = false, report = result, trace = trace, message = null)
            }
        }
    }
}
