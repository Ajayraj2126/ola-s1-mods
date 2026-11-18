package com.ajayraj2126.s1control;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/** A status probe using only the Android ADB connection and authorization messages. */
final class OwnerProbe {
    static final int CONNECT = 0x4e584e43, AUTH = 0x48545541;
    private static final int VERSION = 0x01000000, LIMIT = 4096;

    interface Identity {
        byte[] sign(byte[] token) throws GeneralSecurityException;
        byte[] publicKey();
    }

    static InetAddress privateAddress(String input) throws IOException {
        String[] octets = input.trim().split("\\.", -1);
        if (octets.length == 4) {
            byte[] bytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                if (!octets[i].matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octets[i]) > 255)
                    throw new IOException("Enter the scooter’s private IPv4 address.");
                bytes[i] = (byte) Integer.parseInt(octets[i]);
            }
            InetAddress address = InetAddress.getByAddress(bytes);
            if (address.isSiteLocalAddress()) return address;
        }
        throw new IOException("Enter the scooter’s private IPv4 address.");
    }

    static String verify(InputStream input, OutputStream output, Identity identity, boolean allowPrompt)
            throws IOException, GeneralSecurityException {
        send(output, CONNECT, VERSION, LIMIT, "host::\0".getBytes(StandardCharsets.US_ASCII));
        Packet challenge = read(input);
        if (!challenge.isChallenge()) throw new IOException("The endpoint did not request Android device authorization.");
        send(output, AUTH, 2, 0, identity.sign(challenge.data));
        Packet answer = read(input);
        if (answer.isChallenge()) {
            if (!allowPrompt) throw new IOException("Approval has expired. Tap Connect to request approval again.");
            send(output, AUTH, 3, 0, identity.publicKey());
            answer = read(input);
        }
        if (answer.command != CONNECT) throw new IOException("Device approval was not received.");
        String banner = new String(answer.data, StandardCharsets.UTF_8);
        if (!banner.startsWith("device::")) throw new IOException("Unexpected Android device response.");
        for (String part : banner.substring(8).split("[;\\x00]")) {
            if (part.startsWith("ro.product.model=")) {
                String model = part.substring(17).replaceAll("[^\\p{L}\\p{N} ._-]", "").trim();
                if (!model.isEmpty()) return model.substring(0, Math.min(model.length(), 80));
            }
        }
        return "Android device";
    }

    private static final class Packet {
        final int command, arg;
        final byte[] data;
        Packet(int command, int arg, byte[] data) { this.command = command; this.arg = arg; this.data = data; }
        boolean isChallenge() { return command == AUTH && arg == 1 && data.length == 20; }
    }

    private static Packet read(InputStream input) throws IOException {
        DataInputStream stream = new DataInputStream(input);
        byte[] header = new byte[24];
        stream.readFully(header);
        ByteBuffer fields = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int command = fields.getInt(), arg = fields.getInt();
        fields.getInt();
        int size = fields.getInt(), sum = fields.getInt();
        if (fields.getInt() != ~command || size < 0 || size > LIMIT) throw new IOException("Invalid Android device response.");
        byte[] data = new byte[size];
        stream.readFully(data);
        if (checksum(data) != sum) throw new IOException("Corrupt Android device response.");
        return new Packet(command, arg, data);
    }

    private static int checksum(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) result += b & 255;
        return result;
    }

    static void send(OutputStream out, int cmd, int arg0, int arg1, byte[] data) throws IOException {
        out.write(ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(cmd).putInt(arg0).putInt(arg1).putInt(data.length).putInt(checksum(data)).putInt(~cmd).array());
        out.write(data);
        out.flush();
    }
}
