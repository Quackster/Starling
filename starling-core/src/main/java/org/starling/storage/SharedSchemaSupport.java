package org.starling.storage;

import java.sql.Statement;

public final class SharedSchemaSupport {

    /**
     * Creates a new SharedSchemaSupport.
     */
    private SharedSchemaSupport() {}

    /**
     * Ensures the shared messenger tables exist.
     * @param statement the statement value
     */
    public static void ensureMessengerSchema(Statement statement) {
        try {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messenger_friends (
                        id INT NOT NULL AUTO_INCREMENT,
                        from_id INT NOT NULL,
                        to_id INT NOT NULL,
                        category_id INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_messenger_friends_from_to (from_id, to_id),
                        KEY idx_messenger_friends_to (to_id),
                        KEY idx_messenger_friends_from (from_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messenger_requests (
                        id INT NOT NULL AUTO_INCREMENT,
                        to_id INT NOT NULL,
                        from_id INT NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_messenger_requests_to_from (to_id, from_id),
                        KEY idx_messenger_requests_to (to_id),
                        KEY idx_messenger_requests_from (from_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messenger_messages (
                        id INT NOT NULL AUTO_INCREMENT,
                        receiver_id INT NOT NULL,
                        sender_id INT NOT NULL,
                        unread INT NOT NULL DEFAULT 1,
                        body TEXT NOT NULL,
                        date BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id),
                        KEY idx_messenger_messages_receiver_unread (receiver_id, unread),
                        KEY idx_messenger_messages_sender (sender_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messenger_categories (
                        id INT NOT NULL AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        name VARCHAR(64) NOT NULL,
                        PRIMARY KEY (id),
                        KEY idx_messenger_categories_user (user_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure shared messenger schema", e);
        }
    }
}
