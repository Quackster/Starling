package org.starling.web.theme;

import io.pebbletemplates.pebble.loader.Loader;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class ThemeTemplateLoader implements Loader<String> {

    private final ThemeResourceResolver themeResourceResolver;
    private Charset charset;
    private String prefix;
    private String suffix;

    /**
     * Creates a new ThemeTemplateLoader.
     * @param themeResourceResolver the resolver value
     */
    public ThemeTemplateLoader(ThemeResourceResolver themeResourceResolver) {
        this.themeResourceResolver = themeResourceResolver;
        this.charset = StandardCharsets.UTF_8;
        this.prefix = "";
        this.suffix = ".peb";
    }

    @Override
    public Reader getReader(String cacheKey) {
        return themeResourceResolver.openTemplateReader(cacheKey, charset);
    }

    @Override
    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = normalizePath(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix == null ? "" : suffix;
    }

    @Override
    public String resolveRelativePath(String relativePath, String anchorPath) {
        String normalizedRelativePath = relativePath.replace('\\', '/');
        if (!normalizedRelativePath.startsWith("./") && !normalizedRelativePath.startsWith("../")) {
            return createCacheKey(normalizedRelativePath);
        }

        Path anchor = Path.of(anchorPath);
        Path parent = anchor.getParent();
        Path resolved = parent == null ? Path.of(normalizedRelativePath) : parent.resolve(normalizedRelativePath);
        return createCacheKey(resolved.normalize().toString().replace('\\', '/'));
    }

    @Override
    public String createCacheKey(String templateName) {
        String normalizedTemplateName = normalizePath(templateName);
        if (!prefix.isBlank() && !normalizedTemplateName.startsWith(prefix + "/")) {
            normalizedTemplateName = prefix + "/" + normalizedTemplateName;
        }

        if (!suffix.isBlank() && !normalizedTemplateName.endsWith(".peb") && !normalizedTemplateName.endsWith(".tpl")) {
            normalizedTemplateName += suffix;
        }

        return normalizedTemplateName;
    }

    @Override
    public boolean resourceExists(String templateName) {
        return themeResourceResolver.templateExists(createCacheKey(templateName));
    }

    /**
     * Normalizes a relative resource path.
     * @param path the raw path
     * @return the normalized path
     */
    private String normalizePath(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
