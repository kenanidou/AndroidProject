package com.example.lulufindy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPassword extends AppCompatActivity {

    EditText tempCodeEditText, newPasswordEditText;
    Button resetButton;
    FirebaseAuth mAuth;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        tempCodeEditText = findViewById(R.id.temp_code);
        newPasswordEditText = findViewById(R.id.new_code);
        resetButton = findViewById(R.id.button);

        mAuth = FirebaseAuth.getInstance();
        userEmail = getIntent().getStringExtra("email");

        resetButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String tempCode = tempCodeEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();

        if (tempCode.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.signInWithEmailAndPassword(userEmail, tempCode)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Ο κωδικός άλλαξε επιτυχώς", Toast.LENGTH_SHORT).show();


                                    Intent intent = new Intent(this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Αποτυχία αλλαγής κωδικού", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ο προσωρινός κωδικός δεν είναι σωστός", Toast.LENGTH_SHORT).show());
    }
}
