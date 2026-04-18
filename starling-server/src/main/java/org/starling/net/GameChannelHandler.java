package org.starling.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.RoomExitResult;
import org.starling.gateway.GatewayMappings;
import org.starling.gateway.rpc.GatewayServiceClients;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.message.MessageRouter;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.support.grpc.RequestIds;

/**
 * Netty inbound handler for the gateway process.
 */
public class GameChannelHandler extends SimpleChannelInboundHandler<ClientMessage> {

    private static final Logger log = LogManager.getLogger(GameChannelHandler.class);

    private final MessageRouter messageRouter;
    private final RoomResponseWriter roomResponses = new RoomResponseWriter();

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

        // Send HELLO (opcode 0) - no params
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

        ThreadContext.put(RequestIds.THREAD_CONTEXT_KEY, RequestIds.generate());
        try {
            log.debug("<<< [{}] opcode={} ({}) bodyLen={}",
                    session.getRemoteAddress(), msg.getOpcode(), msg.headerString(), msg.remainingBytes());
            messageRouter.route(session, msg);
        } catch (Exception e) {
            log.error("Error handling message opcode={}: {}", msg.getOpcode(), e.getMessage(), e);
        } finally {
            ThreadContext.remove(RequestIds.THREAD_CONTEXT_KEY);
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
            try {
                disconnectFromRoomService(session);
            } catch (Exception e) {
                log.warn("Failed to mirror disconnect for {}: {}", session.getRemoteAddress(), e.getMessage(), e);
            } finally {
                session.setRoomPresence(Session.RoomPresence.none());
                PlayerManager.getInstance().unregister(session);
            }
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

    /**
     * Mirrors disconnect cleanup to the room service and local sessions.
     * @param session the session value
     */
    private void disconnectFromRoomService(Session session) {
        if (GatewayServiceClients.room() == null) {
            return;
        }

        RoomExitResult result = GatewayServiceClients.room().disconnectSession(
                session.getSessionId(),
                GatewayMappings.toPlayerData(session.getPlayer())
        );
        if (result.getOutcome().getKind() != OutcomeKind.OUTCOME_KIND_SUCCESS) {
            return;
        }
        if (result.getRoomId() <= 0 || result.getLeavingPlayerId() <= 0) {
            return;
        }

        for (Session occupant : GatewayMappings.sessionsInRoom(result.getRoomType(), result.getRoomId())) {
            if (occupant != session) {
                roomResponses.sendLogout(occupant, result.getLeavingPlayerId());
            }
        }
    }
}
