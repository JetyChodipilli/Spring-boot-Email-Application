package com.brainserve.oysmail.controller;

import jakarta.mail.MessagingException;
import com.brainserve.oysmail.service.PartialSendException;
import com.brainserve.oysmail.service.SendThrottleException;
import com.brainserve.oysmail.service.SmtpFailureDiagnoser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "The total upload is too large (35 MB maximum)."));
    }

    @ExceptionHandler(SendThrottleException.class)
    public ResponseEntity<Map<String, String>> throttled(SendThrottleException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PartialSendException.class)
    public ResponseEntity<Map<String, Object>> partialSend(PartialSendException ex) {
        String code = SmtpFailureDiagnoser.code(ex.getCause());
        String explanation = SmtpFailureDiagnoser.advice(code);
        String prefix = ex.accepted() == 0
                ? "SMTP sending failed before any confirmed acceptance (0 of " + ex.total() + "). "
                : "Only " + ex.accepted() + " of " + ex.total()
                        + " messages returned an SMTP acceptance response. ";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", prefix + explanation
                        + " Delivery of unconfirmed attempts is uncertain; inspect the SMTP provider logs before retrying.",
                "errorCode", code,
                "recipientCountSent", ex.accepted(), "recipientCountTotal", ex.total()));
    }

    @ExceptionHandler({MessagingException.class, IOException.class, MailException.class})
    public ResponseEntity<Map<String, String>> mailError(Exception ex) {
        log.error("Email preparation or transport failed before a completed send.", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "The email could not be prepared or transferred. See the server log for the cause.",
                "errorCode", "EMAIL_PREPARATION_FAILED"));
    }
}
