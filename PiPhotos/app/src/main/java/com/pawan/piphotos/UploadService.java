package com.pawan.piphotos;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.json.JSONObject;

public class UploadService extends Service {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile HttpURLConnection active;
    private volatile boolean cancelled;
    static final String CHANNEL = "uploads";
    static final int NOTICE = 12;

    private Notification notification(String text, boolean running) {
        Intent open = new Intent(this, MainActivity.class);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(com.pawan.piphotos.R.drawable.ic_photo)
            .setContentTitle("Pi Photos").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(running).setOnlyAlertOnce(true);
        if (running) {
            Intent cancel = new Intent(this, UploadService.class).setAction("cancel");
            b.addAction(new Notification.Action.Builder(null, "Cancel", PendingIntent.getService(this, 1, cancel,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)).build());
        }
        return b.build();
    }
    private void status(String text, boolean running) {
        Api.prefs(this).edit().putString("upload_status", text).apply();
        getSystemService(NotificationManager.class).notify(NOTICE, notification(text, running));
        sendBroadcast(new Intent("com.pawan.piphotos.UPLOAD_STATUS").setPackage(getPackageName()));
    }
    @Override public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel(CHANNEL, "Photo uploads", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "cancel".equals(intent.getAction())) {
            cancelled = true;
            HttpURLConnection connection = active;
            if (connection != null) connection.disconnect();
            worker.shutdownNow();
            status("Upload cancelled. Completed photos are saved; select remaining photos to retry.", false);
            stopForeground(STOP_FOREGROUND_DETACH);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTICE, notification("Preparing upload…", true), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        ArrayList<Uri> photos = intent == null ? null : intent.getParcelableArrayListExtra("photos", Uri.class);
        if (photos == null || photos.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(startId); return START_NOT_STICKY; }
        String base = Api.base(this), key = Api.key(this);
        worker.execute(() -> {
            int uploaded = 0, duplicate = 0, failed = 0;
            String lastError = "";
            for (int i = 0; i < photos.size() && !cancelled; i++) {
                Uri uri = photos.get(i);
                status("Uploading " + (i + 1) + " of " + photos.size() + "…", true);
                try {
                    String name = "Photo";
                    long length = -1;
                    try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                        if (c != null && c.moveToFirst()) { name = c.getString(0); if (!c.isNull(1)) length = c.getLong(1); }
                    }
                    if (length > 100L * 1024 * 1024) throw new IOException("Photo exceeds 100 MB");
                    HttpURLConnection con = Api.open(base, key, "/api/photos");
                    active = con;
                    try {
                        con.setRequestMethod("POST"); con.setDoOutput(true);
                        con.setRequestProperty("Content-Type", "application/octet-stream");
                        con.setRequestProperty("X-Photo-Name", URLEncoder.encode(name == null ? "Photo" : name, "UTF-8").replace("+", "%20"));
                        if (length >= 0) con.setFixedLengthStreamingMode(length); else con.setChunkedStreamingMode(256 * 1024);
                        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = con.getOutputStream()) {
                            if (in == null) throw new IOException("Cannot open photo");
                            byte[] buffer = new byte[256 * 1024];
                            int n;
                            while ((n = in.read(buffer)) != -1) {
                                if (cancelled) throw new IOException("Cancelled");
                                out.write(buffer, 0, n);
                            }
                        }
                        JSONObject result = new JSONObject(Api.response(con));
                        if (result.optBoolean("duplicate")) duplicate++; else uploaded++;
                    } finally { con.disconnect(); active = null; }
                } catch (Exception e) { failed++; lastError = e.getMessage() == null ? "Connection failed" : e.getMessage(); }
            }
            if (!cancelled) {
                status(uploaded + " uploaded · " + duplicate + " already saved · " + failed + " failed"
                    + (failed > 0 ? ". " + lastError + ". Select failed photos to retry." : ""), false);
                if (stopSelfResult(startId)) stopForeground(STOP_FOREGROUND_DETACH);
            }
        });
        return START_NOT_STICKY;
    }
    @Override public void onTimeout(int startId, int fgsType) {
        cancelled = true;
        if (active != null) active.disconnect();
        status("Android stopped a long upload. Select remaining photos to retry.", false);
        stopForeground(STOP_FOREGROUND_DETACH); stopSelf();
    }
    @Override public void onDestroy() { cancelled = true; if (active != null) active.disconnect(); worker.shutdownNow(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
