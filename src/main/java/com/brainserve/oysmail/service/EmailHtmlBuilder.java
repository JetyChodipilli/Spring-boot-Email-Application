package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.EmailTheme;
import com.brainserve.oysmail.model.MailBlock;
import com.brainserve.oysmail.model.SocialLink;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class EmailHtmlBuilder {
    /** Backward-compatible single-accent entry point. */
    public String render(List<MailBlock> blocks, boolean includeHero, String accentColor) {
        return render(blocks, includeHero, accentColor, "", List.of());
    }

    public String render(List<MailBlock> blocks, boolean includeHero, String accentColor,
                         String socialTitle, List<SocialLink> socialLinks) {
        String accent = accentColor != null && accentColor.matches("#[A-Fa-f0-9]{6}")
                ? accentColor : EmailTheme.defaults().primaryOrange();
        return render(blocks, includeHero, EmailTheme.defaults().withPrimaryOrange(accent),
                socialTitle, socialLinks);
    }

    /** Email-safe, inline CSS only. Never inject raw :root CSS into email clients. */
    public String render(List<MailBlock> blocks, boolean includeHero, EmailTheme t,
                         String socialTitle, List<SocialLink> socialLinks) {
        StringBuilder out = new StringBuilder(8192);
        out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:").append(t.background())
                .append(";font-family:Arial,Helvetica,sans-serif;color:").append(t.textPrimary())
                .append(";\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"background:")
                .append(t.background()).append(";\"><tr><td align=\"center\" style=\"padding:26px 12px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"640\" style=\"width:100%;max-width:640px;background:")
                .append(t.white()).append(";border-collapse:collapse;\">");
        // Brainserve brand mark appears above the OYS banner in every outgoing HTML email.
        out.append("<tr><td align=\"left\" style=\"padding:17px 28px;background:")
                .append(t.white()).append(";border-bottom:1px solid:").append(t.borderColor())
                .append(";\"><img src=\"cid:brainserve-logo\" width=\"228\" alt=\"Brainserve\" "
                        + "style=\"display:block;width:228px;max-width:100%;height:auto;border:0;\"></td></tr>");
        if (includeHero) {
            out.append("<tr><td style=\"padding:0;\"><img src=\"cid:oys-hero\" width=\"640\" "
                    + "alt=\"Operation Yuva Sankalp - Student Learning Initiative\" "
                    + "style=\"display:block;width:100%;max-width:640px;height:auto;border:0;outline:none;\"></td></tr>");
        }
        for (MailBlock b : blocks) {
            switch (b.type()) {
                case "image" -> out.append("<tr><td style=\"padding:0;\"><img src=\"cid:oys-inline-")
                        .append(b.imageIndex()).append("\" alt=\"Email content image\" width=\"640\" "
                                + "style=\"display:block;width:100%;max-width:640px;height:auto;border:0;\"></td></tr>");
                case "text" -> {
                    out.append("<tr><td style=\"padding:25px 36px 12px;line-height:1.7;font-size:16px;color:")
                            .append(t.textPrimary()).append(";\">");
                    if (b.heading() != null && !b.heading().isBlank()) {
                        out.append("<h2 style=\"margin:0 0 12px;color:").append(t.darkNavy())
                                .append(";font-size:22px;line-height:1.35;\">").append(escape(b.heading())).append("</h2>");
                    }
                    out.append("<div style=\"margin:0 0 12px;\">")
                            .append(lines(b.text())).append("</div></td></tr>");
                }
                case "notice" -> {
                    out.append("<tr><td style=\"padding:10px 36px 20px;\"><table role=\"presentation\" "
                            + "width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:")
                            .append(t.background()).append(";border-left:4px solid:").append(t.notificationRed())
                            .append(";\"><tr><td style=\"padding:15px 17px;color:").append(t.textPrimary())
                            .append(";font-size:15px;line-height:1.6;\">");
                    if (b.heading() != null && !b.heading().isBlank()) {
                        out.append("<strong style=\"display:block;margin-bottom:6px;color:")
                                .append(t.notificationRed()).append(";\">").append(escape(b.heading())).append("</strong>");
                    }
                    out.append(lines(b.text())).append("</td></tr></table></td></tr>");
                }
                case "button" -> out.append("<tr><td align=\"left\" style=\"padding:12px 36px 25px;\">")
                        .append("<a href=\"").append(escape(b.url())).append("\" style=\"display:inline-block;background:")
                        .append(t.primaryOrange()).append(";color:").append(t.white())
                        .append(";text-decoration:none;border-radius:7px;font-size:15px;font-weight:bold;padding:13px 22px;\">")
                        .append(escape(b.label())).append("</a></td></tr>");
                case "divider" -> out.append("<tr><td style=\"padding:14px 36px;\">")
                        .append("<div style=\"border-top:1px solid:").append(t.borderColor())
                        .append(";font-size:0;line-height:0;\">&nbsp;</div></td></tr>");
                default -> throw new IllegalArgumentException("Unknown email body section type.");
            }
        }
        if (socialLinks != null && !socialLinks.isEmpty()) {
            out.append("<tr><td align=\"center\" style=\"padding:23px 24px 27px;background:")
                    .append(t.background()).append(";border-top:1px solid:").append(t.borderColor()).append(";\">");
            out.append("<p style=\"margin:0 0 16px;color:").append(t.darkNavy())
                    .append(";font-size:16px;font-weight:bold;line-height:1.5;\">")
                    .append(escape(socialTitle)).append("</p>");
            for (SocialLink link : socialLinks) {
                // The image AND the label are inside the anchor: clicking either opens the configured HTTPS URL.
                // CID PNGs work without remote image hosts; labels remain usable when an inbox blocks images.
                out.append("<a href=\"").append(escape(link.url()))
                        .append("\" target=\"_blank\" rel=\"noopener noreferrer\" "
                                + "style=\"display:inline-block;vertical-align:top;text-align:center;"
                                + "width:77px;margin:0 3px 9px;padding:5px 2px;"
                                + "color:").append(t.darkOrange())
                        .append(";text-decoration:none;font-size:11px;font-weight:bold;line-height:1.35;\">")
                        .append("<img src=\"cid:social-").append(link.platform())
                        .append("\" alt=\"").append(socialLabel(link.platform()))
                        .append("\" width=\"36\" height=\"36\" "
                                + "style=\"display:block;border:0;width:36px;height:36px;margin:0 auto 6px;\">")
                        .append("<span style=\"display:block;\">").append(socialLabel(link.platform()))
                        .append("</span></a>");
            }
            out.append("</td></tr>");
        }
        out.append("<tr><td style=\"padding:23px 36px 28px;border-top:1px solid:")
                .append(t.borderColor()).append(";\"><p style=\"margin:0;color:")
                .append(t.textSecondary()).append(";font-size:12px;line-height:1.6;\">"
                        + "Operation Yuva Sankalp · Student Learning Initiative</p>"
                        + "</td></tr></table></td></tr></table></body></html>");
        return out.toString();
    }

    public String plainText(List<MailBlock> blocks) {
        return plainText(blocks, "", List.of());
    }

    public String plainText(List<MailBlock> blocks, String socialTitle, List<SocialLink> socialLinks) {
        StringBuilder out = new StringBuilder("Brainserve\nOperation Yuva Sankalp – Student Learning Initiative\n\n");
        for (MailBlock b : blocks) {
            switch (b.type()) {
                case "image" -> out.append("[Embedded email image]\n\n");
                case "text", "notice" -> {
                    if (b.heading() != null && !b.heading().isBlank()) out.append(b.heading()).append("\n");
                    out.append(b.text() == null ? "" : b.text()).append("\n\n");
                }
                case "button" -> out.append(b.label()).append(": ").append(b.url()).append("\n\n");
                case "divider" -> out.append("---\n\n");
                default -> throw new IllegalArgumentException("Unknown email body section type.");
            }
        }
        if (socialLinks != null && !socialLinks.isEmpty()) {
            out.append("\n").append(socialTitle).append("\n");
            for (SocialLink link : socialLinks) {
                out.append(socialLabel(link.platform())).append(": ").append(link.url()).append("\n");
            }
        }
        return out.toString().trim();
    }

    private static String lines(String text) {
        return escape(text == null ? "" : text).replace("\n", "<br>");
    }

    private static String socialLabel(String platform) {
        return switch (platform) {
            case "website" -> "Website";
            case "linkedin" -> "LinkedIn";
            case "instagram" -> "Instagram";
            case "facebook" -> "Facebook";
            case "youtube" -> "YouTube";
            case "x" -> "X / Twitter";
            default -> throw new IllegalArgumentException("Unknown social footer platform.");
        };
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text == null ? "" : text).replace("\r", "");
    }
}
