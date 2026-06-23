package com.mifare.mifare;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class System {
    public static ActivityResultLauncher<Intent> saveFileLauncher;
    public static ActivityResultLauncher<Intent> openFileLauncher;

    public static void setFullScreen(AppCompatActivity a) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            a.getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = a.getWindow().getInsetsController();
            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            // for older versions
            a.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    public static void checkPermission(AppCompatActivity a) {
        if (!Settings.canDrawOverlays(a))
        {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + a.getPackageName()));
            a.startActivity(intent);
        }
    }

    public static DevicePolicyManager mDevicePolicyManager;
    public static ComponentName mComponentName;
    public static void setAdmin(AppCompatActivity a) {
        try {
            // Получаем экземпляры DevicePolicyManager и ComponentName
            mDevicePolicyManager = (DevicePolicyManager) a.getSystemService(Context.DEVICE_POLICY_SERVICE);
            mComponentName = new ComponentName(a, AdminReceiver.class);

            // Проверяем, разрешено ли использование режима блокировки задач в настройках
            if (mDevicePolicyManager.isDeviceOwnerApp(a.getPackageName())) {
                // Устанавливаем наше приложение в качестве владельца устройства, если ещё не установлено
                setDefaultCosuPolicies(true, a);
            } else {
                // В случае, если приложение не является владельцем устройства, необходимо запросить административные права
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Пожалуйста, предоставьте административные права для этого приложения.");
                a.startActivityForResult(intent, 1);
            }
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    // Метод для установки политик режима блокировки задач
    public static void setDefaultCosuPolicies(boolean active, AppCompatActivity a) {
        try {
            if (active) {
                // Включаем режим блокировки задач
                mDevicePolicyManager.setLockTaskPackages(mComponentName, new String[]{a.getPackageName()});
                // Запускаем режим блокировки задач для этого приложения
                a.startLockTask();
            } else {
                // Отключаем режим блокировки задач
                mDevicePolicyManager.clearDeviceOwnerApp(a.getPackageName());
                // Завершаем режим блокировки задач для этого приложения
                a.stopLockTask();
            }
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    public static void openDirectory(AppCompatActivity a, int requestCode) {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);

        a.startActivityForResult(intent, requestCode);
    }

    public static void openFile(AppCompatActivity a) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        a.startActivityForResult(intent, 101);
    }

    public static void dialogsInit() {
        saveFileLauncher = MainActivity.activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == MainActivity.activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            saveSettingsToFile(uri);
                        }
                    }
                });
        openFileLauncher = MainActivity.activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == MainActivity.activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            loadSettingsFromFile(uri);
                        }
                    }
                });
    }

    public static void saveSettings() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "settings.json");
        saveFileLauncher.launch(intent);
    }

    static void saveSettingsToFile(Uri uri) {
        Set settings = Helper.set;
        Gson gson = new Gson();
        String json = gson.toJson(settings);

        try (FileOutputStream fos = (FileOutputStream) MainActivity.activity.getContentResolver().openOutputStream(uri);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(json);
            osw.flush();
            Helper.toast("Настройки сохранены");
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    public static void loadSettings() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        openFileLauncher.launch(intent);
    }

    static void loadSettingsFromFile(Uri uri) {
        try (InputStream inputStream = MainActivity.activity.getContentResolver().openInputStream(uri);
             InputStreamReader isr = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            Helper.set = gson.fromJson(isr, Set.class);
            Helper.saveSettings("set", gson.toJson(Helper.set));
            Helper.toast("Настройки загружены");
            Helper.executeJavascript("loadSettings()");
            Helper.applySettings();
        }
        catch (Exception e) { Helper.toast(e.getMessage()); }
    }

    public static void uploadFile(List<String> items) {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(now);
        String content = TextUtils.join("{n}", items);
        if (Helper.set.yandexToken == null || Helper.set.yandexToken.isEmpty()) {

            SimpleDateFormat sdf1 = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault());
            String fileName = sdf1.format(now) + ".json";

            try {
                // Открываем поток для записи в файл
                ContentResolver resolver = Helper.context.getContentResolver();

                // Создаем URI для нового файла в выбранной папке
                Uri folderUri = Uri.parse(Helper.set.yandexFolder);
                Uri fileUri = createFileUri(folderUri, fileName);

                // Открываем поток для записи в файл
                try (OutputStream outputStream = resolver.openOutputStream(fileUri)) {
                    if (outputStream != null) {
                        // Записываем данные в файл
                        outputStream.write(content.getBytes());
                        outputStream.flush();
                        Log.d("FileSave", "File saved successfully!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FileSave", "Error saving file", e);
            }
        }
        else {
            MainActivity.activity.runOnUiThread(new Runnable() {
                public void run() {
                    String s = String.format("uploadFile('%s')", content);
                    Helper.webView.evaluateJavascript(s, null);
                }
            });
        }
    }

    private static Uri createFileUri(Uri folderUri, String fileName) throws FileNotFoundException {
        Uri parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri));
        // Добавляем имя файла к URI папки
        Uri fileUri = DocumentsContract.createDocument(Helper.context.getContentResolver(), parentDocumentUri, "text/plain", fileName);
        if (fileUri == null) {
            throw new FileNotFoundException("Unable to create file: " + fileName);
        }
        return fileUri;
    }

    public static void hideStatusBar() {
        try {
            Intent intent = new Intent("android.intent.action.STATUSBAR");
            intent.putExtra("status", 0);
            Helper.context.sendBroadcast(intent);
        }
        catch (Exception ex) {
            try {
                Intent intent = new Intent("android.intent.action.STATUSBAR");
                intent.putExtra("status", 0);
                MainActivity.activity.sendBroadcast(intent);
            }
            catch (Exception ex1) { }
        }
    }

    public static void showStatusBar() {
        try {
            Intent intent = new Intent("android.intent.action.STATUSBAR");
            intent.putExtra("status", 1);
            Helper.context.sendBroadcast(intent);
        }
        catch (Exception ex) {
            try {
                Intent intent = new Intent("android.intent.action.STATUSBAR");
                intent.putExtra("status", 1);
                MainActivity.activity.sendBroadcast(intent);
            }
            catch (Exception ex1) { }
        }
    }

    public static void hideNavigationBar() {
        try {
            Intent intent = new Intent();
            String action = "com.android.internal.policy.impl.hideNavigationBar";
            intent.setAction(action);
            Helper.context.sendBroadcast(intent);
        }
        catch (Exception ex) {
            try {
                Intent intent = new Intent();
                String action = "com.android.internal.policy.impl.hideNavigationBar";
                intent.setAction(action);
                MainActivity.activity.sendBroadcast(intent);
            }
            catch (Exception ex1) { }
        }
    }

    public static void showNavigationBar() {
        try {
            Intent intent = new Intent();
            String action = "com.android.internal.policy.impl.showNavigationBar";
            intent.setAction(action);
            Helper.context.sendBroadcast(intent);
        }
        catch (Exception ex) {
            try {
                Intent intent = new Intent();
                String action = "com.android.internal.policy.impl.showNavigationBar";
                intent.setAction(action);
                MainActivity.activity.sendBroadcast(intent);
            }
            catch (Exception ex1) { }
        }
    }
}
