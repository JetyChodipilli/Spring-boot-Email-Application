# Brainserve / Operation Yuva Sankalp — Email Studio (No Login)

A complete Java 21 / Spring Boot 4.1.2 application for composing branded student emails. **No account, sign-in, registration, admin password, or logout is required.** Open the editor directly at `http://localhost:8080`.

## Included

- To (up to 50 unique recipients), verified/allowlisted From, From name, optional Reply-to, Subject
- Visual editor with text, notice, button, divider, image sections; reorder, edit, or delete
- Official Brainserve logo above the OYS banner, embedded as a CID PNG in every outgoing HTML email
- Original OYS banner embedded as a CID image; choose your own banner or hide the banner
- Up to 8 custom inline images, 6 attachments, or 35 MB total request (10 MB individual attachment, 5 MB individual image)
- All nine OYS colors editable via HEX inputs or a pasted `:root` palette; instant desktop/mobile preview
- Optional social footer: **clickable image icons** for Website, LinkedIn, Instagram, Facebook, YouTube, and X/Twitter (each PNG embedded as its own CID asset, not an external image URL); clicking icon or label opens the configured HTTPS page
- Multipart HTML + plain text, accessible email-client-friendly HTML tables, SMTP sending
- Individual email per recipient to avoid exposing other students' email addresses
- Upload and hyperlink validation, HTML escaping, CSRF protection, DNS-rebinding Host filter and quota (200 recipient attempts/hour per instance)

## Diagnosing the `0 of 1 SMTP acceptance` error

**New in this version:** The browser shows an SMTP error code and safe instructions while the Java console logs the full nested exception. On startup, look for `SMTP destination: ...` to confirm the actual host/port and TLS/auth settings; no passwords are printed. If the destination is `localhost:1025`, it is local Mailpit and does not deliver to Gmail. The generated Spring Security development password is no longer created; the existing no-login access and CSRF control are preserved.

See **`SMTP_DEBUGGING_GUIDE.md`** for a copy-paste Gmail `.env`, Eclipse working-directory settings, and retry precautions. Keep `src/main/resources/application.yml`'s Spring Boot 4 `spring.web.error` keys.

## Requirements

- Java **21**, Apache Maven 3.9+, or Docker with Docker Compose.
- Internet access **the first time Maven/Docker retrieves dependencies and container images**.

## Quick start (recommended: local Mailpit)

```bash
# Extract the source ZIP, open a terminal in oys-springboot-email-studio
# No .env or login credentials are required for the default Mailpit setup.
docker compose up --build -d
```

Open **http://localhost:8080** — the editor loads immediately, **without login**. Local email previews/inbox are at **http://localhost:8025**. This setup does **not** deliver email to real recipients: Mailpit safely captures test email.

To stop: `docker compose down`.

## Run locally without Docker (requires SMTP capture service)

Start a local Mailpit service on `127.0.0.1:1025`, then:

```bash
mvn clean verify
mvn spring-boot:run
```

Visit `http://localhost:8080`. The app binds to `127.0.0.1` when launched directly.

## SMTP configuration for sending real messages

The bundled Docker Compose configuration intentionally routes messages only to **Mailpit**. To deliver real email, run the app outside the dev Compose configuration with environment variables matching your authorized provider:

```text
SMTP_HOST=smtp.your-provider.example
SMTP_PORT=587
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_USERNAME=verified@yourdomain.example
SMTP_PASSWORD=<set through your local secret manager/environment>
SMTP_FROM_ADDRESS=verified@yourdomain.example
APP_ALLOWED_FROM=other-verified@yourdomain.example   # optional
```

`SMTP_FROM_ADDRESS` and `APP_ALLOWED_FROM` must be addresses your SMTP provider permits. The From chooser is populated from these settings; arbitrary spoofed sender addresses are rejected server-side. Remote SMTP must use authentication, required STARTTLS, and certificate hostname verification.

