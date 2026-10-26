package com.brainserve.oysmail.config;

import java.util.Locale;

/** Local host allowlist for an unauthenticated development mail server. */
public final class LocalHostGuard {
    private LocalHostGuard() { }

    public static boolean isAllowedHost(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 100
                || raw.indexOf('\r') >= 0 || raw.indexOf('\n') >= 0) return false;
        String host = raw.toLowerCase(Locale.ROOT);
        if (host.startsWith("[::1]")) {
            return host.equals("[::1]") || validPort(host.substring(5));
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            if (host.indexOf(':', colon + 1) >= 0 || !validPort(host.substring(colon))) return false;
            host = host.substring(0, colon);
        }
        return host.equals("localhost") || host.equals("127.0.0.1");
    }

    private static boolean validPort(String suffix) {
        if (!suffix.matches(":\\d{1,5}")) return false;
        int port = Integer.parseInt(suffix.substring(1));
        return port >= 1 && port <= 65535;
    }
}
