package com.ignaciovalero.saludario.core.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalParsingTest {

    // ────────── parseDecimalOrNull ──────────

    @Test
    fun `parseDecimalOrNull returns null for empty string`() {
        assertNull("".parseDecimalOrNull())
    }

    @Test
    fun `parseDecimalOrNull returns null for blank string`() {
        assertNull("   ".parseDecimalOrNull())
    }

    @Test
    fun `parseDecimalOrNull returns null for non numeric input`() {
        assertNull("abc".parseDecimalOrNull())
        assertNull("1,2,3".parseDecimalOrNull())
        assertNull("1..2".parseDecimalOrNull())
    }

    @Test
    fun `parseDecimalOrNull accepts dot separator`() {
        assertEquals(1.5, "1.5".parseDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `parseDecimalOrNull accepts comma separator`() {
        assertEquals(2.75, "2,75".parseDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `parseDecimalOrNull trims surrounding whitespace`() {
        assertEquals(3.14, "  3,14  ".parseDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `parseDecimalOrNull parses integer values`() {
        assertEquals(10.0, "10".parseDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `parseDecimalOrNull parses zero`() {
        assertEquals(0.0, "0".parseDecimalOrNull()!!, 0.0)
        assertEquals(0.0, "0,0".parseDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `parseDecimalOrNull parses negative numbers`() {
        // No es un caso esperado en la app pero el helper no debe sesgar el
        // contrato; quien lo llame es responsable de validar el rango.
        assertEquals(-1.5, "-1,5".parseDecimalOrNull()!!, 0.0)
    }

    // ────────── parseDecimalOrZero ──────────

    @Test
    fun `parseDecimalOrZero falls back to zero when blank`() {
        assertEquals(0.0, "".parseDecimalOrZero(), 0.0)
        assertEquals(0.0, "   ".parseDecimalOrZero(), 0.0)
    }

    @Test
    fun `parseDecimalOrZero falls back to zero when non numeric`() {
        assertEquals(0.0, "abc".parseDecimalOrZero(), 0.0)
    }

    @Test
    fun `parseDecimalOrZero returns parsed value when valid`() {
        assertEquals(4.5, "4,5".parseDecimalOrZero(), 0.0)
        assertEquals(4.5, "4.5".parseDecimalOrZero(), 0.0)
    }
}
