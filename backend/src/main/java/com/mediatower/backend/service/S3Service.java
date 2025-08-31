package com.mediatower.backend.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    public S3Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Génère une URL pré-signée et temporaire pour télécharger un objet depuis S3.
     * @param objectKey La clé de l'objet dans S3 (ex: "produits/mon-ebook-uuid.zip")
     * @return Une URL de téléchargement valide pour une durée limitée.
     */
    public URL generatePresignedDownloadUrl(String objectKey) {
        Date expiration = new Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        expTimeMillis += 1000 * 60 * 15; // 15 minutes
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        logger.info("Generating presigned URL for key: {}", objectKey);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }

    // ====================== NOUVELLES MÉTHODES AJOUTÉES ======================

    /**
     * Téléverse un fichier vers Amazon S3.
     * @param file Le fichier envoyé par l'utilisateur.
     * @param directory Le sous-dossier dans le bucket S3 (ex: "produits").
     * @return La clé S3 complète de l'objet téléversé.
     * @throws IOException Si la lecture du fichier échoue.
     */
    public String uploadFile(MultipartFile file, String directory) throws IOException {
        // Générer un nom de fichier unique pour éviter les conflits
        String uniqueFileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String objectKey = directory + "/" + uniqueFileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        logger.info("Uploading file '{}' to S3 with key: {}", file.getOriginalFilename(), objectKey);

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, file.getInputStream(), metadata);
        s3Client.putObject(request);

        return objectKey;
    }

    /**
     * Supprime un fichier d'Amazon S3.
     * @param objectKey La clé complète de l'objet à supprimer.
     */
    public void deleteFile(String objectKey) {
        try {
            logger.warn("Deleting file from S3 with key: {}", objectKey);
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, objectKey));
        } catch (Exception e) {
            logger.error("Error while deleting file from S3: {}", objectKey, e);
            // On peut choisir de lancer une exception ici ou juste de logger l'erreur
        }
    }
    // ========================================================================
}