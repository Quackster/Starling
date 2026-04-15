package org.starling.net.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.crypto.DiffieHellman;
import org.starling.crypto.HabboCipher;
import org.starling.game.Player;
import org.starling.message.OutgoingPackets;
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
    private HabboCipher cipher;
    private volatile boolean encrypted;
    private Player player;
    private RoomState roomState = RoomState.empty();

    public Session(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    /** Send a server message (always plaintext). */
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

    public HabboCipher getCipher() {
        return cipher;
    }

    public void setCipher(HabboCipher cipher) {
        this.cipher = cipher;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
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

    public RoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(RoomState roomState) {
        this.roomState = roomState == null ? RoomState.empty() : roomState;
    }

    public record RoomState(boolean active, boolean publicRoom, int roomId, String marker, int doorId) {
        public static RoomState empty() {
            return new RoomState(false, false, 0, "", 0);
        }
    }
}
