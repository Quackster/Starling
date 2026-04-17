package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Serializes server->client messages into their plaintext wire format.
 * Any optional encryption is handled by a later outbound handler.
 */
public class GameEncoder extends MessageToByteEncoder<ServerMessage> {

    /**
     * Encodes.
     * @param ctx the ctx value
     * @param msg the msg value
     * @param out the out value
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        out.writeBytes(msg.toBytes());
    }
}
