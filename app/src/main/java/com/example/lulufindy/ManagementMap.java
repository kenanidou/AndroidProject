package com.example.lulufindy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

import android.location.Geocoder;
import android.location.Address;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.view.KeyEvent;


public class ManagementMap extends AppCompatActivity implements OnMapReadyCallback {

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
        setContentView(R.layout.activity_management_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        EditText searchBar = findViewById(R.id.search_bar);

        Intent intent = getIntent();
        String parkingType = intent.getStringExtra("PARKING_TYPE");
        String parkingName = intent.getStringExtra("PARKING_NAME");


        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) ||
                    (event == null && actionId == EditorInfo.IME_NULL)) {

                String input = searchBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    if (input.matches("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$")) {
                        String[] parts = input.split(",");
                        double latitude = Double.parseDouble(parts[0].trim());
                        double longitude = Double.parseDouble(parts[1].trim());
                        LatLng latLng = new LatLng(latitude, longitude);

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Συντεταγμένες")
                                .icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(parkingType))));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        showAddLocationDialog(latLng, parkingType, parkingName);

                    } else {
                        Geocoder geocoder = new Geocoder(ManagementMap.this);
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(input, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                                mMap.clear();
                                mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(input)
                                        .icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(parkingType))));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                showAddLocationDialog(latLng, parkingType, parkingName);

                            } else {
                                Toast.makeText(this, "Δεν βρέθηκε η τοποθεσία", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Σφάλμα στην αναζήτηση", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                }

                return true;
            }
            return false;
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        getUserLocation();

        loadAllParkings();

        Intent intent = getIntent();
        String parkingType = intent.getStringExtra("PARKING_TYPE");
        String parkingName = intent.getStringExtra("PARKING_NAME");


        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Επιλεγμένη τοποθεσία")
                    .icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(parkingType))));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            showAddLocationDialog(latLng, parkingType, parkingName);
        });
    }


    private void showAddLocationDialog(LatLng latLng, String parkingType, String parkingName) {
        new AlertDialog.Builder(ManagementMap.this)
                .setTitle("Προσθήκη θέσης")
                .setMessage("Θέλετε να προσθέσετε αυτή τη θέση στο σύστημα;")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (parkingType != null && parkingName != null) {
                        Map<String, Object> parkingData = new HashMap<>();
                        parkingData.put("name", parkingName);
                        parkingData.put("latitude", latLng.latitude);
                        parkingData.put("longitude", latLng.longitude);
                        parkingData.put("taken", false);

                        String collectionName;
                        switch (parkingType) {
                            case "Κανονική Θέση":
                                collectionName = "ClassicParkings";
                                break;
                            case "Ηλεκτρική Θέση":
                                collectionName = "ElectricParkings";
                                break;
                            case "Θέση Αναπήρων":
                                collectionName = "DisabledParkings";
                                break;
                            default:
                                Toast.makeText(ManagementMap.this, "Άγνωστος τύπος θέσης", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        db.collection(collectionName)
                                .add(parkingData)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(ManagementMap.this, "Η θέση προστέθηκε!", Toast.LENGTH_SHORT).show();


                                    db.collection(collectionName)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                int totalSpots = querySnapshot.size();


                                                Intent intent = new Intent(ManagementMap.this, AddParking.class);
                                                intent.putExtra("total_spots", totalSpots);
                                                intent.putExtra("parking_type", parkingType);
                                                startActivity(intent);
                                                finish(); // Αν θέλεις να κλείσει η ManagementMap μετά

                                            });
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(ManagementMap.this, "Αποτυχία αποθήκευσης.", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(ManagementMap.this, "Λείπει τύπος ή όνομα θέσης.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Άκυρο", (dialog, which) -> dialog.dismiss())
                .show();
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
            } else {
                Toast.makeText(this, "Δεν βρέθηκε τοποθεσία χρήστη", Toast.LENGTH_SHORT).show();
            }
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

                        new AlertDialog.Builder(ManagementMap.this)
                                .setTitle("Θέλετε να δεσμεύσετε αυτή την θέση;")
                                .setMessage("Όνομα: " + displayName + "\nΑπόσταση: " + distance)
                                .setPositiveButton("Ναι", (dialog, which) -> {
                                    db.collection("ClassicParkings")
                                            .document(docId)
                                            .update("taken", true)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(ManagementMap.this, "Η θέση δεσμεύτηκε!", Toast.LENGTH_SHORT).show();

                                                String name = displayName != null ? displayName : "Parking " + dest.latitude + "," + dest.longitude;
                                                String type = "Classic";
                                                long startTimestamp = System.currentTimeMillis();

                                                Map<String, Object> historyEntry = new HashMap<>();
                                                historyEntry.put("parkingName", name); // Εδώ μπαίνει το όνομα από Firestore
                                                historyEntry.put("parkingType", type);
                                                historyEntry.put("timestamp", startTimestamp);
                                                historyEntry.put("endTimestamp", startTimestamp); // μπορεί να ενημερωθεί αργότερα

                                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                                FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(userId)
                                                        .update("reservedHistory", FieldValue.arrayUnion(historyEntry))
                                                        .addOnFailureListener(e -> {
                                                            // Σε περίπτωση που δεν υπάρχει το πεδίο, κάνε set
                                                            Map<String, Object> data = new HashMap<>();
                                                            data.put("reservedHistory", Arrays.asList(historyEntry));
                                                            FirebaseFirestore.getInstance()
                                                                    .collection("users")
                                                                    .document(userId)
                                                                    .set(data, SetOptions.merge());
                                                        });
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(ManagementMap.this, "Αποτυχία δέσμευσης.", Toast.LENGTH_SHORT).show());

                                    Intent intent = new Intent(ManagementMap.this, AddParking.class);
                                    intent.putExtra("parking_name", displayName);
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("return_from", "startclassic");
                                    returnIntent.putExtra("parking_name", displayName);
                                    setResult(RESULT_OK, returnIntent);
                                    finish();
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
    private void loadAllParkings() {
        loadParkingCollection("ClassicParkings", "Κανονική Θέση");
        loadParkingCollection("ElectricParkings", "Ηλεκτρική Θέση");
        loadParkingCollection("DisabledParkings", "Θέση Αναπήρων");
    }

    private void loadParkingCollection(String collectionName, String parkingType) {
        db.collection(collectionName).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            GeoPoint geoPoint = doc.getGeoPoint("pin");
                            String name = doc.getString("name");

                            if (geoPoint == null) {
                                Log.w("FirestoreWarning", "Λείπει το pin στο έγγραφο: " + doc.getId());
                                continue;
                            }

                            LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name != null ? name : "Χωρίς όνομα")
                                    .icon(BitmapDescriptorFactory.fromResource(getMarkerIcon(parkingType))));
                        } catch (Exception e) {
                            Log.e("MarkerError", "Σφάλμα δημιουργίας marker για έγγραφο: " + doc.getId(), e);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Σφάλμα ανάκτησης θέσεων από " + collectionName, e));
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
                return R.drawable.parking; // default icon
        }
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

