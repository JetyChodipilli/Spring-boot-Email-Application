package com.brainserve.oysmail.controller;

import com.brainserve.oysmail.model.SendEmailCommand;
import com.brainserve.oysmail.service.EmailService;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class EmailController {
    private final EmailService service;

    public EmailController(EmailService service) { this.service = service; }

    @GetMapping("/config")
    public Map<String, Object> configuration(@RequestAttribute("_csrf") CsrfToken csrf) {
        return Map.of("defaultFrom", service.defaultFrom(), "allowedFrom", service.allowedFrom(),
                "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName());
    }

    @PostMapping(path = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> send(
            @RequestParam("to") String to,
            @RequestParam("from") String from,
            @RequestParam(value = "fromName", required = false) String fromName,
            @RequestParam(value = "replyTo", required = false) String replyTo,
            @RequestParam("subject") String subject,
            @RequestParam("blocksJson") String blocksJson,
            @RequestParam(value = "heroMode", defaultValue = "default") String heroMode,
            @RequestParam(value = "accentColor", defaultValue = "#f45127") String accentColor,
            @RequestParam(value = "themeJson", required = false) String themeJson,
            @RequestParam(value = "socialTitle", defaultValue = "Connect with Brainserve") String socialTitle,
            @RequestParam(value = "socialLinksJson", defaultValue = "[]") String socialLinksJson,
            @RequestParam(value = "heroBanner", required = false) MultipartFile heroBanner,
            @RequestParam(value = "inlineImages", required = false) List<MultipartFile> inlineImages,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments)
            throws IOException, MessagingException {
        int count = service.send(new SendEmailCommand(to, from, fromName, replyTo,
                subject, blocksJson, heroMode, accentColor, themeJson, socialTitle, socialLinksJson, heroBanner, inlineImages, attachments));
        return Map.of("message", "Email accepted by the configured SMTP server.", "recipientCount", count);
    }
}
