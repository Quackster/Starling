package org.starling.web.feature.me.mail;

import java.time.Instant;

public record MinimailMessage(
        int id,
        int senderId,
        int recipientId,
        String subject,
        String body,
        Instant sentAt,
        boolean read,
        boolean deleted,
        int conversationId,
        String senderUsername,
        String senderFigure,
        String recipientUsername,
        String recipientFigure
) {
}
