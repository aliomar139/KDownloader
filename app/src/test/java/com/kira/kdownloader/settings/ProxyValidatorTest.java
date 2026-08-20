package com.kira.kdownloader.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProxyValidatorTest {
    @Test public void disabledProxyIsAlwaysValid() {
        assertEquals(ProxyValidator.Result.Valid, ProxyValidator.validate(ProxyType.DISABLED, "", 0, "", ""));
    }

    @Test public void emptyHostIsRejectedWhenEnabled() {
        ProxyValidator.Result result = ProxyValidator.validate(ProxyType.HTTP, "", 8080, "", "");
        assertTrue(result instanceof ProxyValidator.Result.Invalid
                && ((ProxyValidator.Result.Invalid) result).getField() == ProxyValidator.Field.HOST);
    }

    @Test public void validHostnameAndIpAreAccepted() {
        assertTrue(ProxyValidator.isValidHost("proxy.example.com"));
        assertTrue(ProxyValidator.isValidHost("192.168.1.1"));
        assertFalse(ProxyValidator.isValidHost("999.999.999.999"));
        assertFalse(ProxyValidator.isValidHost("bad host"));
    }

    @Test public void portMustBeInRange() {
        assertFalse(ProxyValidator.isValidPort(0));
        assertFalse(ProxyValidator.isValidPort(70000));
        assertTrue(ProxyValidator.isValidPort(1080));
        ProxyValidator.Result result = ProxyValidator.validate(ProxyType.SOCKS, "host", 70000, "", "");
        assertTrue(result instanceof ProxyValidator.Result.Invalid
                && ((ProxyValidator.Result.Invalid) result).getField() == ProxyValidator.Field.PORT);
    }

    @Test public void passwordWithoutUsernameIsRejected() {
        ProxyValidator.Result result = ProxyValidator.validate(ProxyType.HTTP, "host", 8080, "", "secret");
        assertTrue(result instanceof ProxyValidator.Result.Invalid
                && ((ProxyValidator.Result.Invalid) result).getField() == ProxyValidator.Field.USERNAME);
    }

    @Test public void aCompleteValidConfigPasses() {
        assertEquals(ProxyValidator.Result.Valid, ProxyValidator.validate(ProxyType.HTTP, "10.0.0.1", 3128, "user", "pass"));
    }
}
