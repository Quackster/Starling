package org.oldskooler.vibe.web.app.asset;

import net.h4bbo.avatara4j.figure.Avatar;
import net.h4bbo.avatara4j.figure.readers.FiguredataReader;
import net.h4bbo.avatara4j.figure.readers.LegacyFiguredataReader;
import net.h4bbo.avatara4j.figure.readers.ManifestReader;

public final class AvatarImagingService {

    private static final String DEFAULT_FIGURE = "hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61";
    private static final Object INITIALIZATION_MONITOR = new Object();

    private static volatile boolean initialized;

    /**
     * Creates a new AvatarImagingService.
     */
    public AvatarImagingService() {
        ensureInitialized();
    }

    /**
     * Renders an avatar image using the bundled Avatara4j renderer.
     * @param figure the figure string
     * @param size the requested size
     * @param bodyDirection the body direction
     * @param headDirection the head direction
     * @param action the body action
     * @param gesture the head gesture
     * @param headOnly whether only the head should render
     * @param frame the animation frame
     * @param carryDrink the carry item id
     * @param cropImage whether to crop the image
     * @return the rendered avatar png bytes
     */
    public byte[] renderAvatar(
            String figure,
            String size,
            int bodyDirection,
            int headDirection,
            String action,
            String gesture,
            boolean headOnly,
            int frame,
            int carryDrink,
            boolean cropImage
    ) {
        ensureInitialized();

        byte[] rendered = render(
                normalizeFigure(figure),
                normalizeSize(size),
                normalizeDirection(bodyDirection),
                normalizeDirection(headDirection),
                normalizeAction(action),
                normalizeAction(gesture),
                headOnly,
                Math.max(1, frame),
                Math.max(0, carryDrink),
                cropImage
        );
        if (rendered != null && rendered.length > 0) {
            return rendered;
        }

        rendered = render(
                DEFAULT_FIGURE,
                normalizeSize(size),
                normalizeDirection(bodyDirection),
                normalizeDirection(headDirection),
                normalizeAction(action),
                normalizeAction(gesture),
                headOnly,
                Math.max(1, frame),
                Math.max(0, carryDrink),
                cropImage
        );
        if (rendered != null && rendered.length > 0) {
            return rendered;
        }

        throw new IllegalStateException("Failed to render avatar image");
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (INITIALIZATION_MONITOR) {
            if (initialized) {
                return;
            }

            FiguredataReader.getInstance().load();
            LegacyFiguredataReader.getInstance().load();
            ManifestReader.getInstance().load();
            initialized = true;
        }
    }

    private byte[] render(
            String figure,
            String size,
            int bodyDirection,
            int headDirection,
            String action,
            String gesture,
            boolean headOnly,
            int frame,
            int carryDrink,
            boolean cropImage
    ) {
        Avatar avatar = new Avatar(
                FiguredataReader.getInstance(),
                figure,
                size,
                bodyDirection,
                headDirection,
                action,
                gesture,
                headOnly,
                frame,
                carryDrink,
                cropImage
        );

        try {
            return avatar.run();
        } finally {
            avatar.dispose();
        }
    }

    private String normalizeFigure(String figure) {
        if (figure == null || figure.trim().isBlank()) {
            return DEFAULT_FIGURE;
        }
        return figure.trim();
    }

    private String normalizeSize(String size) {
        if (size == null) {
            return "s";
        }

        String normalized = size.trim().toLowerCase();
        if ("b".equals(normalized) || "l".equals(normalized)) {
            return normalized;
        }

        return "s";
    }

    private int normalizeDirection(int direction) {
        if (direction < 0 || direction > 7) {
            return 2;
        }
        return direction;
    }

    private String normalizeAction(String value) {
        if (value == null || value.trim().isBlank()) {
            return "std";
        }
        return value.trim().toLowerCase();
    }
}
