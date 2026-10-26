package com.brainserve.oysmail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/** Fails startup rather than silently sending passwords through downgraded/plaintext SMTP. */
@Component
public class MailTransportGuard {
    private final String host;
    private final boolean auth;
    private final boolean starttls;
    private final boolean allowLocalMailpit;
    private final String username;
    private final String password;

    public MailTransportGuard(
            @Value("${spring.mail.host:localhost}") String host,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean auth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean starttls,
            @Value("${app.mail.allow-local-mailpit:false}") boolean allowLocalMailpit,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        this.host = host;
        this.auth = auth;
        this.starttls = starttls;
        this.allowLocalMailpit = allowLocalMailpit;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void validate() {
        boolean loopback = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
        boolean localMailpit = "mailpit".equalsIgnoreCase(host) && allowLocalMailpit;
        if (loopback || localMailpit) {
            if (auth && !starttls) throw new IllegalStateException(
                    "SMTP_AUTH cannot be used without TLS, even for local test SMTP.");
            return;
        }
        if (!auth || !starttls || username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Remote SMTP requires SMTP_AUTH=true, SMTP_STARTTLS=true, "
                    + "SMTP_USERNAME, and SMTP_PASSWORD. STARTTLS is required (not optional).");
        }
    }
}
