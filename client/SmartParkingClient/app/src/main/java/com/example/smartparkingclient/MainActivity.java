package com.example.smartparkingclient;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView tvResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnFetch = findViewById(R.id.btnFetch);
        tvResponse = findViewById(R.id.tvResponse);

        btnFetch.setOnClickListener(v -> fetchData());
    }

    private void fetchData() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://jsonplaceholder.typicode.com/posts/1"; // тестовый URL

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvResponse.setText("Ошибка: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    runOnUiThread(() -> tvResponse.setText(responseData));
                } else {
                    runOnUiThread(() -> tvResponse.setText("Ошибка сервера: " + response.code()));
                }
            }
        });
    }
}
