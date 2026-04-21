package org.oldskooler.vibe.net.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.crypto.HabboCipher;
import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.net.codec.ServerMessage;

/**
 * Per-connection session state. Attached to the Netty Channel via an
 * AttributeKey and used by the Netty pipeline handlers.
 */
public class Session {

    private static final Logger log = LogManager.getLogger(Session.class);
    public static final AttributeKey<Session> KEY = AttributeKey.valueOf("session");

    private final Channel channel;
    private HabboCipher inboundCipher;
    private HabboCipher outboundCipher;
    private volatile boolean inboundEncrypted;
    private volatile boolean outboundEncrypted;
    private CryptoMode cryptoMode = CryptoMode.NONE;
    private Player player;
    private RoomPresence roomPresence = RoomPresence.none();

    /**
     * Creates a new Session.
     * @param channel the channel value
     */
    public Session(Channel channel) {
        this.channel = channel;
    }

    /**
     * Returns the channel.
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /** Send a server message through the Netty pipeline. */
    public void send(ServerMessage msg) {
        if (channel.isActive()) {
            byte[] wireBytes = msg.toBytes();
            log.debug(">>> [{}] opcode={} ({})", getRemoteAddress(), msg.getOpcode(), msg.headerString());
            if (shouldTraceWirePacket(msg.getOpcode())) {
                log.debug(">>> [{}] raw={}", getRemoteAddress(), formatWireBytes(wireBytes));
            }
            channel.writeAndFlush(msg);
        }
    }

    /**
     * Shoulds trace wire packet.
     * @param opcode the opcode value
     * @return the result of this operation
     */
    private static boolean shouldTraceWirePacket(int opcode) {
        return true;
    }

    /**
     * Formats wire bytes.
     * @param wireBytes the wire bytes value
     * @return the resulting format wire bytes
     */
    private static String formatWireBytes(byte[] wireBytes) {
        StringBuilder formatted = new StringBuilder(wireBytes.length * 3);
        for (byte wireByte : wireBytes) {
            int value = wireByte & 0xFF;
            if (value >= 32 && value <= 126) {
                formatted.append((char) value);
            } else {
                formatted.append('[').append(value).append(']');
            }
        }
        return formatted.toString();
    }

    // --- Crypto ---

    /**
     * Returns the inbound cipher.
     * @return the inbound cipher
     */
    public HabboCipher getInboundCipher() {
        return inboundCipher;
    }

    /**
     * Sets the inbound cipher.
     * @param inboundCipher the inbound cipher value
     */
    public void setInboundCipher(HabboCipher inboundCipher) {
        this.inboundCipher = inboundCipher;
    }

    /**
     * Returns the outbound cipher.
     * @return the outbound cipher
     */
    public HabboCipher getOutboundCipher() {
        return outboundCipher;
    }

    /**
     * Sets the outbound cipher.
     * @param outboundCipher the outbound cipher value
     */
    public void setOutboundCipher(HabboCipher outboundCipher) {
        this.outboundCipher = outboundCipher;
    }

    /**
     * Returns whether inbound encrypted.
     * @return whether inbound encrypted
     */
    public boolean isInboundEncrypted() {
        return inboundEncrypted;
    }

    /**
     * Sets the inbound encrypted.
     * @param inboundEncrypted the inbound encrypted value
     */
    public void setInboundEncrypted(boolean inboundEncrypted) {
        this.inboundEncrypted = inboundEncrypted;
    }

    /**
     * Returns whether outbound encrypted.
     * @return whether outbound encrypted
     */
    public boolean isOutboundEncrypted() {
        return outboundEncrypted;
    }

    /**
     * Sets the outbound encrypted.
     * @param outboundEncrypted the outbound encrypted value
     */
    public void setOutboundEncrypted(boolean outboundEncrypted) {
        this.outboundEncrypted = outboundEncrypted;
    }

    /**
     * Returns the crypto mode.
     * @return the crypto mode
     */
    public CryptoMode getCryptoMode() {
        return cryptoMode;
    }

    /**
     * Sets the crypto mode.
     * @param cryptoMode the crypto mode value
     */
    public void setCryptoMode(CryptoMode cryptoMode) {
        this.cryptoMode = cryptoMode == null ? CryptoMode.NONE : cryptoMode;
    }

    /**
     * Resets crypto.
     */
    public void resetCrypto() {
        this.inboundCipher = null;
        this.outboundCipher = null;
        this.inboundEncrypted = false;
        this.outboundEncrypted = false;
        this.cryptoMode = CryptoMode.NONE;
    }

    // --- Player ---

    /**
     * Returns the player.
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the player.
     * @param player the player value
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Returns whether logged in.
     * @return whether logged in
     */
    public boolean isLoggedIn() {
        return player != null;
    }

    /**
     * Returns the remote address.
     * @return the remote address
     */
    public String getRemoteAddress() {
        return channel.remoteAddress() != null ? channel.remoteAddress().toString() : "unknown";
    }

    /**
     * Returns the room presence.
     * @return the room presence
     */
    public RoomPresence getRoomPresence() {
        return roomPresence;
    }

    /**
     * Sets the room presence.
     * @param roomPresence the room presence value
     */
    public void setRoomPresence(RoomPresence roomPresence) {
        this.roomPresence = roomPresence == null ? RoomPresence.none() : roomPresence;
    }

    public record RoomPresence(RoomPhase phase, RoomType type, int roomId, String marker, int doorId) {
        /**
         * Nones.
         * @return the result of this operation
         */
        public static RoomPresence none() {
            return new RoomPresence(RoomPhase.NONE, RoomType.PRIVATE, 0, "", 0);
        }

        /**
         * Pendings private.
         * @param roomId the room id value
         * @param marker the marker value
         * @return the result of this operation
         */
        public static RoomPresence pendingPrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.PENDING_PRIVATE_ENTRY, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        /**
         * Actives private.
         * @param roomId the room id value
         * @param marker the marker value
         * @return the result of this operation
         */
        public static RoomPresence activePrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        /**
         * Actives public.
         * @param roomId the room id value
         * @param marker the marker value
         * @param doorId the door id value
         * @return the result of this operation
         */
        public static RoomPresence activePublic(int roomId, String marker, int doorId) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PUBLIC, roomId,
                    marker == null ? "" : marker, doorId);
        }

        /**
         * Actives.
         * @return the result of this operation
         */
        public boolean active() {
            return phase == RoomPhase.ACTIVE;
        }
    }

    public enum RoomPhase {
        NONE,
        PENDING_PRIVATE_ENTRY,
        ACTIVE
    }

    public enum RoomType {
        PRIVATE,
        PUBLIC
    }

    public enum CryptoMode {
        NONE,
        INIT
    }
}
