package com.nfcard.plus.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CityCodesTest {

    @Test
    fun `known codes resolve to city names`() {
        assertEquals("北京", CityCodes.name("1000"))
        assertEquals("上海", CityCodes.name("2900"))
        assertEquals("郑州", CityCodes.name("4910"))
        assertEquals("广州", CityCodes.name("5810"))
        assertEquals("深圳", CityCodes.name("5840"))
        assertEquals("武汉", CityCodes.name("5210"))
    }

    @Test
    fun `unknown code returns null`() {
        assertNull(CityCodes.name("9999"))
    }
}
