package com.mediatower.backend.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    @Value("${firebase.storage.bucket-name}")
    private String bucketName;

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
    public String storeProfileImage(MultipartFile file) {
        String subfolder = "profile-images";
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + originalFileName);
            }

            // Générer un nom de fichier unique pour éviter les conflits
            String fileExtension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFileName.substring(i);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // S'assurer que le sous-dossier existe
            Path targetLocationFolder = this.fileStorageLocation.resolve(subfolder);
            Files.createDirectories(targetLocationFolder);

            // Copier le fichier
            Path targetLocation = targetLocationFolder.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retourner le chemin web public, ex: "/uploads/profile-images/uuid.jpg"
            return "/uploads/" + subfolder + "/" + uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store profile image " + originalFileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    public String saveFileToFirebase(MultipartFile file) throws IOException {
        StorageClient storageClient = StorageClient.getInstance();
        String fileName = "uploads/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        storageClient.bucket(bucketName).create(fileName, file.getBytes(), file.getContentType());
        return getPublicUrl(fileName);
    }

    public String getPublicUrl(String fileName) {
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    public URL generateSignedUploadUrlForChat(String uniqueFileName, String contentType) {
        Bucket bucket = StorageClient.getInstance().bucket(bucketName);
        Storage storage = bucket.getStorage();

        BlobInfo blobInfo = BlobInfo.newBuilder(bucket.getName(), uniqueFileName)
                .setContentType(contentType)
                .build();

        return storage.signUrl(
                blobInfo,
                15,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );
    }

    public String getPublicUrlForChat(String uploadedFileName) {
        String encodedFileName = URLEncoder.encode(uploadedFileName, StandardCharsets.UTF_8);
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                encodedFileName);
    }
}