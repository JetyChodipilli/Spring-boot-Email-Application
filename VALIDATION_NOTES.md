# Validation notes (no-login edition)

- All login-related code, UI elements, administrative password configuration and required Docker environment variables were removed.
- Session cookie and CSRF token remain intentionally (they prevent cross-site email submissions; they are not login controls).
- Project preserved: original OYS banner, 9 brand colors, custom images, social footer, attachments, live preview and SMTP interface.
- See `DEBUG_REPORT_NO_LOGIN.md` for evidence and unresolved runtime tests.
