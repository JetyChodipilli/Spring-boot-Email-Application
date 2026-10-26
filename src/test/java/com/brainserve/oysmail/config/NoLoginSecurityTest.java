package com.brainserve.oysmail.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end local HTTP tests. No login is required, but forged POSTs are rejected. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoLoginSecurityTest {
    @LocalServerPort int port;

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

    @Test void doesNotCreateGeneratedUserOrPassword() {
        assertThat(context.getBeanNamesForType(org.springframework.security.core.userdetails.UserDetailsService.class))
                .isEmpty();
    }

    @Test void editorLoadsWithoutLogin() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("Design your email").doesNotContain("Sign out");
        }
    }

    @Test void configurationIsPublicAndProvidesCsrfToken() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/config")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("csrfToken", "csrfHeader", "allowedFrom");
            assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        }
    }

    @Test void directSendWithoutCsrfTokenIsForbidden() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/send"))
                    .header("Content-Type", "multipart/form-data; boundary=abc")
                    .POST(HttpRequest.BodyPublishers.ofString("--abc--\r\n")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @Test void rejectsNonLocalHostToPreventDnsRebinding() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            // Raw TCP is used because the built-in HttpClient prohibits overriding Host.
            try (var socket = new java.net.Socket("127.0.0.1", port)) {
                socket.setSoTimeout(3000);
                socket.getOutputStream().write(("GET /api/config HTTP/1.1\r\nHost: attacker.example\r\nConnection: close\r\n\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                String status = new String(socket.getInputStream().readNBytes(100),
                        java.nio.charset.StandardCharsets.US_ASCII);
                assertThat(status).startsWith("HTTP/1.1 403");
            }
        }
    }
}
