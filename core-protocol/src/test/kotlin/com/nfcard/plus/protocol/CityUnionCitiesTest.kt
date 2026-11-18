package com.nfcard.plus.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CityUnionCitiesTest {

    @Test
    fun `resolves major city codes`() {
        assertEquals("南京", CityUnionCities.city("2100"))
        assertEquals("苏州", CityUnionCities.city("2150"))
        assertEquals("无锡", CityUnionCities.city("2140"))
        assertEquals("天津", CityUnionCities.city("3000"))
        assertEquals("广州", CityUnionCities.city("5100"))
        assertEquals("杭州", CityUnionCities.city("3100"))
    }

    @Test
    fun `known brands resolve`() {
        assertEquals("金陵通", CityUnionCities.brand("2100"))
        assertEquals("长安通", CityUnionCities.brand("7100"))
        assertEquals("上海公共交通卡", CityUnionCities.brand("2000"))
    }

    @Test
    fun `unknown code returns null`() {
        assertNull(CityUnionCities.city("9999"))
        assertNull(CityUnionCities.brand("9999"))
    }
}
