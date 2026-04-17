package org.starling.message;

/** Server->Client opcode constants. Source: regMsgList() tMsgs across all handler classes. */
public final class OutgoingPackets {
    private OutgoingPackets() {}

    // Handshake
    public static final int HELLO = 0;
    public static final int SERVER_SECRET_KEY = 1;
    public static final int CRYPTO_PARAMETERS = 277;
    public static final int END_OF_CRYPTO_PARAMS = 278;
    public static final int SESSION_PARAMETERS = 257;

    // Login
    public static final int USER_RIGHTS = 2;
    public static final int LOGIN_OK = 3;
    public static final int USER_OBJECT = 5;
    public static final int CREDIT_BALANCE = 6;
    public static final int ERROR = 33;
    public static final int PING = 50;
    public static final int AVAILABLE_BADGES = 229;
    public static final int SOUND_SETTING = 308;
    public static final int POSSIBLE_ACHIEVEMENTS = 436;

    // Messenger / Friend list
    public static final int FRIEND_LIST_INIT = 12;

    // Room
    public static final int OPC_OK = 19;
    public static final int HOTEL_VIEW = 18;
    public static final int LOGOUT = 29;
    public static final int ROOM_USERS = 28;
    public static final int ROOM_OBJECTS = 30;
    public static final int HEIGHTMAP = 31;
    public static final int ROOM_ACTIVE_OBJECTS = 32;
    public static final int STATUS = 34;
    public static final int FLAT_LETIN = 41;
    public static final int ROOM_RIGHTS_CONTROLLER = 42;
    public static final int FLAT_PROPERTY = 46;
    public static final int ROOM_RIGHTS_OWNER = 47;
    public static final int ROOM_ITEMS = 45;
    public static final int ROOM_READY = 69;
    public static final int ROOM_URL = 166;
    public static final int ROOM_AD = 208;
    public static final int INTERSTITIAL_DATA = 258;
    public static final int SPECTATOR_AMOUNT = 298;

    // Navigator
    public static final int FLAT_RESULTS = 16;
    public static final int FLAT_INFO = 54;
    public static final int SEARCH_FLAT_RESULTS = 55;
    public static final int FLAT_CREATED = 59;
    public static final int NO_FLATS_FOR_USER = 57;
    public static final int NO_FLATS = 58;
    public static final int FAVORITE_ROOM_RESULTS = 61;
    public static final int NAV_NODE_INFO = 220;
    public static final int USER_FLAT_CATS = 221;
    public static final int FLAT_CATEGORY = 222;
    public static final int SPACE_NODE_USERS = 223;
    public static final int SUCCESS = 225;
    public static final int FAILURE = 226;
    public static final int PARENT_CHAIN = 227;
    public static final int RECOMMENDED_ROOM_LIST = 351;
}
