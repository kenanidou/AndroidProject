package com.example.lulufindy;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class IncomesChart_Admin extends AppCompatActivity {

    private BarChart barChart;
    private FirebaseFirestore db;
    private Map<String, Float> incomeMap = new TreeMap<>();
    private String currentFilter = "day";
    private ImageView filterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incomeschart_admin);

        barChart = findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();
        filterButton = findViewById(R.id.filterButton);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        filterButton.setOnClickListener(this::showFilterMenu);

        loadIncomeData();
    }

    private void showFilterMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add("Ανά Ημέρα");
        popupMenu.getMenu().add("Ανά Μήνα");
        popupMenu.setOnMenuItemClickListener(item -> {
            currentFilter = item.getTitle().equals("Ανά Ημέρα") ? "day" : "month";
            loadIncomeData();
            return true;
        });
        popupMenu.show();
    }

    private void loadIncomeData() {
        incomeMap.clear();

        db.collection("admin").document("admin")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> incomes = (Map<String, Object>) documentSnapshot.get("Incomes");
                    if (incomes != null) {
                        SimpleDateFormat sdf = currentFilter.equals("day")
                                ? new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                : new SimpleDateFormat("yyyy-MM", Locale.getDefault());

                        for (Map.Entry<String, Object> entry : incomes.entrySet()) {
                            List<Map<String, Object>> payments = (List<Map<String, Object>>) entry.getValue();
                            if (payments != null) {
                                for (Map<String, Object> payment : payments) {
                                    Double amount = (Double) payment.get("amount");
                                    Long timestampLong = (Long) payment.get("timestamp");

                                    if (amount != null && timestampLong != null) {
                                        Date timestamp = new Date(timestampLong);
                                        String key = sdf.format(timestamp);
                                        float current = incomeMap.getOrDefault(key, 0f);
                                        incomeMap.put(key, current + amount.floatValue());
                                    }
                                }
                            }
                        }
                    }

                    drawBarChart();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void drawBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : incomeMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Έσοδα " + (currentFilter.equals("day") ? "Ανά Ημέρα" : "Ανά Μήνα"));
        dataSet.setColor(Color.parseColor("#ED6091"));
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.3f);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextSize(14f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextSize(12f);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }
}