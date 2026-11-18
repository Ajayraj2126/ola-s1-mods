package com.ajayraj2126.s1control;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/** A per-installation device credential, independent of the APK release certificate. */
final class PhoneIdentity implements OwnerProbe.Identity {
    private static final String ALIAS = "aj-owner-connection-v1";
    private final PrivateKey key;
    private final RSAPublicKey pub;

    private PhoneIdentity(PrivateKey key, RSAPublicKey pub) { this.key = key; this.pub = pub; }

    static synchronized PhoneIdentity load() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setKeySize(2048).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256).build());
            generator.generateKeyPair();
        }
        return new PhoneIdentity((PrivateKey) store.getKey(ALIAS, null),
            (RSAPublicKey) store.getCertificate(ALIAS).getPublicKey());
    }

    public byte[] sign(byte[] token) throws GeneralSecurityException {
        // Android supplies a 20-byte prehashed token. PKCS#1 DigestInfo for SHA-1.
        if (token.length != 20) throw new GeneralSecurityException("Invalid Android challenge");
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initSign(key);
        signature.update(new byte[]{0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14});
        signature.update(token);
        return signature.sign();
    }

    public byte[] publicKey() {
        // Android's RSA public-key file format stores 64 little-endian 32-bit words.
        BigInteger n = pub.getModulus();
        ByteBuffer data = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(64).putInt(n.modInverse(BigInteger.ONE.shiftLeft(32)).negate().intValue());
        putWords(data, n);
        putWords(data, BigInteger.ONE.shiftLeft(4096).mod(n));
        data.putInt(pub.getPublicExponent().intValue());
        return (Base64.getEncoder().encodeToString(data.array()) + " S1-Control-AJ\0").getBytes(StandardCharsets.US_ASCII);
    }

    private static void putWords(ByteBuffer out, BigInteger value) {
        for (int i = 0; i < 64; i++) { out.putInt(value.intValue()); value = value.shiftRight(32); }
    }
}
