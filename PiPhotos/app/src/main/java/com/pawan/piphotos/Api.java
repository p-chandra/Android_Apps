package com.pawan.piphotos;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

final class Api {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("connection", Context.MODE_PRIVATE); }
    static String base(Context c) { return prefs(c).getString("url", "http://100.117.156.54:8090"); }
    static String key(Context c) { return prefs(c).getString("key", ""); }
    static HttpURLConnection open(Context c, String path) throws IOException {
        return open(base(c), key(c), path);
    }
    static HttpURLConnection open(String base, String key, String path) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(base + path).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(120000);
        con.setInstanceFollowRedirects(false);
        con.setRequestProperty("Authorization", "Bearer " + key);
        return con;
    }
    static String response(HttpURLConnection con) throws IOException {
        int code = con.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream();
        String body = "";
        if (stream != null) try (InputStream in = stream) { body = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        if (code < 200 || code >= 300) {
            String error = "Server returned " + code;
            try { error = new JSONObject(body).optString("error", error); } catch (Exception ignored) { }
            throw new IOException(error);
        }
        return body;
    }
    static boolean validUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                && uri.getHost() != null && uri.getUserInfo() == null && uri.getRawQuery() == null
                && uri.getFragment() == null && (uri.getPath().isEmpty() || "/".equals(uri.getPath()));
        } catch (Exception e) { return false; }
    }
}
