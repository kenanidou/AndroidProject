package com.example.lulufindy;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Payment_History extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView emptyMessage;
    private ProgressBar loadingBar;
    private Button backBtn, clearFilterButton;
    private ImageView filterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        historyContainer = findViewById(R.id.historyContainer);
        emptyMessage = findViewById(R.id.empty_message);
        loadingBar = findViewById(R.id.loadingBar);
        backBtn = findViewById(R.id.backButton);
        filterButton = findViewById(R.id.filterButton); // ΝΕΟ

        backBtn.setOnClickListener(v -> finish());

        fetchPaymentHistory();

        filterButton.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    Payment_History.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth, 0, 0, 0);
                        long selectedStart = calendar.getTimeInMillis();
                        calendar.set(year, month, dayOfMonth, 23, 59, 59);
                        long selectedEnd = calendar.getTimeInMillis();

                        fetchPaymentHistoryForDate(selectedStart, selectedEnd);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        clearFilterButton = findViewById(R.id.clearFilterButton);

        clearFilterButton.setOnClickListener(v -> {
            fetchPaymentHistory();
            clearFilterButton.setVisibility(View.GONE);
        });

    }

    private void fetchPaymentHistory() {
        loadingBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    loadingBar.setVisibility(View.GONE);

                    if (!snapshot.exists() || !snapshot.contains("paymentHistory")) {
                        showEmpty();
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> history =
                            (List<Map<String, Object>>) snapshot.get("paymentHistory");

                    if (history == null || history.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    List<Map<String, Object>> sortedList = new ArrayList<>();
                    for (Map<String, Object> item : history) {
                        if (item.get("timestamp") instanceof Long) {
                            sortedList.add(item);
                        }
                    }

                    Collections.sort(sortedList, (a, b) -> Long.compare(
                            (Long) b.get("timestamp"),
                            (Long) a.get("timestamp"))
                    );

                    populateHistoryList(sortedList);
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    showEmpty();
                });
    }


    private void fetchPaymentHistoryForDate(long startMillis, long endMillis) {
        loadingBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    loadingBar.setVisibility(View.GONE);

                    if (!snapshot.exists() || !snapshot.contains("paymentHistory")) {
                        showEmpty();
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> history =
                            (List<Map<String, Object>>) snapshot.get("paymentHistory");

                    if (history == null || history.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    List<Map<String, Object>> filteredList = new ArrayList<>();
                    for (Map<String, Object> item : history) {
                        if (item.get("timestamp") instanceof Long) {
                            long ts = (Long) item.get("timestamp");
                            if (ts >= startMillis && ts <= endMillis) {
                                filteredList.add(item);
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        showEmpty();
                    } else {
                        Collections.sort(filteredList, (a, b) -> Long.compare(
                                (Long) b.get("timestamp"),
                                (Long) a.get("timestamp"))
                        );
                        populateHistoryList(filteredList);
                    }
                    clearFilterButton.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    showEmpty();
                });
    }

    private void populateHistoryList(List<Map<String, Object>> list) {
        historyContainer.removeAllViews();

        for (Map<String, Object> item : list) {
            String method = (String) item.get("method");
            double amount = item.get("amount") instanceof Long
                    ? ((Long) item.get("amount")).doubleValue()
                    : (Double) item.get("amount");
            long timestamp = (Long) item.get("timestamp");

            String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));


            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(24, 24, 24, 24);
            card.setBackgroundResource(R.drawable.rounded_card);
            card.setElevation(8f);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, 32);
            card.setLayoutParams(cardLp);


            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(160, 160);
            icon.setLayoutParams(iconLp);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

            String ml = method.toLowerCase(Locale.ROOT);
            if (ml.contains("πορτοφόλι"))
                icon.setImageResource(R.drawable.ic_wallet);
            else if (ml.contains("google"))
                icon.setImageResource(R.drawable.ic_google_pay);
            else if (ml.contains("apple"))
                icon.setImageResource(R.drawable.ic_apple_pay);
            else
                icon.setImageResource(R.drawable.ic_card);


            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(24, 0, 0, 0);

            TextView amountTv = new TextView(this);
            amountTv.setText(String.format("Ποσό: €%.2f", amount));
            amountTv.setTextSize(16);

            TextView dateTv = new TextView(this);
            dateTv.setText("Ημερομηνία: " + dateStr);
            dateTv.setTextSize(14);
            dateTv.setTextColor(getResources().getColor(android.R.color.darker_gray));

            TextView methodTv = new TextView(this);
            methodTv.setText("Μέθοδος: " + method);
            methodTv.setTextSize(14);
            methodTv.setTextColor(getResources().getColor(android.R.color.darker_gray));

            textCol.addView(amountTv);
            textCol.addView(dateTv);
            textCol.addView(methodTv);

            card.addView(icon);
            card.addView(textCol);

            historyContainer.addView(card);
        }

        historyContainer.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
    }

    private void showEmpty() {
        historyContainer.removeAllViews();
        historyContainer.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
    }
}