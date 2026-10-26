package com.brainserve.oysmail.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureLinkValidatorTest {
    @Test void acceptsHttpsResources() {
        assertThat(SecureLinkValidator.httpsUrl("https://brainserve.in/resources?x=1&y=2", "URL"))
                .isEqualTo("https://brainserve.in/resources?x=1&y=2");
    }
    @Test void rejectsUnsafeOrMalformedUrls() {
        for (String url : new String[]{"javascript:alert(1)", "http://brainserve.in",
                "https://", "https://user@brainserve.in", "https://brainserve.in:3000",
                "https://brainserve.in#frag", "https://brainserve.in/\r\nHeader: x"}) {
            assertThatThrownBy(() -> SecureLinkValidator.httpsUrl(url, "URL"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
