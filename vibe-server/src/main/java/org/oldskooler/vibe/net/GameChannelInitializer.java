package org.oldskooler.vibe.net;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.oldskooler.vibe.message.MessageRouter;

public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final MessageRouter messageRouter;

    /**
     * Creates a new GameChannelInitializer.
     * @param messageRouter the message router value
     */
    public GameChannelInitializer(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    /**
     * Inits channel.
     * @param ch the ch value
     */
    @Override
    protected void initChannel(SocketChannel ch) {
        GameChannelPipeline.configure(ch, messageRouter);
    }
}
