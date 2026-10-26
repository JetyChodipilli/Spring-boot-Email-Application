package com.brainserve.oysmail.service;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidatorTest {
    private final UploadValidator validator = new UploadValidator();

    @Test
    void acceptsValidImageAndPreservesBytes() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] png = output.toByteArray();
        var upload = new MockMultipartFile("banner", "banner.png", "image/png", png);
        var asset = validator.banner(upload);
        assertThat(asset.contentType()).isEqualTo("image/png");
        assertThat(asset.bytes()).containsExactly(png);
    }

    @Test
    void rejectsTruncatedPngRatherThanTreatingItAsSmtpFailure() {
        byte[] headerOnly = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        var file = new MockMultipartFile("banner", "broken.png", "image/png", headerOnly);
        assertThatThrownBy(() -> validator.banner(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizeAndWrongImageFormat() {
        var tooLarge = new MockMultipartFile("banner", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> validator.banner(tooLarge))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5 MB");
        var pdf = new MockMultipartFile("banner", "image.pdf", "application/pdf", "%PDF-1.5".getBytes());
        assertThatThrownBy(() -> validator.banner(pdf))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PNG or JPEG");
    }

    @Test
    void sanitizesAttachmentFilename() throws IOException {
        var upload = new MockMultipartFile("attachments", "../student\r\nnotes.pdf", "application/pdf", "pdf".getBytes());
        var asset = validator.attachment(upload);
        assertThat(asset.filename()).isEqualTo("student__notes.pdf");
        assertThat(asset.bytes()).containsExactly("pdf".getBytes());
    }
}
