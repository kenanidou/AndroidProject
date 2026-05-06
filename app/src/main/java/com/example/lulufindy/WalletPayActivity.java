package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class WalletPayActivity extends AppCompatActivity {

    double paymentAmount;
    TextView walletBalanceText, balanceCheckText, paymentAmountText;
    Button btnPay, btnBack, btnManage;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_pay);


        walletBalanceText = findViewById(R.id.wallet_balance);
        balanceCheckText = findViewById(R.id.balance_check);
        paymentAmountText = findViewById(R.id.payment_amount_text);
        btnPay = findViewById(R.id.btn_pay);
        btnManage = findViewById(R.id.btn_wallet_manage);


        paymentAmount = getIntent().getDoubleExtra("amount", 0.0);
        paymentAmountText.setText("Ποσό πληρωμής: €" + String.format("%.2f", paymentAmount));


        refreshBalance();





        btnManage.setOnClickListener(v -> {
            Intent intent = new Intent(WalletPayActivity.this, WalletManagerActivity.class);
            startActivity(intent);
        });


        btnPay.setOnClickListener(v -> {
            WalletManager.deductFromWallet(paymentAmount, newBalance -> {
                if (newBalance >= 0) {
                    Toast.makeText(this, "Η πληρωμή ολοκληρώθηκε", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(WalletPayActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Αποτυχία πληρωμής. Μη επαρκές υπόλοιπο.", Toast.LENGTH_SHORT).show();
                    refreshBalance();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
    }

    private void refreshBalance() {
        WalletManager.getWalletBalance(balance -> {
            walletBalanceText.setText("Υπόλοιπο: €" + String.format("%.2f", balance));

            if (balance >= paymentAmount) {
                balanceCheckText.setText("✅ Επαρκές υπόλοιπο");
                balanceCheckText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnPay.setEnabled(true);
            } else {
                balanceCheckText.setText("❌ Μη επαρκές υπόλοιπο");
                balanceCheckText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnPay.setEnabled(false);
            }
        });
    }
}
