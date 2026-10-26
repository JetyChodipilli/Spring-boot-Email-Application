# 2026-10-26 — SMTP troubleshooting patch validation

## Verified directly in source

- Spring Boot startup logs supplied by user show successful app startup, not an SMTP root cause.
- Correct Spring Boot 4 `UserDetailsServiceAutoConfiguration` class name checked against the official 4.1.2 API.
- Unnecessary generated-user auto-configuration excluded; custom `SecurityFilterChain` and CSRF remain.
- `EmailService` now distinguishes MIME preparation from the SMTP send call.
- Errors are categorized with fixed safe user-facing messages; nested SMTP causes and `MailSendException.getFailedMessages()` are examined.
- Gmail/SMTP connection settings are logged with password excluded.
- Added unit tests in `SmtpFailureDiagnoserTest`, `EmailServiceTest`, `NoLoginSecurityTest`.

## Checks executed

- Java 21 `javac` compilation of the SMTP categorizer with isolated API stubs: PASS.
- 11 Java runtime SMTP diagnostic assertions including nested exceptions: PASS.
- Python/PyYAML parse of `application.yml`: PASS.
- Configuration structure and expected Spring Boot 4 error properties: PASS.
- Byte-for-byte integrity of the original user-provided OYS banner: PASS.
- ZIP integrity, file presence, secret-file exclusion: PASS after packaging.

## Still outstanding

- Full Maven `clean verify`, Spring Boot startup, JUnit execution, Docker Compose operation, real Gmail SMTP and mailbox render tests.
- Vulnerability scan using a resolved dependency tree. Only a source review was done for this patch.

## Conclusion

This fixes the misleading error-reporting path and generated security user, and makes the missing SMTP root cause discoverable. It does **not** prove that the user's SMTP credentials, network connection or sender authorization is working.
