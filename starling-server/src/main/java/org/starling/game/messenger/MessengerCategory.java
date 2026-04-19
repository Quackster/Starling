package org.starling.game.messenger;

/**
 * Immutable messenger category metadata.
 */
public final class MessengerCategory {

    private final int id;
    private final int userId;
    private final String name;

    /**
     * Creates a new MessengerCategory.
     * @param id the id value
     * @param userId the user id value
     * @param name the name value
     */
    public MessengerCategory(int id, int userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name == null ? "" : name;
    }

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the user id.
     * @return the user id
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Returns the name.
     * @return the name
     */
    public String getName() {
        return name;
    }
}
