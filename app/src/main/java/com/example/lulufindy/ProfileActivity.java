package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText nameInput, lastNameInput, emailInput, passwordInput;
    private Button updateProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameInput = findViewById(R.id.name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        updateProfileButton = findViewById(R.id.update_button);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> finish());


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            startActivity(new Intent(this, Sing_In.class));
            return;
        }


        emailInput.setText(user.getEmail());
        emailInput.setEnabled(false);

        loadUserData();

        updateProfileButton.setOnClickListener(v -> updateProfile());
    }

    private void loadUserData() {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        nameInput.setText(documentSnapshot.getString("name"));
                        lastNameInput.setText(documentSnapshot.getString("last_name"));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Σφάλμα φόρτωσης δεδομένων", Toast.LENGTH_SHORT).show());
    }

    private void updateProfile() {
        String newName = nameInput.getText().toString().trim();
        String newLastName = lastNameInput.getText().toString().trim();
        String newPassword = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newLastName)) {
            Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
            return;
        }


        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("last_name", newLastName);

        db.collection("users").document(user.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Το προφίλ ενημερώθηκε", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Σφάλμα ενημέρωσης", Toast.LENGTH_SHORT).show();
                });


        String oldPassword = ((TextInputEditText) findViewById(R.id.old_password_input)).getText().toString().trim();

        if (!TextUtils.isEmpty(newPassword)) {
            if (TextUtils.isEmpty(oldPassword)) {
                Toast.makeText(this, "Συμπλήρωσε τον παλιό κωδικό για αλλαγή", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
            user.reauthenticate(credential)
                    .addOnSuccessListener(unused -> {
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(unused2 -> {
                                    Toast.makeText(this, "Ο κωδικός ενημερώθηκε", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Σφάλμα αλλαγής κωδικού: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ο παλιός κωδικός δεν είναι σωστός", Toast.LENGTH_SHORT).show();
                    });
        }

    }
}
