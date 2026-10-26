package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.InlineAsset;
import com.brainserve.oysmail.model.MailBlock;
import com.brainserve.oysmail.model.EmailTheme;
import com.brainserve.oysmail.model.SendEmailCommand;
import com.brainserve.oysmail.model.SocialLink;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final UploadValidator uploads;
    private final EmailHtmlBuilder htmlBuilder;
    private final String defaultFrom;
    private final Set<String> allowedFrom;
    private final Semaphore sendLock = new Semaphore(1);
    private long sendWindowStarted = System.currentTimeMillis();
    private int sendWindowAttempts;
    private static final int MAX_RECIPIENT_ATTEMPTS_PER_HOUR = 200;

    public EmailService(JavaMailSender mailSender, ObjectMapper objectMapper,
            UploadValidator uploads, EmailHtmlBuilder htmlBuilder,
            @Value("${app.mail.from:oys@localhost.test}") String defaultFrom,
            @Value("${app.mail.allowed-from:}") String extraFrom) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.uploads = uploads;
        this.htmlBuilder = htmlBuilder;
        this.defaultFrom = validateEmail(defaultFrom);
        Set<String> senders = new LinkedHashSet<>();
        senders.add(this.defaultFrom.toLowerCase(Locale.ROOT));
        if (extraFrom != null && !extraFrom.isBlank()) {
            for (String item : extraFrom.split(",")) {
                senders.add(validateEmail(item.trim()).toLowerCase(Locale.ROOT));
            }
        }
        allowedFrom = Set.copyOf(senders);
    }

    public String defaultFrom() { return defaultFrom; }
    public Set<String> allowedFrom() { return allowedFrom; }

    public int send(SendEmailCommand cmd) throws IOException, MessagingException {
        if (!sendLock.tryAcquire()) {
            throw new SendThrottleException("Another email batch is in progress. Please retry after it finishes.");
        }
        try {
            return sendSingleBatch(cmd);
        } finally {
            sendLock.release();
        }
    }

    private int sendSingleBatch(SendEmailCommand cmd) throws IOException, MessagingException {
        List<String> recipients = parseRecipients(cmd.to());
        String from = validateEmail(cmd.from());
        if (!allowedFrom.contains(from.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("From address must be an SMTP-authorized sender configured by the administrator.");
        }
        String subject = singleLine(cmd.subject(), "Subject", 180);
        String fromName = cmd.fromName() == null || cmd.fromName().isBlank()
                ? "Operation Yuva Sankalp" : singleLine(cmd.fromName(), "Sender name", 100);
        String replyTo = cmd.replyTo() == null || cmd.replyTo().isBlank()
                ? null : validateEmail(cmd.replyTo());
        List<MailBlock> blocks = parseBlocks(cmd.blocksJson());
        List<SocialLink> socialLinks = parseSocialLinks(cmd.socialLinksJson());
        String socialTitle = socialLinks.isEmpty() ? "" : singleLine(
                cmd.socialTitle() == null || cmd.socialTitle().isBlank()
                        ? "Connect with Brainserve" : cmd.socialTitle(), "Social footer title", 100);
        String heroMode = cmd.heroMode() == null ? "default" : cmd.heroMode();
        if (!Set.of("default", "custom", "none").contains(heroMode)) {
            throw new IllegalArgumentException("Unknown hero banner mode.");
        }
        if ("custom".equals(heroMode) && (cmd.heroBanner() == null || cmd.heroBanner().isEmpty())) {
            throw new IllegalArgumentException("Upload a replacement hero banner.");
        }
        EmailTheme theme = EmailTheme.fromJson(objectMapper, cmd.themeJson(), cmd.accentColor());

        List<MultipartFile> imageUploads = nonempty(cmd.inlineImages());
        List<MultipartFile> attachmentUploads = nonempty(cmd.attachments());
        long imageBlockCount = blocks.stream().filter(b -> "image".equals(b.type())).count();
        if (imageBlockCount != imageUploads.size()) {
            throw new IllegalArgumentException("Image blocks and uploaded banners do not match.");
        }
        if (imageUploads.size() > 8 || attachmentUploads.size() > 6) {
            throw new IllegalArgumentException("Limit: 8 additional banners and 6 attachments per email.");
        }
        InlineAsset hero = switch (heroMode) {
            case "default" -> defaultHero();
            case "custom" -> uploads.banner(cmd.heroBanner());
            default -> null;
        };
        InlineAsset logo = defaultBrandLogo();
        List<InlineAsset> images = new ArrayList<>();
        for (MultipartFile part : imageUploads) images.add(uploads.banner(part));
        List<InlineAsset> attachments = new ArrayList<>();
        for (MultipartFile part : attachmentUploads) attachments.add(uploads.attachment(part));

        // Count only fully validated attempts, immediately before connecting to SMTP.
        reserveSendQuota(recipients.size());

        // Do not expose student addresses to other recipients in the email's To header.
        // A separate message is prepared and sent for each recipient.
        int accepted = 0;
        for (String recipient : recipients) {
            // Compose/validate the MIME message separately from SMTP transfer.
            // A MIME preparation error is NOT an SMTP delivery attempt.
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(from, fromName);
            helper.setTo(recipient);
            if (replyTo != null) helper.setReplyTo(replyTo);
            helper.setSubject(subject);
            helper.setText(htmlBuilder.plainText(blocks, socialTitle, socialLinks),
                    htmlBuilder.render(blocks, hero != null, theme, socialTitle, socialLinks));
            // Inline assets are CID-linked so icons/logo work in received emails without remote hosting.
            helper.addInline("brainserve-logo", new ByteArrayResource(logo.bytes()), logo.contentType());
            if (hero != null) {
                helper.addInline("oys-hero", new ByteArrayResource(hero.bytes()), hero.contentType());
            }
            for (SocialLink link : socialLinks) {
                InlineAsset icon = socialIcon(link.platform());
                helper.addInline("social-" + link.platform(),
                        new ByteArrayResource(icon.bytes()), icon.contentType());
            }
            for (int i = 0; i < images.size(); i++) {
                InlineAsset asset = images.get(i);
                helper.addInline("oys-inline-" + i, new ByteArrayResource(asset.bytes()), asset.contentType());
            }
            for (InlineAsset file : attachments) {
                helper.addAttachment(file.filename(), new ByteArrayResource(file.bytes()), file.contentType());
            }
            try {
                mailSender.send(message);
                accepted++;
            } catch (MailException ex) {
                String code = SmtpFailureDiagnoser.code(ex);
                // Full stack trace is server-side only. Never log passwords or MIME bodies.
                log.error("SMTP transmission failed (category={}, accepted={}, total={}). "
                        + "Check the nested exception in the server log.",
                        code, accepted, recipients.size(), ex);
                throw new PartialSendException(accepted, recipients.size(), ex);
            }
        }
        return accepted;
    }

    /** A defense-in-depth limit, local to one JVM. Production gateways should also enforce quotas. */
    private void reserveSendQuota(int number) {
        long now = System.currentTimeMillis();
        if (now - sendWindowStarted >= 3_600_000L || now < sendWindowStarted) {
            sendWindowStarted = now;
            sendWindowAttempts = 0;
        }
        if (sendWindowAttempts + number > MAX_RECIPIENT_ATTEMPTS_PER_HOUR) {
            throw new SendThrottleException("Hourly sending limit reached (200 recipient attempts). Try again later.");
        }
        sendWindowAttempts += number;
    }

    private InlineAsset defaultBrandLogo() throws IOException {
        byte[] bytes = new ClassPathResource("static/images/brainserve-logo.png").getContentAsByteArray();
        return new InlineAsset("brainserve-logo.png", bytes, "image/png");
    }

    private InlineAsset socialIcon(String platform) throws IOException {
        // Platform values have already been validated against the fixed allowlist.
        if (!Set.of("website", "linkedin", "instagram", "facebook", "youtube", "x").contains(platform)) {
            throw new IllegalArgumentException("Unknown social footer platform.");
        }
        String name = "social-" + platform + ".png";
        byte[] bytes = new ClassPathResource("static/images/social/" + platform + ".png").getContentAsByteArray();
        return new InlineAsset(name, bytes, "image/png");
    }

    private InlineAsset defaultHero() throws IOException {
        byte[] bytes = new ClassPathResource("static/images/oys-banner.png").getContentAsByteArray();
        return new InlineAsset("oys-banner.png", bytes, "image/png");
    }

    private List<MailBlock> parseBlocks(String json) throws IOException {
        if (json == null || json.isBlank() || json.length() > 90_000) {
            throw new IllegalArgumentException("Provide at least one email body section.");
        }
        final List<MailBlock> blocks;
        try {
            blocks = objectMapper.readValue(json, new TypeReference<List<MailBlock>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid email section data.");
        }
        if (blocks == null || blocks.isEmpty() || blocks.size() > 25) {
            throw new IllegalArgumentException("Provide 1–25 ordered body sections.");
        }
        int imageIndex = 0;
        int totalCharacters = 0;
        for (MailBlock block : blocks) {
            if (block == null) throw new IllegalArgumentException("Invalid body section.");
            if ("image".equals(block.type())) {
                if (block.imageIndex() == null || block.imageIndex() != imageIndex++) {
                    throw new IllegalArgumentException("Banner section order does not match uploaded banners.");
                }
            } else if ("text".equals(block.type()) || "notice".equals(block.type())) {
                totalCharacters += (block.heading() == null ? 0 : block.heading().length());
                totalCharacters += (block.text() == null ? 0 : block.text().length());
                if (block.heading() != null && block.heading().length() > 150) {
                    throw new IllegalArgumentException("Each section heading is limited to 150 characters.");
                }
                if ((block.heading() == null || block.heading().isBlank())
                        && (block.text() == null || block.text().isBlank())) {
                    throw new IllegalArgumentException("Text sections cannot be empty.");
                }
            } else if ("button".equals(block.type())) {
                String label = singleLine(block.label(), "Button label", 90);
                if (label.isBlank()) throw new IllegalArgumentException("Button label is required.");
                SecureLinkValidator.httpsUrl(block.url(), "Button URL");
                totalCharacters += label.length() + block.url().length();
            } else if (!"divider".equals(block.type())) {
                throw new IllegalArgumentException("Unknown email body section type.");
            }
        }
        if (totalCharacters > 40_000) {
            throw new IllegalArgumentException("Email body is limited to 40,000 characters.");
        }
        return blocks;
    }

    /** Accept only HTTPS URLs and keep each social name bound to its actual host. */
    private List<SocialLink> parseSocialLinks(String json) throws IOException {
        if (json == null || json.isBlank()) return List.of();
        if (json.length() > 15_000) throw new IllegalArgumentException("Social footer data is too large.");
        final List<SocialLink> links;
        try {
            links = objectMapper.readValue(json, new TypeReference<List<SocialLink>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid social footer data.");
        }
        if (links == null || links.size() > 6) {
            throw new IllegalArgumentException("You can include up to 6 footer links.");
        }
        Set<String> seen = new HashSet<>();
        for (SocialLink link : links) {
            if (link == null || link.platform() == null || link.url() == null
                    || !Set.of("website", "linkedin", "instagram", "facebook", "youtube", "x")
                        .contains(link.platform())) {
                throw new IllegalArgumentException("Unknown social footer platform.");
            }
            if (!seen.add(link.platform())) {
                throw new IllegalArgumentException("Each footer platform may be used only once.");
            }
            String url = link.url();
            if (url.isBlank() || url.length() > 2048 || !url.equals(url.trim())) {
                throw new IllegalArgumentException("Social footer links must contain a valid HTTPS URL.");
            }
            SecureLinkValidator.httpsUrl(url, "Social footer link");
            final URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null
                    || host == null || !host.contains(".") || uri.getPort() != -1 && uri.getPort() != 443
                    || url.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Social footer links must use a valid HTTPS website without credentials.");
            }
            String domain = host.toLowerCase(Locale.ROOT);
            String[] allowedHosts = switch (link.platform()) {
                case "linkedin" -> new String[]{"linkedin.com"};
                case "instagram" -> new String[]{"instagram.com"};
                case "facebook" -> new String[]{"facebook.com", "fb.com"};
                case "youtube" -> new String[]{"youtube.com", "youtu.be"};
                case "x" -> new String[]{"x.com", "twitter.com"};
                default -> new String[0];
            };
            if (allowedHosts.length > 0) {
                boolean matches = false;
                for (String allowed : allowedHosts) {
                    if (domain.equals(allowed) || domain.endsWith("." + allowed)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) throw new IllegalArgumentException(
                        "The " + link.platform() + " footer link must point to that platform's website.");
            }
        }
        return List.copyOf(links);
    }

    private static List<MultipartFile> nonempty(List<MultipartFile> uploads) {
        return uploads == null ? List.of() : uploads.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private static List<String> parseRecipients(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Add at least one recipient.");
        Set<String> addresses = new LinkedHashSet<>();
        for (String token : input.split("[,;\\n]+")) {
            String address = validateEmail(token.trim());
            addresses.add(address);
            if (addresses.size() > 50) throw new IllegalArgumentException("Limit: 50 recipients per email.");
        }
        return new ArrayList<>(addresses);
    }

    private static String validateEmail(String input) {
        String value = singleLine(input, "Email address", 254);
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            if (!value.equals(address.getAddress()) || !value.contains("@") || value.contains(" ")) {
                throw new IllegalArgumentException("Invalid email address: " + value);
            }
            return value;
        } catch (jakarta.mail.internet.AddressException exception) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
    }

    private static String singleLine(String input, String field, int max) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException(field + " is required.");
        String trimmed = input.trim();
        if (trimmed.length() > max || trimmed.chars().anyMatch(Character::isISOControl)
                || trimmed.chars().anyMatch(c -> c >= 0x202A && c <= 0x202E
                    || c >= 0x2066 && c <= 0x2069)) {
            throw new IllegalArgumentException(field + " contains invalid characters or is too long.");
        }
        return trimmed;
    }
}
