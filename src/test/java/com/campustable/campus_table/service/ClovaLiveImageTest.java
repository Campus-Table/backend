package com.campustable.campus_table.service;

import java.nio.file.*;
import java.util.Properties;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "CLOVA_LIVE_TEST", matches = "true")
class ClovaLiveImageTest {
    @Test void analyzesUserProvidedImage() throws Exception {
        var env = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(".env"))) { env.load(reader); }
        String key = env.getProperty("CLOVA_API_KEY", "").trim();
        assertFalse(key.isBlank(), "CLOVA API key must be configured");
        var image = ImageIO.read(Path.of(System.getenv("CLOVA_TEST_IMAGE")).toFile());
        var rgb = new java.awt.image.BufferedImage(image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(rgb, "jpeg", bytes));
        System.out.println("LIVE_IMAGE dimensions=" + rgb.getWidth() + "x" + rgb.getHeight() + " jpeg_bytes=" + bytes.size());
        var estimator = new ClovaPeopleEstimator(key,
                env.getProperty("CLOVA_ENDPOINT", "https://clovastudio.stream.ntruss.com/v3/chat-completions/HCX-005"), 90);
        int count = estimator.estimate(new MockMultipartFile("image", "occupancy.jpg", "image/jpeg", bytes.toByteArray()));
        System.out.println("LIVE_CLOVA estimated_count=" + count);
        assertTrue(count >= 0);
    }
}
