package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * Represents a parsed client->server message.
 *
 * Client messages arrive as: [3-byte B64 length][2-byte B64 header][params...]
 * This class wraps the opcode and body (params portion) after decoding.
 *
 * Client sends strings as: [2-byte B64 length][UTF-8 bytes]
 * Client sends integers as: VL64 encoded
 */
public class ClientMessage {

    private final int opcode;
    private final ByteBuf body;

    public ClientMessage(int opcode, ByteBuf body) {
        this.opcode = opcode;
        this.body = body;
    }

    public int getOpcode() {
        return opcode;
    }

    public boolean hasRemaining() {
        return body.isReadable();
    }

    public int remainingBytes() {
        return body.readableBytes();
    }

    /** Read a B64-length-prefixed string (client->server format). */
    public String readString() {
        return Base64Encoding.readB64String(body);
    }

    /** Read a VL64-encoded integer. */
    public int readInt() {
        return VL64Encoding.decode(body);
    }

    /** Read a single B64-masked boolean. */
    public boolean readBoolean() {
        return (body.readByte() & 63) != 0;
    }

    /** Read a 2-byte B64 short. */
    public int readShort() {
        return Base64Encoding.readB64Short(body);
    }

    /** Read the entire remaining body as a raw UTF-8 string. */
    public String readRawBody() {
        if (!body.isReadable()) return "";
        byte[] bytes = new byte[body.readableBytes()];
        body.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Release the underlying ByteBuf. */
    public void release() {
        if (body.refCnt() > 0) {
            body.release();
        }
    }

    /** Opcode as a 2-char B64 header string, for logging. */
    public String headerString() {
        byte[] h = Base64Encoding.encodeHeader(opcode);
        return new String(h, StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        return "ClientMessage{opcode=" + opcode + " [" + headerString() + "], bodyLen=" + body.readableBytes() + "}";
    }
}
