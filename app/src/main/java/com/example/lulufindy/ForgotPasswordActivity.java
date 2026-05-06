package com.example.lulufindy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    TextInputEditText emailInput;
    Button resetButton;
    TextView backToLogin;
    FirebaseAuth mAuth;
    FirebaseFunctions mFunctions;
    FirebaseFirestore db;

    private static final String TAG = "FORGOT_PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.email);
        resetButton = findViewById(R.id.button_reset_password);
        backToLogin = findViewById(R.id.back_to_login);

        mAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();
        db = FirebaseFirestore.getInstance();

        resetButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim().toLowerCase() : "";

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Εισάγετε email", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "🔍 Αναζήτηση χρήστη στο Firestore με email: " + email);

            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String uid = doc.getId();

                            Log.d(TAG, "✅ Βρέθηκε UID: " + uid);

                            if (TextUtils.isEmpty(uid)) {
                                Toast.makeText(this, "Το UID του χρήστη δεν βρέθηκε", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 🔁 Κλήση της Cloud Function με σωστό serialization
                            Map<String, Object> data = new HashMap<>();
                            data.put("uid", uid);
                            Log.d(TAG, "📤 Αποστολή δεδομένων στη function: uid=" + uid);


                            mFunctions
                                    .getHttpsCallable("generateTempPassword")
                                    .call(data)
                                    .addOnSuccessListener(result -> {
                                        Object rawData = result.getData();
                                        if (rawData instanceof Map) {
                                            Map<String, Object> res = (Map<String, Object>) rawData;
                                            String tempPassword = (String) res.get("tempPassword");

                                            Log.d(TAG, "✅ Προσωρινός κωδικός: " + tempPassword);

                                            new AlertDialog.Builder(this)
                                                    .setTitle("Προσωρινός Κωδικός")
                                                    .setMessage("Ο προσωρινός σου κωδικός είναι:\n\n" + tempPassword +
                                                            "\n\nΧρησιμοποίησέ τον για να συνδεθείς και μετά άλλαξέ τον.")
                                                    .setPositiveButton("OK", (dialog, which) -> {
                                                        dialog.dismiss();
                                                        Intent intent = new Intent(ForgotPasswordActivity.this, ResetPassword.class);
                                                        intent.putExtra("email", email);
                                                        startActivity(intent);
                                                    })
                                                    .show();
                                        } else {
                                            Toast.makeText(this, "⚠️ Σφάλμα στην απάντηση από τον server", Toast.LENGTH_LONG).show();
                                            Log.e(TAG, "❌ rawData δεν ήταν Map: " + rawData);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Σφάλμα Cloud Function: " + e.getMessage(), e);
                                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            Log.e(TAG, "❌ Δεν βρέθηκε χρήστης με email: " + email);
                            Toast.makeText(this, "Το email δεν βρέθηκε στη βάση", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Σφάλμα Firestore: " + e.getMessage(), e);
                        Toast.makeText(this, "Σφάλμα κατά την αναζήτηση χρήστη", Toast.LENGTH_SHORT).show();
                    });
        });

        backToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, Sing_In.class));
            finish();
        });
    }
}


