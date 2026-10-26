package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.SendEmailCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.Multipart;


import org.springframework.mail.MailSendException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.when;

class EmailServiceTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailService service = new EmailService(sender, new ObjectMapper(),
            new UploadValidator(), new EmailHtmlBuilder(), "oys@localhost.test", "");

    private SendEmailCommand cmd(String from, String blocks) {
        return new SendEmailCommand("student@example.com", from, "Operation Yuva Sankalp", null,
                "Learning material", blocks, "default", "#f45127", "Connect with Brainserve", "[]", null, null, null);
    }

    @Test
    void constructsMultipartMessageWithInlineBanner() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        int count = service.send(cmd("oys@localhost.test",
                "[{\"type\":\"text\",\"heading\":\"Welcome\",\"text\":\"Your PDF is available.\"}]"));
        assertThat(count).isEqualTo(1);
        ArgumentCaptor<MimeMessage> messageCapture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(messageCapture.capture());
        MimeMessage sent = messageCapture.getValue();
        assertThat(sent.getSubject()).isEqualTo("Learning material");
        assertThat(sent.getAllRecipients()).hasSize(1);
        assertThat(sent.getFrom()[0].toString()).contains("oys@localhost.test");
        Multipart mixed = (Multipart) sent.getContent();
        assertThat(mixed.getCount()).isGreaterThan(0);
    }

    @Test
    void sendsSeparateMessagesWithoutExposingOtherRecipients() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        SendEmailCommand two = new SendEmailCommand("one@example.com,two@example.com", "oys@localhost.test",
                "Brainserve", null, "Resources", "[{\"type\":\"text\",\"text\":\"Hello\"}]",
                "none", "#f45127", "Connect with Brainserve", "[]", null, null, null);
        assertThat(service.send(two)).isEqualTo(2);
        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(2)).send(capture.capture());
        assertThat(capture.getAllValues()).hasSize(2);
        assertThat(capture.getAllValues().get(0).getAllRecipients()).hasSize(1);
        assertThat(capture.getAllValues().get(1).getAllRecipients()).hasSize(1);
        assertThat(capture.getAllValues().get(0).getAllRecipients()[0].toString()).isEqualTo("one@example.com");
        assertThat(capture.getAllValues().get(1).getAllRecipients()[0].toString()).isEqualTo("two@example.com");
    }

    @Test
    void mimePreparationFailureIsNotMisreportedAsSmtpSendAttempt() {
        when(sender.createMimeMessage()).thenThrow(new MailSendException("Cannot create MIME message"));
        assertThatThrownBy(() -> service.send(cmd("oys@localhost.test",
                "[{\"type\":\"text\",\"text\":\"Hello\"}]")))
                .isInstanceOf(MailSendException.class)
                .isNotInstanceOf(PartialSendException.class);
    }

    @Test
    void reportsHowManyRecipientsWereAcceptedBeforeSmtpFailure() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 2) throw new MailSendException("SMTP unavailable");
            return null;
        }).when(sender).send(any(MimeMessage.class));
        SendEmailCommand two = new SendEmailCommand("one@example.com,two@example.com", "oys@localhost.test",
                "Brainserve", null, "Resources", "[{\"type\":\"text\",\"text\":\"Hello\"}]",
                "none", "#f45127", "Connect with Brainserve", "[]", null, null, null);
        assertThatThrownBy(() -> service.send(two))
                .isInstanceOf(PartialSendException.class)
                .satisfies(ex -> {
                    PartialSendException failure = (PartialSendException) ex;
                    assertThat(failure.accepted()).isEqualTo(1);
                    assertThat(failure.total()).isEqualTo(2);
                });
    }

    @Test
    void rejectsImageCountMismatch() {
        var invalid = cmd("oys@localhost.test", "[{\"type\":\"image\",\"imageIndex\":0}]");
        assertThatThrownBy(() -> service.send(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void rejectsForgedFromAddress() {
        assertThatThrownBy(() -> service.send(cmd("attacker@example.com",
                "[{\"type\":\"text\",\"text\":\"Hello\"}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SMTP-authorized sender");
    }

    @Test
    void rejectsWrongSocialDomainAndJavascriptLinks() {
        SendEmailCommand forged = withSocial("[{\"platform\":\"linkedin\",\"url\":\"https://linkedin.com.evil.example/user\"}]");
        assertThatThrownBy(() -> service.send(forged))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linkedin");
        SendEmailCommand script = withSocial("[{\"platform\":\"instagram\",\"url\":\"javascript:alert(1)\"}]");
        assertThatThrownBy(() -> service.send(script))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsRepeatedFooterPlatforms() {
        SendEmailCommand duplicate = withSocial("[{\"platform\":\"x\",\"url\":\"https://x.com/brainserve\"},"
                + "{\"platform\":\"x\",\"url\":\"https://twitter.com/brainserve\"}]");
        assertThatThrownBy(() -> service.send(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only once");
    }

    @Test
    void sendsWithValidSocialFooter() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        assertThat(service.send(withSocial("[{\"platform\":\"youtube\",\"url\":\"https://www.youtube.com/@brainserve\"}]")))
                .isEqualTo(1);
        verify(sender, times(1)).send(any(MimeMessage.class));
    }

    private SendEmailCommand withSocial(String json) {
        return new SendEmailCommand("student@example.com", "oys@localhost.test", "Brainserve", null,
                "Resources", "[{\"type\":\"text\",\"text\":\"Hello\"}]",
                "none", "#f45127", "Connect with Brainserve", json, null, null, null);
    }

    @Test
    void rejectsInvalidButtonLinks() {
        assertThatThrownBy(() -> service.send(cmd("oys@localhost.test",
                "[{\"type\":\"button\",\"label\":\"Click\",\"url\":\"javascript:alert(1)\"}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsMalformedHttpsButtonAuthorities() {
        for (String invalid : new String[]{"https://", "https://user@example.com/path",
                "https://example.com:8080/path", "https://example.com#fragment"}) {
            String body = "[{\"type\":\"button\",\"label\":\"Go\",\"url\":\"" + invalid + "\"}]";
            assertThatThrownBy(() -> service.send(cmd("oys@localhost.test", body)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HTTPS");
        }
    }

    @Test
    void limitsHourlyRecipientAttempts() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        String recipients = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> "student" + i + "@example.com")
                .collect(java.util.stream.Collectors.joining(","));
        SendEmailCommand batch = new SendEmailCommand(recipients, "oys@localhost.test", "Brainserve", null,
                "Resources", "[{\"type\":\"text\",\"text\":\"Hello\"}]",
                "none", "#F97316", "Connect with Brainserve", "[]", null, null, null);
        for (int i = 0; i < 4; i++) assertThat(service.send(batch)).isEqualTo(50);
        assertThatThrownBy(() -> service.send(batch))
                .isInstanceOf(SendThrottleException.class).hasMessageContaining("Hourly");
        verify(sender, times(200)).send(any(MimeMessage.class));
    }


    @Test
    void sendsLogoAndClickableSocialIconsAsInlineCidMimeParts() throws Exception {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        SendEmailCommand withIcons = withSocial("[{\"platform\":\"linkedin\",\"url\":\"https://www.linkedin.com/company/brainserve\"},"
                + "{\"platform\":\"youtube\",\"url\":\"https://www.youtube.com/@brainserve\"}]");
        assertThat(service.send(withIcons)).isEqualTo(1);
        ArgumentCaptor<MimeMessage> cap = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(cap.capture());
        StringBuilder html = new StringBuilder();
        java.util.Set<String> cids = new java.util.HashSet<>();
        walkPart(cap.getValue(), html, cids);
        assertThat(cids).contains("<brainserve-logo>", "<social-linkedin>", "<social-youtube>");
        assertThat(cids).doesNotContain("<social-facebook>", "<social-instagram>");
        assertThat(html.toString()).contains("cid:brainserve-logo", "cid:social-linkedin", "cid:social-youtube",
                "href=\"https://www.linkedin.com/company/brainserve\"");
    }

    private static void walkPart(jakarta.mail.Part part, StringBuilder html, java.util.Set<String> cids)
            throws Exception {
        String[] contentId = part.getHeader("Content-ID");
        if (contentId != null) cids.addAll(java.util.Arrays.asList(contentId));
        if (part.isMimeType("text/html")) {
            html.append(part.getContent().toString());
        } else if (part.isMimeType("multipart/*")) {
            jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                walkPart(multipart.getBodyPart(i), html, cids);
            }
        }
    }
}
