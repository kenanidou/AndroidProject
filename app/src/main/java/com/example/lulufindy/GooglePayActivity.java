package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GooglePayActivity extends AppCompatActivity {

    private Button payButton;
    private Button returnButton;
    private Button returnSuccessButton;
    private LinearLayout successLayout;
    private TextView amountText;
    private double amount;
    private LinearLayout paymentDetailsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_pay);

        amount = getIntent().getDoubleExtra("amount", 0.0);
        amountText = findViewById(R.id.amount_text_google_pay);
        payButton = findViewById(R.id.btn_google_pay);
        returnSuccessButton = findViewById(R.id.btn_return_success);
        successLayout = findViewById(R.id.success_layout);
        paymentDetailsLayout = findViewById(R.id.payment_details_layout);

        amountText.setText(String.format("%.2f", amount) + " €");

        payButton.setOnClickListener(v -> {
            paymentDetailsLayout.setVisibility(View.GONE);
            successLayout.setVisibility(View.VISIBLE);
        });

        returnSuccessButton.setOnClickListener(v -> {
            Intent intent = new Intent(GooglePayActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}