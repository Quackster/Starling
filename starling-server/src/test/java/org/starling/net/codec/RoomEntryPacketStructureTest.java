package org.starling.net.codec;

import org.junit.jupiter.api.Test;
import org.starling.message.OutgoingPackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomEntryPacketStructureTest {

    @Test
    void openConnectionIsBodyless() {
        assertSerialized(new ServerMessage(OutgoingPackets.OPC_OK), "@S[1]");
    }

    @Test
    void flatLetInIsBodyless() {
        assertSerialized(new ServerMessage(OutgoingPackets.FLAT_LETIN), "@i[1]");
    }

    @Test
    void roomUrlUsesSingleTerminatedString() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.ROOM_URL).writeString("/client/"),
                "Bf/client/[2][1]"
        );
    }

    @Test
    void roomReadyMatchesLisbonStructure() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.ROOM_READY)
                        .writeString("model_a")
                        .writeString(" ")
                        .writeInt(1),
                "AEmodel_a[2] [2]I[1]"
        );
    }

    @Test
    void flatPropertyUsesRawKeyValueBody() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw("wallpaper/201"),
                "@nwallpaper/201[1]"
        );
    }

    @Test
    void roomAdWritesSingleZeroInteger() {
        assertSerialized(new ServerMessage(OutgoingPackets.ROOM_AD).writeInt(0), "CPH[1]");
    }

    @Test
    void interstitialWritesSingleZeroInteger() {
        assertSerialized(new ServerMessage(OutgoingPackets.INTERSTITIAL_DATA).writeInt(0), "DBH[1]");
    }

    @Test
    void spectatorAmountWritesTwoZeroIntegers() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                        .writeInt(0)
                        .writeInt(0),
                "DjHH[1]"
        );
    }

    private static void assertSerialized(ServerMessage message, String expected) {
        assertEquals(expected, describe(message.toBytes()));
    }

    private static String describe(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                builder.append((char) unsigned);
            } else {
                builder.append('[').append(unsigned).append(']');
            }
        }
        return builder.toString();
    }
}
