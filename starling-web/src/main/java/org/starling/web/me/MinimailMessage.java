package org.starling.web.me;

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
