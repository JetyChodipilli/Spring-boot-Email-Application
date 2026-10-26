package com.brainserve.oysmail.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Validated OYS palette. Only #RRGGBB values can reach inline email CSS. */
public record EmailTheme(
        String primaryOrange, String darkOrange, String darkNavy,
        String background, String white, String textPrimary, String textSecondary,
        String borderColor, String notificationRed) {

    public EmailTheme {
        validate("Primary orange", primaryOrange);
        validate("Dark orange", darkOrange);
        validate("Dark navy", darkNavy);
        validate("Background", background);
        validate("White / card background", white);
        validate("Primary text", textPrimary);
        validate("Secondary text", textSecondary);
        validate("Border", borderColor);
        validate("Notification red", notificationRed);
    }

    public static EmailTheme defaults() {
        return new EmailTheme("#F97316", "#EA580C", "#0D2A52", "#F9FAFB", "#FFFFFF",
                "#1F2937", "#4B5563", "#E5E7EB", "#EF4444");
    }

    public EmailTheme withPrimaryOrange(String color) {
        return new EmailTheme(color, darkOrange, darkNavy, background, white,
                textPrimary, textSecondary, borderColor, notificationRed);
    }

    public static EmailTheme fromJson(ObjectMapper mapper, String json, String legacyAccent) {
        if (json == null || json.isBlank()) {
            return legacyAccent == null ? defaults() : defaults().withPrimaryOrange(legacyAccent);
        }
        if (json.length() > 3000) throw new IllegalArgumentException("Brand color payload is too large.");
        final JsonNode palette;
        try {
            palette = mapper.readTree(json);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Brand color data must be a JSON object.");
        }
        if (palette == null || !palette.isObject() || palette.size() != 9) {
            throw new IllegalArgumentException("Provide all 9 OYS brand colors.");
        }
        return new EmailTheme(
                color(palette, "primaryOrange"), color(palette, "darkOrange"),
                color(palette, "darkNavy"), color(palette, "background"), color(palette, "white"),
                color(palette, "textPrimary"), color(palette, "textSecondary"),
                color(palette, "borderColor"), color(palette, "notificationRed"));
    }

    private static String color(JsonNode palette, String key) {
        JsonNode node = palette.get(key);
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Missing or invalid brand color: " + key);
        }
        validate(key, node.textValue());
        return node.textValue().toUpperCase(java.util.Locale.ROOT);
    }

    private static void validate(String label, String color) {
        if (color == null || !color.matches("#[A-Fa-f0-9]{6}")) {
            throw new IllegalArgumentException(label + " must be a six-digit HEX color such as #F97316.");
        }
    }
}
