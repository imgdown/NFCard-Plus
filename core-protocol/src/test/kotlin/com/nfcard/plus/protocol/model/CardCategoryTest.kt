package com.nfcard.plus.protocol.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CardCategoryTest {

    @Test
    fun `transit kinds belong to transit category`() {
        listOf(
            CardKind.T_UNION, CardKind.CITY_UNION, CardKind.BEIJING,
            CardKind.WUHAN, CardKind.SHENZHEN, CardKind.PBOC_EP, CardKind.MEMORY_CARD,
        ).forEach { kind -> assertEquals(CardCategory.TRANSIT, kind.category) }
    }

    @Test
    fun `etc kind belongs to etc category`() {
        assertEquals(CardCategory.ETC, CardKind.ETC.category)
    }

    @Test
    fun `categories have readable labels`() {
        assertEquals("公共交通卡", CardCategory.TRANSIT.label)
        assertEquals("ETC卡", CardCategory.ETC.label)
    }
}
