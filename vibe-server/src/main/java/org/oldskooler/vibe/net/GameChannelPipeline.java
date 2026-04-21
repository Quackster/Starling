package org.oldskooler.vibe.net;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.oldskooler.vibe.crypto.HabboCipher;
import org.oldskooler.vibe.message.MessageRouter;
import org.oldskooler.vibe.net.codec.GameDecoder;
import org.oldskooler.vibe.net.codec.GameEncoder;
import org.oldskooler.vibe.net.codec.InboundCryptoDecoder;
import org.oldskooler.vibe.net.codec.OutboundCryptoEncoder;
import org.oldskooler.vibe.net.session.Session;

public final class GameChannelPipeline {

    public static final String INBOUND_CRYPTO = "inboundCrypto";
    public static final String DECODER = "decoder";
    public static final String OUTBOUND_CRYPTO = "outboundCrypto";
    public static final String ENCODER = "encoder";
    public static final String HANDLER = "handler";

    /**
     * Creates a new GameChannelPipeline.
     */
    private GameChannelPipeline() {}

    /**
     * Configures.
     * @param channel the channel value
     * @param messageRouter the message router value
     */
    public static void configure(SocketChannel channel, MessageRouter messageRouter) {
        channel.pipeline()
                .addLast(DECODER, new GameDecoder())
                .addLast(ENCODER, new GameEncoder())
                .addLast(HANDLER, new GameChannelHandler(messageRouter));
    }

    /**
     * Resets crypto.
     * @param session the session value
     */
    public static void resetCrypto(Session session) {
        ChannelPipeline pipeline = session.getChannel().pipeline();
        removeIfPresent(pipeline, INBOUND_CRYPTO);
        removeIfPresent(pipeline, OUTBOUND_CRYPTO);
        session.resetCrypto();
    }

    /**
     * Enables inbound crypto.
     * @param session the session value
     * @param cipher the cipher value
     */
    public static void enableInboundCrypto(Session session, HabboCipher cipher) {
        session.setInboundCipher(cipher);
        session.setInboundEncrypted(true);
        addBefore(session.getChannel().pipeline(), DECODER, GameDecoder.class, INBOUND_CRYPTO, new InboundCryptoDecoder());
    }

    /**
     * Disables inbound crypto.
     * @param session the session value
     */
    public static void disableInboundCrypto(Session session) {
        removeIfPresent(session.getChannel().pipeline(), INBOUND_CRYPTO);
        session.setInboundCipher(null);
        session.setInboundEncrypted(false);
    }

    /**
     * Enables outbound crypto.
     * @param session the session value
     * @param cipher the cipher value
     */
    public static void enableOutboundCrypto(Session session, HabboCipher cipher) {
        session.setOutboundCipher(cipher);
        session.setOutboundEncrypted(true);
        addBefore(session.getChannel().pipeline(), ENCODER, GameEncoder.class, OUTBOUND_CRYPTO, new OutboundCryptoEncoder());
    }

    /**
     * Disables outbound crypto.
     * @param session the session value
     */
    public static void disableOutboundCrypto(Session session) {
        removeIfPresent(session.getChannel().pipeline(), OUTBOUND_CRYPTO);
        session.setOutboundCipher(null);
        session.setOutboundEncrypted(false);
    }

    /**
     * Adds before.
     * @param pipeline the pipeline value
     * @param baseName the base name value
     * @param baseType the base type value
     * @param handlerName the handler name value
     * @param handler the handler value
     */
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

    /**
     * Removes if present.
     * @param pipeline the pipeline value
     * @param handlerName the handler name value
     */
    private static void removeIfPresent(ChannelPipeline pipeline, String handlerName) {
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }
}
