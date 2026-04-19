package org.starling.storage;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.entity.MessengerCategoryEntity;
import org.starling.storage.entity.MessengerFriendEntity;
import org.starling.storage.entity.MessengerMessageEntity;
import org.starling.storage.entity.MessengerRequestEntity;

public final class SharedSchemaSupport {

    /**
     * Creates a new SharedSchemaSupport.
     */
    private SharedSchemaSupport() {}

    /**
     * Ensures the shared messenger tables exist.
     * @param context the context value
     */
    public static void ensureMessengerSchema(DbContext context) {
        context.createTables(
                MessengerFriendEntity.class,
                MessengerRequestEntity.class,
                MessengerMessageEntity.class,
                MessengerCategoryEntity.class
        );

        DatabaseSupport.ensureUniqueIndex(context.conn(), "messenger_friends", "uk_messenger_friends_from_to", "from_id", "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_friends", "idx_messenger_friends_to", false, "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_friends", "idx_messenger_friends_from", false, "from_id");

        DatabaseSupport.ensureUniqueIndex(context.conn(), "messenger_requests", "uk_messenger_requests_to_from", "to_id", "from_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_requests", "idx_messenger_requests_to", false, "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_requests", "idx_messenger_requests_from", false, "from_id");

        DatabaseSupport.ensureIndex(context.conn(), "messenger_messages", "idx_messenger_messages_receiver_unread", false, "receiver_id", "unread");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_messages", "idx_messenger_messages_sender", false, "sender_id");

        DatabaseSupport.ensureIndex(context.conn(), "messenger_categories", "idx_messenger_categories_user", false, "user_id");
    }
}
