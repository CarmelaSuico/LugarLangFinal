package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.driverconductor.LocationService;
import com.usc.lugarlangfinal.driverconductor.StartEndTrip;
import com.usc.lugarlangfinal.models.Trip;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class DriverOrConductoerDashboard extends AppCompatActivity {

    private TextView tvStart, tvEnd, tvPlate, tvVehicle, tvTime, tvFranchise, tvDriver, tvConductor;
    private Button btnStartTrip;
    private MapView map;

    private Trip currentTrip; // Variable to store the assigned trip
    private String userEmail, userName, userFranchise;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_or_conductoer_dashboard);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupOSM();

        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    }, 101);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            fetchUserProfile();
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // UPDATED: Now passes data to the next activity
        btnStartTrip.setOnClickListener(v -> {
            if (currentTrip != null) {
                // Create the Intent
                Intent serviceIntent = new Intent(DriverOrConductoerDashboard.this, LocationService.class);

                // Pass the Trip ID and Franchise so the service knows where to save coordinates[cite: 1]
                serviceIntent.putExtra("TRIP_ID", currentTrip.tripId);
                serviceIntent.putExtra("FRANCHISE", currentTrip.franchise);

                // Use ContextCompat to avoid the version error
                androidx.core.content.ContextCompat.startForegroundService(DriverOrConductoerDashboard.this, serviceIntent);

                // Move to the next screen
                Intent intent = new Intent(DriverOrConductoerDashboard.this, StartEndTrip.class);
                intent.putExtra("ROUTE_CODE", currentTrip.routeCode);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No trip assigned!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        tvStart = findViewById(R.id.tvDriverStart);
        tvEnd = findViewById(R.id.tvDriverEnd);
        tvPlate = findViewById(R.id.tvDriverPlate);
        tvVehicle = findViewById(R.id.tvDriverVehicle);
        tvTime = findViewById(R.id.tvDriverDeparture);
        tvFranchise = findViewById(R.id.tvDriverFranchise);
        tvDriver = findViewById(R.id.tvDriverName);
        tvConductor = findViewById(R.id.tvConductorName);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        map = findViewById(R.id.mapOSM);
    }

    private void setupOSM() {
        if (map == null) return;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        GeoPoint startPoint = new GeoPoint(10.3157, 123.8854);
        map.getController().setZoom(15.0);
        map.getController().setCenter(startPoint);
    }

    private void fetchUserProfile() {
        DatabaseReference empRef = FirebaseDatabase.getInstance(DB_URL).getReference("employee");
        empRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        userName = ds.child("name").getValue(String.class);
                        userFranchise = ds.child("Franchise").getValue(String.class);
                        if (userName != null && userFranchise != null) {
                            fetchAssignedTrip();
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchAssignedTrip() {
        DatabaseReference tripRef = FirebaseDatabase.getInstance(DB_URL).getReference("trips").child(userFranchise);
        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean tripFound = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Trip trip = ds.getValue(Trip.class);
                    if (trip != null && userName != null) {
                        if (userName.equalsIgnoreCase(trip.driverName) || userName.equalsIgnoreCase(trip.conductorName)) {
                            currentTrip = trip; // Save the trip globally for the Intent
                            displayTripData(trip);
                            fetchRouteCoordinates(trip.routeCode);
                            tripFound = true;
                            break;
                        }
                    }
                }
                if (!tripFound) {
                    Toast.makeText(DriverOrConductoerDashboard.this, "No assigned trip found.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayTripData(Trip trip) {
        if (trip == null) return;
        tvStart.setText("From: " + (trip.terminal1 != null ? trip.terminal1 : "N/A"));
        tvEnd.setText("To: " + (trip.terminal2 != null ? trip.terminal2 : "N/A"));
        tvPlate.setText("Plate Number: " + (trip.plateNumber != null ? trip.plateNumber : "N/A"));
        tvVehicle.setText("Assigned Vehicle: " + (trip.vehicleCode != null ? trip.vehicleCode : "N/A"));
        tvTime.setText("Departure Time: " + (trip.departureTime != null ? trip.departureTime : "N/A"));
        tvFranchise.setText("Franchise: " + (trip.franchise != null ? trip.franchise : "N/A"));
        tvDriver.setText("Assigned Driver: " + (trip.driverName != null ? trip.driverName : "N/A"));
        tvConductor.setText("Assigned Conductor: " + (trip.conductorName != null ? trip.conductorName : "N/A"));
    }

    private void fetchRouteCoordinates(String routeCode) {
        if (routeCode == null) return;
        DatabaseReference routeRef = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        routeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String t1Coords = snapshot.child("T1_Coords").getValue(String.class);
                    if (t1Coords != null && !t1Coords.isEmpty()) {
                        String[] latLng = t1Coords.split(",");
                        double lat = Double.parseDouble(latLng[0].trim());
                        double lon = Double.parseDouble(latLng[1].trim());
                        updateMapLocation(lat, lon, snapshot.child("Terminal1").getValue(String.class));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateMapLocation(double lat, double lon, String terminalName) {
        if (map == null) return;
        GeoPoint startPoint = new GeoPoint(lat, lon);
        map.getController().setZoom(17.0);
        map.getController().setCenter(startPoint);
        map.getOverlays().clear();
        Marker startMarker = new Marker(map);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle(terminalName);
        map.getOverlays().add(startMarker);
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}