**IMPORTANT — no-login security model:** This is a **single-machine, local-only application**. Its REST endpoints are accessible to any local user or process, subject to CSRF and quota limits. Never expose the editor or SMTP test inbox to the public internet or an untrusted LAN. `compose.yaml` binds published ports to `127.0.0.1`; `application.yml` binds the app directly to `127.0.0.1`. The HTTP Host allowlist rejects non-local Host headers, reducing DNS-rebinding risk. Do not replace these safeguards with `0.0.0.0` hosting. For shared or public deployment, add proper authentication and authorization first.

CSRF protection still uses a short-lived session cookie; **this is not a login**. The browser fetches a CSRF token from `GET /api/config` and sends it in the request header for `POST /api/send`. Cookies are HttpOnly/SameSite Strict. You may need to refresh the page if the CSRF session expires.

## Email composer workflow

1. Open `http://localhost:8080`, no password prompt.
2. Enter recipients, choose the configured sender, enter subject and body.
3. The Brainserve logo appears automatically above the header banner; upload a different OYS banner if you like. Add PDF/doc attachments and the official social profile HTTPS URLs. Blank social fields are omitted.
4. Manually edit any OYS HEX brand color (or paste all nine as CSS variables).
5. Review desktop/mobile preview and confirm sending.
6. Open Mailpit at `http://localhost:8025` to inspect the message, inline logo, clickable social icons/links, and actual attachments.

## REST endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/` | Email editor; no login |
| GET | `/api/config` | Allowed From addresses and CSRF token |
| POST | `/api/send` | Multipart email send with CSRF token; validated fields and files |

Requests to invalid HTTP Host headers are rejected. Unsupported paths are not provided as authentication endpoints.

## Tests and checks

```bash
mvn clean verify
bash scripts/security_verify.sh
```

`src/test/` includes SMTP message assembly, one-recipient-per-message privacy, malicious URL rejection, image validation, theme validation, URL and SMTP guards, host allowlist, and no-login HTTP/CSRF integration tests. `scripts/security_verify.sh` optionally runs Trivy and Docker checks when installed.

See `LOGO_AND_SOCIAL_ICONS_VALIDATION.md` for checks of the latest logo/social-icon changes, and `DEBUG_REPORT_NO_LOGIN.md` for the earlier no-login work. A real Maven compile/test, Docker startup, full dependency audit, and SMTP integration test **must pass before production use**.

## Notes

- Browser draft is in memory: closing/reloading the tab resets unsaved content. No database or user profiles are part of the project.
- Uploads are held in memory while sending; they are not saved to a persistent file store.
- The free-form email body is structured text, not arbitrary HTML; the backend escapes user-provided strings.
- The HTML preview approximates email-client rendering. Check actual messages in Mailpit and major client apps before wide delivery.
- Do not commit `.env` or SMTP credentials. Store secrets through your environment/secret management solution.

## Brainserve logo and social icons (latest update)

- `src/main/resources/static/images/brainserve-logo.png`: extracted from the supplied reference and included **above the banner**, even when the banner is hidden.
- `src/main/resources/static/images/social/*.png`: six built-in email-compatible social graphics. No external CDN required.
- `EmailHtmlBuilder`: wraps the icon and its text label in an `<a href="https://...">` anchor. Missing URLs mean no icon is rendered. User-entered destinations are HTML-escaped.
- `EmailService`: attaches the logo and only the active footer icons to each recipient's individual MIME message as `Content-ID` PNG parts.
- The editor preview uses local PNG files. Recipient emails use `cid:brainserve-logo` and `cid:social-PLATFORM` to load images.
- The preview permits HTTPS social links to open a new tab. Outgoing links are validated server-side; official platform domains are enforced for non-website icons.

**Email-client note:** Some inboxes initially block inline images. The visible text label remains clickable, and the plain-text alternative includes every configured HTTPS URL. The recipient's mail client decides whether links open a browser or a web view.
