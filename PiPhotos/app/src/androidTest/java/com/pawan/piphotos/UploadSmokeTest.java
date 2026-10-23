package com.pawan.piphotos;

import android.app.Instrumentation;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import org.json.*;

/** Device integration test. Use only with a separate, disposable test backend. */
public class UploadSmokeTest extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { arguments = args; start(); }
    @Override public void onStart() {
        Context context = getTargetContext();
        String previousUrl = Api.base(context);
        String previousStatus = Api.prefs(context).getString("upload_status", "");
        File sample = new File(context.getCacheDir(), "PiPhotos-integration-test.jpg");
        Bundle result = new Bundle();
        int resultCode = -1;
        try {
            String testUrl = arguments.getString("testUrl", "");
            if (!testUrl.endsWith(":8091") || Api.key(context).isEmpty())
                throw new IllegalArgumentException("Use a disposable test backend on port 8091 and configure the app key first");
            Api.prefs(context).edit().putString("url", testUrl).commit();
            Bitmap bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.rgb(40,117,104));
            Paint paint = new Paint(); paint.setColor(Color.WHITE); paint.setTextSize(38);
            canvas.drawText("Pi Photos upload test", 75, 240, paint);
            try (OutputStream out = new FileOutputStream(sample)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); }
            bitmap.recycle();
            startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            for (int attempt = 0; attempt < 2; attempt++) {
                Api.prefs(context).edit().putString("upload_status", "pending-test").commit();
                ArrayList<Uri> uris = new ArrayList<>(); uris.add(Uri.fromFile(sample));
                Intent intent = new Intent(context, UploadService.class).putParcelableArrayListExtra("photos", uris);
                runOnMainSync(() -> context.startForegroundService(intent));
                long deadline = SystemClock.elapsedRealtime() + 45000;
                String status;
                do {
                    Thread.sleep(200);
                    status = Api.prefs(context).getString("upload_status", "");
                } while (!status.contains(" failed") && SystemClock.elapsedRealtime() < deadline);
                String expected = attempt == 0 ? "1 uploaded · 0 already saved · 0 failed" : "0 uploaded · 1 already saved · 0 failed";
                if (!expected.equals(status)) throw new AssertionError("Upload attempt " + attempt + ": " + status);
                // Allow the prior service instance to complete its lifecycle before the retry.
                waitForIdleSync(); Thread.sleep(300);
            }
            HttpURLConnection list = Api.open(context, "/api/photos");
            JSONObject library;
            try { library = new JSONObject(Api.response(list)); } finally { list.disconnect(); }
            if (library.getInt("total") != 1) throw new AssertionError("Expected one deduplicated photo");
            long id = library.getJSONArray("photos").getJSONObject(0).getLong("id");
            for (String kind : new String[]{"original", "thumbnail"}) {
                HttpURLConnection image = Api.open(context, "/api/photos/" + id + "/" + kind);
                try {
                    if (image.getResponseCode() != 200) throw new AssertionError("Cannot fetch " + kind);
                    byte[] bytes;
                    try (InputStream in = image.getInputStream()) { bytes = in.readAllBytes(); }
                    if ("original".equals(kind) && !Arrays.equals(bytes, java.nio.file.Files.readAllBytes(sample.toPath())))
                        throw new AssertionError("Original bytes changed");
                    Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (decoded == null) throw new AssertionError("Cannot display " + kind);
                    decoded.recycle();
                } finally { image.disconnect(); }
            }
            result.putString("result", "PASS: real phone upload service, duplicate retry, gallery listing, exact original, thumbnail decoding");
        } catch (Throwable error) {
            result.putString("error", error.toString()); resultCode = 0;
        } finally {
            Api.prefs(context).edit().putString("url", previousUrl).putString("upload_status", previousStatus).commit();
            sample.delete();
        }
        finish(resultCode, result);
    }
}
