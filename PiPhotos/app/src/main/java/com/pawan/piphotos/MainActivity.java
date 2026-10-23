package com.pawan.piphotos;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final ExecutorService images = Executors.newFixedThreadPool(3);
    private final ArrayList<JSONObject> photos = new ArrayList<>();
    private final LruCache<String, Bitmap> cache = new LruCache<>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private TextView subtitle, status;
    private Button more, refresh;
    private PhotoAdapter adapter;
    private boolean loading;
    private Long next;
    private int generation;
    private final BroadcastReceiver updates = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            status.setText(Api.prefs(c).getString("upload_status", ""));
            if (!status.getText().toString().startsWith("Uploading")) load(true);
        }
    };
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); return b;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(245,247,243));
        page.setPadding(dp(16), dp(8), dp(16), dp(6));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets edges = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            page.setPadding(dp(16) + edges.left, dp(8) + edges.top, dp(16) + edges.right, dp(6) + edges.bottom);
            return insets;
        });
        LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Pi Photos", 32, Color.rgb(25,54,47)); title.setTypeface(null, Typeface.BOLD);
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(60), 1));
        heading.addView(button("Settings", this::settings)); page.addView(heading);
        subtitle = text("Your private photo library", 15, Color.DKGRAY); page.addView(subtitle);
        LinearLayout actions = new LinearLayout(this);
        actions.addView(button("＋ Upload photos", this::pick), new LinearLayout.LayoutParams(0, dp(58), 1));
        refresh = button("Refresh", () -> load(true)); actions.addView(refresh); page.addView(actions);
        status = text(Api.prefs(this).getString("upload_status", "Keep Tailscale connected on your phone and Pi."), 13, Color.DKGRAY);
        status.setPadding(0, dp(5), 0, dp(10)); page.addView(status);
        GridView grid = new GridView(this); grid.setNumColumns(3); grid.setHorizontalSpacing(dp(5)); grid.setVerticalSpacing(dp(5));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); adapter = new PhotoAdapter(); grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, v, position, id) -> detail(photos.get(position)));
        page.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        more = button("Load more", () -> load(false)); more.setVisibility(View.GONE); page.addView(more);
        setContentView(page);
        if (Api.key(this).isEmpty()) {
            subtitle.setText("Connect to your Raspberry Pi in Settings");
            status.setText("Add your server address and access key to get started.");
        } else load(true);
    }
    @Override protected void onStart() { super.onStart(); registerReceiver(updates, new IntentFilter("com.pawan.piphotos.UPLOAD_STATUS"), Context.RECEIVER_NOT_EXPORTED); }
    @Override protected void onStop() { unregisterReceiver(updates); super.onStop(); }
    @Override protected void onDestroy() { generation++; network.shutdownNow(); images.shutdownNow(); cache.evictAll(); super.onDestroy(); }

    private void settings() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), 0);
        form.addView(text("Tailscale server address", 14, Color.DKGRAY));
        EditText url = new EditText(this); url.setSingleLine(); url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI); url.setText(Api.base(this)); form.addView(url);
        form.addView(text("Access key", 14, Color.DKGRAY));
        EditText key = new EditText(this); key.setSingleLine(); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); key.setText(Api.key(this)); form.addView(key);
        form.addView(text("Uploads keep original quality. Photos stay on the Pi; selecting a photo does not remove it from your phone.", 13, Color.DKGRAY));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Connect your Pi").setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save & connect", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String address = url.getText().toString().trim().replaceAll("/+$", "");
            String token = key.getText().toString().trim();
            if (!Api.validUrl(address)) { url.setError("Use http://100.117.156.54:8090 or an HTTPS server address"); return; }
            if (token.length() < 32 || token.contains("\n") || token.contains("\r")) { key.setError("Enter the complete server access key"); return; }
            generation++; loading = false; cache.evictAll(); photos.clear(); adapter.notifyDataSetChanged();
            Api.prefs(this).edit().putString("url", address).putString("key", token).apply();
            dialog.dismiss(); load(true);
        })); dialog.show();
    }
    private void load(boolean reset) {
        if (loading || Api.key(this).isEmpty() || (!reset && next == null)) return;
        loading = true; refresh.setEnabled(false); more.setEnabled(false);
        subtitle.setText("Connecting to your Pi…");
        int requestGeneration = generation;
        String base = Api.base(this), key = Api.key(this);
        String path = "/api/photos?limit=60" + (!reset ? "&before=" + next : "");
        network.execute(() -> {
            try {
                HttpURLConnection con = Api.open(base, key, path);
                JSONObject result;
                try { result = new JSONObject(Api.response(con)); } finally { con.disconnect(); }
                runOnUiThread(() -> {
                    if (isDestroyed() || requestGeneration != generation) return;
                    if (reset) photos.clear();
                    JSONArray list = result.optJSONArray("photos");
                    if (list != null) for (int i = 0; i < list.length(); i++) photos.add(list.optJSONObject(i));
                    next = result.isNull("next_before") ? null : result.optLong("next_before");
                    subtitle.setText(result.optInt("total") + " photos · Connected to your Pi");
                    if (result.optBoolean("indexing")) status.setText("Finding existing photos in Photos… Tap Refresh shortly.");
                    else if (photos.isEmpty()) status.setText("Your library is empty. Upload your first photos.");
                    loading = false; refresh.setEnabled(true); more.setEnabled(true);
                    more.setVisibility(next == null ? View.GONE : View.VISIBLE); adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isDestroyed() || requestGeneration != generation) return;
                    loading = false; refresh.setEnabled(true); more.setEnabled(true);
                    subtitle.setText("Could not connect"); status.setText("Check Tailscale and Settings. " + e.getMessage());
                });
            }
        });
    }
    private void pick() {
        if (Api.key(this).isEmpty()) { settings(); return; }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, 8);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 8 || resultCode != RESULT_OK || data == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) return;
        Intent upload = new Intent(this, UploadService.class).putParcelableArrayListExtra("photos", uris);
        ClipData clips = ClipData.newRawUri("photos", uris.get(0));
        for (int i = 1; i < uris.size(); i++) clips.addItem(new ClipData.Item(uris.get(i)));
        upload.setClipData(clips); upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        status.setText("Preparing " + uris.size() + " photos for upload…");
        startForegroundService(upload);
    }
    private class PhotoAdapter extends BaseAdapter {
        public int getCount() { return photos.size(); }
        public Object getItem(int p) { return photos.get(p); }
        public long getItemId(int p) { return photos.get(p).optLong("id"); }
        public View getView(int position, View convert, android.view.ViewGroup parent) {
            ImageView image = convert instanceof ImageView ? (ImageView) convert : new ImageView(MainActivity.this);
            int side = (getResources().getDisplayMetrics().widthPixels - dp(42)) / 3;
            image.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, side));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP); image.setBackgroundColor(Color.rgb(222,231,224));
            JSONObject photo = photos.get(position); String id = Long.toString(photo.optLong("id"));
            image.setContentDescription(photo.optString("name")); image.setTag(id);
            Bitmap cached = cache.get(id); image.setImageBitmap(cached);
            int currentGeneration = generation;
            String base = Api.base(MainActivity.this), key = Api.key(MainActivity.this);
            if (cached == null) images.execute(() -> {
                try {
                    if (currentGeneration != generation) return;
                    HttpURLConnection con = Api.open(base, key, "/api/photos/" + id + "/thumbnail");
                    Bitmap bitmap;
                    try {
                        if (con.getResponseCode() != 200) return;
                        try (InputStream in = con.getInputStream()) { bitmap = BitmapFactory.decodeStream(in); }
                    } finally { con.disconnect(); }
                    if (bitmap != null && currentGeneration == generation) {
                        cache.put(id, bitmap);
                        runOnUiThread(() -> { if (!isDestroyed() && currentGeneration == generation && id.equals(image.getTag())) image.setImageBitmap(bitmap); });
                    }
                } catch (Exception ignored) { }
            });
            return image;
        }
    }
    private void detail(JSONObject photo) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(dp(12), dp(36), dp(12), dp(32));
        layout.setBackgroundColor(Color.rgb(245,247,243));
        layout.addView(button("← Back to library", dialog::dismiss));
        TextView title = text(photo.optString("name"), 18, Color.rgb(25,54,47)); layout.addView(title);
        TextView info = text(photo.optInt("width") + " × " + photo.optInt("height") + " · " + String.format(Locale.US, "%.1f MB", photo.optLong("bytes") / 1048576.0) + " · Pinch to zoom", 13, Color.DKGRAY); layout.addView(info);
        ZoomImage image = new ZoomImage(this);
        FrameLayout viewport = new FrameLayout(this); viewport.addView(image, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(viewport, new LinearLayout.LayoutParams(-1, 0, 1));
        Bitmap thumb = cache.get(Long.toString(photo.optLong("id"))); image.setImageBitmap(thumb);
        TextView progress = text("Loading photo…", 13, Color.DKGRAY); layout.addView(progress);
        dialog.setContentView(layout); dialog.show(); dialog.getWindow().setLayout(-1, -1);
        String base = Api.base(this), key = Api.key(this);
        images.execute(() -> {
            File temp = null;
            try {
                temp = File.createTempFile("view-", ".image", getCacheDir());
                HttpURLConnection con = Api.open(base, key, "/api/photos/" + photo.optLong("id") + "/original");
                try {
                    if (con.getResponseCode() != 200) throw new IOException("Cannot load photo");
                    try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(temp)) {
                        byte[] buffer = new byte[65536]; int n;
                        while ((n = in.read(buffer)) != -1) {
                            if (!dialog.isShowing()) return;
                            out.write(buffer, 0, n);
                        }
                    }
                } finally { con.disconnect(); }
                BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(temp.getPath(), opts); opts.inSampleSize = 1;
                while (Math.max(opts.outWidth, opts.outHeight) / opts.inSampleSize > 2560) opts.inSampleSize *= 2;
                opts.inJustDecodeBounds = false;
                Bitmap decoded = BitmapFactory.decodeFile(temp.getPath(), opts);
                if (decoded == null) throw new IOException("This photo format cannot be displayed on this phone");
                android.media.ExifInterface exif = new android.media.ExifInterface(temp);
                Matrix rotation = new Matrix(); int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1);
                switch (orientation) {
                    case 2: rotation.setScale(-1, 1); break;
                    case 3: rotation.setRotate(180); break;
                    case 4: rotation.setScale(1, -1); break;
                    case 5: rotation.setRotate(90); rotation.postScale(-1, 1); break;
                    case 6: rotation.setRotate(90); break;
                    case 7: rotation.setRotate(270); rotation.postScale(-1, 1); break;
                    case 8: rotation.setRotate(270); break;
                }
                Bitmap display = rotation.isIdentity() ? decoded : Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), rotation, true);
                runOnUiThread(() -> { if (!isDestroyed() && dialog.isShowing()) { image.setImageBitmap(display); progress.setText("Original quality stored on your Pi"); } });
            } catch (Exception e) {
                runOnUiThread(() -> { if (dialog.isShowing()) progress.setText("Could not load photo: " + e.getMessage()); });
            } finally { if (temp != null) temp.delete(); }
        });
    }
    private static class ZoomImage extends ImageView {
        private float scale = 1, x, y;
        private final ScaleGestureDetector detector;
        ZoomImage(Context c) {
            super(c); setScaleType(ScaleType.FIT_CENTER);
            detector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    scale = Math.max(1, Math.min(5, scale * d.getScaleFactor()));
                    setScaleX(scale); setScaleY(scale);
                    if (scale == 1) { setTranslationX(0); setTranslationY(0); }
                    return true;
                }
            });
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            detector.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { x = e.getRawX(); y = e.getRawY(); }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !detector.isInProgress() && e.getPointerCount() == 1) {
                if (scale > 1) { setTranslationX(getTranslationX() + e.getRawX() - x); setTranslationY(getTranslationY() + e.getRawY() - y); }
                x = e.getRawX(); y = e.getRawY();
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
