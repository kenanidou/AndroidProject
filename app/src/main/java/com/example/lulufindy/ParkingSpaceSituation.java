package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ParkingSpaceSituation extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextView;
    private FirebaseFirestore firestore;
    private Switch specialSwitch;
    private List<String> allNames = new ArrayList<>();
    private Button backBtn;
    private static final List<String> COLLECTIONS = Arrays.asList("ClassicParkings", "DisabledParkings", "ElectricParkings");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_space_situation);

        autoCompleteTextView = findViewById(R.id.autoCompleteSearch);
        specialSwitch = findViewById(R.id.SpecialSwitch);
        firestore = FirebaseFirestore.getInstance();
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Management.class);
            startActivity(intent);
            finish();
        });

        loadParkingNames();

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            Toast.makeText(ParkingSpaceSituation.this, "Επέλεξες: " + selected, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.button8).setOnClickListener(v -> {
            String selectedName = autoCompleteTextView.getText().toString().trim();
            boolean isActive = specialSwitch.isChecked();

            if (selectedName.isEmpty()) {
                Toast.makeText(this, "Παρακαλώ επίλεξε θέση", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean takenValue = !isActive; // Αν ενεργή, τότε taken = false

            updateParkingTakenField(selectedName, takenValue);
        });
    }

    private void loadParkingNames() {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (String collection : COLLECTIONS) {
            tasks.add(firestore.collection(collection).get());
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(snapshots -> {
                    for (Object obj : snapshots) {
                        QuerySnapshot snapshot = (QuerySnapshot) obj;
                        for (DocumentSnapshot doc : snapshot) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                allNames.add(name);
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ParkingSpaceSituation.this, R.layout.custom_dropdown_item, new ArrayList<>(allNames)) {
                        @Override
                        public android.widget.Filter getFilter() {
                            return new android.widget.Filter() {
                                @Override
                                protected FilterResults performFiltering(CharSequence constraint) {
                                    FilterResults results = new FilterResults();
                                    List<String> filteredList = new ArrayList<>();

                                    if (constraint == null || constraint.length() == 0) {
                                        filteredList.addAll(allNames);
                                    } else {
                                        String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();

                                        for (String name : allNames) {
                                            String lowerName = name.toLowerCase(Locale.ROOT);

                                            if (lowerName.contains(filterPattern) ||
                                                    lowerName.startsWith(filterPattern) ||
                                                    (filterPattern.equals("c") && name.startsWith("C")) ||
                                                    (filterPattern.equals("d") && name.startsWith("D")) ||
                                                    (filterPattern.equals("e") && name.startsWith("E")) ||
                                                    (filterPattern.contains("κανονικ") && name.startsWith("C")) ||
                                                    (filterPattern.contains("ηλεκτρ") && name.startsWith("E")) ||
                                                    (filterPattern.contains("αναπηρ") && name.startsWith("D"))) {
                                                filteredList.add(name);
                                            }
                                        }
                                    }

                                    results.values = filteredList;
                                    results.count = filteredList.size();
                                    return results;
                                }

                                @Override
                                protected void publishResults(CharSequence constraint, FilterResults results) {
                                    clear();
                                    if (results != null && results.values != null) {
                                        addAll((List<String>) results.values);
                                    }
                                    notifyDataSetChanged();
                                }
                            };
                        }
                    };

                    autoCompleteTextView.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                    autoCompleteTextView.setAdapter(adapter);
                    autoCompleteTextView.setThreshold(1); // Εμφάνιση από το 1ο γράμμα

                    autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            autoCompleteTextView.showDropDown();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ParkingSpaceSituation.this, "Σφάλμα κατά την ανάγνωση δεδομένων", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateParkingTakenField(String name, boolean takenValue) {
        for (String collection : COLLECTIONS) {
            firestore.collection(collection)
                    .whereEqualTo("name", name)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            doc.getReference().update("taken", takenValue)
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(this, "Ενημερώθηκε επιτυχώς!", Toast.LENGTH_SHORT).show()
                                    )
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Σφάλμα κατά την ενημέρωση", Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Σφάλμα κατά την αναζήτηση", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
