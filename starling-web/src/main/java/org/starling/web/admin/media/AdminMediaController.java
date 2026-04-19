package org.starling.web.admin.media;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.media.MediaAssetService;
import org.starling.web.cms.media.MediaViewFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.util.Htmx;

import java.util.Map;

public final class AdminMediaController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final MediaAssetService mediaAssetService;
    private final MediaViewFactory mediaViewFactory;

    /**
     * Creates a new AdminMediaController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param mediaAssetService the media asset service
     * @param mediaViewFactory the media view factory
     */
    public AdminMediaController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            MediaAssetService mediaAssetService,
            MediaViewFactory mediaViewFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.mediaAssetService = mediaAssetService;
        this.mediaViewFactory = mediaViewFactory;
    }

    /**
     * Renders the media library.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/media");
        model.put("assets", mediaAssetService.listAll().stream().map(mediaViewFactory::mediaAsset).toList());
        context.html(templateRenderer.render("admin-layout", "admin/media/index", model));
    }

    /**
     * Uploads media.
     * @param context the request context
     */
    public void upload(Context context) {
        UploadedFile uploadedFile = context.uploadedFile("file");
        if (uploadedFile == null) {
            Htmx.redirect(context, "/admin/media?error=Choose%20a%20file%20first");
            return;
        }

        mediaAssetService.store(uploadedFile, RequestValues.valueOrEmpty(context.formParam("altText")).trim());
        Htmx.redirect(context, "/admin/media?notice=Media%20uploaded");
    }

    /**
     * Updates media metadata.
     * @param context the request context
     */
    public void update(Context context) {
        int assetId = Integer.parseInt(context.pathParam("id"));
        mediaAssetService.updateAltText(assetId, RequestValues.valueOrEmpty(context.formParam("altText")).trim());
        Htmx.redirect(context, "/admin/media?notice=Media%20metadata%20saved");
    }
}
