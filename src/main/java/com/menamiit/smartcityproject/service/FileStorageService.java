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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Path UPLOAD_DIR = Path.of("uploads", "complaints");
    private static final Path RESOLUTION_UPLOAD_DIR = Path.of("uploads", "resolutions");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_RESOLUTION_IMAGES = 5;

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

    public List<String> storeResolutionImages(List<MultipartFile> files, Long grievanceId) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<MultipartFile> nonEmptyFiles = files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .toList();

        if (nonEmptyFiles.isEmpty()) {
            return List.of();
        }

        if (nonEmptyFiles.size() > MAX_RESOLUTION_IMAGES) {
            throw new IllegalArgumentException("You can upload at most 5 resolution images");
        }

        Path grievanceDir = RESOLUTION_UPLOAD_DIR.resolve("grievance-" + grievanceId);
        Files.createDirectories(grievanceDir);

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : nonEmptyFiles) {
            if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new IllegalArgumentException("Each image must be 5 MB or smaller");
            }

            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String normalizedExt = ext == null ? "" : ext.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(normalizedExt)) {
                throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP files are allowed");
            }

            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + "."
                + normalizedExt;

            Path target = grievanceDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            savedPaths.add(target.toString().replace('\\', '/'));
        }

        return savedPaths;
    }
}
