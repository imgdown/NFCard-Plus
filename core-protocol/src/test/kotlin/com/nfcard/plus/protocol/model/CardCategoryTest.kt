package com.nfcard.plus.protocol.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CardCategoryTest {

    @Test
    fun `all current kinds belong to transit category`() {
        CardKind.entries.forEach { kind ->
            assertEquals(CardCategory.TRANSIT, kind.category)
        }
    }

    @Test
    fun `transit category has readable label`() {
        assertEquals("公共交通卡", CardCategory.TRANSIT.label)
    }
}
