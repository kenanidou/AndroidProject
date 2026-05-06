package com.example.lulufindy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class StartParking extends AppCompatActivity {

    private AutoCompleteTextView parkingTypeView, parkingTypeInput;
    private TextInputEditText carPlateInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseUser user;

    private Button startButton, areabtn, backBtn, stopBtn;
    private TextView timerTextView, costTextView;

    private ParkingSession currentSession;
    private final double costPerMinute = 0.05;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isParkingSelected = false;


    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentSession != null && currentSession.isActive()) {
                timerTextView.setText("\u03a7\u03c1\u03cc\u03bd\u03bf\u03c2: " + currentSession.getFormattedElapsedTime());
                double runningCost = currentSession.calculateCost(costPerMinute);
                double totalCost = 0.50 + runningCost;
                costTextView.setText(String.format("\u03a7\u03c1\u03ad\u03c9\u03c3\u03b7: %.2f€", totalCost));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_parking);

        parkingTypeView = findViewById(R.id.parking_type);
        db = FirebaseFirestore.getInstance();
        startButton = findViewById(R.id.startButton);
        timerTextView = findViewById(R.id.timerTextView);
        costTextView = findViewById(R.id.costTextView);
        areabtn = findViewById(R.id.areaCodeButton);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        carPlateInput = findViewById(R.id.car_license_plate);
        parkingTypeInput = findViewById(R.id.parking_type);
        stopBtn = findViewById(R.id.stopButton);

        final String origin = getIntent().getStringExtra("origin");
        Log.d("DEBUG", "Origin: " + origin);


        backBtn = findViewById(R.id.back2);
        backBtn.setOnClickListener(v -> {

            if ("start".equals(origin)) {
                startActivity(new Intent(StartParking.this, MainActivity.class));
            } else if ("admin".equals(origin)) {
                startActivity(new Intent(StartParking.this, AdminMainActivity.class));
            }
            finish();
        });

        TextView parkingNameTextView = findViewById(R.id.parking_name);
        final String[] parkingNameHolder = new String[1];
        parkingNameHolder[0] = getIntent().getStringExtra("parking_name");

        if (parkingNameHolder[0] != null && !parkingNameHolder[0].isEmpty()) {
            parkingNameTextView.setVisibility(View.VISIBLE);
            parkingNameTextView.setText("\u0395\u03c0\u03b9\u03bb\u03b5\u03b3\u03bc\u03ad\u03bd\u03b7 \u03b8\u03ad\u03c3\u03b7: " + parkingNameHolder[0]);
        } else {
            parkingNameTextView.setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            String savedPlate = savedInstanceState.getString("car_plate");
            String savedType = savedInstanceState.getString("parking_type");
            if (savedPlate != null) carPlateInput.setText(savedPlate);
            if (savedType != null) parkingTypeInput.setText(savedType);
        }

        startButton.setOnClickListener(v -> {
            String vehicleNumber = carPlateInput.getText().toString().trim();

            if (!isParkingSelected) {
                Toast.makeText(this, "Πρέπει πρώτα να επιλέξετε θέση!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Συμπληρώστε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                return;
            }

            currentSession = new ParkingSession(vehicleNumber, System.currentTimeMillis());
            handler.post(updateTimerRunnable);

            Toast.makeText(this, "Η στάθμευση ξεκίνησε!", Toast.LENGTH_SHORT).show();


            startButton.setEnabled(false);
            areabtn.setEnabled(false);
            carPlateInput.setEnabled(false);
            parkingTypeInput.setEnabled(false);
            backBtn.setEnabled(false);

        });

        stopBtn.setOnClickListener(v -> {
            if (currentSession != null && currentSession.isActive()) {
                new AlertDialog.Builder(this)
                        .setTitle("\u039f\u03bb\u03bf\u03ba\u03bb\u03ae\u03c1\u03c9\u03c3\u03b7 \u03a3\u03c4\u03ac\u03b8\u03bc\u03b5\u03c5\u03c3\u03b7\u03c2")
                        .setMessage("Τελειώσατε την στάθμευσή σας;")

                        .setPositiveButton("\u039d\u03b1\u03b9", (dialog, which) -> {
                            currentSession.stopSession();
                            handler.removeCallbacks(updateTimerRunnable);

                            final String parkingName = parkingNameTextView.getText().toString().replace("\u0395\u03c0\u03b9\u03bb\u03b5\u03b3\u03bc\u03ad\u03bd\u03b7 \u03b8\u03ad\u03c3\u03b7: ", "");
                            final String parkingType = parkingTypeInput.getText().toString().trim();

                            if (!parkingName.isEmpty() && !parkingType.isEmpty()) {
                                String collectionName = "";
                                switch (parkingType) {
                                    case "Κανονική Θέση": collectionName = "ClassicParkings"; break;
                                    case "Ηλεκτρική Θέση": collectionName = "ElectricParkings"; break;
                                    case "Θέση Αναπήρων": collectionName = "DisabledParkings"; break;
                                    default:
                                        Toast.makeText(this, "Άγνωστος τύπος θέσης.", Toast.LENGTH_SHORT).show();
                                        return;
                                }
                                String finalCollectionName = collectionName;
                                db.collection(finalCollectionName)
                                        .whereEqualTo("name", parkingName)
                                        .get()
                                        .addOnSuccessListener(snapshots -> {
                                            for (QueryDocumentSnapshot doc : snapshots) {
                                                db.collection(finalCollectionName).document(doc.getId())
                                                        .update("taken", false)
                                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Parking freed"))
                                                        .addOnFailureListener(e -> Log.e("Firestore", "Error freeing parking", e));
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to find parking document", e));
                            }

                            Toast.makeText(this, "Η στάθμευση τερματίστηκε!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(StartParking.this, Payment.class);
                            intent.putExtra("total_time", currentSession.getFormattedElapsedTime());
                            intent.putExtra("total_cost", 0.50 + currentSession.calculateCost(costPerMinute));
                            startActivity(intent);
                        })
                        .setNegativeButton("Όχι", null)
                        .show();
            } else {
                Toast.makeText(this, "Δεν υπάρχει ενεργή στάθμευση.", Toast.LENGTH_SHORT).show();
            }
        });

        String[] parkingTypes = {"Ηλεκτρική Θέση", "Θέση Αναπήρων", "Κανονική Θέση"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, parkingTypes);
        parkingTypeView.setAdapter(adapter);
        parkingTypeView.setOnClickListener(v -> parkingTypeView.showDropDown());

        areabtn.setOnClickListener(v -> {
            if (user != null || "admin".equals(origin)) {
                String carPlate = carPlateInput.getText().toString().trim();
                String parkingType = parkingTypeInput.getText().toString().trim();


                if (carPlate.isEmpty()) {
                    carPlateInput.setError("Το πεδίο είναι υποχρεωτικό");
                    return;
                }
                if (parkingType.isEmpty()) {
                    parkingTypeInput.setError("Επιλέξτε τύπο θέσης");
                    return;
                }


                if (!carPlate.matches("^[Α-ΩA-Z]{2,3}\\d{4}$")) {
                    carPlateInput.setError("Μη έγκυρη πινακίδα (π.χ. ΑΒ1234 ή ΧΥΖ5678)");
                    return;
                }

                if ("admin".equals(origin)) {
                    switch (parkingType) {
                        case "Ηλεκτρική Θέση":
                            startActivityForResult(new Intent(this, StartElectricParking.class).putExtra("origin", "admin"), 1);
                            break;
                        case "Θέση Αναπήρων":
                            startActivityForResult(new Intent(this, StartDisabledParking.class).putExtra("origin", "admin"), 1);
                            break;
                        case "Κανονική Θέση":
                            startActivityForResult(new Intent(this, StartClassicParking.class).putExtra("origin", "admin"), 1);
                            break;
                        default:
                            Toast.makeText(this, "Άγνωστος τύπος θέσης.", Toast.LENGTH_SHORT).show();
                    }
                    isParkingSelected = true;
                    return;
                }

                String userId = user.getUid();

                Map<String, Object> update = new HashMap<>();
                update.put("car_plate", carPlate);
                update.put("parking_type", parkingType);

                db.collection("users").document(userId).update(update);

                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                switch (parkingType) {
                                    case "Ηλεκτρική Θέση":
                                        startActivityForResult(new Intent(this, StartElectricParking.class).putExtra("origin", "start"), 1);
                                        break;
                                    case "Θέση Αναπήρων":
                                        startActivityForResult(new Intent(this, StartDisabledParking.class).putExtra("origin", "start"), 1);
                                        break;
                                    case "Κανονική Θέση":
                                        startActivityForResult(new Intent(this, StartClassicParking.class).putExtra("origin", "start"), 1);
                                        break;
                                    default:
                                        Toast.makeText(this, "Άγνωστος τύπος θέσης.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.d("Firestore", "Error getting document", e);
                            Toast.makeText(this, "Σφάλμα κατά την λήψη δεδομένων.", Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(this, "Δεν είστε συνδεδεμένοι", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Sing_In.class));
                finish();
            }
            isParkingSelected = true;
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String from = data.getStringExtra("return_from");
            if (from != null && (from.equals("startclassic") || from.equals("startdisabled") || from.equals("startelectric"))) {
                String parkingName = data.getStringExtra("parking_name");
                if (parkingName != null) {
                    TextView parkingNameTextView = findViewById(R.id.parking_name);
                    parkingNameTextView.setText("Επιλεγμένη θέση: " + parkingName);
                    parkingNameTextView.setVisibility(View.VISIBLE);
                }
                findViewById(R.id.warning).setVisibility(View.VISIBLE);
                return;
            }
        }
        clearInputs();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("car_plate", carPlateInput.getText().toString());
        outState.putString("parking_type", parkingTypeInput.getText().toString());
    }

    private void clearInputs() {
        carPlateInput.setText("");
        parkingTypeInput.setText("");
    }
}