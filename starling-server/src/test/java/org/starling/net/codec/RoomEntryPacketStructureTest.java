package org.starling.net.codec;

import org.junit.jupiter.api.Test;
import org.starling.message.OutgoingPackets;
import org.starling.support.PacketDebugStrings;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomEntryPacketStructureTest {

    /**
     * Opens connection is bodyless.
     */
    @Test
    void openConnectionIsBodyless() {
        assertSerialized(new ServerMessage(OutgoingPackets.OPC_OK), "@S[1]");
    }

    /**
     * Hotels view is bodyless.
     */
    @Test
    void hotelViewIsBodyless() {
        assertSerialized(new ServerMessage(OutgoingPackets.HOTEL_VIEW), "@R[1]");
    }

    /**
     * Flats let in is bodyless.
     */
    @Test
    void flatLetInIsBodyless() {
        assertSerialized(new ServerMessage(OutgoingPackets.FLAT_LETIN), "@i[1]");
    }

    /**
     * Logouts writes single instance id.
     */
    @Test
    void logoutWritesSingleInstanceId() {
        assertSerialized(new ServerMessage(OutgoingPackets.LOGOUT).writeInt(1), "@]I[1]");
    }

    /**
     * Rooms url uses single terminated string.
     */
    @Test
    void roomUrlUsesSingleTerminatedString() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.ROOM_URL).writeRaw("http://wwww.vista4life.com/bf.php?p=emu"),
                "Bfhttp://wwww.vista4life.com/bf.php?p=emu[1]"
        );
    }

    /**
     * Rooms ready matches holograph structure.
     */
    @Test
    void roomReadyMatchesHolographStructure() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.ROOM_READY).writeRaw("model_a 1"),
                "AEmodel_a 1[1]"
        );
    }

    /**
     * Flats property uses raw key value body.
     */
    @Test
    void flatPropertyUsesRawKeyValueBody() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw("wallpaper/201"),
                "@nwallpaper/201[1]"
        );
    }

    /**
     * Rooms ad writes single zero integer.
     */
    @Test
    void roomAdWritesSingleZeroInteger() {
        assertSerialized(new ServerMessage(OutgoingPackets.ROOM_AD).writeRaw("0"), "CP0[1]");
    }

    /**
     * Interstitials writes single zero integer.
     */
    @Test
    void interstitialWritesSingleZeroInteger() {
        assertSerialized(new ServerMessage(OutgoingPackets.INTERSTITIAL_DATA).writeRaw("0"), "DB0[1]");
    }

    /**
     * Spectators amount writes two zero integers.
     */
    @Test
    void spectatorAmountWritesTwoZeroIntegers() {
        assertSerialized(
                new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                        .writeInt(0)
                        .writeInt(0),
                "DjHH[1]"
        );
    }

    /**
     * Asserts serialized.
     * @param message the message value
     * @param expected the expected value
     */
    private static void assertSerialized(ServerMessage message, String expected) {
        assertEquals(expected, PacketDebugStrings.describe(message.toBytes()));
    }
}
