package com.nfcard.plus.protocol

import com.nfcard.plus.protocol.model.CardApp
import com.nfcard.plus.protocol.model.CardKind

/** 单个卡种读取器。read 内部自行完成复位与应用选择。 */
interface CardReader {
    val kind: CardKind

    /** 非本卡种返回 null；通信异常抛出 IOException。 */
    fun read(tag: Iso7816Tag): CardApp?
}
