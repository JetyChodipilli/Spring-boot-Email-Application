package com.brainserve.oysmail.model;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record SendEmailCommand(
        String to, String from, String fromName, String replyTo, String subject, String blocksJson,
        String heroMode, String accentColor, String themeJson, String socialTitle, String socialLinksJson,
        MultipartFile heroBanner, List<MultipartFile> inlineImages, List<MultipartFile> attachments) {
    /** Compatibility for previously written code/tests that passed only one accent color. */
    public SendEmailCommand(String to, String from, String fromName, String replyTo,
            String subject, String blocksJson, String heroMode, String accentColor,
            String socialTitle, String socialLinksJson, MultipartFile heroBanner,
            List<MultipartFile> inlineImages, List<MultipartFile> attachments) {
        this(to, from, fromName, replyTo, subject, blocksJson, heroMode, accentColor,
                null, socialTitle, socialLinksJson, heroBanner, inlineImages, attachments);
    }
}
