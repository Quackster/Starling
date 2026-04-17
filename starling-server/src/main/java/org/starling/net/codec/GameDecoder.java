package org.starling.net.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

/**
 * Decodes plaintext client->server messages from the TCP byte stream.
 * Any protocol-specific decryption is handled earlier in the pipeline.
 */
public class GameDecoder extends ByteToMessageDecoder {

    private static final Logger log = LogManager.getLogger(GameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= 3) {
            in.markReaderIndex();

            // Read 3-byte B64 length prefix
            byte b0 = in.readByte();
            byte b1 = in.readByte();
            byte b2 = in.readByte();
            int bodyLength = Base64Encoding.decodeLength(b0, b1, b2);

            if (bodyLength < 2) {
                log.warn("Invalid message length: {}", bodyLength);
                return;
            }

            if (in.readableBytes() < bodyLength) {
                in.resetReaderIndex();
                return; // wait for more data
            }

            // Read body: first 2 bytes = header, rest = params
            int opcode = Base64Encoding.readHeader(in);
            int paramsLength = bodyLength - 2;

            ByteBuf paramsBuf;
            if (paramsLength > 0) {
                paramsBuf = in.readRetainedSlice(paramsLength);
            } else {
                paramsBuf = Unpooled.EMPTY_BUFFER;
            }

            out.add(new ClientMessage(opcode, paramsBuf));
        }
    }
}
