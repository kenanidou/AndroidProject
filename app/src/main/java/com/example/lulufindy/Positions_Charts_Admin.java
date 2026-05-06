package com.example.lulufindy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class Positions_Charts_Admin extends AppCompatActivity {

    private PieChart pieChart;
    private Button backButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_stats);

        pieChart = findViewById(R.id.pieChart);
        backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());

        loadParkingFrequency();
    }

    private void loadParkingFrequency() {
        db.collection("admin").document("admin").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    Map<String, Object> freqMap = (Map<String, Object>) doc.get("parkingFrequency");

                    if (freqMap != null && !freqMap.isEmpty()) {
                        ArrayList<PieEntry> entries = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : freqMap.entrySet()) {
                            String label = entry.getKey();
                            Object value = entry.getValue();
                            float count = (value instanceof Number) ? ((Number) value).floatValue() : 0f;

                            if (count > 0f) {
                                entries.add(new PieEntry(count, label));
                            }
                        }

                        if (entries.isEmpty()) {
                            Toast.makeText(this, "Δεν υπάρχουν θετικά δεδομένα για εμφάνιση.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        PieDataSet dataSet = new PieDataSet(entries, "Συχνότερες Θέσεις");
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        dataSet.setValueTextColor(0xFFFFFFFF);
                        dataSet.setValueTextSize(18f);

                        PieData data = new PieData(dataSet);

                        pieChart.setData(data);
                        pieChart.getDescription().setEnabled(false);
                        pieChart.setCenterText("Συχνότερες Θέσεις");
                        pieChart.setCenterTextSize(18f);
                        pieChart.setEntryLabelColor(0xFF000000);
                        pieChart.setUsePercentValues(true);
                        pieChart.setHoleRadius(40f);
                        pieChart.setTransparentCircleRadius(45f);
                        pieChart.animateY(1000);
                        pieChart.invalidate();

                    } else {
                        Toast.makeText(this, "Δεν βρέθηκαν δεδομένα για τις θέσεις parking.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Το έγγραφο admin δεν βρέθηκε.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Αποτυχία φόρτωσης δεδομένων.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
