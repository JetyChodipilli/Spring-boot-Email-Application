package com.brainserve.oysmail.service;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

/**
 * Only expose fixed, non-sensitive SMTP troubleshooting messages to the browser.
 * Never return raw SMTP exception text: it may contain addresses or credentials.
 */
public final class SmtpFailureDiagnoser {
    private SmtpFailureDiagnoser() { }

    public static String code(Throwable error) {
        if (error == null) return "SMTP_SEND_FAILED";
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        pending.add(error);
        String best = "SMTP_SEND_FAILED";
        int nodes = 0;
        while (!pending.isEmpty() && nodes++ < 30) {
            Throwable current = pending.removeFirst();
            if (!visited.add(current)) continue;
            if (current instanceof MailAuthenticationException || current instanceof AuthenticationFailedException) {
                return "SMTP_AUTHENTICATION_FAILED";
            }
            if (current instanceof SSLException) {
                best = "SMTP_TLS_FAILED";
            } else if (current instanceof UnknownHostException || current instanceof ConnectException
                    || current instanceof NoRouteToHostException || current instanceof SocketTimeoutException) {
                if (!"SMTP_TLS_FAILED".equals(best)) best = "SMTP_CONNECTION_FAILED";
            } else if (current instanceof SendFailedException) {
                if ("SMTP_SEND_FAILED".equals(best)) best = "SMTP_REJECTED";
            }
            if (current.getCause() != null) pending.addLast(current.getCause());
            if (current instanceof MessagingException m && m.getNextException() != null) {
                pending.addLast(m.getNextException());
            }
            // Spring MailSendException may contain errors only in failedMessages,
            // with no root cause. This is how a server rejection is often represented.
            if (current instanceof MailSendException m) {
                for (Exception nested : m.getFailedMessages().values()) {
                    if (nested != null) pending.addLast(nested);
                }
            }
        }
        return best;
    }

    public static String advice(String code) {
        return switch (code) {
            case "SMTP_AUTHENTICATION_FAILED" ->
                    "SMTP authentication failed. Check SMTP_USERNAME and the provider's app password; do not use your normal Gmail password.";
            case "SMTP_CONNECTION_FAILED" ->
                    "Could not reach the SMTP server. Check SMTP_HOST, SMTP_PORT, firewall, and whether Mailpit is running if using localhost:1025.";
            case "SMTP_TLS_FAILED" ->
                    "SMTP TLS negotiation failed. For Gmail use port 587 with required STARTTLS and verify the server certificate.";
            case "SMTP_REJECTED" ->
                    "The SMTP server rejected the sender, recipient, or message. Check the permitted From address and the SMTP provider's logs.";
            default ->
                    "The SMTP transfer failed. Read the new server-side stack trace to identify the cause; confirm the SMTP host, port, credentials and TLS settings.";
        };
    }
}
