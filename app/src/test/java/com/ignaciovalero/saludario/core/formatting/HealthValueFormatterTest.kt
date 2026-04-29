package com.ignaciovalero.saludario.core.formatting

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class HealthValueFormatterTest {

    // ────────── formatHealthValuePlain ──────────

    @Test
    fun `plain returns integer string when value has no fractional part`() {
        assertEquals("120", 120.0.formatHealthValuePlain())
        assertEquals("0", 0.0.formatHealthValuePlain())
        assertEquals("-5", (-5.0).formatHealthValuePlain())
    }

    @Test
    fun `plain returns full toString for non integer values`() {
        assertEquals("12.5", 12.5.formatHealthValuePlain())
        assertEquals("36.789", 36.789.formatHealthValuePlain())
    }

    // ────────── formatHealthValueCompact ──────────

    @Test
    fun `compact returns integer string when value has no fractional part`() {
        assertEquals("120", 120.0.formatHealthValueCompact(Locale.US))
        assertEquals("0", 0.0.formatHealthValueCompact(Locale.US))
    }

    @Test
    fun `compact rounds to one decimal in US locale`() {
        assertEquals("12.5", 12.5.formatHealthValueCompact(Locale.US))
        assertEquals("36.8", 36.789.formatHealthValueCompact(Locale.US))
    }

    @Test
    fun `compact uses comma in Spanish locale`() {
        assertEquals("12,5", 12.5.formatHealthValueCompact(Locale.forLanguageTag("es-ES")))
    }
}
