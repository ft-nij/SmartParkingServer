package com.example.smartparkingclient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgTheme, rgLang;
    private RadioButton rbThemeLight, rbThemeDark, rbThemeNeutral;
    private RadioButton rbLangRu, rbLangEn;
    private Switch switchNotify;
    private Button btnApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();               // применяем тему до super.onCreate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgTheme = findViewById(R.id.rgTheme);
        rgLang = findViewById(R.id.rgLang);

        rbThemeLight = findViewById(R.id.rbThemeLight);
        rbThemeDark = findViewById(R.id.rbThemeDark);
        rbThemeNeutral = findViewById(R.id.rbThemeNeutral);

        rbLangRu = findViewById(R.id.rbLangRu);
        rbLangEn = findViewById(R.id.rbLangEn);

        switchNotify = findViewById(R.id.switchNotify);
        btnApply = findViewById(R.id.btnApply);

        loadSettings();
        initApplyButton();
    }

    // применяем тему для самого экрана настроек
    private void applyThemeFromPrefs() {
        String theme = AppPrefs.getTheme(this);
        switch (theme) {
            case "dark":
                setTheme(R.style.Theme_SmartParkingClient_Dark);
                break;
            case "neutral":
                setTheme(R.style.Theme_SmartParkingClient_Neutral);
                break;
            default:
                setTheme(R.style.Theme_SmartParkingClient_Light);
                break;
        }
    }

    // выставляем переключатели в соответствии с сохранёнными настройками
    private void loadSettings() {
        String theme = AppPrefs.getTheme(this);
        String lang = AppPrefs.getLang(this);
        boolean notify = AppPrefs.isNotificationsEnabled(this);

        // Тема
        if ("dark".equals(theme)) rbThemeDark.setChecked(true);
        else if ("neutral".equals(theme)) rbThemeNeutral.setChecked(true);
        else rbThemeLight.setChecked(true);

        // Язык
        if ("en".equals(lang)) rbLangEn.setChecked(true);
        else rbLangRu.setChecked(true);

        // Уведомления
        switchNotify.setChecked(notify);
    }

    // логика кнопки "Применить"
    private void initApplyButton() {
        btnApply.setOnClickListener(v -> {

            // --- читаем выбранную тему ---
            String theme = "light";
            int themeId = rgTheme.getCheckedRadioButtonId();
            if (themeId == R.id.rbThemeDark) theme = "dark";
            else if (themeId == R.id.rbThemeNeutral) theme = "neutral";

            // --- читаем выбранный язык ---
            String lang = "ru";
            int langId = rgLang.getCheckedRadioButtonId();
            if (langId == R.id.rbLangEn) lang = "en";

            // --- читаем состояние уведомлений ---
            boolean notify = switchNotify.isChecked();

            // --- сохраняем все настройки в SharedPreferences через AppPrefs ---
            AppPrefs.setTheme(this, theme);
            AppPrefs.setLang(this, lang);
            AppPrefs.setNotificationsEnabled(this, notify);

            Toast.makeText(this, "Настройки применены", Toast.LENGTH_SHORT).show();

            // закрываем экран настроек и возвращаемся на главное
            finish();
        });
    }
}
