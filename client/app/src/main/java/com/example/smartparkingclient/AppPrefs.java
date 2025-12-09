package com.example.smartparkingclient;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String PREFS_NAME = "app_prefs";

    // ----------- КЛЮЧИ -----------
    public static final String KEY_THEME = "theme";                // "light", "dark", "neutral"
    public static final String KEY_LANG = "lang";                  // "ru", "en"
    public static final String KEY_NOTIFICATIONS = "notify";       // boolean
    public static final String KEY_BALANCE = "balance";            // int

    public static final String KEY_IS_AUTH = "is_auth";            // boolean
    public static final String KEY_USER_NAME = "user_name";        // String

    public static final String KEY_CURRENT_PLACE_ID = "current_place_id";     // int
    public static final String KEY_CURRENT_PLACE_START = "current_place_start"; // long

    public static final String KEY_TRIP_HISTORY = "trip_history";  // String


    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    // ---------- ТЕМА ----------
    public static String getTheme(Context context) {
        return getPrefs(context).getString(KEY_THEME, "light");
    }

    public static void setTheme(Context context, String theme) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply();
    }


    // ---------- ЯЗЫК ----------
    public static String getLang(Context context) {
        return getPrefs(context).getString(KEY_LANG, "ru");
    }

    public static void setLang(Context context, String lang) {
        getPrefs(context).edit().putString(KEY_LANG, lang).apply();
    }


    // ---------- УВЕДОМЛЕНИЯ ----------
    public static boolean isNotificationsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }


    // ---------- БАЛАНС ----------
    public static int getBalance(Context context) {
        return getPrefs(context).getInt(KEY_BALANCE, 0);
    }

    public static void setBalance(Context context, int value) {
        getPrefs(context).edit().putInt(KEY_BALANCE, Math.max(0, value)).apply();
    }

    public static void addToBalance(Context context, int delta) {
        int current = getBalance(context);
        setBalance(context, current + Math.max(0, delta));
    }


    // ---------- АВТОРИЗАЦИЯ ----------
    public static boolean isAuthorized(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_AUTH, false);
    }

    public static void setAuthorized(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(KEY_IS_AUTH, value).apply();
    }

    public static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "Гость");
    }

    public static void setUserName(Context context, String name) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply();
    }


    // ---------- ТЕКУЩЕЕ МЕСТО ----------
    public static int getCurrentPlaceId(Context context) {
        return getPrefs(context).getInt(KEY_CURRENT_PLACE_ID, -1);
    }

    public static void setCurrentPlaceId(Context context, int placeId) {
        getPrefs(context).edit().putInt(KEY_CURRENT_PLACE_ID, placeId).apply();
    }

    public static long getCurrentPlaceStart(Context context) {
        return getPrefs(context).getLong(KEY_CURRENT_PLACE_START, 0L);
    }

    public static void setCurrentPlaceStart(Context context, long startTime) {
        getPrefs(context).edit().putLong(KEY_CURRENT_PLACE_START, startTime).apply();
    }


    // ---------- ИСТОРИЯ ПОЕЗДОК ----------
    public static void addTripToHistory(Context context, String record) {
        String oldHistory = getTripHistory(context);
        String newHistory = record + "\n" + oldHistory;

        getPrefs(context).edit()
                .putString(KEY_TRIP_HISTORY, newHistory.trim())
                .apply();
    }

    public static String getTripHistory(Context context) {
        return getPrefs(context).getString(KEY_TRIP_HISTORY, "Пока нет записей.");
    }

    public static void clearHistory(Context context) {
        getPrefs(context).edit().putString(KEY_TRIP_HISTORY, "").apply();
    }
}
