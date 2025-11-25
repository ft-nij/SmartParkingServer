package com.example.smartparkingclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ParkingActivity extends AppCompatActivity {

    private LinearLayout leftColumn;
    private LinearLayout rightColumn;
    private TextView titleText;

    private final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://10.0.2.2:8000";
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private String mode = "enter";   // enter / exit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        leftColumn = findViewById(R.id.leftColumn);
        rightColumn = findViewById(R.id.rightColumn);
        titleText = findViewById(R.id.titleText);

        String fromIntent = getIntent().getStringExtra("mode");
        if (fromIntent != null) {
            mode = fromIntent;
        }

        if ("exit".equals(mode)) {
            titleText.setText("Выберите место для выезда");
        } else {
            titleText.setText("Выберите место для заезда");
        }

        loadParkingPlaces();
    }

    // ====== Тема ======
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

    private boolean notificationsEnabled() {
        return AppPrefs.isNotificationsEnabled(this);
    }

    // ====== Загрузка мест ======
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
                                "Ошибка соединения: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(ParkingActivity.this,
                                    "Ошибка сервера: " + response.code(),
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                String jsonData = response.body().string();

                try {
                    JSONObject json = new JSONObject(jsonData);
                    JSONArray places = json.getJSONArray("places");

                    runOnUiThread(() -> {
                        leftColumn.removeAllViews();
                        rightColumn.removeAllViews();

                        for (int i = 0; i < places.length(); i++) {
                            JSONObject place = places.optJSONObject(i);
                            if (place == null) continue;

                            int id = place.optInt("id", -1);
                            String status = place.optString("status", "unknown");

                            addSlotView(i, id, status);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(ParkingActivity.this,
                                    "Ошибка парсинга данных",
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    // ====== Один слот ======
    private void addSlotView(int index, int id, String status) {
        boolean isFree = "free".equalsIgnoreCase(status);

        LinearLayout column = (index % 2 == 0) ? leftColumn : rightColumn;

        TextView slot = new TextView(this);
        slot.setText("Место " + id);
        slot.setTextSize(16);
        slot.setGravity(Gravity.CENTER);

        int padding = dpToPx(8);
        slot.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(56)
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        slot.setLayoutParams(params);

        int bgColor = isFree ? 0xFFA8E6CF : 0xFFFF8C8C; // зелёный/красный
        slot.setBackgroundColor(bgColor);

        slot.setOnClickListener(v -> {
            if ("enter".equals(mode)) {
                if (isFree) {
                    showEnterDialog(id, status);
                } else {
                    if (notificationsEnabled()) {
                        Toast.makeText(this,
                                "Место " + id + " занято",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (!isFree) {
                    showExitDialog(id, status);
                } else {
                    if (notificationsEnabled()) {
                        Toast.makeText(this,
                                "Место " + id + " уже свободно",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        column.addView(slot);
    }

    // ====== Диалог ЗАЕЗДА ======
    private void showEnterDialog(int placeId, String currentStatus) {
        long now = System.currentTimeMillis();
        saveStartTime(placeId, now);

        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(now));

        String message = "Вы выбрали место " + placeId +
                "\nС " + timeStr +
                "\nЧерез 5 сек откроется шлагбаум." +
                "\nПриятного времяпрепровождения!";

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_parking_info, null);
        TextView titleView = dialogView.findViewById(R.id.dialogTitleText);
        TextView messageView = dialogView.findViewById(R.id.dialogMessageText);

        titleView.setText("Заезд на парковку");
        messageView.setText(message);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Ок", (dialog, which) -> {
                    togglePlaceStatus(placeId, currentStatus);

                    if (notificationsEnabled()) {
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> Toast.makeText(this,
                                        "Шлагбаум открыт",
                                        Toast.LENGTH_SHORT).show(),
                                5000
                        );
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> clearStartTime(placeId))
                .show();
    }

    // ====== Диалог ВЫЕЗДА ======
    private void showExitDialog(int placeId, String currentStatus) {
        long startTime = getStartTime(placeId);
        long now = System.currentTimeMillis();

        String message;
        long price = 0;
        int oldBalance = AppPrefs.getBalance(this);
        int newBalance = oldBalance;

        if (startTime <= 0) {
            message = "Вы покидаете место " + placeId +
                    "\nНе удалось определить время стоянки." +
                    "\nЧерез 5 сек откроется шлагбаум." +
                    "\nПриятного пути!";
        } else {
            long durationMillis = now - startTime;
            long minutes = Math.max(1, durationMillis / 60000); // не меньше 1 мин
            price = minutes * 2; // 2 ₽/мин
            long hours = minutes / 60;
            long minsLeft = minutes % 60;

            String durationStr;
            if (hours > 0) {
                durationStr = hours + " ч " + minsLeft + " мин";
            } else {
                durationStr = minutes + " мин";
            }

            newBalance = (int) Math.max(0, oldBalance - price);

            message = "Вы покидаете место " + placeId +
                    "\nВы были на парковке " + durationStr + "." +
                    "\nС вашего баланса спишется " + price + " ₽." +
                    "\nТекущий баланс: " + oldBalance + " ₽." +
                    "\nПосле списания останется: " + newBalance + " ₽." +
                    "\nЧерез 5 сек откроется шлагбаум." +
                    "\nПриятного пути!";
        }

        final long finalPrice = price;
        final int finalNewBalance = newBalance;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_parking_info, null);
        TextView titleView = dialogView.findViewById(R.id.dialogTitleText);
        TextView messageView = dialogView.findViewById(R.id.dialogMessageText);

        titleView.setText("Выезд с парковки");
        messageView.setText(message);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Ок", (dialog, which) -> {
                    togglePlaceStatus(placeId, currentStatus);
                    clearStartTime(placeId);

                    if (startTime > 0) {
                        AppPrefs.setBalance(this, finalNewBalance);
                    }

                    if (notificationsEnabled()) {
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> Toast.makeText(this,
                                        "Шлагбаум открыт",
                                        Toast.LENGTH_SHORT).show(),
                                5000
                        );
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ====== Смена статуса на сервере ======
    private void togglePlaceStatus(int id, String currentStatus) {
        String newStatus = "free".equalsIgnoreCase(currentStatus) ? "busy" : "free";

        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("status", newStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                                "Ошибка при обновлении: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(ParkingActivity.this::loadParkingPlaces);
            }
        });
    }

    // ====== Время начала стоянки ======
    private void saveStartTime(int placeId, long timeMillis) {
        getSharedPreferences("parking_times", MODE_PRIVATE)
                .edit()
                .putLong("place_" + placeId + "_start", timeMillis)
                .apply();
    }

    private long getStartTime(int placeId) {
        return getSharedPreferences("parking_times", MODE_PRIVATE)
                .getLong("place_" + placeId + "_start", 0L);
    }

    private void clearStartTime(int placeId) {
        getSharedPreferences("parking_times", MODE_PRIVATE)
                .edit()
                .remove("place_" + placeId + "_start")
                .apply();
    }

    // ====== dp -> px ======
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
