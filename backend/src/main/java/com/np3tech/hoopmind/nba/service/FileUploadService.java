package com.np3tech.hoopmind.nba.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadImage(MultipartFile file, String subfolder);
    void deleteFile(String filePath);
}
