package org.starling.web.user;

record UserSessionCookie(String tokenHash, long expiresAt, String signature) {
}
