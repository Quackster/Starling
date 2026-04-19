package org.starling.web.cms.bootstrap.seed.data;

import java.util.List;

public final class CmsSocialSeedCatalog {

    private static final String MINIMAIL_SUBJECT = "Welcome to Starling";
    private static final String MINIMAIL_BODY = """
            Thanks for logging in.

            Minimail is now available from your /me page, so you can keep in touch without leaving the hotel site.
            """;
    private static final List<MessengerUserSeed> FRIENDS = List.of(
            new MessengerUserSeed("RetroGuide", "Always around if you need a hand.", true, 5),
            new MessengerUserSeed("PixelPilot", "Lobby lurker and room hopper.", true, 45),
            new MessengerUserSeed("Newsie", "Posting the latest hotel buzz.", false, 7200)
    );
    private static final MessengerUserSeed REQUESTER =
            new MessengerUserSeed("LobbyScout", "Let's hang out in the Welcome Lounge.", false, 300);

    /**
     * Creates a new CmsSocialSeedCatalog.
     */
    private CmsSocialSeedCatalog() {}

    /**
     * Returns the bootstrap minimail subject.
     * @return the subject
     */
    public static String minimailSubject() {
        return MINIMAIL_SUBJECT;
    }

    /**
     * Returns the bootstrap minimail body.
     * @return the body
     */
    public static String minimailBody() {
        return MINIMAIL_BODY;
    }

    /**
     * Returns the bootstrap messenger friends.
     * @return the messenger friends
     */
    public static List<MessengerUserSeed> friends() {
        return FRIENDS;
    }

    /**
     * Returns the bootstrap messenger requester.
     * @return the requester
     */
    public static MessengerUserSeed requester() {
        return REQUESTER;
    }

    public record MessengerUserSeed(String username, String motto, boolean online, long secondsAgo) {}
}
