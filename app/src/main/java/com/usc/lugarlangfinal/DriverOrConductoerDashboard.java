package com.usc.lugarlangfinal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.driverconductor.LocationService;
import com.usc.lugarlangfinal.driverconductor.StartEndTrip;
import com.usc.lugarlangfinal.driverconductor.Ticketing;
import com.usc.lugarlangfinal.models.Trip;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class DriverOrConductoerDashboard extends AppCompatActivity {

    private LinearLayout btnDashboardNav, btnTicket, btnSetting;
    private TextView tvStart, tvEnd, tvPlate, tvVehicle, tvTime, tvFranchise, tvDriver, tvConductor;
    private Button btnStartTrip;
    private MapView map;

    private Trip currentTrip;
    private String userEmail, userName, userFranchise, employeeNumericId;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_driver_or_conductoer_dashboard);

        initViews();
        setupOSM();
        checkInitialPermissions();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            fetchUserProfile();
        }

        btnStartTrip.setOnClickListener(v -> {
            if (currentTrip != null) {
                checkLocationSettingsAndStart();
            } else {
                Toast.makeText(this, "No assigned trip found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDashboardNav.setSelected(true);
        btnTicket.setOnClickListener(v -> {
            Intent intent = new Intent(this, Ticketing.class);
            intent.putExtra("ROUTE_CODE", currentTrip.getRouteCode());
            intent.putExtra("COMPANY_NAME", userFranchise);
            intent.putExtra("EMPLOYEE_ID", employeeNumericId);
            startActivity(intent);
        });

        btnSetting.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingAdminDriCon.class);
            startActivity(intent);
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
        btnDashboardNav = findViewById(R.id.btndriverorconddashoard);
        btnTicket = findViewById(R.id.btnticketing);
        btnSetting = findViewById(R.id.btnsetting);
    }

    private void setupOSM() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
    }

    private void checkInitialPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
            }, 101);
        }
    }

    private void fetchUserProfile() {
        DatabaseReference empRef = FirebaseDatabase.getInstance(DB_URL).getReference("employee");
        empRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    userName = ds.child("name").getValue(String.class);
                    userFranchise = ds.child("Franchise").getValue(String.class);
                    employeeNumericId = ds.child("id").getValue(String.class);
                    if (userFranchise != null) fetchAssignedTrip();
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
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Trip trip = ds.getValue(Trip.class);
                    if (trip != null && (userName.equalsIgnoreCase(trip.getDriverName()) || userName.equalsIgnoreCase(trip.getConductorName()))) {
                        currentTrip = trip;
                        displayTripData(trip);
                        fetchRouteCoordinates(trip.getRouteCode());
                        break;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayTripData(Trip trip) {
        tvStart.setText("From: " + trip.getTerminal1());
        tvEnd.setText("To: " + trip.getTerminal2());
        tvPlate.setText("Plate: " + trip.getPlateNumber());
        tvTime.setText("Departure: " + trip.getDepartureTime());
        tvDriver.setText("Driver: " + trip.getDriverName());
        tvConductor.setText("Conductor: " + trip.getConductorName());
    }

    private void checkLocationSettingsAndStart() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(request);

        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
                .addOnSuccessListener(this, response -> proceedToStartTrip())
                .addOnFailureListener(this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(this, 1001);
                        } catch (Exception ignored) {}
                    }
                });
    }

    private void proceedToStartTrip() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("TRIP_ID", currentTrip.getTripId());
        serviceIntent.putExtra("FRANCHISE", currentTrip.getFranchise());
        ContextCompat.startForegroundService(this, serviceIntent);

        Intent intent = new Intent(this, StartEndTrip.class);
        intent.putExtra("ROUTE_CODE", currentTrip.getRouteCode());
        intent.putExtra("COMPANY_NAME", userFranchise);
        intent.putExtra("EMPLOYEE_ID", employeeNumericId);
        intent.putExtra("TRIP_ID", currentTrip.getTripId()); // PASSING THE TRIP ID
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) proceedToStartTrip();
    }

    private void fetchRouteCoordinates(String routeCode) {
        DatabaseReference routeRef = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        routeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String t1Coords = snapshot.child("T1_Coords").getValue(String.class);
                if (t1Coords != null) {
                    String[] latLng = t1Coords.split(",");
                    GeoPoint start = new GeoPoint(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
                    map.getController().setCenter(start);
                    Marker m = new Marker(map); m.setPosition(start); m.setTitle("Start"); map.getOverlays().add(m);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}