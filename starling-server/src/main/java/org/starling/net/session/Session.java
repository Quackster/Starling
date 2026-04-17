package org.starling.net.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.crypto.DiffieHellman;
import org.starling.crypto.HabboCipher;
import org.starling.game.player.Player;
import org.starling.net.codec.ServerMessage;

/**
 * Per-connection session state. Attached to the Netty Channel via an AttributeKey.
 * Holds DH key exchange state, cipher, and the logged-in Player reference.
 */
public class Session {

    private static final Logger log = LogManager.getLogger(Session.class);
    public static final AttributeKey<Session> KEY = AttributeKey.valueOf("session");

    private final Channel channel;
    private DiffieHellman diffieHellman;
    private HabboCipher inboundCipher;
    private HabboCipher outboundCipher;
    private byte[] inboundSharedSecret;
    private volatile boolean inboundEncrypted;
    private volatile boolean outboundEncrypted;
    private CryptoMode cryptoMode = CryptoMode.NONE;
    private String debugClientPublicKeyHex;
    private String debugServerPrivateKeyHex;
    private String debugServerPublicKeyHex;
    private String debugSharedSecretHex;
    private boolean encryptedDiagnosticsContextLogged;
    private Player player;
    private RoomPresence roomPresence = RoomPresence.none();

    public Session(Channel channel) {
        this.channel = channel;
    }

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

    private static boolean shouldTraceWirePacket(int opcode) {
        return true;
    }

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

    // --- DH / Crypto ---

    public DiffieHellman getDiffieHellman() {
        return diffieHellman;
    }

    public void setDiffieHellman(DiffieHellman dh) {
        this.diffieHellman = dh;
    }

    public HabboCipher getInboundCipher() {
        return inboundCipher;
    }

    public void setInboundCipher(HabboCipher inboundCipher) {
        this.inboundCipher = inboundCipher;
    }

    public HabboCipher getOutboundCipher() {
        return outboundCipher;
    }

    public void setOutboundCipher(HabboCipher outboundCipher) {
        this.outboundCipher = outboundCipher;
    }

    public byte[] getInboundSharedSecret() {
        return inboundSharedSecret == null ? null : inboundSharedSecret.clone();
    }

    public void setInboundSharedSecret(byte[] inboundSharedSecret) {
        this.inboundSharedSecret = inboundSharedSecret == null ? null : inboundSharedSecret.clone();
    }

    public boolean isInboundEncrypted() {
        return inboundEncrypted;
    }

    public void setInboundEncrypted(boolean inboundEncrypted) {
        this.inboundEncrypted = inboundEncrypted;
    }

    public boolean isOutboundEncrypted() {
        return outboundEncrypted;
    }

    public void setOutboundEncrypted(boolean outboundEncrypted) {
        this.outboundEncrypted = outboundEncrypted;
    }

    public CryptoMode getCryptoMode() {
        return cryptoMode;
    }

    public void setCryptoMode(CryptoMode cryptoMode) {
        this.cryptoMode = cryptoMode == null ? CryptoMode.NONE : cryptoMode;
    }

    public void resetCrypto() {
        this.diffieHellman = null;
        this.inboundCipher = null;
        this.outboundCipher = null;
        this.inboundSharedSecret = null;
        this.inboundEncrypted = false;
        this.outboundEncrypted = false;
        this.cryptoMode = CryptoMode.NONE;
        this.debugClientPublicKeyHex = null;
        this.debugServerPrivateKeyHex = null;
        this.debugServerPublicKeyHex = null;
        this.debugSharedSecretHex = null;
        this.encryptedDiagnosticsContextLogged = false;
    }

    /**
     * Backward-compatible alias for the inbound cipher while the rest of the
     * server migrates to explicit inbound/outbound naming.
     */
    public HabboCipher getCipher() {
        return getInboundCipher();
    }

    public void setCipher(HabboCipher cipher) {
        setInboundCipher(cipher);
    }

    public boolean isEncrypted() {
        return isInboundEncrypted();
    }

    public void setEncrypted(boolean encrypted) {
        setInboundEncrypted(encrypted);
    }

    public void setDebugDhMaterial(
            String clientPublicKeyHex,
            String serverPrivateKeyHex,
            String serverPublicKeyHex,
            String sharedSecretHex
    ) {
        this.debugClientPublicKeyHex = clientPublicKeyHex;
        this.debugServerPrivateKeyHex = serverPrivateKeyHex;
        this.debugServerPublicKeyHex = serverPublicKeyHex;
        this.debugSharedSecretHex = sharedSecretHex;
        this.encryptedDiagnosticsContextLogged = false;
    }

    public String getDebugClientPublicKeyHex() {
        return debugClientPublicKeyHex;
    }

    public String getDebugServerPrivateKeyHex() {
        return debugServerPrivateKeyHex;
    }

    public String getDebugServerPublicKeyHex() {
        return debugServerPublicKeyHex;
    }

    public String getDebugSharedSecretHex() {
        return debugSharedSecretHex;
    }

    public boolean markEncryptedDiagnosticsContextLogged() {
        boolean alreadyLogged = encryptedDiagnosticsContextLogged;
        encryptedDiagnosticsContextLogged = true;
        return alreadyLogged;
    }

    // --- Player ---

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isLoggedIn() {
        return player != null;
    }

    public String getRemoteAddress() {
        return channel.remoteAddress() != null ? channel.remoteAddress().toString() : "unknown";
    }

    public RoomPresence getRoomPresence() {
        return roomPresence;
    }

    public void setRoomPresence(RoomPresence roomPresence) {
        this.roomPresence = roomPresence == null ? RoomPresence.none() : roomPresence;
    }

    public record RoomPresence(RoomPhase phase, RoomType type, int roomId, String marker, int doorId) {
        public static RoomPresence none() {
            return new RoomPresence(RoomPhase.NONE, RoomType.PRIVATE, 0, "", 0);
        }

        public static RoomPresence pendingPrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.PENDING_PRIVATE_ENTRY, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        public static RoomPresence activePrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        public static RoomPresence activePublic(int roomId, String marker, int doorId) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PUBLIC, roomId,
                    marker == null ? "" : marker, doorId);
        }

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
