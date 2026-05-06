package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Charts extends AppCompatActivity {

    private Button parkingBtn;
    private Button paymentBtn;
    private Button parking_timeBtn;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_charts);

        parkingBtn = findViewById(R.id.button2);
        paymentBtn = findViewById(R.id.button3);
        parking_timeBtn = findViewById(R.id.button4);
        backBtn = findViewById(R.id.button5);

        backBtn.setOnClickListener(v -> finish());

        parkingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Charts.this, Parking_History.class);
            startActivity(intent);
        });

        paymentBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Charts.this, Payment_History.class);
            startActivity(intent);
        });

        parking_timeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Charts.this, Time_History.class);
            startActivity(intent);
        });
    }
}