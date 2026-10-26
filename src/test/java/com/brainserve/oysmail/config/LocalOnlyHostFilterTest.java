package com.brainserve.oysmail.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalOnlyHostFilterTest {
    @Test void allowsOnlyLocalHostnames() {
        for (String host : new String[]{"localhost", "localhost:8080", "127.0.0.1", "127.0.0.1:8080", "[::1]", "[::1]:8080"}) {
            assertTrue(LocalHostGuard.isAllowedHost(host), host);
        }
    }
    @Test void rejectsDnsRebindingAndMalformedHosts() {
        for (String host : new String[]{"evil.example", "evil.example:8080", "sub.localhost", "127.0.0.1.evil.example", "0.0.0.0", "localhost@evil.example", "localhost:abc", "localhost:0", "localhost:65536", "localhost:80:90", "[::2]", "[::1]evil", "", "localhost\r\nInjected: header"}) {
            assertFalse(LocalHostGuard.isAllowedHost(host), host);
        }
        assertFalse(LocalHostGuard.isAllowedHost(null));
    }
}
