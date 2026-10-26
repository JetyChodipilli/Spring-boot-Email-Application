package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.EmailTheme;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailThemeTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String VALID = """
        {"primaryOrange":"#102030","darkOrange":"#EA580C","darkNavy":"#0D2A52",
        "background":"#F9FAFB","white":"#FFFFFF","textPrimary":"#1F2937",
        "textSecondary":"#4B5563","borderColor":"#E5E7EB","notificationRed":"#EF4444"}
        """;

    @Test void defaultsMatchOfficialOysPalette() {
        EmailTheme t = EmailTheme.defaults();
        assertThat(t.primaryOrange()).isEqualTo("#F97316");
        assertThat(t.darkOrange()).isEqualTo("#EA580C");
        assertThat(t.darkNavy()).isEqualTo("#0D2A52");
        assertThat(t.background()).isEqualTo("#F9FAFB");
        assertThat(t.white()).isEqualTo("#FFFFFF");
        assertThat(t.textPrimary()).isEqualTo("#1F2937");
        assertThat(t.textSecondary()).isEqualTo("#4B5563");
        assertThat(t.borderColor()).isEqualTo("#E5E7EB");
        assertThat(t.notificationRed()).isEqualTo("#EF4444");
    }

    @Test void parsesAllNineColorsAndUppercasesHex() {
        EmailTheme t = EmailTheme.fromJson(mapper, VALID.replace("#102030", "#abcdef"), "#abcdef");
        assertThat(t.primaryOrange()).isEqualTo("#ABCDEF");
        assertThat(t.notificationRed()).isEqualTo("#EF4444");
    }

    @Test void legacyAccentRemainsSupported() {
        assertThat(EmailTheme.fromJson(mapper, null, "#AA0044").primaryOrange()).isEqualTo("#AA0044");
    }

    @Test void rejectsMissingOrMalformedHex() {
        assertThatThrownBy(() -> EmailTheme.fromJson(mapper, VALID.replace("\"darkNavy\":\"#0D2A52\",", ""), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmailTheme.fromJson(mapper, VALID.replace("#102030", "red"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmailTheme.fromJson(mapper, VALID.replace("#102030", "#112233;position:fixed"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
