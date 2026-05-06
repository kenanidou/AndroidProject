package com.example.lulufindy;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Parking_History extends AppCompatActivity {

    private TableLayout tableLayout;
    private TextView emptyMessage;
    private ProgressBar loadingBar;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_history);

        tableLayout = findViewById(R.id.tableLayout);
        emptyMessage = findViewById(R.id.empty_message);
        loadingBar = findViewById(R.id.loadingBar);
        backBtn = findViewById(R.id.backButton);

        fetchHistoryFromFirestore();

        backBtn.setOnClickListener(v -> finish());
    }

    private void fetchHistoryFromFirestore() {
        loadingBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    loadingBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists() &&
                            documentSnapshot.contains("reservedHistory") &&
                            documentSnapshot.contains("paymentHistory")) {

                        List<Map<String, Object>> reservedHistory =
                                (List<Map<String, Object>>) documentSnapshot.get("reservedHistory");

                        List<Map<String, Object>> paymentHistory =
                                (List<Map<String, Object>>) documentSnapshot.get("paymentHistory");

                        if (reservedHistory != null && !reservedHistory.isEmpty()) {
                            tableLayout.removeAllViews();


                            Collections.reverse(reservedHistory);
                            if (paymentHistory != null) {
                                Collections.reverse(paymentHistory);
                            }


                            TableRow header = new TableRow(this);
                            header.addView(createHeaderTextView("Όνομα 🚗 "));
                            header.addView(createHeaderTextView("Τύπος 🅿️"));
                            header.addView(createHeaderTextView("Ημερομηνία 📅 "));
                            header.addView(createHeaderTextView("Διάρκεια ⏳ "));
                            tableLayout.addView(header);

                            for (int i = 0; i < reservedHistory.size(); i++) {
                                Map<String, Object> item = reservedHistory.get(i);
                                String name = (String) item.get("parkingName");
                                String type = (String) item.get("parkingType");
                                Long start = (Long) item.get("timestamp");

                                String dateStr = (start != null)
                                        ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(start))
                                        : "-";

                                String durationStr = "-";
                                if (paymentHistory != null && i < paymentHistory.size()) {
                                    Map<String, Object> payment = paymentHistory.get(i);
                                    if (payment.containsKey("duration")) {
                                        durationStr = (String) payment.get("duration");
                                    }
                                }

                                TableRow row = new TableRow(this);
                                row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#FFF0F5") : Color.parseColor("#FFFFFF"));

                                row.addView(createStyledTextView(name));
                                row.addView(createStyledTextView(type));
                                row.addView(createStyledTextView(dateStr));
                                row.addView(createStyledTextView(durationStr));

                                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                                anim.setDuration(500);
                                anim.setStartOffset(i * 100);
                                row.setAnimation(anim);

                                tableLayout.addView(row);
                            }

                            tableLayout.setVisibility(View.VISIBLE);
                            emptyMessage.setVisibility(View.GONE);
                        } else {
                            showEmpty();
                        }
                    } else {
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingBar.setVisibility(View.GONE);
                    showEmpty();
                });
    }

    private void showEmpty() {
        tableLayout.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
    }

    private TextView createStyledTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setPadding(20, 14, 19, 14);
        tv.setTextColor(Color.DKGRAY);
        return tv;
    }

    private TextView createHeaderTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(22, 14, 18, 14);
        tv.setTextColor(Color.BLACK);
        return tv;
    }
}


