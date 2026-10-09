package com.campustable.campus_table.service;

import static org.junit.jupiter.api.Assertions.*;
import com.campustable.campus_table.common.CustomException;
import org.junit.jupiter.api.Test;

class ClovaPeopleEstimatorTest {
    private final ClovaPeopleEstimator estimator = new ClovaPeopleEstimator("", "https://example.com", 60);
    private String response(String count) {
        return "{\"status\":{\"code\":\"20000\"},\"result\":{\"message\":{\"content\":"
                + new tools.jackson.databind.ObjectMapper().writeValueAsString(count) + "}}}";
    }
    @Test void sendsImageToV3AndParsesResponse() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var requestBody = new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/v3/chat-completions/HCX-005", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            byte[] body = response("{\"status\":\"ok\",\"confirmedCount\":2,\"estimatedCount\":3}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var clova = new ClovaPeopleEstimator("test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/v3/chat-completions/HCX-005", 5);
            var out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            assertEquals(3, clova.estimate(new org.springframework.mock.web.MockMultipartFile("image", out.toByteArray())));
            var sent = new tools.jackson.databind.ObjectMapper().readTree(requestBody.get());
            assertEquals("system", sent.path("messages").get(0).path("role").asString());
            assertTrue(sent.path("messages").get(1).path("content").get(1).path("dataUri").path("data").asString().startsWith("data:image/png;base64,"));
            assertFalse(requestBody.get().contains("test-key"));
        } finally { server.stop(0); }
    }

    @Test void parsesCountAndZero() {
        assertEquals(42, estimator.parse(response("{\"status\":\"ok\",\"confirmedCount\":40,\"estimatedCount\":42}")));
        assertEquals(0, estimator.parse(response("{\"status\":\"ok\",\"confirmedCount\":0,\"estimatedCount\":0}")));
    }
    @Test void rejectsUnusableMalformedAndInvalidCounts() {
        for (String text : new String[]{"not json", "{}",
                "{\"status\":\"unusable\",\"confirmedCount\":null,\"estimatedCount\":null}",
                "{\"status\":\"ok\",\"confirmedCount\":4,\"estimatedCount\":3}",
                "{\"status\":\"ok\",\"confirmedCount\":0,\"estimatedCount\":1.5}",
                "{\"status\":\"ok\",\"confirmedCount\":-1,\"estimatedCount\":2}",
                "{\"status\":\"ok\",\"confirmedCount\":0,\"estimatedCount\":10001}"}) {
            assertThrows(CustomException.class, () -> estimator.parse(response(text)));
        }
        assertThrows(CustomException.class, () -> estimator.parse("{\"status\":{\"code\":\"40100\"}}"));
    }
}
