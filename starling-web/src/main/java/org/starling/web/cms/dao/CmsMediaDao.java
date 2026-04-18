package org.starling.web.cms.dao;

import org.starling.storage.EntityContext;
import org.starling.web.cms.model.CmsMediaAsset;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CmsMediaDao {

    /**
     * Creates a new CmsMediaDao.
     */
    private CmsMediaDao() {}

    /**
     * Counts assets.
     * @return the resulting count
     */
    public static int count() {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement("SELECT COUNT(*) FROM cms_media_assets");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (Exception e) {
                throw new RuntimeException("Failed to count cms media assets", e);
            }
        });
    }

    /**
     * Lists all assets.
     * @return the resulting list
     */
    public static List<CmsMediaAsset> listAll() {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "SELECT * FROM cms_media_assets ORDER BY created_at DESC, id DESC"
            );
                 ResultSet resultSet = statement.executeQuery()) {
                List<CmsMediaAsset> assets = new ArrayList<>();
                while (resultSet.next()) {
                    assets.add(map(resultSet));
                }
                return assets;
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms media assets", e);
            }
        });
    }

    /**
     * Finds an asset by id.
     * @param id the id value
     * @return the resulting asset
     */
    public static Optional<CmsMediaAsset> findById(int id) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "SELECT * FROM cms_media_assets WHERE id = ?"
            )) {
                statement.setInt(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(map(resultSet));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms media asset", e);
            }
        });
    }

    /**
     * Creates an asset.
     * @param fileName the original file name
     * @param relativePath the relative storage path
     * @param mimeType the mime type
     * @param sizeBytes the file size in bytes
     * @param width the width value
     * @param height the height value
     * @param altText the alt text value
     * @return the resulting asset id
     */
    public static int create(
            String fileName,
            String relativePath,
            String mimeType,
            long sizeBytes,
            Integer width,
            Integer height,
            String altText
    ) {
        return EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    INSERT INTO cms_media_assets (
                        file_name,
                        relative_path,
                        mime_type,
                        size_bytes,
                        width,
                        height,
                        alt_text,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, fileName);
                statement.setString(2, relativePath);
                statement.setString(3, mimeType);
                statement.setLong(4, sizeBytes);
                if (width == null) {
                    statement.setNull(5, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(5, width);
                }
                if (height == null) {
                    statement.setNull(6, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(6, height);
                }
                statement.setString(7, altText);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create cms media asset", e);
            }
        });
    }

    /**
     * Updates asset alt text.
     * @param id the asset id value
     * @param altText the alt text value
     */
    public static void updateAltText(int id, String altText) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "UPDATE cms_media_assets SET alt_text = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            )) {
                statement.setString(1, altText);
                statement.setInt(2, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms media alt text", e);
            }
        });
    }

    /**
     * Maps an asset row.
     * @param resultSet the result set value
     * @return the resulting asset
     * @throws Exception if the mapping fails
     */
    private static CmsMediaAsset map(ResultSet resultSet) throws Exception {
        Integer width = (Integer) resultSet.getObject("width");
        Integer height = (Integer) resultSet.getObject("height");
        return new CmsMediaAsset(
                resultSet.getInt("id"),
                resultSet.getString("file_name"),
                resultSet.getString("relative_path"),
                resultSet.getString("mime_type"),
                resultSet.getLong("size_bytes"),
                width,
                height,
                resultSet.getString("alt_text"),
                resultSet.getTimestamp("created_at"),
                resultSet.getTimestamp("updated_at")
        );
    }
}
