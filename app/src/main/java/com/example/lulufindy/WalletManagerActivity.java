package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

public class WalletManagerActivity extends AppCompatActivity {

    private TextView balanceText;
    private Button btn5, btn10, btn15, btnCardPay, btnPayNow;
    private ImageButton btnGooglePay, btnApplePay;

    private double selectedAmount = 0;
    private String selectedMethod = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_manager);

        // Συνδέσεις views
        balanceText = findViewById(R.id.balance_text);
        btn5 = findViewById(R.id.btn_amount_5);
        btn10 = findViewById(R.id.btn_amount_10);
        btn15 = findViewById(R.id.btn_amount_15);
        btnGooglePay = findViewById(R.id.btn_google_pay);
        btnApplePay = findViewById(R.id.btn_apple_pay);
        btnCardPay = findViewById(R.id.btn_card_pay);
        btnPayNow = findViewById(R.id.btn_complete_pay);

        // Επιλογή ποσού
        btn5.setOnClickListener(v -> selectAmount(5, btn5));
        btn10.setOnClickListener(v -> selectAmount(10, btn10));
        btn15.setOnClickListener(v -> selectAmount(15, btn15));

        // Επιλογή μεθόδου
        btnGooglePay.setOnClickListener(v -> selectMethod("Google Pay", btnGooglePay));
        btnApplePay.setOnClickListener(v -> selectMethod("Apple Pay", btnApplePay));
        btnCardPay.setOnClickListener(v -> selectMethod("Κάρτα", btnCardPay));

        // Πληρωμή
        btnPayNow.setOnClickListener(v -> {
            if (selectedAmount == 0) {
                Toast.makeText(this, "Επίλεξε ποσό", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedMethod == null) {
                Toast.makeText(this, "Επίλεξε τρόπο πληρωμής", Toast.LENGTH_SHORT).show();
                return;
            }

            WalletManager.addToWallet(selectedAmount);
            Toast.makeText(this, "Προστέθηκαν " + selectedAmount + "€ μέσω " + selectedMethod, Toast.LENGTH_SHORT).show();

            resetSelections();
            updateBalanceDisplay();
        });


        Button btnBack = findViewById(R.id.btn_back_home);
        btnBack.setOnClickListener(v -> {
            String origin = getIntent().getStringExtra("origin");

            if ("start".equals(origin)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;

                if (uid == null) {

                    Intent intent = new Intent(WalletManagerActivity.this, MainActivity.class);
                    intent.putExtra("balance", WalletManager.getLocalWalletBalance());
                    startActivity(intent);
                    finish();
                } else {

                    WalletManager.getWalletBalance(balance -> {
                        Intent intent = new Intent(WalletManagerActivity.this, MainActivity.class);
                        intent.putExtra("balance", balance);
                        startActivity(intent);
                        finish();
                    });
                }

            } else if ("admin".equals(origin)) {
                Intent intent = new Intent(WalletManagerActivity.this, AdminMainActivity.class);
                startActivity(intent);
                finish();
            } else {
                finish();
            }
        });



        WalletManager.startListening(newBalance -> runOnUiThread(() ->
                balanceText.setText(String.format("%.2f €", newBalance))
        ));

        updateBalanceDisplay();
    }

    private void selectAmount(double amount, Button selectedButton) {
        selectedAmount = amount;
        resetAmountButtonColors();
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_selected));
    }

    private void selectMethod(String method, View selectedView) {
        selectedMethod = method;
        resetMethodButtonColors();

        if (selectedView instanceof ImageButton) {
            selectedView.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_selected));
        } else if (selectedView instanceof Button) {
            ((Button) selectedView).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_selected));
        }
    }

    private void resetAmountButtonColors() {
        btn5.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
        btn10.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
        btn15.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
    }

    private void resetMethodButtonColors() {
        btnGooglePay.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
        btnApplePay.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
        btnCardPay.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink_soft));
    }

    private void resetSelections() {
        selectedAmount = 0;
        selectedMethod = null;
        resetAmountButtonColors();
        resetMethodButtonColors();
    }

    private void updateBalanceDisplay() {
        WalletManager.getWalletBalance(balance ->
                runOnUiThread(() -> balanceText.setText(String.format("%.2f €", balance)))
        );
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        WalletManager.stopListening();
    }
}
