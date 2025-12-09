package com.example.smartparkingclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ParkingActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8000";

    private LinearLayout leftColumn;
    private LinearLayout rightColumn;
    private TextView titleText;

    private final OkHttpClient client = new OkHttpClient();
    private String mode = "enter"; // "enter" или "exit"

    private String lastTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        titleText = findViewById(R.id.titleText);
        leftColumn = findViewById(R.id.leftColumn);
        rightColumn = findViewById(R.id.rightColumn);

        String m = getIntent().getStringExtra("mode");
        if (m != null) mode = m;

        if ("exit".equals(mode)) {
            titleText.setText("Выберите место для выезда");
        } else {
            titleText.setText("Выберите место для заезда");
        }

        // нижняя навигация
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.tab_profile) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("open_profile", true);
                    startActivity(intent);
                    return true;

                } else if (id == R.id.tab_home) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;

                } else if (id == R.id.tab_settings) {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            // только визуально подсвечиваем вкладку, БЕЗ вызова listener
            bottomNav.getMenu().findItem(R.id.tab_home).setChecked(true);
        }
        loadParkingPlaces();
    }

    // ====== ТЕМЫ ======
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

    @Override
    protected void onResume() {
        super.onResume();
        String currentTheme = AppPrefs.getTheme(this);
        if (lastTheme != null && !lastTheme.equals(currentTheme)) {
            lastTheme = currentTheme;
            recreate();
            return;
        }
    }

    // ====== Загрузка мест с сервера ======
    private void loadParkingPlaces() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/places")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ParkingActivity.this,
                                "Ошибка соединения с сервером",
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(ParkingActivity.this,
                                    "Ошибка ответа сервера",
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                String jsonData = response.body().string();
                try {
                    JSONObject json = new JSONObject(jsonData);
                    JSONArray places = json.getJSONArray("places");
                    int myPlaceId = AppPrefs.getCurrentPlaceId(ParkingActivity.this);

                    runOnUiThread(() -> {

                        // защита от null, на всякий случай
                        if (leftColumn == null || rightColumn == null) {
                            Toast.makeText(ParkingActivity.this,
                                    "Ошибка интерфейса (колонки парковки не найдены)",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        leftColumn.removeAllViews();
                        rightColumn.removeAllViews();

                        for (int i = 0; i < places.length(); i++) {
                            JSONObject place = places.optJSONObject(i);
                            if (place == null) continue;

                            int id = place.optInt("id");
                            String status = place.optString("status"); // "free" или "busy"

                            TextView tv = createPlaceView(id, status, myPlaceId);

                            if (i % 2 == 0) {
                                leftColumn.addView(tv);
                            } else {
                                rightColumn.addView(tv);
                            }
                        }

                        if ("exit".equals(mode) && myPlaceId == -1) {
                            Toast.makeText(ParkingActivity.this,
                                    "У вас нет занятого места для выезда",
                                    Toast.LENGTH_LONG).show();
                        }

                        if ("enter".equals(mode) && myPlaceId != -1) {
                            Toast.makeText(ParkingActivity.this,
                                    "У вас уже занято место № " + myPlaceId,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private TextView createPlaceView(int id, String status, int myPlaceId) {
        TextView tv = new TextView(this);
        tv.setText("Место " + id + ": " +
                ("free".equals(status) ? "Свободно" : "Занято"));
        tv.setTextSize(16);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER_VERTICAL);

        if ("free".equals(status)) {
            tv.setBackgroundColor(0xFFA8E6CF); // зелёный
        } else {
            tv.setBackgroundColor(0xFFFF8C8C); // красный
        }

        tv.setOnClickListener(v -> handlePlaceClick(id, status, myPlaceId));

        return tv;
    }

    private void handlePlaceClick(int id, String status, int myPlaceId) {
        if ("enter".equals(mode)) {
            if (myPlaceId != -1) {
                Toast.makeText(this,
                        "У вас уже занято место № " + myPlaceId,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!"free".equals(status)) {
                Toast.makeText(this,
                        "Место уже занято",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showEnterDialog(id);

        } else { // exit
            if (myPlaceId == -1) {
                Toast.makeText(this,
                        "У вас нет занятого места",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (id != myPlaceId) {
                Toast.makeText(this,
                        "Вы можете выехать только с места № " + myPlaceId,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!"busy".equals(status)) {
                Toast.makeText(this,
                        "Это место уже свободно",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showExitDialog(id);
        }
    }

    private void showEnterDialog(int placeId) {
        new AlertDialog.Builder(this)
                .setTitle("Заезд на парковку")
                .setMessage("Вы выбрали место № " + placeId +
                        ".\nЧерез 5 секунд откроется шлагбаум.\nПриятного времяпрепровождения!")
                .setPositiveButton("OK", (d, w) ->
                        updatePlaceStatus(placeId, "busy", true))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showExitDialog(int placeId) {
        String message = "Вы покидаете место № " + placeId +
                ".\nС вашего баланса будет списана сумма по времени стоянки.\n" +
                "Через 5 секунд откроется шлагбаум.\nПриятного пути!";

        new AlertDialog.Builder(this)
                .setTitle("Выезд с парковки")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) ->
                        updatePlaceStatus(placeId, "free", false))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updatePlaceStatus(int id, String newStatus, boolean isEnter) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("status", newStatus);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/update")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(ParkingActivity.this,
                                    "Ошибка при обновлении статуса",
                                    Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(ParkingActivity.this,
                                        "Ошибка ответа сервера",
                                        Toast.LENGTH_SHORT).show());
                        return;
                    }

                    runOnUiThread(() -> {
                        if (isEnter) {
                            // === ЗАЕЗД ===
                            AppPrefs.setCurrentPlaceId(ParkingActivity.this, id);
                            AppPrefs.setCurrentPlaceStart(ParkingActivity.this,
                                    System.currentTimeMillis());

                            if (AppPrefs.isNotificationsEnabled(ParkingActivity.this)) {
                                Toast.makeText(ParkingActivity.this,
                                        "Место № " + id + " занято",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // === ВЫЕЗД ===
                            long start = AppPrefs.getCurrentPlaceStart(ParkingActivity.this);
                            long now = System.currentTimeMillis();

                            long minutes = 1;
                            if (start > 0 && now > start) {
                                minutes = (now - start) / 60000; // минуты
                                if (minutes <= 0) minutes = 1;
                            }

                            int pricePerHour = 50;
                            int hours = (int) ((minutes + 59) / 60); // округление вверх
                            if (hours <= 0) hours = 1;
                            int cost = hours * pricePerHour;

                            int balanceBefore = AppPrefs.getBalance(ParkingActivity.this);
                            int balanceAfter = Math.max(0, balanceBefore - cost);
                            AppPrefs.setBalance(ParkingActivity.this, balanceAfter);

                            // сбрасываем данные по занятости
                            AppPrefs.setCurrentPlaceId(ParkingActivity.this, -1);
                            AppPrefs.setCurrentPlaceStart(ParkingActivity.this, 0L);

                            String record = "Место " + id +
                                    ", " + minutes + " мин (" + hours + " ч), " +
                                    "списано " + cost + " ₽. Остаток: " + balanceAfter + " ₽";
                            AppPrefs.addTripToHistory(ParkingActivity.this, record);

                            if (AppPrefs.isNotificationsEnabled(ParkingActivity.this)) {
                                Toast.makeText(ParkingActivity.this,
                                        "Списано " + cost + " ₽. Остаток: " + balanceAfter + " ₽",
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        loadParkingPlaces();
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
