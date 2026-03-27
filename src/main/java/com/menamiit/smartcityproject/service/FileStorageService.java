package com.menamiit.smartcityproject.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Path UPLOAD_DIR = Path.of("uploads", "complaints");

    public String storeComplaintPhoto(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        Files.createDirectories(UPLOAD_DIR);

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExt = ext == null ? "bin" : ext.replaceAll("[^a-zA-Z0-9]", "");
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-" + UUID.randomUUID().toString().substring(0, 8)
            + "." + safeExt;

        Path target = UPLOAD_DIR.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return target.toString().replace('\\', '/');
    }
}
