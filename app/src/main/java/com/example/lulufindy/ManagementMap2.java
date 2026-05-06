package com.example.lulufindy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class ManagementMap2 extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Map<Marker, DocumentSnapshot> markerDataMap = new HashMap<>();
    private Map<String, Marker> nameToMarkerMap = new HashMap<>();
    private List<String> allNames = new ArrayList<>();
    private AutoCompleteTextView autoCompleteSearch;

    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management_map2);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        autoCompleteSearch = findViewById(R.id.search_bar);

        autoCompleteSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadAllParkings();

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(autoCompleteSearch.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        backBtn = findViewById(R.id.back);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ManagementMap2.this, Management.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        getUserLocation();
        loadAllParkings();

        mMap.setOnMarkerClickListener(marker -> {
            DocumentSnapshot doc = markerDataMap.get(marker);
            if (doc != null) {
                StringBuilder message = new StringBuilder();
                for (String key : doc.getData().keySet()) {
                    Object value = doc.get(key);
                    message.append(key).append(": ").append(value).append("\n");
                }

                new AlertDialog.Builder(this)
                        .setTitle("\u03a0\u03bb\u03b7\u03c1\u03bf\u03c6\u03bf\u03c1\u03af\u03b5\u03c2 \u0398\u03ad\u03c3\u03b7\u03c2")
                        .setMessage(message.toString())
                        .setPositiveButton("OK", null)
                        .show();
            }
            return true;
        });

        autoCompleteSearch.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            Marker marker = nameToMarkerMap.get(selectedName);
            if (marker != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 18));
                marker.showInfoWindow();
            } else {
                Toast.makeText(this, "\u0394\u03b5\u03bd \u03b2\u03c1\u03ad\u03b8\u03b7\u03ba\u03b5 \u03b7 \u03b8\u03ad\u03c3\u03b7", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllParkings() {
        allNames.clear();
        nameToMarkerMap.clear();
        markerDataMap.clear();
        mMap.clear();

        String query = autoCompleteSearch.getText().toString().toLowerCase();

        if (query.contains("κανονικ")) {
            loadParkingCollection("ClassicParkings", "Κανονική Θέση");
        } else if (query.contains("ηλεκτρ")) {
            loadParkingCollection("ElectricParkings", "Ηλεκτρική Θέση");
        } else if (query.contains("αναπηρ")) {
            loadParkingCollection("DisabledParkings", "Θέση Αναπήρων");
        } else if (query.equals("c")) {
            loadParkingCollection("ClassicParkings", "Κανονική Θέση");
        } else if (query.equals("e")) {
            loadParkingCollection("ElectricParkings", "Ηλεκτρική Θέση");
        } else if (query.equals("d")) {
            loadParkingCollection("DisabledParkings", "Θέση Αναπήρων");
        } else {
            loadParkingCollection("ClassicParkings", "Κανονική Θέση");
            loadParkingCollection("ElectricParkings", "Ηλεκτρική Θέση");
            loadParkingCollection("DisabledParkings", "Θέση Αναπήρων");
        }
    }

    private void loadParkingCollection(String collectionName, String parkingType) {
        db.collection(collectionName).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        GeoPoint geoPoint = doc.getGeoPoint("pin");
                        String name = doc.getString("name");

                        if (geoPoint == null || name == null) continue;

                        LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(name)
                                .icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(parkingType))));

                        if (marker != null) {
                            markerDataMap.put(marker, doc);
                            nameToMarkerMap.put(name, marker);
                            allNames.add(name);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            this, android.R.layout.simple_dropdown_item_1line, allNames);

                    autoCompleteSearch.setAdapter(adapter);
                    autoCompleteSearch.setThreshold(1);
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "\u03a3\u03c6\u03ac\u03bb\u03bc\u03b1 \u03b1\u03bd\u03ac\u03ba\u03c4\u03b7\u03c3\u03b7\u03c2 \u03b1\u03c0\u03cc " + collectionName, e));
    }

    private int getMarkerIcon(String parkingType) {
        switch (parkingType) {
            case "Κανονική Θέση":
                return R.drawable.parking;
            case "Ηλεκτρική Θέση":
                return R.drawable.electric_car;
            case "Θέση Αναπήρων":
                return R.drawable.blue;
            default:
                return R.drawable.parking;
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "\u0394\u03b5\u03bd \u03b2\u03c1\u03ad\u03b8\u03b7\u03ba\u03b5 \u03c4\u03bf\u03c0\u03bf\u03b8\u03b5\u03c3\u03af\u03b1 \u03c7\u03c1\u03ae\u03c3\u03c4\u03b7", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(this, "\u0391\u03c0\u03b1\u03c1\u03b1\u03af\u03c4\u03b7\u03c4\u03b7 \u03b7 \u03ac\u03b4\u03b5\u03b9\u03b1 \u03c4\u03bf\u03c0\u03bf\u03b8\u03b5\u03c3\u03af\u03b1\u03c2.", Toast.LENGTH_SHORT).show();
        }
    }
}
