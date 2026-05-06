package com.example.lulufindy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private Button searchBtn;
    private Button startBtn;
    private Button btnCharts, btnWallet;
    private TextView Walletbalance;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_LuluFindy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ImageButton btnCalendar = findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, com.example.lulufindy.Calendar.class)));

        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));


        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        Walletbalance = findViewById(R.id.tvWalletAmount);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        navigationView.setNavigationItemSelectedListener(this);


        startBtn = findViewById(R.id.btnStartParking);
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartParking.class);
            intent.putExtra("origin", "start");
            startActivity(intent);
            finish();
        });

        searchBtn = findViewById(R.id.btnSearch);
        searchBtn.setOnClickListener(v -> showParkingTypeList());

        btnCharts = findViewById(R.id.btnCharts);
        btnCharts.setOnClickListener(v -> startActivity(new Intent(this, Charts.class)));

        btnWallet = findViewById(R.id.btnWallet);
        btnWallet.setOnClickListener(v -> {
            Intent intent = new Intent(this, WalletManagerActivity.class);
            intent.putExtra("origin", "start");
            startActivity(intent);
            finish();
        });


        checkWorkingHours();
    }

    private void showParkingTypeList() {
        String[] parkingOptions = {"Κανονική Θέση", "Ηλεκτρική Θέση", "Θέση Αναπήρων"};

        new AlertDialog.Builder(this)
                .setTitle("Επιλέξτε τύπο θέσης στάθμευσης")
                .setItems(parkingOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startActivity(new Intent(this, ClassicParking.class));
                            break;
                        case 1:
                            startActivity(new Intent(this, ElectricParking.class));
                            break;
                        case 2:
                            startActivity(new Intent(this, DisabledParking.class));
                            break;
                        default:
                            Toast.makeText(this, "Άγνωστη επιλογή", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void checkWorkingHours() {
        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("working_hours");

        dbRef.orderByChild("date").equalTo(todayDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isAllowed = false;

                        for (DataSnapshot entry : snapshot.getChildren()) {
                            WorkingHoursData data = entry.getValue(WorkingHoursData.class);
                            if (data == null) continue;

                            if (data.isHoliday) break;

                            if (!"ΚΛΕΙΣΤΑ".equals(data.fromTime) && !"ΚΛΕΙΣΤΑ".equals(data.toTime)) {
                                try {
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                    Calendar now = Calendar.getInstance();
                                    Calendar from = Calendar.getInstance();
                                    Calendar to = Calendar.getInstance();

                                    from.setTime(timeFormat.parse(data.fromTime));
                                    to.setTime(timeFormat.parse(data.toTime));

                                    int nowHour = now.get(Calendar.HOUR_OF_DAY);
                                    int nowMin = now.get(Calendar.MINUTE);
                                    int nowTotal = nowHour * 60 + nowMin;

                                    int fromTotal = from.get(Calendar.HOUR_OF_DAY) * 60 + from.get(Calendar.MINUTE);
                                    int toTotal = to.get(Calendar.HOUR_OF_DAY) * 60 + to.get(Calendar.MINUTE);

                                    if (nowTotal >= fromTotal && nowTotal <= toTotal) {
                                        isAllowed = true;
                                    }
                                } catch (Exception e) {
                                    Log.e("MainActivity", "Error parsing time", e);
                                }
                            }
                        }

                        if (!isAllowed) {
                            startBtn.setEnabled(false);
                            startBtn.setAlpha(0.5f); // Εμφανιστικά disabled
                            Toast.makeText(MainActivity.this, "Εκτός ωραρίου στάθμευσης ή αργία!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MainActivity", "Failed to read working hours", error.toException());
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_parking) {
            startActivity(new Intent(this, StartParking.class).putExtra("origin", "start"));
            finish();
        } else if (id == R.id.nav_wallet) {
            startActivity(new Intent(this, WalletManagerActivity.class).putExtra("origin", "start"));
            finish();
        } else if (id == R.id.nav_charts) {
            startActivity(new Intent(this, Charts.class));
        } else if (id == R.id.nav_search) {
            showParkingTypeList();
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, Sing_In.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWalletBalance();
    }

    private void loadWalletBalance() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("wallets")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Double balance = documentSnapshot.getDouble("balance");
                        Walletbalance.setText(String.format("%.2f €", balance != null ? balance : 0.0));
                    })
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to load wallet balance", e));
        }
    }
}
