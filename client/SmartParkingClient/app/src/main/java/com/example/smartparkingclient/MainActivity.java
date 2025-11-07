package com.example.smartparkingclient;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView textViewResult;
    private Button buttonRequest;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult = findViewById(R.id.textViewResult);
        buttonRequest = findViewById(R.id.buttonRequest);

        buttonRequest.setOnClickListener(v -> fetchDataFromServer());
    }

    private void fetchDataFromServer() {
        String url = "http://10.0.2.2:8000/";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SERVER_ERROR", "Ошибка при подключении к серверу", e);
                runOnUiThread(() -> textViewResult.setText("Ошибка подключения к серверу"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final String responseData = response.body().string();
                        Log.d("SERVER_RESPONSE", "Ответ от сервера: " + responseData);

                        JSONObject jsonObject = new JSONObject(responseData);
                        final String status = jsonObject.getString("status");
                        final String message = jsonObject.getString("message");

                        runOnUiThread(() ->
                                textViewResult.setText("Статус: " + status + "\nСообщение: " + message)
                        );

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Ошибка парсинга JSON", e);
                        runOnUiThread(() -> textViewResult.setText("Ошибка данных сервера"));
                    }
                } else {
                    runOnUiThread(() -> textViewResult.setText("Ошибка данных сервера"));
                }
            }
        });
    }
}
