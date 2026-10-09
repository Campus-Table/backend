package com.campustable.campus_table.service;

import com.campustable.campus_table.common.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class ImageStorageServiceTest {
    @Test void detectsActualFormatRegardlessOfFilenameAndHeader() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        var image = ImageStorageService.validate(new MockMultipartFile("file", "wrong.jpg", "text/plain", out.toByteArray()));
        assertEquals("png", image.extension());
        assertEquals("image/png", image.contentType());
    }
    @Test void rejectsFakeImageAndEmptyFile() {
        assertCode(ErrorCode.INVALID_IMAGE, new byte[]{1, 2, 3});
        assertCode(ErrorCode.INVALID_IMAGE, new byte[0]);
    }
    @Test void rejectsOversizedFile() {
        assertCode(ErrorCode.IMAGE_TOO_LARGE, new byte[(int) ImageStorageService.MAX_BYTES + 1]);
    }
    @Test void rejectsTruncatedPng() {
        assertCode(ErrorCode.INVALID_IMAGE, new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
    }
    @Test void oldObjectIsDeletedOnlyAfterCommit() {
        var client = org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);
        var storage = new ImageStorageService(client, "bucket", "https://images.example");
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            storage.cleanupAfterCommit("images/menus/old.png");
            org.mockito.Mockito.verifyNoInteractions(client);
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(c -> c.afterCommit());
            org.mockito.Mockito.verify(client).deleteObject(org.mockito.ArgumentMatchers.argThat(
                    (software.amazon.awssdk.services.s3.model.DeleteObjectRequest r) -> r.key().equals("images/menus/old.png")));
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void rollbackDeletesNewlyUploadedObject() throws Exception {
        var client = org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);
        var storage = new ImageStorageService(client, "bucket", "https://images.example");
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            var uploaded = storage.upload("menus", new MockMultipartFile("file", out.toByteArray()));
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(c -> c.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
            org.mockito.Mockito.verify(client).deleteObject(org.mockito.ArgumentMatchers.argThat(
                    (software.amazon.awssdk.services.s3.model.DeleteObjectRequest r) -> r.key().equals(uploaded.key())));
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }

    private void assertCode(ErrorCode code, byte[] bytes) {
        var ex = assertThrows(CustomException.class, () -> ImageStorageService.validate(
                new MockMultipartFile("file", "image.png", "image/png", bytes)));
        assertEquals(code, ex.getErrorCode());
    }
}
