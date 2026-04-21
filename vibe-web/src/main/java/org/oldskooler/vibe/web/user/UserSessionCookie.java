package org.oldskooler.vibe.web.user;

record UserSessionCookie(String tokenHash, long expiresAtMillis, long lastActivityAtMillis, boolean persistent, String signature) {
}
