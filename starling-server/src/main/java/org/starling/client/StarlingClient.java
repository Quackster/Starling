package org.starling.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.starling.crypto.DiffieHellman;
import org.starling.crypto.SecretKeyCodec;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.Base64Encoding;
import org.starling.net.codec.VL64Encoding;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public final class StarlingClient implements AutoCloseable {

    private static final BigInteger INIT_PRIME = new BigInteger(
            "A8EA077D4943CC98E53C21F5F7C7A0DB8BCE7506F8361A7C1690392F2B090C96" +
            "EE8BC67BAA0DCB7183F16401F5CB838E3B6EE86B9EF2E5D0F3C49D4DC4EDC2B9", 16);
    private static final BigInteger INIT_GENERATOR = BigInteger.valueOf(5L);

    private static final String INIT_PRIVATE_CHARS = "012345679abcdef";
    private static final String SECRET_KEY_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890";

    private static final byte[] INIT_XOR_KEY =
            "mWxFRJnGJ5T9Si0OMVvEBBm8laihXkN8GmH6fuv7ldZhLyGRRKCcGzziPYBaJom"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] INIT_PREMIX =
            "NV6VVFPoC7FLDlzDUri3qcOAg9cRoFOmsYR9ffDGy5P8HfF6eekX40SFSVfJ1mDb3lcpYRqdg28sp61eHkPukKbqTu1JsVEKiRavi04YtSzUsLXaYSa5BEGwg5G2OF"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] INIT_POST_FRAME =
            "xllVGKnnQcW8aX4WefdKrBWTqiW5EwT"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SERVER_PREMIX =
            "eb11nmhdwbn733c2xjv1qln3ukpe0hvce0ylr02s12sv96rus2ohexr9cp8rufbmb1mdb732j1l3kehc0l0s2v6u2hx9prfmu"
                    .getBytes(StandardCharsets.US_ASCII);
    private static final int[] ARTIFICIAL_KEY = parseArtificialKey();

    private final Config config;
    private final SecureRandom random = new SecureRandom();

    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private ServerFrameReader serverReader;
    private ClientCipher outgoingCipher;

    private StarlingClient(Config config) {
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        try (StarlingClient client = new StarlingClient(config)) {
            client.run();
        }
    }

    private void run() throws Exception {
        connect();

        ServerPacket hello = readPacket();
        expectOpcode(hello, OutgoingPackets.HELLO, "HELLO");

        sendPlain(IncomingPackets.INIT_CRYPTO);
        ServerPacket cryptoParameters = readPacket();
        expectOpcode(cryptoParameters, OutgoingPackets.CRYPTO_PARAMETERS, "CRYPTO_PARAMETERS");

        int serverToClient = readSingleInt(cryptoParameters.body());
        log("serverToClient=" + serverToClient);

        DhKeyPair dh = DhKeyPair.init(random);
        sendPlainString(IncomingPackets.GENERATEKEY, dh.publicKeyHex);

        ServerPacket serverSecretKey = readPacket();
        expectOpcode(serverSecretKey, OutgoingPackets.SERVER_SECRET_KEY, "SERVER_SECRET_KEY");

        byte[] sharedKey = dh.computeSharedKey(serverSecretKey.bodyString());
        outgoingCipher = ClientCipher.init(sharedKey);

        if (serverToClient != 0) {
            String encodedSecretKey = createEncodedSecretKey();
            int secretKey = SecretKeyCodec.secretDecode(encodedSecretKey);
            serverReader.enableEncrypted(ServerCipher.oldSocket(secretKey));

            sendEncryptedString(IncomingPackets.SECRETKEY, encodedSecretKey);
            ServerPacket endOfCrypto = readPacket();
            expectOpcode(endOfCrypto, OutgoingPackets.END_OF_CRYPTO_PARAMS, "END_OF_CRYPTO_PARAMS");
        }

        if (!config.handshakeOnly) {
            sendEncryptedVersionCheck();
            sendEncryptedString(IncomingPackets.UNIQUEID, config.machineId);
            sendEncrypted(IncomingPackets.GET_SESSION_PARAMETERS, new byte[0]);

            ServerPacket sessionParameters = readPacket();
            expectOpcode(sessionParameters, OutgoingPackets.SESSION_PARAMETERS, "SESSION_PARAMETERS");
            log("sessionParameters bytes=" + sessionParameters.body().length);
        }
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(config.host, config.port), config.timeoutMs);
        socket.setSoTimeout(config.timeoutMs);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        serverReader = new ServerFrameReader(input);
        log("connected " + config.host + ":" + config.port + " mode=init");
    }

    private void sendPlain(int opcode) throws IOException {
        send(opcode, new byte[0], false);
    }

    private void sendPlainString(int opcode, String value) throws IOException {
        send(opcode, encodeString(value), false);
    }

    private void sendEncrypted(int opcode, byte[] body) throws IOException {
        send(opcode, body, true);
    }

    private void sendEncryptedString(int opcode, String value) throws IOException {
        send(opcode, encodeString(value), true);
    }

    private void sendEncryptedVersionCheck() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(VL64Encoding.encode(config.version));
        body.writeBytes(encodeString(config.clientUrl));
        body.writeBytes(encodeString(config.extVarsUrl));
        send(IncomingPackets.VERSIONCHECK, body.toByteArray(), true);
    }

    private void send(int opcode, byte[] body, boolean encrypted) throws IOException {
        byte[] header = Base64Encoding.encodeHeader(opcode);
        byte[] length = Base64Encoding.encodeLength(header.length + body.length);

        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.writeBytes(length);
        frame.writeBytes(header);
        frame.writeBytes(body);

        byte[] wire = frame.toByteArray();
        if (encrypted) {
            if (outgoingCipher == null) {
                throw new IllegalStateException("Outgoing cipher is not ready");
            }
            wire = outgoingCipher.encryptFrame(wire);
        }

        output.write(wire);
        output.flush();
        logSend(opcode, wire, encrypted);
    }

    private ServerPacket readPacket() throws IOException {
        ServerPacket packet = serverReader.readPacket();
        logRecv(packet);
        return packet;
    }

    private static void expectOpcode(ServerPacket packet, int expected, String name) {
        if (packet.opcode != expected) {
            throw new IllegalStateException(
                    "Expected " + name + " (" + expected + "), got " + packet.opcode + " (" + packet.header() + ')');
        }
    }

    private static int readSingleInt(byte[] body) {
        ByteBuf buffer = Unpooled.wrappedBuffer(body);
        try {
            return VL64Encoding.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private String createEncodedSecretKey() {
        int keyLength = 30 + Math.floorMod(random.nextInt(), 40);
        StringBuilder table = new StringBuilder(keyLength * 2);
        StringBuilder key = new StringBuilder(keyLength);

        for (int i = 0; i < keyLength; i++) {
            table.append(SECRET_KEY_CHARS.charAt(Math.floorMod(random.nextInt(), SECRET_KEY_CHARS.length())));
            char keyChar = SECRET_KEY_CHARS.charAt(Math.floorMod(random.nextInt(), SECRET_KEY_CHARS.length()));
            table.append(keyChar);
            key.append(keyChar);
        }

        return table.append(key).toString();
    }

    private static byte[] encodeString(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        byte[] length = Base64Encoding.encodeShort(bytes.length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(length);
        output.writeBytes(bytes);
        return output.toByteArray();
    }

    private static String formatWire(byte[] bytes) {
        StringBuilder formatted = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                formatted.append((char) unsigned);
            } else {
                formatted.append('[').append(unsigned).append(']');
            }
        }
        return formatted.toString();
    }

    private void logSend(int opcode, byte[] wire, boolean encrypted) {
        log(">>> opcode=" + opcode + " encrypted=" + encrypted + " raw=" + formatWire(wire));
    }

    private void logRecv(ServerPacket packet) {
        log("<<< opcode=" + packet.opcode + " (" + packet.header() + ") bodyLen=" + packet.body.length +
                " raw=" + formatWire(packet.rawFrame));
    }

    private void log(String message) {
        System.out.println("[starling-client] " + message);
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    private record Config(
            String host,
            int port,
            int timeoutMs,
            int version,
            String clientUrl,
            String extVarsUrl,
            String machineId,
            boolean handshakeOnly
    ) {
        private static Config parse(String[] args) {
            String host = "127.0.0.1";
            int port = 30000;
            int timeoutMs = 5000;
            int version = 26;
            String clientUrl = "";
            String extVarsUrl = "";
            String machineId = "starling-client";
            boolean handshakeOnly = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--host" -> host = args[++i];
                    case "--port" -> port = Integer.parseInt(args[++i]);
                    case "--timeout-ms" -> timeoutMs = Integer.parseInt(args[++i]);
                    case "--mode" -> {
                        String value = args[++i];
                        if (!"init".equalsIgnoreCase(value)) {
                            throw new IllegalArgumentException("Only hh_entry_init mode is supported: " + value);
                        }
                    }
                    case "--version" -> version = Integer.parseInt(args[++i]);
                    case "--client-url" -> clientUrl = args[++i];
                    case "--ext-vars-url" -> extVarsUrl = args[++i];
                    case "--machine-id" -> machineId = args[++i];
                    case "--handshake-only" -> handshakeOnly = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            return new Config(host, port, timeoutMs, version, clientUrl, extVarsUrl, machineId, handshakeOnly);
        }
    }

    private static final class ServerPacket {
        private final int opcode;
        private final byte[] body;
        private final byte[] rawFrame;

        private ServerPacket(int opcode, byte[] body, byte[] rawFrame) {
            this.opcode = opcode;
            this.body = body;
            this.rawFrame = rawFrame;
        }

        private byte[] body() {
            return body;
        }

        private String bodyString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        private String header() {
            return new String(Base64Encoding.encodeHeader(opcode), StandardCharsets.US_ASCII);
        }
    }

    private static final class ServerFrameReader {
        private final InputStream input;
        private ServerCipher cipher;

        private ServerFrameReader(InputStream input) {
            this.input = input;
        }

        private void enableEncrypted(ServerCipher cipher) {
            this.cipher = cipher;
        }

        private ServerPacket readPacket() throws IOException {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            while (true) {
                int value = readServerByte();
                if (value == -1) {
                    throw new EOFException("Server closed the connection");
                }
                if (value == 0x01) {
                    break;
                }
                frame.write(value);
            }

            byte[] raw = frame.toByteArray();
            if (raw.length < 2) {
                throw new IllegalStateException("Server frame was too short: " + raw.length);
            }

            int opcode = Base64Encoding.decodeHeader(raw[0], raw[1]);
            byte[] body = Arrays.copyOfRange(raw, 2, raw.length);
            return new ServerPacket(opcode, body, raw);
        }

        private int readServerByte() throws IOException {
            if (cipher == null) {
                return input.read();
            }

            int high = input.read();
            int low = input.read();
            if (high == -1 || low == -1) {
                return -1;
            }

            return cipher.decryptHexPair((byte) high, (byte) low);
        }
    }

    private record DhKeyPair(BigInteger prime, BigInteger generator, BigInteger privateKey, String publicKeyHex) {
        private static DhKeyPair init(SecureRandom random) {
            return generate(random, INIT_PRIME, INIT_GENERATOR, 40, INIT_PRIVATE_CHARS, 72);
        }

        private static DhKeyPair generate(
                SecureRandom random,
                BigInteger prime,
                BigInteger generator,
                int privateHexBytes,
                String alphabet,
                int minimumPublicLength
        ) {
            BigInteger privateKey = BigInteger.ONE;
            String publicKeyHex = "1";

            for (int attempt = 0; attempt < 5; attempt++) {
                String privateHex = randomHex(random, privateHexBytes * 2, alphabet);
                privateKey = new BigInteger(privateHex, 16);
                publicKeyHex = generator.modPow(privateKey, prime).toString(16).toUpperCase();
                if (publicKeyHex.length() >= minimumPublicLength) {
                    break;
                }
            }

            return new DhKeyPair(prime, generator, privateKey, publicKeyHex);
        }

        private byte[] computeSharedKey(String otherPublicKeyHex) {
            BigInteger otherPublicKey = DiffieHellman.parsePublicKeyHex(otherPublicKeyHex);
            BigInteger sharedSecret = otherPublicKey.modPow(privateKey, prime);
            String sharedHex = sharedSecret.toString(16);
            if ((sharedHex.length() & 1) != 0) {
                sharedHex = "0" + sharedHex;
            }

            byte[] sharedBytes = new byte[sharedHex.length() / 2];
            for (int i = 0; i < sharedBytes.length; i++) {
                sharedBytes[i] = (byte) Integer.parseInt(sharedHex.substring(i * 2, (i * 2) + 2), 16);
            }
            return sharedBytes;
        }

        private static String randomHex(SecureRandom random, int length, String alphabet) {
            StringBuilder value = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                value.append(alphabet.charAt(Math.floorMod(random.nextInt(), alphabet.length())));
            }
            return value.toString();
        }
    }

    private interface ClientCipher {
        byte[] encryptFrame(byte[] plaintext);

        static ClientCipher init(byte[] sharedKey) {
            return new InitSocketCipher(sharedKey);
        }
    }

    private static final class InitSocketCipher implements ClientCipher {
        private final int[] sbox = new int[256];
        private int q;
        private int j;

        private InitSocketCipher(byte[] sharedKey) {
            byte[] modKey = new byte[sharedKey.length];
            for (int i = 0; i < sharedKey.length; i++) {
                modKey[i] = (byte) ((sharedKey[i] & 0xFF) ^ (INIT_XOR_KEY[i % INIT_XOR_KEY.length] & 0xFF));
            }

            initSbox(modKey);
            apply(INIT_PREMIX);
            for (int i = 1; i < 52; i++) {
                apply(INIT_PREMIX);
            }
        }

        @Override
        public byte[] encryptFrame(byte[] plaintext) {
            byte[] encrypted = apply(plaintext);
            apply(INIT_POST_FRAME);
            return encodeHex(encrypted);
        }

        private void initSbox(byte[] keyBytes) {
            int[] key = new int[256];
            for (int i = 0; i < 256; i++) {
                key[i] = keyBytes[i % keyBytes.length] & 0xFF;
                sbox[i] = i;
            }

            int swapIndex = 0;
            for (int i = 0; i < 256; i++) {
                swapIndex = (swapIndex + sbox[i] + key[i]) & 0xFF;
                swap(i, swapIndex);
            }
            q = 0;
            j = 0;
        }

        private byte[] apply(byte[] input) {
            byte[] output = new byte[input.length];
            for (int index = 0; index < input.length; index++) {
                q = (q + 1) & 0xFF;
                j = (j + sbox[q]) & 0xFF;
                swap(q, j);

                int secondaryIndex = (17 * (q + 19)) & 0xFF;
                int secondarySwapIndex = (j + sbox[secondaryIndex]) & 0xFF;
                swap(secondaryIndex, secondarySwapIndex);

                if (q == 46 || q == 67 || q == 192) {
                    int tertiaryIndex = (297 * (secondaryIndex + 67)) & 0xFF;
                    int tertiarySwapIndex = (secondarySwapIndex + sbox[tertiaryIndex]) & 0xFF;
                    swap(tertiaryIndex, tertiarySwapIndex);
                }

                int keyByte = sbox[(sbox[q] + sbox[j]) & 0xFF];
                output[index] = (byte) ((input[index] & 0xFF) ^ keyByte);
            }
            return output;
        }

        private void swap(int left, int right) {
            int temp = sbox[left];
            sbox[left] = sbox[right];
            sbox[right] = temp;
        }
    }

    private interface ServerCipher {
        int decryptHexPair(byte high, byte low);

        static ServerCipher oldSocket(int secretKey) {
            return new OldServerCipher(secretKey);
        }
    }

    private static final class OldServerCipher implements ServerCipher {
        private final int[] sbox = new int[256];
        private int q;
        private int j;

        private OldServerCipher(int secretKey) {
            initArtificialKey(secretKey);
            for (int i = 0; i < 17; i++) {
                apply(SERVER_PREMIX);
            }
        }

        @Override
        public int decryptHexPair(byte high, byte low) {
            int encrypted = (hexToNibble(high) << 4) | hexToNibble(low);
            q = (q + 1) & 0xFF;
            j = (j + sbox[q]) & 0xFF;
            swap(q, j);
            int keyByte = sbox[(sbox[q] + sbox[j]) & 0xFF];
            return encrypted ^ keyByte;
        }

        private void initArtificialKey(int secretKey) {
            int length = (secretKey & 248) / 8;
            if (length < 20) {
                length += 20;
            }

            int offset = secretKey % 1024;
            int[] ckey = new int[length];
            int[] key = new int[256];
            for (int i = 0; i < length; i++) {
                int given = secretKey >>> (i % 32);
                int own = ARTIFICIAL_KEY[Math.floorMod(offset + i, ARTIFICIAL_KEY.length)];
                ckey[i] = (given ^ own) & 32767;
            }

            for (int i = 0; i < 256; i++) {
                key[i] = ckey[i % length];
                sbox[i] = i;
            }

            int swapIndex = 0;
            for (int i = 0; i < 256; i++) {
                swapIndex = (swapIndex + sbox[i] + key[i]) & 0xFF;
                swap(i, swapIndex);
            }
            q = 0;
            j = 0;
        }

        private void apply(byte[] data) {
            for (byte ignored : data) {
                q = (q + 1) & 0xFF;
                j = (j + sbox[q]) & 0xFF;
                swap(q, j);
                int discard = sbox[(sbox[q] + sbox[j]) & 0xFF];
                if (discard == -1) {
                    throw new IllegalStateException("Unreachable");
                }
            }
        }

        private void swap(int left, int right) {
            int temp = sbox[left];
            sbox[left] = sbox[right];
            sbox[right] = temp;
        }
    }

    private static byte[] encodeHex(byte[] data) {
        byte[] encoded = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xFF;
            encoded[i * 2] = nibbleToHex((value >>> 4) & 0x0F);
            encoded[(i * 2) + 1] = nibbleToHex(value & 0x0F);
        }
        return encoded;
    }

    private static byte nibbleToHex(int value) {
        return (byte) (value < 10 ? ('0' + value) : ('A' + (value - 10)));
    }

    private static int hexToNibble(byte value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'F') {
            return value - 'A' + 10;
        }
        if (value >= 'a' && value <= 'f') {
            return value - 'a' + 10;
        }
        throw new IllegalArgumentException("Invalid hex char: " + (char) value);
    }

    private static int[] parseArtificialKey() {
        String csv =
                "204,53,74,109,63,4,163,182,210,186,19,162,160,115,139,83,235,177,14,15,11,127,4,210,222,138,10,138,151,236,158,186,67,1,168,69,139,214,243,32,157,161,211,155,20,192,214,155,12,153,192,112,98,146,33,30,22,131,81,161,105,142,103,204,112,9,167,185,176,51,27,166,249,228,24,165,197,25,166,216,74,14,104,15,77,49,6,50,65,126,10,187,15,17,189,155,246,221,92,104,79,87,186,88,80,50,223,126,148,217,81,223,91,70,165,237,150,95,195,205,199,176,156,122,187,232,252,230,169,94,157,194,44,164,208,22,141,139,167,236,201,42,130,14,44,57,253,224,130,118,242,226,146,202,154,40,201,171,160,91,143,144,150,197,169,204,121,131,139,112,214,196,74,123,159,220,77,176,151,73,125,135,166,26,176,31,255,234,91,30,218,41,121,17,45,3,234,35,185,52,112,108,65,72,184,93,225,113,62,0,110,38,43,15,44,114,162,167,69,40,103,144,114,215,228,47,112,235,179,211,116,237,70,167,36,224,183,11,0,74,145,241,153,40,151,211,231,199,235,176,109,95,160,141,137,236,39,17,246,97,120,227,12,1,195,239,150,169,85,226,23,58,145,157,37,218,132,168,94,15,240,24,152,230,249,80,145,208,209,144,154,228,197,40,6,248,90,15,1,82,145,77,220,27,167,0,149,0,103,53,226,242,175,9,177,130,65,216,107,4,194,71,135,231,151,178,188,220,33,152,120,165,73,124,32,215,127,130,29,40,20,3,212,254,106,42,98,7,8,129,195,30,74,118,169,81,88,235,149,232,181,182,206,82,163,26,116,37,41,50,63,185,165,2,81,10,149,103,211,168,34,55,32,233,16,238,219,235,170,255,244,12,89,211,88,33,24,38,190,75,70,86,89,2,189,134,207,65,6,148,124,22,57,21,118,227,173,21,236,236,139,189,230,153,153,182,230,216,26,0,9,50,32,189,97,3,208,201,103,163,96,0,42,11,173,98,102,76,31,243,59,71,223,252,186,157,231,90,212,83,10,69,69,165,209,112,157,237,24,90,4,44,247,32,159,126,171,99,216,196,228,217,157,143,32,16,111,67,106,231,10,167,13,240,182,105,52,12,84,91,243,205,180,180,35,58,238,240,0,209,48,249,243,209,93,10,22,183,5,177,110,16,188,201,240,194,11,76,219,67,254,176,139,66,81,138,109,178,71,143,74,217,52,0,127,190,12,214,231,84,239,165,155,89,95,106,62,30,182,137,85,39,221,51,188,149,104,167,71,11,220,212,246,114,10,4,216,127,233,231,178,174,181,29,49,118,177,108,156,174,118,196,216,106,203,96,65,12,140,248,152,35,152,17,89,136,138,94,5,190,92,189,16,216,61,70,165,36,238,167,16,61,206,140,226,251,37,225,211,111,42,195,36,248,233,67,146,100,244,23,154,103,48,4,15,33,169,151,13,151,115,173,37,103,172,23,182,29,22,25,54,46,188,14,24,12,182,241,163,90,121,172,29,73,191,91,232,229,197,200,32,7,67,214,141,248,10,135,168,4,144,17,94,228,76,202,130,174,251,170,100,173,232,183,132,130,35,163,1,154,134,56,202,13,190,224,56,107,107,244,16,12,149,220,120,245,179,103,85,255,195,187,191,82,225,13,206,106,60,212,12,211,247,112,185,5,56,226,236,179,181,208,204,16,159,158,36,65,101,148,23,89,125,27,61,117,255,142,32,138,105,166,203,253,113,138,30,247,250,198,21,244,113,40,161,229,179,100,76,30,177,69,87,90,9,135,254,108,99,145,195,145,138,223,237,52,126,244,109,171,44,0,187,129,127,49,220,100,253,0,116,93,87,39,245,5,54,203,241,155,255,125,80,253,75,71,242,147,153,148,214,91,33,181,78,10,82,171,89,179,221,144,224,138,112,254,152,186,190,224,44,251,60,133,65,70,72,203,126,123,212,108,68,185,42,208,51,11,177,3,24,207,14,148,113,55,1,19,179,31,133,11,227,72,145,242,157,244,239,129,124,109,56,134,56,95,110,161,73,151,136,67,176,201,193,70,53,31,238,84,81,65,50,182,20,17,247,179,217,14,34,182,97,55,117,176,108,234,147,89,168,7,251,212,22,107,63,248,179,222,167,214,136,74,53,47,120,233,131,41,167,220,56,12,51,125,207,112,179,211,47,134,223,112,223,46,249,24,64,58,36,187,77,132,116,116,111,36,127,217,177,24,58,102,166,105,119,234,187,198,77,153,23,157,103,92,33,136,182,131,154,141,149,4,117,213,226,64,116,55,6,159,126,225";

        String[] values = csv.split(",");
        int[] parsed = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            parsed[i] = Integer.parseInt(values[i].trim());
        }
        return parsed;
    }
}
