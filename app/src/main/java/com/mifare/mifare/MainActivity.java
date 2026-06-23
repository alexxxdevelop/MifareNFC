package com.mifare.mifare;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.innohi.YNHAPI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity {
    public static MainActivity activity;
    public WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        activity = this;
        Helper.context = getBaseContext();

        System.setFullScreen(this);
        System.checkPermission(this);
        loadBrowser();
        NFC.nfcInit(this);
        System.dialogsInit();
        System.setAdmin(this);

        // Создаем Handler для выполнения задачи с задержкой
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {

                }
                catch (Exception e) { Helper.log(e.getMessage()); }
            }
        }, 2000);*/
    }

    @Override
    public void onDestroy(){
        SP.CloseComPort();
        super.onDestroy();
    }

    //region WebView
    ValueCallback<Uri[]> upload;

    private void loadBrowser() {
        webView = findViewById(R.id.webView);
        webView.setBackgroundColor(0x00000000);
        Helper.webView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webView.loadUrl("file:///android_asset/wwwroot/index.html");

        webView.setWebChromeClient(new WebChromeClient1());
    }

    private class WebChromeClient1 extends WebChromeClient {
        //Перехват окна выбора файлов
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            Intent intent = fileChooserParams.createIntent();
            upload = filePathCallback;
            startActivityForResult(intent, 101);
            return true;
        }
        //Перехват вывода в консоль
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.v("browser", consoleMessage.message() + " -- From line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            //Окно выбора файла из webView
            if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            /*if (upload == null) return;
            Uri[] uris = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            Helper.log(uris[0]);
            upload.onReceiveValue(uris);
            upload = null;*/

                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                webView.evaluateJavascript("$('#logo').text('" + uri + "');", null);
            }
            //Окно выбора папки
            else if (requestCode == 102 && resultCode == Activity.RESULT_OK) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    //Сохранение разрешений
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    webView.evaluateJavascript("$('#media').val('" + uri + "');", null);
                }
            }
            else if (requestCode == 103 && resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    webView.evaluateJavascript("$('#yandexFolder').val('" + uri + "');", null);
                }
            }
            else if (requestCode == 1) {
                if (resultCode == Activity.RESULT_OK) {
                    Helper.toast("Административные права предоставлены");
                    System.setDefaultCosuPolicies(true, this);
                } else {
                    Helper.toast("Административные права не были предоставлены, приложение не может работать в режиме блокировки задач");
                }
            }
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }
    //endregion

    //region NFC
    @Override
    protected void onResume() {
        super.onResume();
        NFC.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (NFC.mNfcAdapter != null) NFC.mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NFC.onNewIntent(intent, this);
    }
    //endregion
}