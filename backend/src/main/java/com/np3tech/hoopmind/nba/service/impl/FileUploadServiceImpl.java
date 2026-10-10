package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file uploads (profile pictures, team logos, etc.)
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Upload an image file
     */
    public String uploadImage(MultipartFile file, String subfolder) {
        validateImageFile(file);

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, subfolder);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {}", filename);

            // Return relative path
            return subfolder + "/" + filename;

        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new AppException(
                    "Failed to upload file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FILE_UPLOAD_ERROR"
            );
        }
    }

    /**
     * Delete a file
     */
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath);
            Files.deleteIfExists(path);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
        }
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(
                    "File is empty",
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_FILE"
            );
        }

        if (file.getSize() > maxFileSize) {
            throw new AppException(
                    "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB",
                    HttpStatus.BAD_REQUEST,
                    "FILE_TOO_LARGE"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(
                    "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILE_TYPE"
            );
        }
    }
}