package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.crypto.DiffieHellman;
import org.starling.crypto.HabboCipher;
import org.starling.crypto.SecretKeyCodec;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

public final class HandshakeHandlers {

    private static final Logger log = LogManager.getLogger(HandshakeHandlers.class);
    private static final boolean SERVER_TO_CLIENT_ENCRYPTION = false;
    private static final byte[] INIT_COMPAT_SHARED_SECRET = new byte[]{0x01};
    private static final String INIT_COMPAT_SERVER_PUBLIC_KEY = "1";

    private HandshakeHandlers() {}

    /**
     * INIT_CRYPTO (206) - Client requests crypto setup.
     * Respond with CryptoParameters (277) containing VL64(0) when the client
     * should not negotiate server->client encryption,
     * then generate a DH keypair for this session.
     */
    public static void handleInitCrypto(Session session, ClientMessage msg) {
        session.resetCrypto();

        session.send(new ServerMessage(OutgoingPackets.CRYPTO_PARAMETERS)
                .writeInt(SERVER_TO_CLIENT_ENCRYPTION ? 1 : 0));

        log.debug("Sent CryptoParameters (serverToClient={})", SERVER_TO_CLIENT_ENCRYPTION);
    }

    /**
     * GENERATEKEY (2002) - Client sends its DH public key.
     * Compute the shared secret, initialize the hh_entry_init client->server
     * cipher, then send our public key back.
     */
    public static void handleGenerateKey(Session session, ClientMessage msg) {
        String clientPublicKeyHex = msg.readString();
        session.setCryptoMode(Session.CryptoMode.INIT);

        // Director r26 computes the shared key through a hidden bigint bridge.
        // Replying with public key "1" collapses that path to a deterministic
        // shared secret of 0x01 without needing the missing client-side math.
        byte[] sharedSecret = INIT_COMPAT_SHARED_SECRET.clone();
        String sharedSecretHex = bytesToHex(sharedSecret);
        log.debug("Init-compat shared secret selected ({} bytes)", sharedSecret.length);

        HabboCipher cipher = new HabboCipher();
        cipher.initInitSocket(sharedSecret);
        session.setInboundCipher(cipher);
        session.setInboundSharedSecret(sharedSecret);

        // Send our compatibility public key to the client (ServerSecretKey, opcode 1).
        String serverPublicKeyHex = INIT_COMPAT_SERVER_PUBLIC_KEY;
        String serverPublicKeyWire = INIT_COMPAT_SERVER_PUBLIC_KEY;
        ServerMessage response = new ServerMessage(OutgoingPackets.SERVER_SECRET_KEY)
                .writeRaw(serverPublicKeyWire);
        session.send(response);

        session.setDebugDhMaterial(
                clientPublicKeyHex,
                null,
                serverPublicKeyHex,
                sharedSecretHex
        );

        if (log.isDebugEnabled()) {
            log.debug("DH material for {} clientPublicKeyHex={}", session.getRemoteAddress(), clientPublicKeyHex);
            log.debug("DH material for {} serverPublicKeyHex={}", session.getRemoteAddress(), serverPublicKeyHex);
            log.debug("DH material for {} serverPublicKeyWire={}", session.getRemoteAddress(), serverPublicKeyWire);
            log.debug("DH material for {} sharedSecretHex={}", session.getRemoteAddress(), sharedSecretHex);
        }

        // Mark session as encrypted - all subsequent incoming messages will be decrypted
        session.setInboundEncrypted(true);

        log.info("Init handshake complete using compatibility public key for {}",
                session.getRemoteAddress());
    }

    /**
     * SECRETKEY (207) - Client opts into server->client encryption.
     * The client sends an encoded secret string; we decode it, initialize the
     * outbound cipher to match the client decoder, then complete the crypto
     * handshake with END_OF_CRYPTO_PARAMS (278).
     */
    public static void handleSecretKey(Session session, ClientMessage msg) {
        String encodedSecretKey = msg.readString();
        int secretKey = SecretKeyCodec.secretDecode(encodedSecretKey);
        Session.CryptoMode cryptoMode = session.getCryptoMode();

        if (cryptoMode == Session.CryptoMode.NONE) {
            log.warn("Ignoring SECRETKEY before DH setup for {}", session.getRemoteAddress());
            return;
        }

        // hh_entry_init still uses the Director SECRETKEY server->client cipher
        // after the init-socket DH exchange.
        HabboCipher cipher = new HabboCipher();
        cipher.initServerToClientSecretKey(secretKey);
        session.setOutboundCipher(cipher);
        session.setOutboundEncrypted(true);
        session.send(new ServerMessage(OutgoingPackets.END_OF_CRYPTO_PARAMS));

        log.info("Enabled server->client SECRETKEY crypto for {} ({})",
                session.getRemoteAddress(),
                cryptoMode.name().toLowerCase());
    }

    /**
     * VERSIONCHECK (1170) - Client sends version info.
     * Params: VL64 version, B64Str clientURL, B64Str extVarsURL
     */
    public static void handleVersionCheck(Session session, ClientMessage msg) {
        if (msg.hasRemaining()) {
            int version = msg.readInt();
            String clientURL = msg.readString();
            String extVarsURL = msg.readString();
            log.debug("Version check: v={}, client={}, extVars={}", version, clientURL, extVarsURL);
        }
    }

    /**
     * UNIQUEID (813) - Client sends machine ID.
     * Params: B64Str machineID
     */
    public static void handleUniqueId(Session session, ClientMessage msg) {
        if (msg.hasRemaining()) {
            String machineId = msg.readString();
            log.debug("Unique ID: {}", machineId);
        }
    }

    /**
     * GET_SESSION_PARAMETERS (1817) - Client requests session config.
     * Respond with SessionParameters (257) containing key-value pairs.
     */
    public static void handleGetSessionParameters(Session session, ClientMessage msg) {
        ServerMessage response = new ServerMessage(OutgoingPackets.SESSION_PARAMETERS);

        // Match the field IDs used by the r26 hh_entry_init client so it sees a
        // complete, stable configuration block.
        response.writeInt(10);
        response.writeInt(0).writeInt(0);               // conf_coppa
        response.writeInt(1).writeInt(0);               // conf_voucher
        response.writeInt(2).writeInt(0);               // conf_parent_email_request
        response.writeInt(3).writeInt(0);               // conf_parent_email_request_reregistration
        response.writeInt(4).writeInt(0);               // conf_allow_direct_mail
        response.writeInt(5).writeString("dd-MM-yyyy"); // date_format
        response.writeInt(6).writeInt(0);               // conf_partner_integration
        response.writeInt(7).writeInt(1);               // allow_profile_editing
        response.writeInt(8).writeString("");           // tracking_header
        response.writeInt(9).writeInt(0);               // tutorial_enabled

        session.send(response);
        log.debug("Sent SessionParameters");
    }

    private static String bytesToHex(byte[] value) {
        StringBuilder hex = new StringBuilder(value.length * 2);
        for (byte current : value) {
            hex.append(Character.forDigit((current >>> 4) & 0x0F, 16));
            hex.append(Character.forDigit(current & 0x0F, 16));
        }
        return hex.toString().toUpperCase();
    }
}
