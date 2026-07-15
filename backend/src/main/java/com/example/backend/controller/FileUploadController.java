package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file ảnh để tải lên!");
        }

        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png");
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) {
                extension = originalFilename.substring(i);
            } else {
                extension = ".png";
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize().resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "http://localhost:8080/uploads/" + newFileName;

            Map<String, String> responseData = new HashMap<>();
            responseData.put("fileName", newFileName);
            responseData.put("fileUrl", fileUrl);

            return ResponseEntity.ok(ApiResponse.ok(responseData, "Tải file ảnh lên thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi lưu trữ file trên server: " + e.getMessage());
        }
    }
}

// Feature Implementation: api upload avatar, sửa địa chỉ
