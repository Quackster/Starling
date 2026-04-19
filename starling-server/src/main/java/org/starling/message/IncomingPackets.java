package org.starling.message;

/** Client->Server opcode constants. Source: regMsgList() tCmds across all handler classes. */
public final class IncomingPackets {
    /**
     * Creates a new IncomingPackets.
     */
    private IncomingPackets() {}

    // Handshake
    public static final int INIT_CRYPTO = 206;
    public static final int GENERATEKEY = 2002;
    public static final int SECRETKEY = 207;
    public static final int VERSIONCHECK = 1170;
    public static final int UNIQUEID = 813;
    public static final int GET_SESSION_PARAMETERS = 1817;

    // Login
    public static final int TRY_LOGIN = 756;
    public static final int SSO = 204;
    public static final int GET_INFO = 7;
    public static final int GET_CREDITS = 8;
    public static final int PONG = 196;
    public static final int GETAVAILABLEBADGES = 157;
    public static final int GETSELECTEDBADGES = 159;
    public static final int GET_SOUND_SETTING = 228;
    public static final int SET_SOUND_SETTING = 229;
    public static final int GET_POSSIBLE_ACHIEVEMENTS = 370;

    // Messenger / Friend list
    public static final int MESSENGER_INIT = 12;
    public static final int FRIENDLIST_INIT = MESSENGER_INIT;
    public static final int FRIENDLIST_UPDATE = 15;
    public static final int MESSENGER_MARK_READ = 32;
    public static final int MESSENGER_SEND_MESSAGE = 33;
    public static final int INVITE_FRIEND = 34;
    public static final int MESSENGER_ACCEPT_BUDDY = 37;
    public static final int MESSENGER_DECLINE_BUDDY = 38;
    public static final int MESSENGER_REQUEST_BUDDY = 39;
    public static final int MESSENGER_REMOVE_BUDDY = 40;
    public static final int FINDUSER = 41;
    public static final int MESSENGER_GET_MESSAGES = 191;
    public static final int MESSENGER_GET_REQUESTS = 233;
    public static final int FOLLOW_FRIEND = 262;

    // Room
    public static final int ROOM_DIRECTORY = 2;
    public static final int TRYFLAT = 57;
    public static final int GOTOFLAT = 59;
    public static final int G_HMAP = 60;
    public static final int G_USRS = 61;
    public static final int G_OBJS = 62;
    public static final int G_ITEMS = 63;
    public static final int G_STAT = 64;
    public static final int QUIT = 53;
    public static final int WALK = 75;
    public static final int STOP = 88;
    public static final int GETROOMAD = 126;
    public static final int GETINTERST = 182;
    public static final int GET_SPECTATOR_AMOUNT = 216;

    // Navigator
    public static final int SUSERF = 16;
    public static final int SRCHF = 17;
    public static final int GETFVRF = 18;
    public static final int ADD_FAVORITE_ROOM = 19;
    public static final int DEL_FAVORITE_ROOM = 20;
    public static final int GETFLATINFO = 21;
    public static final int DELETEFLAT = 23;
    public static final int UPDATEFLAT = 24;
    public static final int SETFLATINFO = 25;
    public static final int CREATEFLAT = 29;
    public static final int NAVIGATE = 150;
    public static final int GETUSERFLATCATS = 151;
    public static final int GETFLATCAT = 152;
    public static final int SETFLATCAT = 153;
    public static final int GETSPACENODEUSERS = 154;
    public static final int REMOVEALLRIGHTS = 155;
    public static final int GETPARENTCHAIN = 156;
    public static final int GET_RECOMMENDED_ROOMS = 264;
}
