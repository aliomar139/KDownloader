package com.kira.kdownloader.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyValidatorTest {

    @Test
    fun `disabled proxy is always valid`() {
        val result = ProxyValidator.validate(ProxyType.DISABLED, "", 0, "", "")
        assertEquals(ProxyValidator.Result.Valid, result)
    }

    @Test
    fun `empty host is rejected when enabled`() {
        val result = ProxyValidator.validate(ProxyType.HTTP, "", 8080, "", "")
        assertTrue(result is ProxyValidator.Result.Invalid &&
            (result as ProxyValidator.Result.Invalid).field == ProxyValidator.Field.HOST)
    }

    @Test
    fun `valid hostname and ip are accepted`() {
        assertTrue(ProxyValidator.isValidHost("proxy.example.com"))
        assertTrue(ProxyValidator.isValidHost("192.168.1.1"))
        assertFalse(ProxyValidator.isValidHost("999.999.999.999"))
        assertFalse(ProxyValidator.isValidHost("bad host"))
    }

    @Test
    fun `port must be in range`() {
        assertFalse(ProxyValidator.isValidPort(0))
        assertFalse(ProxyValidator.isValidPort(70000))
        assertTrue(ProxyValidator.isValidPort(1080))
        val result = ProxyValidator.validate(ProxyType.SOCKS, "host", 70000, "", "")
        assertTrue(result is ProxyValidator.Result.Invalid &&
            (result as ProxyValidator.Result.Invalid).field == ProxyValidator.Field.PORT)
    }

    @Test
    fun `password without username is rejected`() {
        val result = ProxyValidator.validate(ProxyType.HTTP, "host", 8080, "", "secret")
        assertTrue(result is ProxyValidator.Result.Invalid &&
            (result as ProxyValidator.Result.Invalid).field == ProxyValidator.Field.USERNAME)
    }

    @Test
    fun `a complete valid config passes`() {
        val result = ProxyValidator.validate(ProxyType.HTTP, "10.0.0.1", 3128, "user", "pass")
        assertEquals(ProxyValidator.Result.Valid, result)
    }
}
