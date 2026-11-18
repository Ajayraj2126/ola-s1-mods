package com.ajayraj2126.s1control;

import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.security.KeyStore;
import java.security.Signature;
import java.security.MessageDigest;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OwnerDeviceTest {
    @Test public void keystoreSignatureMatchesAndroidChallengeFormat() throws Exception {
        PhoneIdentity key = PhoneIdentity.load();
        byte[] message = "Android challenge test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] signature = key.sign(MessageDigest.getInstance("SHA-1").digest(message));
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        Signature check = Signature.getInstance("SHA1withRSA");
        check.initVerify(ks.getCertificate("aj-owner-connection-v1")); check.update(message);
        assertTrue(check.verify(signature));
        assertArrayEquals(key.publicKey(), PhoneIdentity.load().publicKey());
    }
    @Test public void launcherOpensWithAnOfflineSession() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        android.app.Activity activity = instrumentation.startActivitySync(intent);
        try {
            instrumentation.runOnMainSync(() -> assertFalse(activity.isFinishing()));
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
}
