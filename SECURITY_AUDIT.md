# Security model — No-login local build

- **Requested behavior:** Public routes inside a local-only application; no login, no account, no password, no logout.
- **Exposure:** Bind to loopback (`127.0.0.1`) and do not expose to LAN/public internet. Any local user/process can interact with the editor. A login-free public mail service would be an unacceptable open-relay/spam risk.
- **Browser:** CSRF header token required for POST. SameSite Strict / HttpOnly cookie. Strict HTML CSP and no-referrer policy; no session authentication.
- **DNS rebinding:** Reject non-local Host headers; allows localhost, 127.0.0.1, [::1] with optional port.
- **Email sending:** From address allowlist; individual messages for each recipient; 50 recipients max per batch; 200 attempts/hour/JVM; one batch in progress per instance.
- **Files:** Multipart size limits, validated images, sanitized attachment filenames. Uploaded user attachments can still be malicious to recipients; use an enterprise virus scanner for untrusted uploads.
- **Links:** HTTPS-only button/social URLs; platform-specific host restrictions.
- **Transport:** Remote SMTP requires AUTH and STARTTLS with hostname verification. Local Mailpit test exception; never expose Mailpit unauthenticated.
- **Outstanding:** No network in validation container, therefore full Maven dependency resolution, resolved CVE scan, Spring test runner, Docker startup and real SMTP delivery are unverified. See DEBUG_REPORT_NO_LOGIN.md.
