package com.brainserve.oysmail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Logs SMTP endpoint and transport mode without exposing passwords or message contents. */
@Configuration
public class SmtpStartupDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(SmtpStartupDiagnostics.class);

    @Bean
    ApplicationRunner smtpSettingsSummary(
            @Value("${spring.mail.host:localhost}") String host,
            @Value("${spring.mail.port:1025}") int port,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean auth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean starttls) {
        return args -> log.info("SMTP destination: {}:{}; SMTP_AUTH={}; SMTP_STARTTLS={}. "
                + "Only local test Mailpit captures messages without external delivery.",
                host, port, auth, starttls);
    }
}
