package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ApplePayActivity extends AppCompatActivity {

    private TextView amountText;
    private Button payButton;
    private Button returnButton;
    private Button returnSuccessButton;
    private LinearLayout successLayout;
    private LinearLayout paymentDetailsLayout;
    private ImageView paymentSuccessIcon;
    private TextView paymentSuccessMessage;
    private double amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apple_pay);

        amount = getIntent().getDoubleExtra("amount", 0.0);
        amountText = findViewById(R.id.amount_text_apple_pay);
        payButton = findViewById(R.id.btn_apple_pay);
        returnSuccessButton = findViewById(R.id.btn_return_success);
        successLayout = findViewById(R.id.success_layout);
        paymentDetailsLayout = findViewById(R.id.payment_details_layout);
        paymentSuccessIcon = findViewById(R.id.payment_success_icon);
        paymentSuccessMessage = findViewById(R.id.payment_success_message);

        amountText.setText(String.format("%.2f", amount) + " €");

        payButton.setOnClickListener(v -> {

            paymentDetailsLayout.setVisibility(View.GONE);
            successLayout.setVisibility(View.VISIBLE);
        });

        returnSuccessButton.setOnClickListener(v -> {
            Intent intent = new Intent(ApplePayActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}