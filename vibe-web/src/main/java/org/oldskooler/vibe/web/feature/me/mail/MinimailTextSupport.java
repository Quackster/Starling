package org.oldskooler.vibe.web.feature.me.mail;

import org.jsoup.nodes.Entities;

final class MinimailTextSupport {

    private MinimailTextSupport() {
    }

    static String replySubject(String originalSubject) {
        String normalized = valueOrDefault(originalSubject, "Message");
        return normalized.regionMatches(true, 0, "Re:", 0, 3) ? normalized : "Re: " + normalized;
    }

    static String valueOrDefault(String preferredValue, String fallback) {
        String normalized = preferredValue == null ? "" : preferredValue.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    static String previewHtml(String bodyRaw) {
        return Entities.escape(bodyRaw == null ? "" : bodyRaw).replace("\n", "<br />");
    }
}
