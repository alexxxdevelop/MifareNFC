package com.mifare.mifare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WebAppInterface {
    Context context;

    /** Instantiate the interface and set the context. */
    WebAppInterface(Context c) {
        context = c;
    }

    /** Show a toast from the web page. */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public String loadSettings() {
        try {
            Helper.sp = Helper.context.getSharedPreferences("Settings", Helper.context.MODE_PRIVATE);
            //Helper.sp.edit().clear().commit();

            Gson gson = new Gson();
            String s = Helper.sp.getString("set", null);
            if (s == null) {
                Helper.set = new Set();
                Helper.saveSettings("set", gson.toJson(Helper.set));
                s = Helper.sp.getString("set", null);
            } else Helper.set = gson.fromJson(s, Set.class);
            if (Helper.set.deviceType == 1) SP.init();
            Helper.applySettings();

            return s;
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
        return "";
    }

    @JavascriptInterface
    public void saveSettings(String json) {
        try {
            Helper.saveSettings("set", json);
            Helper.set = new Gson().fromJson(json, Set.class);
            if (Helper.set.deviceType == 1) SP.init();
            Helper.applySettings();
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    @JavascriptInterface
    public void showLogo() {
        System.openFile(MainActivity.activity);
    }

    @JavascriptInterface
    public void showMedia(int requestCode) {
        System.openDirectory(MainActivity.activity, requestCode);
    }

    @JavascriptInterface
    public String getMedia() {
        if (Objects.equals(Helper.set.media, "")) return null;
        Uri uri = Uri.parse(Helper.set.media);
        //Взять файлы из папки
        DocumentFile dir = DocumentFile.fromTreeUri(Helper.context, uri);
        DocumentFile[] files = dir.listFiles();

        /*StringBuilder sb = new StringBuilder("<video style=\"height: 400px\">");
        for (int i = 0; i < files.length; i++) {
            sb.append("<source src=\"" + files[i].getUri() + "\" type=\"video/mp4\" />");
        }
        sb.append("Your device does not support the video tag.");
        sb.append("</video>");*/
        List<String> sb = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            sb.add(String.valueOf(files[i].getUri()));
        }
        return String.join("|", sb);
    }

    @JavascriptInterface
    public void exitAndroid() {
        System.setDefaultCosuPolicies(false, MainActivity.activity);
        MainActivity.activity.finishAffinity();
    }

    @JavascriptInterface
    public void openWifi() {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @JavascriptInterface
    public void openDatetime() {
        Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @JavascriptInterface
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @JavascriptInterface
    public void saveSettingsToFile() {
        System.saveSettings();
    }

    @JavascriptInterface
    public void loadSettingsFromFile() {
        System.loadSettings();
    }
}