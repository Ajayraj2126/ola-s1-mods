package com.ajayraj2126.s1control;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int generation, failures;
    private boolean visible;
    private Socket pending;
    private String host;
    private SharedPreferences settings;
    private EditText address;
    private TextView status;
    private final Runnable retry = () -> { if (visible && !host.isEmpty()) probe(false); };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = getSharedPreferences("owner-approved-session", MODE_PRIVATE);
        host = settings.getString("address", "");
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int inset = Math.round(24 * getResources().getDisplayMetrics().density);
        body.setPadding(inset, inset, inset, inset);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(body);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("S1 Control by AJ");
        title.setTextSize(26);
        body.addView(title);
        TextView help = new TextView(this);
        help.setText("On a trusted hotspot, enter the parked scooter’s address. Its Android system must already support network debugging with an on-screen approval prompt.");
        body.addView(help);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        address.setHint("Scooter IPv4 address");
        address.setText(host);
        body.addView(address);
        button(body, "Connect", v -> connect());
        button(body, "Disconnect and forget address", v -> {
            cancel();
            host = "";
            settings.edit().remove("address").apply();
            address.setText("");
            status.setText("Disconnected");
        });
        status = new TextView(this);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setText("Disconnected");
        status.setTextSize(17);
        body.addView(status);
        button(body, "AJ releases", v -> browse("releases"));
        button(body, "License and credits", v -> browse("blob/main/NOTICE.md"));
    }

    private void browse(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ajayraj2126/ola-s1-mods/" + path));
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, "A browser is required.", Toast.LENGTH_SHORT).show(); }
    }
    private void button(LinearLayout body, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(action);
        body.addView(button);
    }
    private void connect() {
        final InetAddress selected;
        try { selected = OwnerProbe.privateAddress(address.getText().toString()); }
        catch (Exception e) { address.setError(e.getMessage()); return; }
        cancel();
        settings.edit().remove("address").apply();
        host = selected.getHostAddress();
        failures = 0;
        probe(true);
    }
    private void probe(boolean allowPrompt) {
        final int turn = generation;
        final String target = host;
        final Socket socket = new Socket();
        pending = socket;
        if (allowPrompt) status.setText("Waiting for approval on the scooter…");
        worker.execute(() -> {
            try (Socket connection = socket) {
                connection.connect(new InetSocketAddress(OwnerProbe.privateAddress(target), 5555), 3500);
                connection.setSoTimeout(allowPrompt ? 30000 : 4000);
                String model = OwnerProbe.verify(connection.getInputStream(), connection.getOutputStream(),
                    PhoneIdentity.load(), allowPrompt);
                main.post(() -> {
                    if (generation != turn) return;
                    pending = null;
                    failures = 0;
                    settings.edit().putString("address", target).apply();
                    status.setText("Last verified at " + android.text.format.DateFormat.getTimeFormat(this)
                        .format(new java.util.Date()) + " · " + model);
                    main.postDelayed(retry, 15000);
                });
            } catch (Exception failure) {
                main.post(() -> {
                    if (generation != turn) return;
                    pending = null;
                    if (allowPrompt) {
                        host = "";
                        status.setText("Connection failed. Check the hotspot, address and device approval. " +
                            java.util.Objects.toString(failure.getMessage(), ""));
                    } else {
                        int seconds = Math.min(60, 3 << Math.min(failures++, 5));
                        status.setText("Device unavailable. Checking the saved address again in " + seconds + " s.");
                        main.postDelayed(retry, seconds * 1000L);
                    }
                });
            }
        });
    }
    private void cancel() {
        generation++;
        main.removeCallbacks(retry);
        if (pending != null) {
            try { pending.close(); } catch (java.io.IOException ignored) { }
            pending = null;
        }
    }
    @Override protected void onStart() {
        super.onStart();
        visible = true;
        host = settings.getString("address", "");
        if (!host.isEmpty()) { status.setText("Checking the saved address…"); retry.run(); }
        else status.setText("Disconnected");
    }
    @Override protected void onStop() { visible = false; cancel(); super.onStop(); }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
