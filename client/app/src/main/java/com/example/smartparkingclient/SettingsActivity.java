package com.example.smartparkingclient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgTheme, rgLang;
    private RadioButton rbThemeLight, rbThemeDark, rbThemeNeutral;
    private RadioButton rbLangRu, rbLangEn;
    private Switch switchNotify;
    private Button btnApply;

    private String lastTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyThemeFromPrefs();          // сначала применяем тему
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // стрелка "назад" в ActionBar (если используешь)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Настройки");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rgTheme = findViewById(R.id.rgTheme);
        rbThemeLight = findViewById(R.id.rbThemeLight);
        rbThemeDark = findViewById(R.id.rbThemeDark);
        rbThemeNeutral = findViewById(R.id.rbThemeNeutral);

        rgLang = findViewById(R.id.rgLang);
        rbLangRu = findViewById(R.id.rbLangRu);
        rbLangEn = findViewById(R.id.rbLangEn);

        switchNotify = findViewById(R.id.switchNotify);
        btnApply = findViewById(R.id.btnApply);

        // подставляем текущие значения из AppPrefs
        loadCurrentSettingsToUI();

        // кнопка "Применить"
        btnApply.setOnClickListener(v -> {
            saveSettingsFromUI();
            finish();   // просто закрываем настройки
        });

        // нижняя навигация
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.tab_profile) {
                    // тут можно открыть отдельный экран профиля
                    // пока оставим заглушку
                    return true;
                } else if (id == R.id.tab_home) {
                    finish();   // вернуться на главную
                    return true;
                } else if (id == R.id.tab_settings) {
                    return true; // мы уже на настройках
                }
                return false;
            });
            bottomNav.setSelectedItemId(R.id.tab_settings);
        }
    }

    // стрелка в ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        finish();       // закрываем настройки без сохранения
        return true;
    }

    // применяем тему до super.onCreate()
    private void applyThemeFromPrefs() {
        String theme = AppPrefs.getTheme(this);
        lastTheme = theme;

        switch (theme) {
            case "dark":
                setTheme(R.style.Theme_SmartParkingClient_Dark);
                break;

            default:
                setTheme(R.style.Theme_SmartParkingClient_Light);
                break;
        }
    }

    private void loadCurrentSettingsToUI() {
        // Тема
        String theme = AppPrefs.getTheme(this);
        if ("dark".equals(theme)) {
            rbThemeDark.setChecked(true);
        } else if ("neutral".equals(theme)) {
            rbThemeNeutral.setChecked(true);
        } else {
            rbThemeLight.setChecked(true);
        }

        // Язык
        String lang = AppPrefs.getLang(this);
        if ("en".equals(lang)) {
            rbLangEn.setChecked(true);
        } else {
            rbLangRu.setChecked(true);
        }

        // Уведомления
        boolean notify = AppPrefs.isNotificationsEnabled(this);
        switchNotify.setChecked(notify);
    }

    private void saveSettingsFromUI() {
        // Тема
        String theme;
        int checkedThemeId = rgTheme.getCheckedRadioButtonId();
        if (checkedThemeId == R.id.rbThemeDark) {
            theme = "dark";
        } else if (checkedThemeId == R.id.rbThemeNeutral) {
            theme = "neutral";
        } else {
            theme = "light";
        }
        AppPrefs.setTheme(this, theme);

        // Язык
        String lang = rbLangEn.isChecked() ? "en" : "ru";
        AppPrefs.setLang(this, lang);

        // Уведомления
        AppPrefs.setNotificationsEnabled(this, switchNotify.isChecked());
    }
}
