<div align="center">
  <img src="src/main/resources/static/images/brainserve-logo.png" alt="Brainserve logo" width="300" />

  # Brainserve Email Studio

  **Design. Personalize. Preview. Send.**

  A visual, locally hosted email composer for **Operation Yuva Sankalp**, built with **Java 21** and **Spring Boot 4.1.2**.
  Create branded, responsive HTML emails without hand-coding email templates.

  ![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square)
  ![Spring Boot 4.1.2](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square)
  ![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square)
  ![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square)
  ![Access](https://img.shields.io/badge/Access-Local--only-0D2A52?style=flat-square)

  **[Features](#-features) · [Quick Start](#-quick-start) · [Gmail Setup](#-send-real-emails-with-gmail) · [Troubleshooting](#-troubleshooting) · [Security](#-security--limitations)**
</div>

> [!IMPORTANT]
> **No login required — designed for use on your own computer.** The interface can send email using configured SMTP credentials, so do not expose it to the internet or an untrusted network. The bundled Docker setup uses **Mailpit**, a local test inbox; it does **not** send real emails.

## 📸 Preview

<p align="center">
  <img src="docs/images/email-studio-preview.jpg" alt="Brainserve Email Studio composer with branding, theme editor, and live HTML email preview" width="960" />
</p>

*Live composer and email preview. Screenshot included in the companion README assets; place `docs/images/email-studio-preview.jpg` in your repository.*

## ✨ Features

| Capability | What it does |
| --- | --- |
| **Visual composer** | Set **To**, approved **From**, sender name, optional **Reply-To**, and **Subject**. |
| **Dynamic content** | Create and reorder text, notices, buttons, dividers, and image blocks. |
| **Live preview** | Review desktop and mobile layouts before sending. |
| **Brainserve branding** | Embed the Brainserve logo above the original Operation Yuva Sankalp banner. |
| **Banner controls** | Keep the default banner, upload a replacement, or hide the banner. |
| **Editable theme** | Manually edit nine brand colors using HEX values or paste a CSS `:root` palette. |
| **Social footer** | Add clickable **Website, LinkedIn, Instagram, Facebook, YouTube, and X/Twitter** icons and links. |
| **Attachments** | Include PDFs and other documents, plus additional inline images. |
| **Private recipient delivery** | Send a separate MIME message to each recipient instead of exposing the entire recipient list. |
| **SMTP integration** | Test locally with Mailpit or configure authenticated SMTP for actual sending. |
| **HTML + plain text** | Send multipart email with CID-embedded images and a plain-text alternative. |
| **No-login local interface** | Open the editor directly without an account or password. |

### 🎨 Operation Yuva Sankalp color palette

All nine colors are editable in the composer, and the selected values are applied to outgoing email styling.

| Token | Default | Role |
| --- | --- | --- |
| `--primary-orange` | `#F97316` | Primary actions and highlights |
| `--dark-orange` | `#EA580C` | Stronger orange accents |
| `--dark-navy` | `#0D2A52` | Headings and brand text |
| `--background` | `#F9FAFB` | Outer email background |
| `--white` | `#FFFFFF` | Content cards and surfaces |
| `--text-primary` | `#1F2937` | Main text |
| `--text-secondary` | `#4B5563` | Supporting text |
| `--border-color` | `#E5E7EB` | Separators and borders |
| `--notification-red` | `#EF4444` | Notice styling |

## 🧰 Tech stack

- **Backend:** Java 21, Spring Boot 4.1.2, Spring MVC, Spring Mail/Jakarta Mail, Spring Security
- **Frontend:** HTML5, CSS3, vanilla JavaScript
- **Build:** Apache Maven
- **Local email testing:** Mailpit
- **Containerization:** Docker and Docker Compose
- **Storage:** No application database; email drafts are held in browser memory and uploads are processed in memory

## 🚀 Quick start

### Option A — Docker + Mailpit (recommended for testing)

**Prerequisites:** Docker Desktop with Docker Compose.

```bash
docker compose up --build -d
```

| Service | Local URL | Purpose |
| --- | --- | --- |
| Email Studio | http://localhost:8080 | Compose and preview emails |
| Mailpit inbox | http://localhost:8025 | Inspect captured test emails |

Send a message from the editor, then inspect it in Mailpit. **This mode does not deliver to Gmail or other real inboxes.** Stop the stack with:

```bash
docker compose down
```

> Docker Compose intentionally overrides `SMTP_HOST` with `mailpit` and port `1025`. Editing `.env` alone does **not** switch the bundled Compose application to Gmail.

### Option B — Run with Maven

**Prerequisites:** JDK **21**, Maven **3.9+**, and an SMTP server (Mailpit for testing, or a configured email provider).

```bash
mvn clean verify
mvn spring-boot:run
```

Then open **http://localhost:8080**. Direct runs bind to `127.0.0.1` by default.

For local testing without the entire Docker stack, start only Mailpit using `docker compose up -d mailpit` and keep these settings in your `.env` (or equivalent environment):

```dotenv
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false
SMTP_FROM_ADDRESS=oys@localhost.test
APP_ALLOW_LOCAL_MAILPIT=false
```

## ✉️ Send real emails with Gmail

For **direct Maven/Java launches**, create `.env` next to `pom.xml` (you can start from `.env.example`):

```dotenv
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8080
SESSION_COOKIE_SECURE=false

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_USERNAME=yourname@gmail.com
SMTP_PASSWORD=your_google_app_password

SMTP_FROM_ADDRESS=yourname@gmail.com
APP_ALLOWED_FROM=yourname@gmail.com
APP_ALLOW_LOCAL_MAILPIT=false
```

Replace placeholders with an account you control and a valid **Google App Password** (where your account supports App Passwords). This is **not** your regular Google account password. Never commit `.env` or paste credentials into an issue or screenshot.

Run from the **same directory containing `.env`**:

```bash
mvn spring-boot:run
```

Check the startup log:

```text
SMTP destination: smtp.gmail.com:587; SMTP_AUTH=true; SMTP_STARTTLS=true
```

If the application still reports `localhost:1025`, check your working directory and any environment variables or IDE run configuration overriding `.env`.

> [!NOTE]
> Remote SMTP requires authentication, STARTTLS, a nonempty username/password, and certificate-hostname verification. The `MailTransportGuard` intentionally prevents startup when required remote-SMTP settings are absent. Gmail SMTP access and successful inbox delivery must be tested using your own account.

## 🧑‍🎨 How to use the email composer

1. Open the local editor and enter up to **50 unique recipients**.
2. Choose an authorized sender address, enter the subject, and optionally set a display name and Reply-To address.
3. Add or rearrange content blocks; optionally upload a replacement banner and other images.
4. Update the nine OYS colors and add the HTTPS destinations for your social icons.
5. Attach study material or other relevant files.
6. Check the live preview, then confirm the send operation.
7. For a Mailpit test, inspect the full rendered email, social links, inline images, and attachments at `localhost:8025`.

### Upload limits

| Upload type | Limit |
| --- | --- |
| Additional inline images | Up to 8 |
| Attachments | Up to 6 |
| Individual banner/image | 5 MB; PNG or JPEG |
| Individual attachment | 10 MB |
| Combined multipart HTTP request | 35 MB |

Individual images are additionally validated for dimensions and basic image integrity. Recipient sends are limited to **200 attempts per hour per running application instance**.

## 🗂️ Project structure

```text
oys-email-studio/
├── .env.example
├── .gitignore
├── compose.yaml
├── Dockerfile
├── pom.xml
├── README.md
├── docs/
│   └── images/
│       └── email-studio-preview.jpg
├── scripts/
│   └── security_verify.sh
└── src/
    ├── main/
    │   ├── java/com/brainserve/oysmail/
    │   │   ├── config/          # Host filter, CSRF, SMTP guard and diagnostics
    │   │   ├── controller/      # Configuration and send endpoints
    │   │   ├── model/           # Email blocks, theme, requests, social links
    │   │   └── service/         # MIME builder, sending, uploads, link validation
    │   └── resources/
    │       ├── application.yml
    │       └── static/
    │           ├── index.html
    │           ├── app.js
    │           ├── styles.css
    │           └── images/      # Brainserve logo, OYS banner, social icons
    └── test/java/              # Security, theme, mail and validation tests
```

## 🔌 Application endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/` | Email editor |
| `GET` | `/api/config` | Allowed sender addresses and CSRF token |
| `POST` | `/api/send` | Validated multipart email submission (CSRF required) |

The browser obtains the CSRF token from `/api/config` and includes it in the send request. There are no account-registration or sign-in endpoints in this project.

## 🧪 Tests and validation

Run the tests before publishing changes:

```bash
mvn clean verify
```

For additional checks on Git Bash, WSL, Linux, or macOS:

```bash
bash scripts/security_verify.sh
```

The script runs Maven verification, displays the dependency tree, and invokes Trivy/Docker checks **when those tools are installed**. Automated source tests are included for email assembly, recipient privacy, CSRF/no-login behavior, theme and URL validation, uploads, and SMTP diagnostics.

**Verification status:** Project source and configuration have had targeted checks. A clean full Maven build, Docker launch, provider SMTP delivery, dependency scan, and cross-client email-rendering tests should be run in your own environment before treating a deployment as validated.

## 🛡️ Security & limitations

- **Local-only, no authentication:** The app is intentionally not designed for public hosting. Other local users/processes may be able to use its send endpoint.
- **Host and request controls:** Localhost binding, Host validation, CSRF tokens, restrictive browser headers, and per-instance send quotas provide additional safeguards but do **not** replace authentication.
- **Mail transport:** Remote SMTP must use authentication and required STARTTLS. Keep SMTP credentials in local environment/secret management, not Git.
- **Untrusted input:** Social/button links and inline HTML content are validated/escaped; sender addresses are allowlisted. Uploaded attachments are not antivirus-scanned.
- **Email-client differences:** Some inboxes block CID inline images until the recipient allows them. Linked text and the plain-text alternative remain available.
- **No draft persistence:** Reloading or closing the editor can discard unsaved content. There is no database, background job queue, or delivery-tracking dashboard.
- **Acceptance vs. delivery:** A successful SMTP response means the mail server accepted the message, **not** that it reached the recipient's inbox. Investigate partial send failures before retrying to avoid duplicates.

## 🔧 Troubleshooting

<details>
<summary><strong>SMTP connection refused at localhost:1025</strong></summary>

You're pointing at **Mailpit**, but nothing is listening on that port. Start Mailpit (`docker compose up -d mailpit`) or switch your **direct Java/Maven** launch to an actual SMTP provider. The bundled full Compose stack is deliberately Mailpit-only.
</details>

<details>
<summary><strong>MailTransportGuard: Remote SMTP requires SMTP_AUTH, STARTTLS, username, and password</strong></summary>

Confirm all four `.env` settings are present, your App Password is nonempty, and the app is launched with the correct project-root working directory. Check whether OS/IDE environment variables override the `.env` file. Do **not** delete the guard or disable TLS.
</details>

<details>
<summary><strong>SMTP authentication, TLS, or delivery errors</strong></summary>

Review the startup line `SMTP destination: ...` and the server-side `SMTP transmission failed (category=...)` exception. On Windows, `Test-NetConnection smtp.gmail.com -Port 587` checks TCP connectivity only; it does not validate Gmail credentials or guarantee delivery. Avoid repeatedly resending an uncertain batch.
</details>

<details>
<summary><strong>Request rejected or CSRF error</strong></summary>

Refresh the editor to obtain a new CSRF token. Keep the local session cookie enabled and open the same `localhost:8080` origin as the application.
</details>

<details>
<summary><strong>Images or icons do not show in a recipient's email</strong></summary>

Check the received email in Mailpit and in the target email client. Inline logo, banner, and social icons are sent using Content-ID (CID) image attachments. Some inboxes require recipients to allow images; social-link labels are also clickable.
</details>

## 🤝 Contributing

Improvements to accessibility, email-client compatibility, tests, and developer experience are welcome. Before proposing changes, run `mvn clean verify`, avoid committing credentials, and describe how you tested the email output.

## 📜 License & branding

No open-source license is included with the current project files. Add an explicit `LICENSE` file before granting reuse permissions. **Brainserve** and **Operation Yuva Sankalp** names, logos, and artwork should be used only with appropriate authorization.

---

<div align="center">
  <strong>Brainserve Email Studio</strong><br />
  <sub>Professional branded communication for Operation Yuva Sankalp.</sub>
</div>
