package com.example.lulufindy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Time_History extends AppCompatActivity {

    private BarChart barChart;
    private TextView avgDurationTextView;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_history);

        barChart = findViewById(R.id.barChart);
        avgDurationTextView = findViewById(R.id.avgDurationTextView);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> finish());

        fetchDurationData();
    }

    private void fetchDurationData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || !snapshot.contains("paymentHistory")) {
                        Toast.makeText(this, "Δεν βρέθηκαν εγγραφές", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> history = (List<Map<String, Object>>) snapshot.get("paymentHistory");

                    if (history == null || history.isEmpty()) {
                        Toast.makeText(this, "Άδειο ιστορικό", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    int index = 0;
                    float totalDuration = 0f;

                    Collections.reverse(history);

                    for (Map<String, Object> item : history) {
                        if (item.containsKey("duration")) {
                            String durationStr = (String) item.get("duration");
                            float durationMins = convertDurationToMinutes(durationStr);
                            totalDuration += durationMins;
                            entries.add(new BarEntry(index++, durationMins));
                        }
                    }

                    if (entries.isEmpty()) {
                        Toast.makeText(this, "Καμία διάρκεια διαθέσιμη", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    float avgDuration = totalDuration / entries.size();
                    avgDurationTextView.setText(String.format(Locale.getDefault(),
                            "Μέσος χρόνος: %.2f λεπτά", avgDuration));

                    BarDataSet dataSet = new BarDataSet(entries, "Διάρκεια (λεπτά)");
                    dataSet.setColor(getColor(R.color.purple_200));

                    BarData barData = new BarData(dataSet);
                    barData.setBarWidth(0.5f);

                    barChart.setData(barData);
                    barChart.setFitBars(true);
                    barChart.getDescription().setEnabled(false);

                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGranularity(1f);
                    xAxis.setDrawGridLines(false);

                    barChart.getAxisRight().setEnabled(false);
                    barChart.invalidate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Σφάλμα σύνδεσης", Toast.LENGTH_SHORT).show()
                );
    }

    private float convertDurationToMinutes(String durationStr) {
        try {
            String[] parts = durationStr.split(":");
            int hours = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int seconds = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            return hours * 60 + minutes + seconds / 60f;
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }
}

