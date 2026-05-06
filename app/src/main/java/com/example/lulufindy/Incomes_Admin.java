package com.example.lulufindy;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Incomes_Admin extends AppCompatActivity {

    private LinearLayout historyContainer;
    private ProgressBar loadingBar;
    private FirebaseFirestore db;
    private Button backButton;
    private ImageView filterButton;
    private Button clearFilterButton;
    private PieChart paymentPieChart;
    private Map<String, Integer> paymentMethodCounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_history_admin);

        historyContainer = findViewById(R.id.historyContainer);
        loadingBar = findViewById(R.id.loadingBar);
        backButton = findViewById(R.id.backButton);
        filterButton = findViewById(R.id.filterButton);
        clearFilterButton = findViewById(R.id.clearFilterButton);

        paymentPieChart = findViewById(R.id.paymentPieChart);
        db = FirebaseFirestore.getInstance();

        loadIncomes();

        backButton.setOnClickListener(v -> finish());

        filterButton.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    Incomes_Admin.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth, 0, 0, 0);
                        long selectedStart = calendar.getTimeInMillis();
                        calendar.set(year, month, dayOfMonth, 23, 59, 59);
                        long selectedEnd = calendar.getTimeInMillis();

                        loadIncomesForDate(selectedStart, selectedEnd);
                        clearFilterButton.setVisibility(View.VISIBLE);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        clearFilterButton.setOnClickListener(v -> {
            loadIncomes();
            clearFilterButton.setVisibility(View.GONE);
        });
    }

    private void loadIncomes() {
        loadingBar.setVisibility(View.VISIBLE);
        historyContainer.removeAllViews();
        paymentMethodCounts.clear();

        db.collection("users").get().addOnCompleteListener(task -> {
            loadingBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                List<PaymentEntry> allPayments = new ArrayList<>();

                for (QueryDocumentSnapshot userDoc : task.getResult()) {
                    String userName = userDoc.getString("displayName");
                    if (userName == null || userName.isEmpty()) {
                        userName = userDoc.getString("email");
                    }

                    Object historyObj = userDoc.get("paymentHistory");

                    if (historyObj instanceof Iterable) {
                        for (Object paymentObj : (Iterable<?>) historyObj) {
                            if (paymentObj instanceof Map) {
                                Map<String, Object> payment = (Map<String, Object>) paymentObj;

                                Double amount = payment.get("amount") instanceof Number
                                        ? ((Number) payment.get("amount")).doubleValue()
                                        : null;

                                Long timestamp = payment.get("timestamp") instanceof Number
                                        ? ((Number) payment.get("timestamp")).longValue()
                                        : null;

                                String method = payment.get("method") instanceof String
                                        ? (String) payment.get("method")
                                        : "Άγνωστη μέθοδος";

                                if (userName != null && amount != null && timestamp != null) {
                                    allPayments.add(new PaymentEntry(userName, amount, timestamp, method));
                                    String methodKey = method.toLowerCase(Locale.ROOT);
                                    paymentMethodCounts.put(methodKey, paymentMethodCounts.getOrDefault(methodKey, 0) + 1);
                                }
                            }
                        }
                    }
                }

                if (allPayments.isEmpty()) {
                    addTextMessage("Δεν υπάρχουν έσοδα καταχωρημένα.");
                } else {
                    Collections.sort(allPayments, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    for (PaymentEntry entry : allPayments) {
                        addIncomeCard(entry);
                    }
                    updatePieChart();
                }
            } else {
                Toast.makeText(this, "Σφάλμα κατά την ανάκτηση των δεδομένων.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadIncomesForDate(long startMillis, long endMillis) {
        loadingBar.setVisibility(View.VISIBLE);
        historyContainer.removeAllViews();
        paymentMethodCounts.clear();

        db.collection("users").get().addOnCompleteListener(task -> {
            loadingBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                List<PaymentEntry> filteredPayments = new ArrayList<>();

                for (QueryDocumentSnapshot userDoc : task.getResult()) {
                    String userName = userDoc.getString("displayName");
                    if (userName == null || userName.isEmpty()) {
                        userName = userDoc.getString("email");
                    }

                    Object historyObj = userDoc.get("paymentHistory");

                    if (historyObj instanceof Iterable) {
                        for (Object paymentObj : (Iterable<?>) historyObj) {
                            if (paymentObj instanceof Map) {
                                Map<String, Object> payment = (Map<String, Object>) paymentObj;

                                Double amount = payment.get("amount") instanceof Number
                                        ? ((Number) payment.get("amount")).doubleValue()
                                        : null;

                                Long timestamp = payment.get("timestamp") instanceof Number
                                        ? ((Number) payment.get("timestamp")).longValue()
                                        : null;

                                String method = payment.get("method") instanceof String
                                        ? (String) payment.get("method")
                                        : "Άγνωστη μέθοδος";

                                if (userName != null && amount != null && timestamp != null) {
                                    if (timestamp >= startMillis && timestamp <= endMillis) {
                                        filteredPayments.add(new PaymentEntry(userName, amount, timestamp, method));
                                        String methodKey = method.toLowerCase(Locale.ROOT);
                                        paymentMethodCounts.put(methodKey, paymentMethodCounts.getOrDefault(methodKey, 0) + 1);
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredPayments.isEmpty()) {
                    addTextMessage("Δεν υπάρχουν πληρωμές για αυτή την ημερομηνία.");
                } else {
                    Collections.sort(filteredPayments, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    for (PaymentEntry entry : filteredPayments) {
                        addIncomeCard(entry);
                    }
                    updatePieChart();
                }
            } else {
                Toast.makeText(this, "Σφάλμα κατά την ανάκτηση των δεδομένων.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : paymentMethodCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Μέθοδοι Πληρωμής");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueTextSize(16f);
        data.setValueTextColor(getResources().getColor(android.R.color.black));

        paymentPieChart.setData(data);
        paymentPieChart.setUsePercentValues(true);
        paymentPieChart.getDescription().setEnabled(false);
        paymentPieChart.setEntryLabelColor(getResources().getColor(android.R.color.black));
        paymentPieChart.invalidate();
    }

    private void addIncomeCard(PaymentEntry entry) {
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

        String methodLower = entry.method.toLowerCase(Locale.ROOT);
        if (methodLower.contains("πορτοφόλι")) {
            icon.setImageResource(R.drawable.ic_wallet);
        } else if (methodLower.contains("google")) {
            icon.setImageResource(R.drawable.ic_google_pay);
        } else if (methodLower.contains("apple")) {
            icon.setImageResource(R.drawable.ic_apple_pay);
        } else {
            icon.setImageResource(R.drawable.ic_card);
        }

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(24, 0, 0, 0);

        TextView userTv = new TextView(this);
        userTv.setText("Χρήστης: " + entry.user);
        userTv.setTextSize(16);
        userTv.setTypeface(null, Typeface.BOLD);

        TextView amountTv = new TextView(this);
        amountTv.setText(String.format("Ποσό: €%.2f", entry.amount));
        amountTv.setTextSize(16);

        String formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(entry.timestamp));
        TextView dateTv = new TextView(this);
        dateTv.setText("Ημερομηνία: " + formattedDate);
        dateTv.setTextSize(14);
        dateTv.setTextColor(getResources().getColor(android.R.color.darker_gray));

        TextView methodTv = new TextView(this);
        methodTv.setText("Μέθοδος: " + entry.method);
        methodTv.setTextSize(14);
        methodTv.setTextColor(getResources().getColor(android.R.color.darker_gray));

        textCol.addView(userTv);
        textCol.addView(amountTv);
        textCol.addView(dateTv);
        textCol.addView(methodTv);

        card.addView(icon);
        card.addView(textCol);

        historyContainer.addView(card);
    }

    private void addTextMessage(String msg) {
        TextView emptyMessage = new TextView(this);
        emptyMessage.setText(msg);
        emptyMessage.setTextSize(16);
        emptyMessage.setTextColor(getResources().getColor(android.R.color.black));
        emptyMessage.setGravity(Gravity.CENTER);
        historyContainer.addView(emptyMessage);
    }

    private static class PaymentEntry {
        String user;
        double amount;
        long timestamp;
        String method;

        public PaymentEntry(String user, double amount, long timestamp, String method) {
            this.user = user;
            this.amount = amount;
            this.timestamp = timestamp;
            this.method = method;
        }
    }
}