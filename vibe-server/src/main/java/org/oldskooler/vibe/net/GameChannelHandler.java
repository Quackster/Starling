package org.oldskooler.vibe.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.game.room.lifecycle.RoomLifecycleService;
import org.oldskooler.vibe.message.MessageRouter;
import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.net.codec.ClientMessage;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.net.session.Session;

public class GameChannelHandler extends SimpleChannelInboundHandler<ClientMessage> {

    private static final Logger log = LogManager.getLogger(GameChannelHandler.class);

    private final MessageRouter messageRouter;
    private final RoomLifecycleService roomLifecycleService = RoomLifecycleService.getInstance();

    /**
     * Creates a new GameChannelHandler.
     * @param messageRouter the message router value
     */
    public GameChannelHandler(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    /**
     * Channels active.
     * @param ctx the ctx value
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Session session = new Session(ctx.channel());
        ctx.channel().attr(Session.KEY).set(session);

        log.info("Client connected: {}", session.getRemoteAddress());

        // Send HELLO (opcode 0) — no params
        session.send(new ServerMessage(OutgoingPackets.HELLO));
    }

    /**
     * Channels read0.
     * @param ctx the ctx value
     * @param msg the msg value
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ClientMessage msg) {
        Session session = ctx.channel().attr(Session.KEY).get();
        if (session == null) {
            log.warn("No session for channel, dropping message");
            return;
        }

        try {
            log.debug("<<< [{}] opcode={} ({}) bodyLen={}",
                    session.getRemoteAddress(), msg.getOpcode(), msg.headerString(), msg.remainingBytes());
            messageRouter.route(session, msg);
        } catch (Exception e) {
            log.error("Error handling message opcode={}: {}", msg.getOpcode(), e.getMessage(), e);
        } finally {
            msg.release();
        }
    }

    /**
     * Channels inactive.
     * @param ctx the ctx value
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Session session = ctx.channel().attr(Session.KEY).get();
        if (session != null) {
            roomLifecycleService.handleDisconnect(session);
            log.info("Client disconnected: {}", session.getRemoteAddress());
        }
    }

    /**
     * Exceptions caught.
     * @param ctx the ctx value
     * @param cause the cause value
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
