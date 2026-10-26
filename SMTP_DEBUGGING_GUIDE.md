# SMTP send failure and no-login startup warning — fix guide

## What the supplied log proves

- Java 21 and Spring Boot 4.1.2 started successfully on HTTP port 8080.
- A single send attempt produced `0 of 1 messages returned an SMTP acceptance response`.
- The supplied log contains only application startup and DispatcherServlet initialization, **not the underlying SMTP exception**.
- Spring Boot created an unused in-memory default security user even though the app's own filter chain does not require login.

## Changes in this ZIP

1. Exclude Spring Boot 4's `UserDetailsServiceAutoConfiguration` while keeping the custom no-login Spring Security filter chain and CSRF controls.
2. Catch SMTP `MailException` only around `mailSender.send(...)`. An error while creating MIME headers/images/attachments is now a preparation error, not falsely labeled SMTP failure.
3. Log SMTP failure stack traces server-side, with no intentionally printed credentials or email body.
4. Return a safe error code and troubleshooting hint to the browser. Categories: `SMTP_AUTHENTICATION_FAILED`, `SMTP_CONNECTION_FAILED`, `SMTP_TLS_FAILED`, `SMTP_REJECTED`, `SMTP_SEND_FAILED`.
5. Detect nested mail exceptions from Spring `MailSendException.getFailedMessages()` in addition to the ordinary exception cause chain.
6. Print selected SMTP host:port, authentication and STARTTLS mode on startup, without secrets. This helps identify a missing or overridden `.env` configuration.
7. Add optional project-root `.env` import, Spring Boot 4 `spring.web.error` properties, and explicit SMTP timeouts.
8. Add regression tests for SMTP error classification, premature MIME failures, and absence of generated users.

## Actual next check

When restarting, locate the new log entry resembling:

`SMTP destination: smtp.gmail.com:587; SMTP_AUTH=true; SMTP_STARTTLS=true`

If it instead says `localhost:1025`, the app is configured for a local capture server and **will not send to Gmail**. Start Mailpit to test locally or configure Gmail SMTP correctly. The bundled `compose.yaml` explicitly forces SMTP to `mailpit:1025` and is designed for testing; `.env` cannot override those Compose hard-coded SMTP values.

Use real Gmail SMTP in a **local Java/Maven launch**, with an `.env` file in the IDE's working directory (normally the project root):

```dotenv
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_USERNAME=YOUR_GMAIL@gmail.com
SMTP_PASSWORD=YOUR_GOOGLE_APP_PASSWORD
SMTP_FROM_ADDRESS=YOUR_GMAIL@gmail.com
APP_ALLOWED_FROM=YOUR_GMAIL@gmail.com
APP_ALLOW_LOCAL_MAILPIT=false
```

- Replace placeholders with your account. The Google App Password is not the account sign-in password.
- Remove spaces from a displayed 16-character app password if needed.
- Ensure the Google account permits App Passwords and the account's sender matches the From choice in the editor.
- Do not commit `.env` to Git or upload the password when requesting help.
- Port 587 uses STARTTLS. Do not casually substitute port 465, which generally uses implicit TLS and needs a different configuration.
- If using Eclipse, set Run Configuration > Working directory to the actual project root containing `.env`.
- In PowerShell, `Test-NetConnection smtp.gmail.com -Port 587` verifies TCP connectivity only; it does **not** test Gmail authentication or delivery.
- After a failed send, look for `SMTP transmission failed (category=...)` and the nested exception in the console. Share the error code and sanitized exception type/status (never your password or tokens).

## Avoid duplicate email

Even a connection drop can occur after the SMTP server accepted a message. `0/1` means that the client has not confirmed acceptance, **not** that no recipient will receive it. Inspect SMTP provider logs/Sent mail before retrying. For multi-recipient batches the application counts only sends that returned successfully.

## Local verification steps

```bash
mvn clean verify
mvn spring-boot:run
```

Test one recipient first. Verify the email in Gmail and check that the embedded Brainserve logo, OYS banner, attachment and linked social icons render. Run an SMTP sandbox test before enabling larger sends.

### Limitations

The submitted startup log does not contain a real SMTP exception, so the original cause cannot be conclusively established. The patch adds diagnostics and fixes an incorrect exception boundary; no assertion of successful real Gmail SMTP sending is possible without actual credentials and a mail-provider test. Maven and Docker are not installed in the current execution environment, and network access to Maven Central is unavailable, so full build and integration tests remain outstanding.
