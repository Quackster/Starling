package org.starling.web.service;

import io.javalin.http.UploadedFile;
import org.starling.web.cms.dao.CmsMediaDao;
import org.starling.web.cms.media.MediaStorageService;
import org.starling.web.cms.model.CmsMediaAsset;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class MediaAssetService {

    private final MediaStorageService mediaStorageService;

    /**
     * Creates a new MediaAssetService.
     * @param mediaStorageService the underlying storage service
     */
    public MediaAssetService(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    /**
     * Returns the asset count.
     * @return the asset count
     */
    public int count() {
        return CmsMediaDao.count();
    }

    /**
     * Returns every media asset.
     * @return the asset list
     */
    public List<CmsMediaAsset> listAll() {
        return CmsMediaDao.listAll();
    }

    /**
     * Finds a media asset by id.
     * @param id the asset id
     * @return the asset, when present
     */
    public Optional<CmsMediaAsset> findById(int id) {
        return CmsMediaDao.findById(id);
    }

    /**
     * Stores an uploaded media asset.
     * @param uploadedFile the uploaded file
     * @param altText the alt text
     * @return the stored media asset
     */
    public CmsMediaAsset store(UploadedFile uploadedFile, String altText) {
        return mediaStorageService.store(uploadedFile, altText);
    }

    /**
     * Resolves a media asset path.
     * @param asset the asset
     * @return the resolved filesystem path
     */
    public Path resolve(CmsMediaAsset asset) {
        return mediaStorageService.resolve(asset);
    }

    /**
     * Updates the asset alt text.
     * @param id the asset id
     * @param altText the alt text
     */
    public void updateAltText(int id, String altText) {
        CmsMediaDao.updateAltText(id, altText);
    }
}
