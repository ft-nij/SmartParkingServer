package com.example.smartparkingclient;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String PREFS_NAME = "app_prefs";

    public static final String KEY_THEME = "theme";          // "light", "dark", "neutral"
    public static final String KEY_LANG = "lang";            // "ru", "en"
    public static final String KEY_NOTIFICATIONS = "notify"; // true / false
    public static final String KEY_BALANCE = "balance";      // баланс в рублях (int)

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---------- тема ----------
    public static String getTheme(Context context) {
        return getPrefs(context).getString(KEY_THEME, "light");
    }

    public static void setTheme(Context context, String theme) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply();
    }

    // ---------- язык ----------
    public static String getLang(Context context) {
        return getPrefs(context).getString(KEY_LANG, "ru");
    }

    public static void setLang(Context context, String lang) {
        getPrefs(context).edit().putString(KEY_LANG, lang).apply();
    }

    // ---------- уведомления ----------
    public static boolean isNotificationsEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    // ---------- баланс ----------
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
}
