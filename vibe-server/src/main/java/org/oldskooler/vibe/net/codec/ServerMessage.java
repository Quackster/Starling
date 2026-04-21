package org.oldskooler.vibe.net.codec;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Builder for server->client messages.
 *
 * Server messages are sent as: [2-byte B64 header][params...][0x01]
 * Strings are terminated by 0x02 (NOT length-prefixed).
 * Integers use VL64 encoding.
 */
public class ServerMessage {

    private final int opcode;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();

    /**
     * Creates a new ServerMessage.
     * @param opcode the opcode value
     */
    public ServerMessage(int opcode) {
        this.opcode = opcode;
    }

    /**
     * Returns the opcode.
     * @return the opcode
     */
    public int getOpcode() {
        return opcode;
    }

    /** Write a 0x02-terminated string. */
    public ServerMessage writeString(String value) {
        if (value != null) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            body.write(bytes, 0, bytes.length);
        }
        body.write(0x02);
        return this;
    }

    /** Write a VL64-encoded integer. */
    public ServerMessage writeInt(int value) {
        byte[] encoded = VL64Encoding.encode(value);
        body.write(encoded, 0, encoded.length);
        return this;
    }

    /** Write a single B64-masked boolean. */
    public ServerMessage writeBoolean(boolean value) {
        body.write(64 | (value ? 1 : 0));
        return this;
    }

    /** Write a 2-byte B64 short value. */
    public ServerMessage writeShort(int value) {
        byte[] encoded = Base64Encoding.encodeShort(value);
        body.write(encoded, 0, encoded.length);
        return this;
    }

    /** Write raw bytes (no encoding). */
    public ServerMessage writeRaw(String value) {
        if (value != null) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            body.write(bytes, 0, bytes.length);
        }
        return this;
    }

    /** Write raw bytes. */
    public ServerMessage writeRawBytes(byte[] bytes) {
        body.write(bytes, 0, bytes.length);
        return this;
    }

    /**
     * Build the complete wire-format message: [2-byte header][body][0x01]
     */
    public byte[] toBytes() {
        byte[] header = Base64Encoding.encodeHeader(opcode);
        byte[] bodyBytes = body.toByteArray();

        byte[] result = new byte[2 + bodyBytes.length + 1];
        result[0] = header[0];
        result[1] = header[1];
        System.arraycopy(bodyBytes, 0, result, 2, bodyBytes.length);
        result[result.length - 1] = 0x01; // terminator

        return result;
    }

    /** Opcode as a 2-char B64 header string, for logging. */
    public String headerString() {
        byte[] h = Base64Encoding.encodeHeader(opcode);
        return new String(h, StandardCharsets.US_ASCII);
    }

    /**
     * Returns the string representation.
     * @return the result of this operation
     */
    @Override
    public String toString() {
        return "ServerMessage{opcode=" + opcode + " [" + headerString() + "], bodyLen=" + body.size() + "}";
    }
}
