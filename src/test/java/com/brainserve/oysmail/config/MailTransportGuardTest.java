package com.brainserve.oysmail.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailTransportGuardTest {
    @Test void allowsExplicitLocalMailpitWithoutAuth() {
        new MailTransportGuard("mailpit", false, false, true, "", "").validate();
    }
    @Test void rejectsRemotePlaintextOrMissingCredentials() {
        assertThatThrownBy(() -> new MailTransportGuard("smtp.example.com", true, false,
                false, "sender", "pass").validate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MailTransportGuard("smtp.example.com", true, true,
                false, "", "").validate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new MailTransportGuard("mailpit", false, false,
                false, "", "").validate()).isInstanceOf(IllegalStateException.class);
    }
    @Test void acceptsAuthenticatedRemoteStartTls() {
        new MailTransportGuard("smtp.example.com", true, true, false, "sender", "pass").validate();
    }
}
