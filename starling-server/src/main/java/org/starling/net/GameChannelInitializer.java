package org.starling.net;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.starling.message.MessageRouter;

public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final MessageRouter messageRouter;

    public GameChannelInitializer(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        GameChannelPipeline.configure(ch, messageRouter);
    }
}
