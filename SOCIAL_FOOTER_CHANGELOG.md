# Social footer update

- Added a dedicated editable social-media footer to the email composer.
- Supported platforms: Website, LinkedIn, Instagram, Facebook, YouTube, X/Twitter.
- Configurable heading, enable/disable toggle, and live desktop/mobile preview.
- Outgoing MIME HTML and plain-text alternative now include only populated links.
- Email-safe HTML tables, inline CSS, accessible text link labels; no remote icons.
- Server validates up to six links, one per platform, with HTTPS and matching social domains.
- Security: HTML escaping, credential-bearing URL rejection, and length limits.
- Added JUnit regression tests for rendering, malformed URLs, duplicate platforms and a valid request.
- No existing SMTP, banner, attachment or security configuration should need changes.

## Verification boundaries

Run `mvn clean verify` and perform a real SMTP/Mailpit send in your environment before production rollout. The source changes are not proof of inbox rendering in every mail client.
