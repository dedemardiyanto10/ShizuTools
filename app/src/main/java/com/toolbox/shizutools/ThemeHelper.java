package com.toolbox.shizutools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static final String PREF_NAME = "theme_prefs";
    public static final String KEY_THEME = "selected_theme";
    private static final String KEY_BLACK_MODE = "black_mode_enabled";
    private static final String KEY_LANG = "selected_lang";

    public static void applyTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme).apply();

        switch (theme) {
            case "Always on":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "Always off":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, "Follow system");
    }

    public static void setBlackMode(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BLACK_MODE, enabled)
                .apply();
    }

    public static boolean isBlackModeEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_BLACK_MODE, false);
    }

    public static void applyAmoledMode(Activity activity, View root, View appBar, View toolbar) {
        boolean isNight =
                (activity.getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isNight && isBlackModeEnabled(activity)) {
            int black = Color.BLACK;
            root.setBackgroundColor(black);
            if (appBar != null) appBar.setBackgroundColor(black);
            if (toolbar != null) toolbar.setBackgroundColor(black);

            activity.getWindow().setStatusBarColor(black);
            activity.getWindow().setNavigationBarColor(black);
        } else {
            TypedValue tv = new TypedValue();
            activity.getTheme()
                    .resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true);
            int surfaceColor = tv.data;

            root.setBackgroundColor(surfaceColor);
            if (appBar != null) appBar.setBackgroundColor(surfaceColor);
            if (toolbar != null) toolbar.setBackgroundColor(Color.TRANSPARENT);

            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    public static void setLocale(Context context, String langCode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANG, langCode)
                .apply();
    }

    public static String getLocale(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, "in");
    }

    public static void applyLanguage(Context context) {
        String langCode = getLocale(context);
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);

        android.content.res.Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
        context.getResources()
                .updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}
