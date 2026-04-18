package org.starling.web.user;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class CaptchaService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Creates a new CaptchaService.
     */
    private CaptchaService() {}

    /**
     * Generates a captcha text.
     * @param length the text length
     * @return the resulting captcha text
     */
    public static String generateText(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    /**
     * Generates a png image for the given text.
     * @param text the text value
     * @return the resulting png bytes
     */
    public static byte[] renderPng(String text) {
        try {
            BufferedImage image = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(240, 240, 240));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(198, 198, 198));
            for (int index = 0; index < 8; index++) {
                graphics.drawLine(0, RANDOM.nextInt(image.getHeight()), image.getWidth(), RANDOM.nextInt(image.getHeight()));
            }

            graphics.setColor(new Color(30, 30, 30));
            graphics.setFont(new Font("Arial", Font.BOLD, 30));
            graphics.drawString(text, 22, 40);
            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render captcha image for " + text, e);
        }
    }

    /**
     * Renders a simple avatar placeholder png.
     * @param text the label text
     * @param width the width value
     * @param height the height value
     * @return the resulting png bytes
     */
    public static byte[] renderAvatarPlaceholder(String text, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(232, 181, 67));
            graphics.fillRoundRect(0, 0, width, height, 18, 18);
            graphics.setColor(new Color(72, 45, 20));
            graphics.fillOval(width / 4, 12, width / 2, height / 4);
            graphics.fillRoundRect(width / 3, height / 3, width / 3, height / 2, 16, 16);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Arial", Font.BOLD, Math.max(10, width / 7)));
            String label = text == null ? "" : new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            graphics.drawString(label.length() > 8 ? label.substring(0, 8) : label, 8, height - 10);
            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render avatar placeholder", e);
        }
    }
}
