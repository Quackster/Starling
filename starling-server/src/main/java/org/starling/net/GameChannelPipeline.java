package org.starling.net;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.starling.crypto.HabboCipher;
import org.starling.message.MessageRouter;
import org.starling.net.codec.GameDecoder;
import org.starling.net.codec.GameEncoder;
import org.starling.net.codec.InboundCryptoDecoder;
import org.starling.net.codec.OutboundCryptoEncoder;
import org.starling.net.session.Session;

public final class GameChannelPipeline {

    public static final String INBOUND_CRYPTO = "inboundCrypto";
    public static final String DECODER = "decoder";
    public static final String OUTBOUND_CRYPTO = "outboundCrypto";
    public static final String ENCODER = "encoder";
    public static final String HANDLER = "handler";

    private GameChannelPipeline() {}

    public static void configure(SocketChannel channel, MessageRouter messageRouter) {
        channel.pipeline()
                .addLast(DECODER, new GameDecoder())
                .addLast(ENCODER, new GameEncoder())
                .addLast(HANDLER, new GameChannelHandler(messageRouter));
    }

    public static void resetCrypto(Session session) {
        ChannelPipeline pipeline = session.getChannel().pipeline();
        removeIfPresent(pipeline, INBOUND_CRYPTO);
        removeIfPresent(pipeline, OUTBOUND_CRYPTO);
        session.resetCrypto();
    }

    public static void enableInboundCrypto(Session session, HabboCipher cipher, byte[] sharedSecret) {
        session.setInboundCipher(cipher);
        session.setInboundSharedSecret(sharedSecret);
        session.setInboundEncrypted(true);
        addBefore(session.getChannel().pipeline(), DECODER, GameDecoder.class, INBOUND_CRYPTO, new InboundCryptoDecoder());
    }

    public static void disableInboundCrypto(Session session) {
        removeIfPresent(session.getChannel().pipeline(), INBOUND_CRYPTO);
        session.setInboundCipher(null);
        session.setInboundSharedSecret(null);
        session.setInboundEncrypted(false);
    }

    public static void enableOutboundCrypto(Session session, HabboCipher cipher) {
        session.setOutboundCipher(cipher);
        session.setOutboundEncrypted(true);
        addBefore(session.getChannel().pipeline(), ENCODER, GameEncoder.class, OUTBOUND_CRYPTO, new OutboundCryptoEncoder());
    }

    public static void disableOutboundCrypto(Session session) {
        removeIfPresent(session.getChannel().pipeline(), OUTBOUND_CRYPTO);
        session.setOutboundCipher(null);
        session.setOutboundEncrypted(false);
    }

    private static void addBefore(
            ChannelPipeline pipeline,
            String baseName,
            Class<? extends ChannelHandler> baseType,
            String handlerName,
            ChannelHandler handler
    ) {
        if (pipeline.get(handlerName) != null) {
            return;
        }

        if (pipeline.get(baseName) != null) {
            pipeline.addBefore(baseName, handlerName, handler);
            return;
        }

        ChannelHandlerContext baseContext = pipeline.context(baseType);
        if (baseContext != null) {
            pipeline.addBefore(baseContext.name(), handlerName, handler);
            return;
        }

        pipeline.addLast(handlerName, handler);
    }

    private static void removeIfPresent(ChannelPipeline pipeline, String handlerName) {
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }
}
