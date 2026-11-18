package com.ajayraj2126.s1control;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class OwnerProbeTest {
    private final ByteArrayOutputStream peer = new ByteArrayOutputStream(), sent = new ByteArrayOutputStream();
    private int signatures, offers;
    private final OwnerProbe.Identity key = new OwnerProbe.Identity() {
        public byte[] sign(byte[] token) { signatures++; return new byte[256]; }
        public byte[] publicKey() { offers++; return new byte[]{1, 0}; }
    };
    private void challenge() throws IOException { OwnerProbe.send(peer, OwnerProbe.AUTH, 1, 0, new byte[20]); }
    private void approved() throws IOException {
        OwnerProbe.send(peer, OwnerProbe.CONNECT, 0x01000000, 4096,
            "device::ro.product.model=Demo S1;features=test\0".getBytes(StandardCharsets.US_ASCII));
    }
    private String verify(boolean manual) throws Exception {
        return OwnerProbe.verify(new ByteArrayInputStream(peer.toByteArray()), sent, key, manual);
    }
    @Test public void explicitApproval() throws Exception {
        challenge(); challenge(); approved(); assertEquals("Demo S1", verify(true));
        assertEquals(1, signatures); assertEquals(1, offers);
    }
    @Test public void previouslyApprovedReconnects() throws Exception {
        challenge(); approved(); assertEquals("Demo S1", verify(false)); assertEquals(0, offers);
    }
    @Test public void revokedKeyCannotPromptAutomatically() throws Exception {
        challenge(); challenge(); assertThrows(IOException.class, () -> verify(false)); assertEquals(0, offers);
    }
    @Test public void rejectsDaemonWithoutAuthorization() throws Exception {
        approved(); assertThrows(IOException.class, () -> verify(true)); assertEquals(0, signatures);
    }
    @Test public void droppedConnectionFails() throws Exception {
        challenge(); assertThrows(EOFException.class, () -> verify(false));
    }
    @Test public void malformedChallengeCannotSign() throws Exception {
        OwnerProbe.send(peer, OwnerProbe.AUTH, 1, 0, new byte[19]);
        assertThrows(IOException.class, () -> verify(true)); assertEquals(0, signatures);
    }
    @Test public void onlyPrivateNumericAddresses() throws Exception {
        assertEquals("192.168.1.4", OwnerProbe.privateAddress("192.168.1.4").getHostAddress());
        for (String bad : new String[]{"localhost", "127.0.0.1", "8.8.8.8", "192.168.999.1", "10.0.0.01"})
            assertThrows(IOException.class, () -> OwnerProbe.privateAddress(bad));
    }
}
