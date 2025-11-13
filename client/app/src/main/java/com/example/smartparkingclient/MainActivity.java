package com.example.smartparkingclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout parkingList;
    private final OkHttpClient client = new OkHttpClient();
    // Для эмулятора Android: 10.0.2.2 = localhost хоста
    private static final String BASE_URL = "http://10.0.2.2:8000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        parkingList = findViewById(R.id.parkingList);

        // Загружаем список мест при запуске
        loadParkingPlaces();
    }

    // ================= Загрузка списка =================
    private void loadParkingPlaces() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/places")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Ошибка соединения с сервером: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
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
                        parkingList.removeAllViews();

                        if (places.length() == 0) {
                            TextView empty = new TextView(MainActivity.this);
                            empty.setText("Нет доступных парковочных мест");
                            empty.setTextSize(18);
                            empty.setPadding(20, 20, 20, 20);
                            parkingList.addView(empty);
                            return;
                        }

                        for (int i = 0; i < places.length(); i++) {
                            JSONObject place = places.optJSONObject(i);
                            if (place == null) continue;

                            int id = place.optInt("id", -1);
                            String status = place.optString("status", "unknown");

                            addParkingCard(id, status);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Ошибка парсинга данных",
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    // ================= Отрисовка одной “карточки” =================
    private void addParkingCard(int id, String status) {
        // Флаг статуса
        boolean isFree = "free".equalsIgnoreCase(status);
        String statusText = isFree ? "Свободно" : "Занято";

        // Корневой контейнер карточки
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);

        // Отступы между карточками
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 32);
        card.setLayoutParams(params);

        // Фон в зависимости от статуса
        int bgColor = isFree ? 0xFFA8E6CF : 0xFFFF8C8C; // зелёный/красный
        card.setBackgroundColor(bgColor);

        // Текст: "Место X"
        TextView titleView = new TextView(this);
        titleView.setText("Место " + id);
        titleView.setTextSize(20);
        titleView.setTextColor(0xFF212121);
        titleView.setPadding(0, 0, 0, 8);

        // Текст: "Свободно"/"Занято"
        TextView statusView = new TextView(this);
        statusView.setText(statusText);
        statusView.setTextSize(16);
        statusView.setTextColor(isFree ? 0xFF1B5E20 : 0xFFB71C1C);

        // Добавляем текстовые элементы в карточку
        card.addView(titleView);
        card.addView(statusView);

        // Делаем карточку кликабельной
        String statusForClick = status; // “замораживаем” строку для лямбды
        card.setClickable(true);
        card.setOnClickListener(v -> {
            if (isFree) {
                Toast.makeText(MainActivity.this,
                        "Вы выбрали место " + id,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,
                        "Место " + id + " уже занято",
                        Toast.LENGTH_SHORT).show();
            }
            // Отправляем запрос на смену статуса
            togglePlaceStatus(id, statusForClick);
        });

        parkingList.addView(card);
    }

    // ================= Обновление статуса =================
    private void togglePlaceStatus(int id, String currentStatus) {
        String newStatus = "free".equalsIgnoreCase(currentStatus) ? "busy" : "free";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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
                        Toast.makeText(MainActivity.this,
                                "Ошибка при обновлении: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this,
                                "Статус изменён!",
                                Toast.LENGTH_SHORT).show();
                        // После успешного обновления заново подгружаем список
                        loadParkingPlaces();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Ошибка обновления на сервере: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
