package com.brainserve.oysmail.service;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import static org.assertj.core.api.Assertions.assertThat;

class SmtpFailureDiagnoserTest {
    @Test void categorizesFailedGmailAuthentication() {
        var exception = new MailAuthenticationException(new AuthenticationFailedException("535 Auth failed"));
        assertThat(SmtpFailureDiagnoser.code(exception)).isEqualTo("SMTP_AUTHENTICATION_FAILED");
        assertThat(SmtpFailureDiagnoser.advice(SmtpFailureDiagnoser.code(exception)))
                .contains("app password").doesNotContain("535");
    }

    @Test void categorizesConnectionAndTimeout() {
        assertThat(SmtpFailureDiagnoser.code(new MailSendException("x", new ConnectException("refused"))))
                .isEqualTo("SMTP_CONNECTION_FAILED");
        assertThat(SmtpFailureDiagnoser.code(new MailSendException("x", new SocketTimeoutException("timed out"))))
                .isEqualTo("SMTP_CONNECTION_FAILED");
    }

    @Test void categorizesStarttlsAndRejections() {
        assertThat(SmtpFailureDiagnoser.code(new MailSendException("x", new SSLHandshakeException("tls"))))
                .isEqualTo("SMTP_TLS_FAILED");
        assertThat(SmtpFailureDiagnoser.code(new MailSendException("x", new SendFailedException("sender denied"))))
                .isEqualTo("SMTP_REJECTED");
    }

    @Test void diagnosesSmtpRejectionsStoredOnlyInFailedMessages() {
        var failures = java.util.Map.<Object, Exception>of(
                "recipient-1", new SendFailedException("550 sender rejected"));
        assertThat(SmtpFailureDiagnoser.code(new MailSendException(failures)))
                .isEqualTo("SMTP_REJECTED");
    }

    @Test void unknownFailureUsesGenericMessageWithoutRawExceptionText() {
        assertThat(SmtpFailureDiagnoser.code(new MailSendException("private error detail")))
                .isEqualTo("SMTP_SEND_FAILED");
        assertThat(SmtpFailureDiagnoser.advice("SMTP_SEND_FAILED"))
                .doesNotContain("private error detail");
    }
}
