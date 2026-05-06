package com.example.lulufindy;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Sing_In extends AppCompatActivity {
    TextInputEditText editTextEmail ,editTextPassword;
    Button buttonSignIn;
    FirebaseAuth mAuth;
    TextView sign_up;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && currentUser.getEmail().equals("admin@gmail.com")){
            Intent intent= new Intent(getApplicationContext(),AdminMainActivity.class);
            startActivity(intent);
            finish();
        } else if (currentUser != null ) {
            Intent intent= new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sing_in);

        mAuth= FirebaseAuth.getInstance();
        editTextEmail=findViewById(R.id.email);
        editTextPassword=findViewById(R.id.password);
        buttonSignIn=findViewById(R.id.button_sign_in);
        sign_up=findViewById(R.id.Sing_up_page);

        TextView forgotPassword = findViewById(R.id.forgot_password);

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
            startActivity(intent);
        });


        sign_up.setOnClickListener(v -> {
            Intent intent= new Intent(getApplicationContext(), Sign_Up.class);
            startActivity(intent);
            finish();
        });

        buttonSignIn.setOnClickListener(v -> {
            String email,password;
            email= Objects.requireNonNull(editTextEmail.getText()).toString();
            password= Objects.requireNonNull(editTextPassword.getText()).toString();

            if (TextUtils.isEmpty(email)){
                Toast.makeText(Sing_In.this,"Enter email",Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)){
                Toast.makeText(Sing_In.this,"Enter password",Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(),"Sign In Succesful",Toast.LENGTH_SHORT).show();
                            Intent intent;
                            if(email.equals("admin@gmail.com")&&(password.equals("admin123"))){
                                intent = new Intent(getApplicationContext(), AdminMainActivity.class);
                            }
                            else {
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                            }
                            startActivity(intent);
                            finish();

                        } else {

                            Toast.makeText(Sing_In.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }
}