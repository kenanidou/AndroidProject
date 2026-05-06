
package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WorkingHours extends AppCompatActivity {

    private Button backBtn;
    private TextInputEditText editFrom;
    private TextInputEditText editTo;
    private Switch holidaySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working_hours);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(WorkingHours.this, Management.class);
            startActivity(intent);
            finish();
        });

        TextView datePickerText = findViewById(R.id.dateTextView);
        datePickerText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        datePickerText.setText(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        editFrom = findViewById(R.id.editFromTime);
        editTo = findViewById(R.id.editToTime);
        holidaySwitch = findViewById(R.id.holidaySwitch);

        setupTimePickers();


        holidaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editFrom.setEnabled(!isChecked);
            editTo.setEnabled(!isChecked);

            if (isChecked) {
                editFrom.setText("ΚΛΕΙΣΤΑ");
                editTo.setText("ΚΛΕΙΣΤΑ");
            } else {
                editFrom.setText("");
                editTo.setText("");
            }
        });

        MaterialButton saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(v -> saveDataToFirebase());
    }

    private void setupTimePickers() {
        View.OnClickListener timeClickListener = view -> {
            final TextInputEditText editText = (TextInputEditText) view;
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (timePicker, selectedHour, selectedMinute) -> {
                        String formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                        editText.setText(formattedTime);
                    },
                    hour,
                    minute,
                    true
            );
            timePickerDialog.show();
        };

        editFrom.setOnClickListener(timeClickListener);
        editTo.setOnClickListener(timeClickListener);
    }

    private void saveDataToFirebase() {
        TextView dateTextView = findViewById(R.id.dateTextView);
        TextInputEditText fromTime = findViewById(R.id.editFromTime);
        TextInputEditText toTime = findViewById(R.id.editToTime);
        Switch specialSwitch = findViewById(R.id.SpecialSwitch);

        String date = dateTextView.getText().toString().trim();
        boolean isHoliday = holidaySwitch.isChecked();
        boolean isSpecial = specialSwitch.isChecked();

        String from;
        String to;

        if (isHoliday) {
            from = "ΚΛΕΙΣΤΑ";
            to = "ΚΛΕΙΣΤΑ";
        } else {
            from = fromTime.getText().toString().trim();
            to = toTime.getText().toString().trim();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("fromTime", from);
        data.put("toTime", to);
        data.put("isHoliday", isHoliday);
        data.put("isSpecial", isSpecial);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("working_hours");
        String id = dbRef.push().getKey();
        if (id != null) {
            dbRef.child(id).setValue(data)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Επιτυχής αποθήκευση!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Σφάλμα αποθήκευσης", Toast.LENGTH_SHORT).show());
        }
    }
}
