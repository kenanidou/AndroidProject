package com.example.lulufindy;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class ClassicParking extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userMarker;
    private ProgressDialog progressDialog;
    private ProgressBar mapLoading;
    private LocationCallback locationCallback;
    private boolean loadingHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_classic_parking);

        mapView = findViewById(R.id.map);
        mapLoading = findViewById(R.id.mapLoading);
        MaterialButton backButton = findViewById(R.id.back);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                hideProgressBarOnce();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                hideProgressBarOnce();
                return true;
            }
        });

        backButton.setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();
        loadParkingSpots();
    }

    private void hideProgressBarOnce() {
        if (!loadingHidden) {
            loadingHidden = true;
            runOnUiThread(() -> mapLoading.setVisibility(View.GONE));
        }
    }

    private void getLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(ClassicParking.this, "Δεν βρέθηκε τοποθεσία.", Toast.LENGTH_SHORT).show();
                    return;
                }

                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    updateUserLocation(location.getLatitude(), location.getLongitude());
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            } catch (SecurityException e) {
                Toast.makeText(this, "Απορρίφθηκε η άδεια τοποθεσίας.", Toast.LENGTH_SHORT).show();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateUserLocation(double latitude, double longitude) {
        GeoPoint userLocation = new GeoPoint(latitude, longitude);

        mapView.post(() -> {
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(userLocation);
            mapView.invalidate();
        });

        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            userMarker.setTitle("Βρίσκεσαι εδώ!");
            userMarker.setIcon(ResourcesCompat.getDrawable(getResources(), org.osmdroid.library.R.drawable.marker_default, null));
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(userLocation);

        loadParkingSpots();
    }

    private void loadParkingSpots() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ClassicParkings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            com.google.firebase.firestore.GeoPoint location = document.getGeoPoint("pin");
                            if (location != null) {
                                addParkingMarker(location.getLatitude(), location.getLongitude());
                            }
                        }
                    } else {
                        Toast.makeText(ClassicParking.this, "Σφάλμα κατά την ανάκτηση θέσεων.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ClassicParking.this, "Απέτυχε η σύνδεση με τη βάση.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addParkingMarker(double latitude, double longitude) {
        GeoPoint parkingLocation = new GeoPoint(latitude, longitude);
        Marker parkingMarker = new Marker(mapView);
        parkingMarker.setPosition(parkingLocation);
        parkingMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        parkingMarker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.parking, null));

        parkingMarker.setOnMarkerClickListener((marker, mapView) -> {
            openGoogleMaps(userMarker.getPosition(), parkingLocation);

            return true;
        });

        mapView.getOverlays().add(parkingMarker);
    }

    private void openGoogleMaps(GeoPoint from, GeoPoint to) {
        String uri = "https://www.google.com/maps/dir/?api=1&origin=" + from.getLatitude() + "," + from.getLongitude()
                + "&destination=" + to.getLatitude() + "," + to.getLongitude() + "&travelmode=driving";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Δεν βρέθηκε το Google Maps.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Η άδεια τοποθεσίας είναι απαραίτητη.", Toast.LENGTH_SHORT).show();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}