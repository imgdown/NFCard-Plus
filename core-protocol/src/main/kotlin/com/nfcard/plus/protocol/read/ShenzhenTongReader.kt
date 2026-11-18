package com.nfcard.plus.protocol.read

import com.nfcard.plus.protocol.model.CardKind

/** 深圳通读取器（标准 PBOC 电子钱包布局）。 */
class ShenzhenTongReader : PbocEpReader(CardKind.SHENZHEN)
