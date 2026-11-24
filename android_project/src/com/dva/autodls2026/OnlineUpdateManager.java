package com.dva.autodls2026;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OnlineUpdateManager {
    public static final String FILE_NAME = "dls_script.json";
    public static final String PREF_NAME = "auto_dls_prefs";
    public static final String PREF_KEY_URL = "update_url";
    public static final String DEFAULT_UPDATE_URL = "https://dpaste.org/B3ELS/raw";

    public interface UpdateCallback {
        void onSuccess(String version, boolean isNewer);
        void onError(String error);
    }

    public static String getUpdateUrl(Context context) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(PREF_KEY_URL, DEFAULT_UPDATE_URL);
    }

    public static void setUpdateUrl(Context context, String url) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(PREF_KEY_URL, url).apply();
    }

    public static JSONObject loadConfig(Context context) {
        try {
            File localFile = new File(context.getFilesDir(), FILE_NAME);
            InputStream is;
            if (localFile.exists()) {
                is = new FileInputStream(localFile);
            } else {
                is = context.getAssets().open(FILE_NAME);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultConfig();
        }
    }

    public static void checkAndUpdate(final Context context, final UpdateCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject currentConfig = loadConfig(context);
                    String currentVersion = currentConfig.optString("version", "1.0.0");
                    String updateUrl = getUpdateUrl(context);

                    URL url = new URL(updateUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 AutoDLS2026");
                    conn.setRequestMethod("GET");
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            sb.append(line);
                        }
                        in.close();

                        JSONObject newConfig = new JSONObject(sb.toString());
                        String newVersion = newConfig.optString("version", "1.0.0");

                        // Lưu vào bộ nhớ máy
                        File localFile = new File(context.getFilesDir(), FILE_NAME);
                        FileOutputStream fos = new FileOutputStream(localFile);
                        fos.write(sb.toString().getBytes("UTF-8"));
                        fos.close();

                        boolean isNewer = !newVersion.equals(currentVersion);
                        if (callback != null) {
                            callback.onSuccess(newVersion, isNewer);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Mã lỗi máy chủ: " + responseCode + " (" + updateUrl + ")");
                        }
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            }
        }).start();
    }

    public static JSONObject createDefaultConfig() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("version", "1.0.0");
            obj.put("updateUrl", DEFAULT_UPDATE_URL);
            obj.put("pressB_duration_ms", 1400);
            obj.put("tapA_delay_ms", 200);
            obj.put("skip_delay_ms", 150);
            obj.put("continue_interval", 3);
            obj.put("match_interval", 10);
            JSONObject coords = new JSONObject();
            coords.put("btn_b", new JSONArray("[88.5, 84.5]"));
            coords.put("btn_a", new JSONArray("[95.5, 69.0]"));
            coords.put("btn_c", new JSONArray("[82.0, 67.0]"));
            coords.put("continue_br", new JSONArray("[86.0, 89.0]"));
            coords.put("continue_c", new JSONArray("[50.0, 85.0]"));
            coords.put("skip_cutscene", new JSONArray("[50.0, 50.0]"));
            coords.put("play_match", new JSONArray("[82.0, 82.0]"));
            coords.put("claim_reward", new JSONArray("[50.0, 72.0]"));
            coords.put("close_ad", new JSONArray("[94.5, 7.5]"));
            obj.put("coords", coords);
        } catch (Exception ignored) {}
        return obj;
    }
}

