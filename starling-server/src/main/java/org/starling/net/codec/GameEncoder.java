package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encodes server->client messages.
 * Server always sends plaintext (serverToClient encryption = false).
 * The ServerMessage.toBytes() already produces the complete wire format:
 * [2-byte B64 header][params][0x01 terminator]
 */
public class GameEncoder extends MessageToByteEncoder<ServerMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        out.writeBytes(msg.toBytes());
    }
}
