package com.example.lulufindy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private Button startBtn, searchBtn, walletbtn, btnStatisticsAdmin, managementbtn;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_admin);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_admin);
        navigationView = findViewById(R.id.navigation_view_admin);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        startBtn = findViewById(R.id.btnAdminButton2);
        searchBtn = findViewById(R.id.btnSearchParkingAdmin);
        walletbtn = findViewById(R.id.btnSearchAdmin);
        btnStatisticsAdmin = findViewById(R.id.btnStatisticsAdmin);
        managementbtn = findViewById(R.id.btnWalletAdmin);


        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartParking.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        });

        searchBtn.setOnClickListener(v -> showParkingTypeList());

        walletbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, WalletManagerActivity.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        });

        btnStatisticsAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChartsAdmin.class);
            startActivity(intent);
        });

        managementbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, Management.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        });
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_admin_manage_parking) {
            Intent intent = new Intent(this, Management.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_parking_admin) {
            Intent intent = new Intent(this, StartParking.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_search_admin) {
            showParkingTypeList();
        } else if (id == R.id.nav_wallet_admin) {
            Intent intent = new Intent(this, WalletManagerActivity.class);
            intent.putExtra("origin", "admin");
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_charts_admin) {
            Intent intent = new Intent(this, ChartsAdmin.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout_admin) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, Sing_In.class);
            startActivity(intent);
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
}
