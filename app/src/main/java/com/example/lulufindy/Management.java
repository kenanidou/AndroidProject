package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Management extends AppCompatActivity {

    private Button addparkingBtn,workingHoursBtn,on_offParkingBtn,pictureSearchBtn,backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management);
        addparkingBtn = findViewById(R.id.button2);
        workingHoursBtn = findViewById(R.id.button3);
        on_offParkingBtn = findViewById(R.id.button6);
        pictureSearchBtn = findViewById(R.id.button7);
        backBtn = findViewById(R.id.button5);

        addparkingBtn.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(), AddParking.class);
            startActivity(intent);
            finish();
        });
        workingHoursBtn.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(), WorkingHours.class);
            startActivity(intent);
            finish();
        });
        on_offParkingBtn.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(),ParkingSpaceSituation.class);
            startActivity(intent);
            finish();
        });
        pictureSearchBtn.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(),ManagementMap2.class);
            startActivity(intent);
            finish();
        });
        backBtn.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(), AdminMainActivity.class);
            startActivity(intent);
            finish();
        });

    }
}