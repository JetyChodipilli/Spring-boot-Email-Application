package com.brainserve.oysmail.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Validate URLs with a parser rather than a permissive regular expression. */
public final class SecureLinkValidator {
    private SecureLinkValidator() { }

    public static String httpsUrl(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 2048 || !value.equals(value.trim())
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " must be a valid HTTPS URL.");
        }
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || !host.contains(".")
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || !(uri.getPort() == -1 || uri.getPort() == 443)
                    || host.startsWith(".") || host.endsWith(".")
                    || !host.matches("(?i)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")) {
                throw new IllegalArgumentException(label + " must be a valid HTTPS URL.");
            }
            // Avoid invalid port/authority forms, IPv6 host tricks and Unicode control in links.
            return value;
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + " must be a valid HTTPS URL.");
        }
    }
}
