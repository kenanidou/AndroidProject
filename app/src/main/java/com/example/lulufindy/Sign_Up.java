package com.example.lulufindy;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Sign_Up extends AppCompatActivity {
    private TextInputEditText editTextEmail, editTextPassword, nameInput, lastNameInput;
    private Button buttonSignUp;
    private FirebaseAuth mAuth;
    private TextView sign_in;


    private FirebaseFirestore db;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonSignUp = findViewById(R.id.button_sign_up);
        sign_in = findViewById(R.id.Sing_in_page);


        nameInput = findViewById(R.id.name);
        lastNameInput = findViewById(R.id.last_name);


        db = FirebaseFirestore.getInstance();



        sign_in.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Sing_In.class);
            startActivity(intent);
            finish();
        });

        buttonSignUp.setOnClickListener(v -> {
            String email, password;
            email = Objects.requireNonNull(editTextEmail.getText()).toString();
            password = Objects.requireNonNull(editTextPassword.getText()).toString();

            String name = nameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();

            if (name.isEmpty() || lastName.isEmpty() ||  email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                return;
            }


            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Sign_Up.this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Sign_Up.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Sign_Up.this, "Account Created", Toast.LENGTH_SHORT).show();

                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                String userId = currentUser.getUid();


                                Map<String, Object> user = new HashMap<>();
                                user.put("name", name);
                                user.put("last_name", lastName);
                                user.put("email", email);




                                db.collection("users").document(userId).set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            clearInputs();


                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });

                            }

                        } else {

                            Exception exception = task.getException();
                            if (exception != null) {
                                Log.e("SignUpError", "Error creating account: " + exception.getMessage());
                                Toast.makeText(Sign_Up.this, "Authentication failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        });

    }
    private void clearInputs() {
        nameInput.setText("");
        lastNameInput.setText("");
    }
}
