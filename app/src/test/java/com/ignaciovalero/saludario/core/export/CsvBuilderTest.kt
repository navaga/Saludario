package com.ignaciovalero.saludario.core.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvBuilderTest {

    @Test
    fun `row with simple values uses comma separator and CRLF terminator`() {
        val line = CsvBuilder.row("a", "b", "c")
        assertEquals("a,b,c\r\n", line)
    }

    @Test
    fun `null values become empty fields`() {
        val line = CsvBuilder.row("a", null, "c")
        assertEquals("a,,c\r\n", line)
    }

    @Test
    fun `value with comma is wrapped in quotes`() {
        val line = CsvBuilder.row("a,b", "c")
        assertEquals("\"a,b\",c\r\n", line)
    }

    @Test
    fun `value with double quote is escaped by doubling`() {
        val line = CsvBuilder.row("she said \"hi\"", "ok")
        assertEquals("\"she said \"\"hi\"\"\",ok\r\n", line)
    }

    @Test
    fun `value with newline is wrapped in quotes`() {
        val line = CsvBuilder.row("line1\nline2", "ok")
        assertEquals("\"line1\nline2\",ok\r\n", line)
    }

    @Test
    fun `value with carriage return is wrapped in quotes`() {
        val line = CsvBuilder.row("line1\rline2")
        assertEquals("\"line1\rline2\"\r\n", line)
    }

    @Test
    fun `numeric values are stringified`() {
        val line = CsvBuilder.row(1, 2.5, true)
        assertEquals("1,2.5,true\r\n", line)
    }

    @Test
    fun `list overload behaves like vararg`() {
        val line = CsvBuilder.row(listOf("a", "b,c", null))
        assertEquals("a,\"b,c\",\r\n", line)
    }
}
