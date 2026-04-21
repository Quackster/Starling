package org.oldskooler.vibe.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.oldskooler.vibe.crypto.HabboCipher;
import org.oldskooler.vibe.net.GameChannelPipeline;
import org.oldskooler.vibe.net.session.Session;

/**
 * Encrypts plaintext outbound frames after {@link GameEncoder} has serialized
 * them into their on-the-wire byte layout.
 */
public class OutboundCryptoEncoder extends MessageToByteEncoder<ByteBuf> {

    /**
     * Encodes.
     * @param ctx the ctx value
     * @param msg the msg value
     * @param out the out value
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        Session session = ctx.channel().attr(Session.KEY).get();
        if (session == null || !session.isOutboundEncrypted()) {
            out.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
            return;
        }

        HabboCipher cipher = session.getOutboundCipher();
        if (cipher == null) {
            GameChannelPipeline.disableOutboundCrypto(session);
            out.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
            return;
        }

        byte[] plaintext = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), plaintext);
        out.writeBytes(cipher.encryptToHex(plaintext));
    }
}
