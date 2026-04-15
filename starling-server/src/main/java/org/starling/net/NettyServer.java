package org.starling.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.message.MessageRouter;

public class NettyServer {

    private static final Logger log = LogManager.getLogger(NettyServer.class);

    private final int port;
    private final MessageRouter messageRouter;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(int port, MessageRouter messageRouter) {
        this.port = port;
        this.messageRouter = messageRouter;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new GameChannelInitializer(messageRouter));

        ChannelFuture future = bootstrap.bind(port).sync();
        log.info("Starling server listening on port {}", port);
        future.channel().closeFuture().addListener(f -> shutdown());
    }

    public void shutdown() {
        log.info("Shutting down server...");
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }
}
