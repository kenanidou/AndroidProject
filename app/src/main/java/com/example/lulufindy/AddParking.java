package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class AddParking extends AppCompatActivity {

    private Button nextbtn, backBtn;
    private TextView parking_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parking);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(AddParking.this, Management.class);
            startActivity(intent);
            finish();
        });

        Spinner spinner = findViewById(R.id.spinner2);
        parking_name = findViewById(R.id.parking_name);
        nextbtn = findViewById(R.id.next);

        nextbtn.setOnClickListener(v -> {
            String parkingName = parking_name.getText().toString().trim();
            String parkingType = spinner.getSelectedItem().toString();


            if (!parkingName.matches("^[CDE](\\d{1,3})$")) {
                parking_name.setError("Το όνομα πρέπει να είναι της μορφής C1, D1 ή E1");
                parking_name.requestFocus();
                return;
            }


            int number = Integer.parseInt(parkingName.substring(1));
            if (number < 1) {
                parking_name.setError("Ο αριθμός πρέπει να είναι μεγαλύτερος από 0");
                parking_name.requestFocus();
                return;
            }


            if (spinner.getSelectedItemPosition() == 0) {
                TextView errorText = (TextView) spinner.getSelectedView();
                errorText.setError("Απαραίτητη επιλογή");
                errorText.setTextColor(Color.RED);
                errorText.setText("Επίλεξε τύπο");
                spinner.requestFocus();
                return;
            }


            char prefix = parkingName.charAt(0);
            boolean validPrefix = (parkingType.equals("Κανονική Θέση") && prefix == 'C') ||
                    (parkingType.equals("Ηλεκτρική Θέση") && prefix == 'E') ||
                    (parkingType.equals("Θέση Αναπήρων") && prefix == 'D');

            if (!validPrefix) {
                parking_name.setError("Το πρόθεμα δεν αντιστοιχεί στον τύπο θέσης.\nC=Κανονική, E=Ηλεκτρική, D=Αναπήρων");
                parking_name.requestFocus();
                return;
            }


            checkIfParkingNameExists(parkingName, exists -> {
                if (exists) {
                    parking_name.setError("Αυτό το όνομα θέσης υπάρχει ήδη!");
                    parking_name.requestFocus();
                } else {
                    Intent intent = new Intent(AddParking.this, ManagementMap.class);
                    intent.putExtra("PARKING_NAME", parkingName);
                    intent.putExtra("PARKING_TYPE", parkingType);
                    startActivity(intent);
                }
            });
        });


        List<String> categories = Arrays.asList(
                "Τύπος Parking",
                "Κανονική Θέση",
                "Ηλεκτρική Θέση",
                "Θέση Αναπήρων"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                categories
        ) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int totalSpots = getIntent().getIntExtra("total_spots", -1);
        String parkingType = getIntent().getStringExtra("parking_type");

        TextView availableTextView = findViewById(R.id.available_spots_text);
        if (totalSpots >= 0) {
            availableTextView.setText("Διαθέσιμες Θέσεις: " + totalSpots);
        } else {
            availableTextView.setText("");
        }

        if (parkingType != null) {
            int index = categories.indexOf(parkingType);
            if (index > 0) {
                spinner.setSelection(index);
            }
        }
    }


    private void checkIfParkingNameExists(String name, final OnExistenceCheckListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> collections = Arrays.asList("ClassicParkings", "ElectricParkings", "DisabledParkings");
        final int[] checksRemaining = {collections.size()};
        final boolean[] found = {false};

        for (String collection : collections) {
            db.collection(collection)
                    .whereEqualTo("name", name)
                    .get()
                    .addOnCompleteListener(task -> {
                        checksRemaining[0]--;

                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            found[0] = true;
                        }

                        if (checksRemaining[0] == 0) {
                            listener.onCheckComplete(found[0]);
                        }
                    });
        }
    }


    interface OnExistenceCheckListener {
        void onCheckComplete(boolean exists);
    }
}
