package org.starling.net;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.starling.message.MessageRouter;
import org.starling.net.codec.GameDecoder;
import org.starling.net.codec.GameEncoder;

public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final MessageRouter messageRouter;

    public GameChannelInitializer(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast("decoder", new GameDecoder())
                .addLast("encoder", new GameEncoder())
                .addLast("handler", new GameChannelHandler(messageRouter));
    }
}
