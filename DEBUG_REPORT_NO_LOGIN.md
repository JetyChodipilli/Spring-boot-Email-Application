# Brainserve / OYS Email Studio — No-login debugging report

Date: October 26, 2026
Source: `Operation_Yuva_Sankalp_Email_Studio_Security_Fixes_2026-10-26.zip`

## Changes made

1. Removed all user/admin authentication services, credential validation, login throttling filter, `/logout` form, and Sign out button.
2. Removed `APP_ADMIN_USER` and `APP_ADMIN_PASSWORD` requirements from Compose, configuration, and setup instructions.
3. Left `GET /`, `GET /api/config`, and `POST /api/send` accessible without user accounts. Form login, HTTP Basic, logout, remember-me and request-cache authentication mechanisms are disabled explicitly in `SecurityConfig`.
4. Retained CSRF token requirement for `POST /api/send` and fixed UI error text so it never asks users to log in. A CSRF cookie is **not** an authentication or login session.
5. Added a Host allowlist filter that accepts only `localhost`, `127.0.0.1`, and `[::1]`, optionally with valid TCP ports. This mitigates browser DNS rebinding in the local-only setup. Responses set `Cache-Control: no-store`.
6. Added no-login integration tests for editor access, configuration access and CSRF rejection, plus host allowlist tests.
7. Preserved: all nine OYS editable HEX colors, CSS paste/reset, social media footer, original embedded OYS banner, custom banners/images, attachments, drag/drop editor, email send confirmation and SMTP service.
8. Replaced a Spring Boot 4-incompatible `TestRestTemplate` import in a new test with Java 21 `HttpClient`, avoiding dependency on an additional Boot test-client module.
9. Updated README, `.env.example`, application YAML and Docker Compose for the no-login, localhost-only workflow.

## Checks that were actually executed

| Check | Result | Detail |
|---|---|---|
| JavaScript syntax (`node --check`) | PASS | Editor JS parses successfully |
| CSS/HTML browser interaction via Chromium | PASS | Login UI absent, config response, nine color controls, instant preview, notice block, social footer, send confirmation, CSRF header + send payload, mobile preview and reset; no browser JS exceptions |
| Java source smoke (`javac` + `java`) | PASS | 27 assertions across actual `LocalHostGuard` / `SecureLinkValidator` classes |
| Image and attachment validator (`javac` with isolated framework interface stubs) | PASS | 8 assertions for PNG validity, bad image rejection, path cleaning and content type; **not a Spring integration test** |
| Original OYS banner | PASS | File SHA-256 matches the user's original PNG exactly; 1200 × 364 |
| XML / YAML structure | PASS | Maven POM XML and Compose / Spring YAML syntax parsed |
| Source scan for removed authentication code | PASS | No `APP_ADMIN`, `LoginRateLimitFilter`, login/logout endpoints or password credential setup in production source |
| ZIP integrity / project structure | PASS | All entries listed and ZIP CRC tested |

## Checks still outstanding (not represented as passed)

- **Full `mvn clean verify` build and JUnit integration suite:** Maven absent, and this environment cannot resolve Maven Central (DNS blocked). This is an important release gate.
- **Real Spring Boot HTTP test against `/api/config` and `/api/send`:** No runnable Spring backend without Maven dependencies.
- **Chromium navigation against an HTTP server:** Browser policy blocked local HTTP navigation (`net::ERR_BLOCKED_BY_ADMINISTRATOR`). Browser UI interaction test used an in-memory page and mocked API responses; therefore not the same as production integration testing.
- **Docker Compose build/startup:** Docker is not installed here.
- **Resolved dependency vulnerability scan:** No network or dependency cache for scanning fully resolved Java libraries. The source retains Spring Boot 4.1.2 and Mailpit v1.31.2; their presence does not prove the dependency tree is CVE-free.
- **Actual outgoing email over SMTP, Gmail/Outlook rendering, virus scanning of uploaded attachments, deployment penetration testing:** Not performed.

## Security limitations of intentionally removing login

A no-login mail composer must be treated as **local-only**. This version binds host-published ports to 127.0.0.1 and verifies local Host headers, but any user/process on the same machine may access the app. CSRF protection, quotas and the From allowlist are useful defense in depth; **they do not substitute for access control**. Do not publish or port-forward this service to the internet or an untrusted LAN. Configure SMTP secrets carefully; local Mailpit is the default safe test destination.

## Recommended release verification

```bash
# From the extracted project folder with Java 21 and Maven 3.9+
mvn clean verify

# On a machine with Docker Compose and network access
docker compose config
docker compose up --build -d

# Open http://localhost:8080 directly — verify NO sign-in screen.
# Test with a valid CSRF token, a custom OYS palette, real PDF attachment,
# the social footer and banner. Inspect the result at http://localhost:8025.
```

**Release classification:** Updated complete source project, code/browser smoke checked. **NOT claimed to be fully compiled, end-to-end tested or production certified** until the outstanding gates pass.
