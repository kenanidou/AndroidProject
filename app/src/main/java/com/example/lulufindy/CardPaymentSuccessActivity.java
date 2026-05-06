package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CardPaymentSuccessActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_payment_success);

        TextView message = findViewById(R.id.success_message);
        Button doneButton = findViewById(R.id.done_button);

        doneButton.setOnClickListener(v -> {
            Intent intent = new Intent(CardPaymentSuccessActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}