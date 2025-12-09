package com.example.smartparkingclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    // Drawer / Navigation
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BottomNavigationView bottomNav;

    // Кнопки главного экрана
    private Button btnEnter;
    private Button btnExit;

    // Авторизация
    private boolean isAuthorized = false;
    private String currentUserName = "Гость";

    // Шапка бокового меню
    private View headerView;
    private android.widget.TextView headerUserName;
    private android.widget.TextView headerBalance;

    // Для отслеживания изменения темы
    private String lastTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();          // СНАЧАЛА тема
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ----- инициализация Drawer / Toolbar -----
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottomNav);

        setSupportActionBar(toolbar);

        // читаем состояние авторизации из настроек
        isAuthorized = AppPrefs.isAuthorized(this);
        currentUserName = AppPrefs.getUserName(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open, R.string.nav_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this::onNavItemSelected);

        headerView = navigationView.getHeaderView(0);
        headerUserName = headerView.findViewById(R.id.headerUserName);
        headerBalance = headerView.findViewById(R.id.headerBalance);
        updateHeader();

        // ----- кнопки "Заехать" / "Выехать" -----
        btnEnter = findViewById(R.id.btnEnter);
        btnExit = findViewById(R.id.btnExit);

        btnEnter.setOnClickListener(v -> {
            if (!isAuthorized) {
                showNeedAuthDialog();
                return;
            }
            if (AppPrefs.isNotificationsEnabled(this)) {
                Toast.makeText(this, "Заезд на парковку", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(MainActivity.this, ParkingActivity.class);
            intent.putExtra("mode", "enter");
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            if (!isAuthorized) {
                showNeedAuthDialog();
                return;
            }
            if (AppPrefs.isNotificationsEnabled(this)) {
                Toast.makeText(this, "Выезд с парковки", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(MainActivity.this, ParkingActivity.class);
            intent.putExtra("mode", "exit");
            startActivity(intent);
        });

        // ----- нижняя навигация -----
        setupBottomNavigation();

        // ----- обработка системной кнопки "Назад" с учётом открытого меню -----
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // применяем тему + запоминаем какую применили
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

    // Если тема изменилась в настройках — пересоздаём экран
    @Override
    protected void onResume() {
        super.onResume();

        String currentTheme = AppPrefs.getTheme(this);
        if (lastTheme != null && !lastTheme.equals(currentTheme)) {
            lastTheme = currentTheme;
            recreate();
            return;
        }

        // обновляем имя и баланс в шапке
        updateHeader();
    }

    // ---------- нижняя навигация ----------
    private void setupBottomNavigation() {
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tab_profile) {
                if (!isAuthorized) {
                    showLoginDialog();
                } else {
                    showProfileDialog();
                }
                return true;
            } else if (id == R.id.tab_home) {
                return true; // уже на главном
            } else if (id == R.id.tab_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.tab_home);
    }

    // ---------- профиль с историей ----------
    private void showProfileDialog() {
        String name = AppPrefs.getUserName(this);
        int balance = AppPrefs.getBalance(this);
        String history = AppPrefs.getTripHistory(this);

        String message = "Имя: " + name +
                "\nБаланс: " + balance + " ₽" +
                "\n\nИстория поездок:\n" + history;

        new AlertDialog.Builder(this)
                .setTitle("Профиль")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------- обработка пунктов БОКОВОГО меню ----------
    private boolean onNavItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            showLoginDialog();
        } else if (id == R.id.nav_balance) {
            if (!isAuthorized) {
                showNeedAuthToast();
            } else {
                showTopUpDialog();
            }
        } else if (id == R.id.nav_info) {
            showAboutDialog();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            if (isAuthorized) {
                isAuthorized = false;
                currentUserName = "Гость";

                AppPrefs.setAuthorized(this, false);
                AppPrefs.setUserName(this, "Гость");

                AppPrefs.setBalance(this, 0);
                AppPrefs.setCurrentPlaceId(this, -1);
                AppPrefs.setCurrentPlaceStart(this, 0L);

                updateHeader();
                Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show();
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ---------- диалог "нужно авторизоваться" ----------
    private void showNeedAuthDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Требуется авторизация")
                .setMessage("Перед тем, как заехать или выехать, необходимо авторизоваться.")
                .setPositiveButton("Авторизоваться", (dialog, which) -> showLoginDialog())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showNeedAuthToast() {
        Toast.makeText(this, "Сначала авторизуйтесь", Toast.LENGTH_SHORT).show();
    }

    // ---------- простой диалог авторизации ----------
    private void showLoginDialog() {
        final EditText inputName = new EditText(this);
        inputName.setHint("Введите имя");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, padding);
        container.addView(inputName,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Авторизация")
                .setView(container)
                .setPositiveButton("Войти", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this,
                                "Имя не может быть пустым",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    isAuthorized = true;
                    currentUserName = name;

                    // сохраняем в настройки
                    AppPrefs.setAuthorized(this, true);
                    AppPrefs.setUserName(this, name);

                    // стартовый баланс
                    AppPrefs.setBalance(this, 150);
                    updateHeader();

                    Toast.makeText(this,
                            "Добро пожаловать, " + currentUserName + "!",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ---------- пополнение баланса ----------
    private void showTopUpDialog() {
        int current = AppPrefs.getBalance(this);

        EditText input = new EditText(this);
        input.setHint("Сумма пополнения");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, padding);

        android.widget.TextView info = new android.widget.TextView(this);
        info.setText("Текущий баланс: " + current + " ₽");

        container.addView(info,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(input,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Пополнение баланса")
                .setView(container)
                .setPositiveButton("Пополнить", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this,
                                "Введите сумму",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int amount = Integer.parseInt(text);
                        if (amount <= 0) {
                            Toast.makeText(this,
                                    "Сумма должна быть > 0",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.addToBalance(this, amount);
                        updateHeader();
                        Toast.makeText(this,
                                "Баланс пополнен на " + amount + " ₽",
                                Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                "Некорректная сумма",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ---------- "О приложении" ----------
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("О приложении")
                .setMessage("Smart Parking\n\nДемонстрационное приложение для управления парковкой.")
                .setPositiveButton("OK", null)
                .show();
    }

    // ---------- обновление шапки меню ----------
    private void updateHeader() {
        if (headerUserName != null) {
            // имя всегда берём из настроек, чтобы было актуально
            headerUserName.setText(AppPrefs.getUserName(this));
        }
        if (headerBalance != null) {
            int balance = AppPrefs.getBalance(this);
            headerBalance.setText("Баланс: " + balance + " ₽");
        }
    }

    // ---------- утилита dp -> px ----------
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
