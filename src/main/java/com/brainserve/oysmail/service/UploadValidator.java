package com.brainserve.oysmail.service;

import com.brainserve.oysmail.model.InlineAsset;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadValidator {
    private static final long IMAGE_LIMIT = 5L * 1024 * 1024;
    private static final long ATTACHMENT_LIMIT = 10L * 1024 * 1024;

    public InlineAsset banner(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a banner image.");
        }
        if (file.getSize() > IMAGE_LIMIT) {
            throw new IllegalArgumentException("Each banner must be 5 MB or smaller.");
        }
        byte[] bytes = file.getBytes();
        String type;
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13
                && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            type = "image/png";
        } else if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            type = "image/jpeg";
        } else {
            throw new IllegalArgumentException("Banners must be PNG or JPEG images.");
        }
        // Check dimensions before decoding to reject oversized decompression bombs.
        // Then decode once to reject corrupt/truncated images before sending them.
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IllegalArgumentException("Invalid image content.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image content.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 6000 || height > 6000
                        || (long) width * height > 16_000_000) {
                    throw new IllegalArgumentException("Banner dimensions must fit within 6000 pixels and 16 MP.");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("Invalid or incomplete image content.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Invalid or incomplete PNG/JPEG image.", ex);
        }
        return new InlineAsset(cleanFilename(file.getOriginalFilename()), bytes, type);
    }

    public InlineAsset attachment(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty attachment supplied.");
        if (file.getSize() > ATTACHMENT_LIMIT) {
            throw new IllegalArgumentException("Each attachment must be 10 MB or smaller.");
        }
        // MIME is not trusted as a security boundary: forwarded attachment bytes are not executed.
        return new InlineAsset(cleanFilename(file.getOriginalFilename()), file.getBytes(),
                "application/octet-stream");
    }

    private static String cleanFilename(String name) {
        if (name == null || name.isBlank()) return "attachment";
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1)
                .replaceAll("[\\r\\n\\u0000-\\u001F\\u007F]", "_").trim();
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) return "attachment";
        return cleaned.length() > 150 ? cleaned.substring(cleaned.length() - 150) : cleaned;
    }
}
