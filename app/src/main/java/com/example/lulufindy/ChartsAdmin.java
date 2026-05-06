package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ChartsAdmin extends AppCompatActivity{

    private Button btn_positionsAdmin, btnIncomesAdmin, backbtn, btnIncomesCharts;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts_admin);

        btn_positionsAdmin = findViewById(R.id.btn_positionsAdmin);
        btn_positionsAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(ChartsAdmin.this, Positions_Charts_Admin.class);
            startActivity(intent);
        });

        btnIncomesAdmin = findViewById(R.id.btnIncomesAdmin);
        btnIncomesAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(ChartsAdmin.this, Incomes_Admin.class);
            startActivity(intent);
        });

        btnIncomesCharts = findViewById(R.id.btnIncomesChart);
        btnIncomesCharts.setOnClickListener(v -> {
            Intent intent = new Intent(ChartsAdmin.this, IncomesChart_Admin.class);
            startActivity(intent);
        });




        backbtn = findViewById(R.id.button5);
        backbtn.setOnClickListener(v -> finish());
    }
}
