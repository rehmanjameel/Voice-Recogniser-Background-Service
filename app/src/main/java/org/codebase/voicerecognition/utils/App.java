package org.codebase.voicerecognition.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;

import net.gotev.speech.Logger;

import org.codebase.voicerecognition.services.VoiceBackgroundService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class App extends Application {

    private static Context sContext;
    public static final String KEY_LOGIN = "login";
    public static final String KEY_QR = "qr";


    @Override
    public void onCreate() {
        super.onCreate();

        sContext = getApplicationContext();
        Logger.setLogLevel(Logger.LogLevel.DEBUG);

    }

    public static Context getContext() {
        return sContext;
    }

    public static SharedPreferences getPreferenceManager() {
        return getContext().getSharedPreferences("shared_prefs", MODE_PRIVATE);
    }

    public static void clearSettings() {
        SharedPreferences sharedPreferences = getPreferenceManager();

        sharedPreferences.edit().clear().apply();
    }

    public static void saveDataToSharedPreferences(String key, String value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String getStringFromSharedPreferences(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getString(key, "");
    }

    public static void serviceRunning(String KEY_LOGIN, boolean type) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_LOGIN, type).apply();
    }

    public static boolean isServiceRunning(String KEY_LOGIN) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_LOGIN, false);
    }

    public static void qrCodeScanned(boolean type) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_QR, type).apply();
    }

    public static boolean isQrScanned() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_QR, false);
    }

    /*
        3. Using Gson
     */
    public static void storeDataArrayList3(String KEY_ARRAY_LIST, List<String> stringList) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        Gson gson = new Gson();
        String s = gson.toJson(stringList);
        sharedPreferences.edit().putString(KEY_ARRAY_LIST, s).apply();
    }

    /*
        3. Using Gson
     */
    public static List<String> getDataArrayList3(String KEY_ARRAY_LIST) {
        SharedPreferences sharedPreferences = getPreferenceManager();

        String s = sharedPreferences.getString(KEY_ARRAY_LIST, null);
        List<String> list = new ArrayList<>();
        if (s != null) {
            Gson gson = new Gson();
            String[] arrString = gson.fromJson(s, String[].class);
            list = Arrays.asList(arrString);
        }
        return list;
    }

    /*
        2. Serialize your ArrayList and then save to SharedPreferences.
     */
    public static void storeDataArrayList2(String KEY_ARRAY_LIST, List<String> stringList) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        Set<String> set = new HashSet<String>();
        set.addAll(stringList);
        sharedPreferences.edit().putStringSet(KEY_ARRAY_LIST, set).apply();
    }

    /*
        2. Serialize your ArrayList and then read it from SharedPreferences.
     */
    public static List<String> getDataArrayList2(String KEY_ARRAY_LIST) {
        SharedPreferences sharedPreferences = getPreferenceManager();

        Set<String> set = sharedPreferences.getStringSet(KEY_ARRAY_LIST, null);
        List<String> list = new ArrayList<>();
        if (set != null) {
            list = new ArrayList<>(set);
        }
        return list;
    }
    
    public static void clearSingleKey(String KEY_NAME) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().remove(KEY_NAME).apply();
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) sContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
