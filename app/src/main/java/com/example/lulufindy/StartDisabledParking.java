package com.example.lulufindy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.lulufindy.StartParking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class StartDisabledParking extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String API_KEY = AppConfig.API_KEY;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation;
    private Polyline currentPolyline;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration parkingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_disabled_parking);

        MaterialButton backButton = findViewById(R.id.back);
        backButton.setOnClickListener(v -> finish());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        getUserLocation();
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
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                mMap.setMyLocationEnabled(true);
                loadParkingMarkers();
            } else {
                Toast.makeText(this, "Δεν βρέθηκε τοποθεσία χρήστη", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadParkingMarkers() {
        if (parkingListener != null) {
            parkingListener.remove();
        }

        parkingListener = db.collection("DisabledParkings")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed", error);
                        Toast.makeText(this, "Σφάλμα στη σύνδεση με τη βάση.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mMap.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        GeoPoint geo = doc.getGeoPoint("pin");
                        Boolean taken = doc.getBoolean("taken");
                        String name = doc.getString("name");
                        String docId = doc.getId();

                        if (geo != null && (taken == null || !taken)) {
                            LatLng pos = new LatLng(geo.getLatitude(), geo.getLongitude());

                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.blue);

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(name != null ? name : "Θέση Parking")
                                    .snippet("Διαθέσιμη")
                                    .icon(icon));

                            Map<String, Object> tagData = new HashMap<>();
                            tagData.put("pos", pos);
                            tagData.put("docId", docId);
                            tagData.put("name", name);
                            marker.setTag(tagData);
                        }
                    }

                    mMap.setOnMarkerClickListener(marker -> {
                        Map<String, Object> tagData = (Map<String, Object>) marker.getTag();
                        if (tagData == null) return false;

                        LatLng dest = (LatLng) tagData.get("pos");
                        String docId = (String) tagData.get("docId");
                        String displayName = (String) tagData.get("name");

                        if (userLocation != null && dest != null) {
                            fetchRoute(userLocation, dest, docId, displayName, marker.getSnippet());
                        } else {
                            Toast.makeText(this, "Δεν βρέθηκε προορισμός ή τοποθεσία χρήστη", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    });
                });
    }

    private void fetchRoute(LatLng origin, LatLng dest, String docId, String displayName, String status) {
        new Thread(() -> {
            try {
                String urlStr = String.format(Locale.US,
                        "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&mode=driving&key=%s",
                        origin.latitude, origin.longitude, dest.latitude, dest.longitude, API_KEY);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                    String distance = leg.getJSONObject("distance").getString("text");
                    String polyline = route.getJSONObject("overview_polyline").getString("points");

                    List<LatLng> points = decodePolyline(polyline);

                    runOnUiThread(() -> {
                        if (currentPolyline != null) currentPolyline.remove();
                        currentPolyline = mMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .width(10)
                                .color(0xFF2196F3)
                                .geodesic(true));

                        new AlertDialog.Builder(StartDisabledParking.this)
                                .setTitle("Θέλετε να δεσμεύσετε αυτή την θέση;")
                                .setMessage("Όνομα: " + displayName + "\nΑπόσταση: " + distance)
                                .setPositiveButton("Ναι", (dialog, which) -> {
                                    db.collection("DisabledParkings")
                                            .document(docId)
                                            .update("taken", true)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(StartDisabledParking.this, "Η θέση δεσμεύτηκε!", Toast.LENGTH_SHORT).show();

                                                // --- Δημιουργία ιστορικού entry ---
                                                String name = displayName != null ? displayName : "Parking " + dest.latitude + "," + dest.longitude;
                                                String type = "Disabled";
                                                long startTimestamp = System.currentTimeMillis();

                                                Map<String, Object> historyEntry = new HashMap<>();
                                                historyEntry.put("parkingName", name);
                                                historyEntry.put("parkingType", type);
                                                historyEntry.put("timestamp", startTimestamp);
                                                historyEntry.put("endTimestamp", startTimestamp);

                                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                                DocumentReference userRef = FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(userId);


                                                userRef.update("reservedHistory", FieldValue.arrayUnion(historyEntry))
                                                        .addOnFailureListener(e -> {
                                                            Map<String, Object> data = new HashMap<>();
                                                            data.put("reservedHistory", Arrays.asList(historyEntry));
                                                            userRef.set(data, SetOptions.merge());
                                                        });


                                                DocumentReference adminRef = FirebaseFirestore.getInstance()
                                                        .collection("admin")
                                                        .document("admin");

                                                String frequencyField = "parkingFrequency." + name;

                                                adminRef.update(frequencyField, FieldValue.increment(1))
                                                        .addOnFailureListener(e -> {
                                                            Map<String, Object> data = new HashMap<>();
                                                            data.put(frequencyField, 1);
                                                            adminRef.set(data, SetOptions.merge());
                                                        });


                                                Intent intent = new Intent(StartDisabledParking.this, StartParking.class);
                                                intent.putExtra("parking_name", displayName);
                                                Intent returnIntent = new Intent();
                                                returnIntent.putExtra("return_from", "startdisabled");
                                                returnIntent.putExtra("parking_name", displayName);
                                                setResult(RESULT_OK, returnIntent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(StartDisabledParking.this, "Αποτυχία δέσμευσης.", Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Όχι", (dialog, which) -> dialog.dismiss())
                                .show();
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Δεν βρέθηκε διαδρομή.", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e("DirectionsAPI", "Route fetch failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Αποτυχία φόρτωσης διαδρομής.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length(), lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift; shift += 5;
            } while (b >= 0x20);
            lat += ((result & 1) != 0 ? ~(result >> 1) : result >> 1);

            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift; shift += 5;
            } while (b >= 0x20);
            lng += ((result & 1) != 0 ? ~(result >> 1) : result >> 1);

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return poly;
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
            Toast.makeText(this, "Απαραίτητη η άδεια τοποθεσίας.", Toast.LENGTH_SHORT).show();
        }
    }
}
