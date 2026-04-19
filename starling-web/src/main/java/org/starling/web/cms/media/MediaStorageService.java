package org.starling.web.cms.media;

import io.javalin.http.UploadedFile;
import org.starling.web.config.WebConfig;
import org.starling.web.util.Slugifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class MediaStorageService {

    private final Path uploadDirectory;

    /**
     * Creates a new MediaStorageService.
     * @param config the config value
     */
    public MediaStorageService(WebConfig config) {
        this.uploadDirectory = config.uploadDirectory();
    }

    /**
     * Stores an uploaded file and creates its metadata row.
     * @param uploadedFile the uploaded file value
     * @param altText the alt text value
     * @return the resulting media asset
     */
    public CmsMediaAsset store(UploadedFile uploadedFile, String altText) {
        try {
            String originalFileName = uploadedFile.filename();
            String safeBaseName = Slugifier.slugify(stripExtension(originalFileName));
            if (safeBaseName.isBlank()) {
                safeBaseName = "asset";
            }

            String extension = fileExtension(originalFileName);
            LocalDate today = LocalDate.now();
            Path relativeDirectory = Path.of(
                    Integer.toString(today.getYear()),
                    String.format("%02d", today.getMonthValue())
            );
            String storedFileName = safeBaseName + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path relativePath = relativeDirectory.resolve(storedFileName);
            Path targetPath = uploadDirectory.resolve(relativePath);

            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = uploadedFile.content()) {
                Files.copy(inputStream, targetPath);
            }

            long sizeBytes = Files.size(targetPath);
            String mimeType = Optional.ofNullable(uploadedFile.contentType())
                    .orElse(Optional.ofNullable(Files.probeContentType(targetPath)).orElse("application/octet-stream"));

            Integer width = null;
            Integer height = null;
            try {
                BufferedImage bufferedImage = ImageIO.read(targetPath.toFile());
                if (bufferedImage != null) {
                    width = bufferedImage.getWidth();
                    height = bufferedImage.getHeight();
                }
            } catch (Exception ignored) {
                // Non-image assets do not expose dimensions.
            }

            int assetId = CmsMediaDao.create(
                    originalFileName,
                    relativePath.toString().replace('\\', '/'),
                    mimeType,
                    sizeBytes,
                    width,
                    height,
                    altText == null ? "" : altText
            );

            return CmsMediaDao.findById(assetId).orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded media", e);
        }
    }

    /**
     * Resolves a stored media asset path.
     * @param asset the asset value
     * @return the resulting path
     */
    public Path resolve(CmsMediaAsset asset) {
        return uploadDirectory.resolve(asset.relativePath());
    }

    /**
     * Strips a filename extension.
     * @param fileName the filename value
     * @return the resulting base name
     */
    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    /**
     * Returns a filename extension.
     * @param fileName the filename value
     * @return the resulting extension
     */
    private static String fileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase();
    }
}
