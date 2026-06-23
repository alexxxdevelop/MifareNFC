package com.mifare.mifare;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Helper {
    public static Context context;
    public static WebView webView;
    public static SharedPreferences sp;
    public static Set set;
    public static boolean isError;

    public static void logtoast(Object o) {
        log(o);
        toast(o);
    }

    public static void log(Object o) {
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                String s = String.format("log('%s')", o.toString());
                webView.evaluateJavascript(s, null);
            }
        });
    }

    public static void saveSettings(String name, Object value) {
        String s = value.getClass().getName();
        if (s.equals("java.lang.String")){
            sp.edit().putString(name, value.toString()).commit();
            applySettings();
        }
    }

    public static void toast(Object s) {
        Toast.makeText(context, s.toString(), Toast.LENGTH_LONG).show();
        Log.v("ok", s.toString());
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void showVar(String name, Object value) {
        if (!Helper.set.debugMode) return;
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                String s = String.format("showVar('%s', '%s')", name, value.toString());
                webView.evaluateJavascript(s, null);
            }
        });
    }

    public static void closeDialog() {
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                webView.evaluateJavascript("closeDialog()", null);
            }
        });
    }

    public static void showNfc(String html, String html_cont, int secs) {
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                String s = String.format("showNfc('%s', '%s', %d)", html, html_cont, secs);
                webView.evaluateJavascript(s, null);
            }
        });
    }

    public static void executeJavascript(String f) {
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                webView.evaluateJavascript(f, null);
            }
        });
    }

    public static void applySettings() {
        MainActivity.activity.runOnUiThread(new Runnable() {
            public void run() {
                if (Helper.set.statusBar == true) System.hideStatusBar();
                else if (Helper.set.statusBar == false) System.showStatusBar();
                if (Helper.set.navigationBar == true) System.hideNavigationBar();
                else if (Helper.set.navigationBar == false) System.showNavigationBar();
            }
        });
    }
}