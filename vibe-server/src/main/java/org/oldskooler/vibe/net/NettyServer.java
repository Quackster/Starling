package org.oldskooler.vibe.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.message.MessageRouter;

public class NettyServer {

    private static final Logger log = LogManager.getLogger(NettyServer.class);

    private final int port;
    private final MessageRouter messageRouter;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * Creates a new NettyServer.
     * @param port the port value
     * @param messageRouter the message router value
     */
    public NettyServer(int port, MessageRouter messageRouter) {
        this.port = port;
        this.messageRouter = messageRouter;
    }

    /**
     * Starts.
     * @throws InterruptedException if the operation fails
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new GameChannelInitializer(messageRouter));

        ChannelFuture future = bootstrap.bind(port).sync();
        log.info("Vibe server listening on port {}", port);
        future.channel().closeFuture().addListener(f -> shutdown());
    }

    /**
     * Shutdowns.
     */
    public void shutdown() {
        log.info("Shutting down server...");
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }
}
