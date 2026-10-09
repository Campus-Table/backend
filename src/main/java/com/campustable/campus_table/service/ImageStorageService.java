package com.campustable.campus_table.service;

import com.campustable.campus_table.common.*;
import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Slf4j
@Service
public class ImageStorageService {
    static final long MAX_BYTES = 2 * 1024 * 1024;
    private final S3Client client;
    private final String bucket;
    private final String baseUrl;

    @org.springframework.beans.factory.annotation.Autowired
    public ImageStorageService(@Value("${app.object-storage.enabled}") boolean enabled,
            @Value("${app.object-storage.endpoint}") String endpoint,
            @Value("${app.object-storage.region}") String region,
            @Value("${app.object-storage.bucket}") String bucket,
            @Value("${app.object-storage.access-key}") String accessKey,
            @Value("${app.object-storage.secret-key}") String secretKey,
            @Value("${app.object-storage.public-base-url}") String publicBaseUrl) {
        this.bucket = bucket;
        this.baseUrl = (publicBaseUrl.isBlank() ? endpoint + "/" + bucket : publicBaseUrl).replaceAll("/+$", "");
        if (enabled && (bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank() || baseUrl.length() > 400)) {
            throw new IllegalArgumentException("Object Storage bucket, keys and a public base URL of at most 400 characters are required");
        }
        this.client = enabled ? S3Client.builder().endpointOverride(URI.create(endpoint))
                .region(Region.of(region)).forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .serviceConfiguration(c -> c.chunkedEncodingEnabled(false))
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(20)))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(30)))
                .build() : null;
    }

    ImageStorageService(S3Client client, String bucket, String baseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.baseUrl = baseUrl;
    }

    record Image(byte[] bytes, String extension, String contentType) { }
    public record Uploaded(String url, String key) { }

    static Image validate(MultipartFile file) {
        if (file.isEmpty()) throw new CustomException(ErrorCode.INVALID_IMAGE);
        if (file.getSize() > MAX_BYTES) throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
        try {
            byte[] bytes;
            try (var input = file.getInputStream()) { bytes = input.readNBytes((int) MAX_BYTES + 1); }
            if (bytes.length > MAX_BYTES) throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new CustomException(ErrorCode.INVALID_IMAGE);
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    String ext = switch (format) {
                        case "jpeg", "jpg" -> "jpg";
                        case "png" -> "png";
                        case "webp" -> "webp";
                        default -> throw new CustomException(ErrorCode.INVALID_IMAGE);
                    };
                    reader.setInput(input);
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels <= 0 || pixels > 20_000_000) throw new CustomException(ErrorCode.INVALID_IMAGE);
                    if (reader.read(0) == null) throw new CustomException(ErrorCode.INVALID_IMAGE);
                    return new Image(bytes, ext, ext.equals("jpg") ? "image/jpeg" : "image/" + ext);
                } finally { reader.dispose(); }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    public Uploaded upload(String directory, MultipartFile file) {
        Image image = validate(file);
        if (client == null) throw new CustomException(ErrorCode.STORAGE_NOT_CONFIGURED);
        String key = "images/" + directory + "/" + UUID.randomUUID() + "." + image.extension();
        // Register before PUT: timeouts can occur after the remote object was stored.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) deleteQuietly(key);
            }
        });
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(image.contentType()).contentLength((long) image.bytes().length).build(),
                    RequestBody.fromBytes(image.bytes()));
            return new Uploaded(baseUrl + "/" + key, key);
        } catch (RuntimeException e) {
            log.warn("Object Storage upload failed for {}", key);
            throw new CustomException(ErrorCode.STORAGE_FAILED);
        }
    }

    public void cleanupAfterCommit(String key) {
        if (key == null) return; // External URLs are never deleted.
        if (client == null) throw new CustomException(ErrorCode.STORAGE_NOT_CONFIGURED);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteQuietly(key); }
        });
    }

    private void deleteQuietly(String key) {
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()); }
        catch (RuntimeException e) { log.error("Object Storage cleanup failed; retry deletion for key {}", key); }
    }

    @PreDestroy public void close() { if (client != null) client.close(); }
}
