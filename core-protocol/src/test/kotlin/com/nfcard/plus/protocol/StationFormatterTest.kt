package com.nfcard.plus.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationFormatterTest {

    @Test
    fun `formats line and station number with city name`() {
        assertEquals("（郑州）3号线37号", StationFormatter.format("0100000000000337", "4910"))
        assertEquals("（郑州）5号线21号", StationFormatter.format("0100000000000521", "4910"))
    }

    @Test
    fun `unknown city falls back to code`() {
        assertEquals("（9999）3号线37号", StationFormatter.format("0100000000000337", "9999"))
    }

    @Test
    fun `all zero station returns null`() {
        assertNull(StationFormatter.format("0000000000000000", "4910"))
    }

    @Test
    fun `too short code returns null`() {
        assertNull(StationFormatter.format("033", "4910"))
    }

    @Test
    fun `zero line still shows station number`() {
        assertEquals("（郑州）37号", StationFormatter.format("0100000000000037", "4910"))
    }
}
