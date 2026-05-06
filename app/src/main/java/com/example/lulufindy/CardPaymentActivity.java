package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CardPaymentActivity extends AppCompatActivity {

    private EditText nameInput, cardNumberInput, expiryDateInput, cvvInput;
    private TextView cardHolderName, cardNumber, cardExpiry, cardCVV;
    private TextView paymentAmountText;

    double paymentAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_payment);

        nameInput = findViewById(R.id.name_input);
        cardNumberInput = findViewById(R.id.card_number_input);
        expiryDateInput = findViewById(R.id.expiry_date_input);
        cvvInput = findViewById(R.id.cvv_input);

        cardHolderName = findViewById(R.id.card_holder_name);
        cardNumber = findViewById(R.id.card_number);
        cardExpiry = findViewById(R.id.card_expiry);
        cardCVV = findViewById(R.id.card_cvv);
        paymentAmountText = findViewById(R.id.payment_amount_text);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("amount")) {
            paymentAmount = intent.getDoubleExtra("amount", 0.0);
        }

        updatePaymentAmountText();

        nameInput.addTextChangedListener(new CardTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                cardHolderName.setText(s);
            }
        });

        cardNumberInput.addTextChangedListener(new CardTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                cardNumber.setText(formatCardNumber(s.toString()));
            }
        });

        expiryDateInput.addTextChangedListener(new CardTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                cardExpiry.setText(s);
            }
        });

        cvvInput.addTextChangedListener(new CardTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                cardCVV.setText(s);
            }
        });

        findViewById(R.id.submit_button).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String cardNum = cardNumberInput.getText().toString().replaceAll("\\s", "");
            String expiry = expiryDateInput.getText().toString().trim();
            String cvv = cvvInput.getText().toString().trim();

            if (name.isEmpty() || cardNum.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                Toast.makeText(CardPaymentActivity.this, "Συμπλήρωσε όλα τα πεδία.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cardNum.length() != 16) {
                Toast.makeText(CardPaymentActivity.this, "Ο αριθμός κάρτας πρέπει να έχει 16 ψηφία.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cvv.length() != 3) {
                Toast.makeText(CardPaymentActivity.this, "Το CVV πρέπει να έχει 3 ψηφία.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent successIntent = new Intent(CardPaymentActivity.this, CardPaymentSuccessActivity.class);
            successIntent.putExtra("amount", paymentAmount);
            startActivity(successIntent);
        });
    }

    private void updatePaymentAmountText() {
        paymentAmountText.setText(String.format("Ποσό πληρωμής: €%.2f", paymentAmount));
    }

    private String formatCardNumber(String input) {
        input = input.replaceAll("\\s", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(" ");
            }
            sb.append(input.charAt(i));
        }
        return sb.toString();
    }

    private abstract class CardTextWatcher implements TextWatcher {
        public abstract void onTextChanged(CharSequence s);

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTextChanged(s);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}