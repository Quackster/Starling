package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.starling.crypto.HabboCipher;
import org.starling.net.session.Session;

/**
 * Encodes server->client messages.
 * Plaintext wire format is [2-byte B64 header][params][0x01 terminator].
 * When server->client crypto is enabled, the full frame is encrypted and
 * hex-encoded to match the Director SECRETKEY transport.
 */
public class GameEncoder extends MessageToByteEncoder<ServerMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) {
        Session session = ctx.channel().attr(Session.KEY).get();
        byte[] plaintext = msg.toBytes();
        if (session == null || !session.isOutboundEncrypted()) {
            out.writeBytes(plaintext);
            return;
        }

        HabboCipher cipher = session.getOutboundCipher();
        if (cipher == null) {
            out.writeBytes(plaintext);
            return;
        }

        out.writeBytes(cipher.encryptToHex(plaintext));
    }
}
