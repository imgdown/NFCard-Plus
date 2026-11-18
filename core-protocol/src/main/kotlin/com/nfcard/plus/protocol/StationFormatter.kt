package com.nfcard.plus.protocol

/** 地铁站点编码 → 可读文本。编码末四位 BCD 为线路号(2位)+站点号(2位)，前缀为机构代码。 */
object StationFormatter {

    /** 返回如"（郑州）3号线37号"；无法解析或全零返回 null。 */
    fun format(stationCode: String, cityCode: String): String? {
        if (stationCode.length < 4 || stationCode.all { it == '0' }) return null
        val line = stationCode.substring(stationCode.length - 4, stationCode.length - 2).toIntOrNull()
            ?: return null
        val number = stationCode.substring(stationCode.length - 2).toIntOrNull() ?: return null
        if (line == 0 && number == 0) return null
        val city = CityCodes.name(cityCode) ?: cityCode
        val parts = buildString {
            if (line > 0) append("${line}号线")
            if (number > 0) append("${number}号")
        }
        return "（$city）$parts"
    }
}
