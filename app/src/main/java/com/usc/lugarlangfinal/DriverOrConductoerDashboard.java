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

    // FIX 1: keep a reference to the persistent trip listener so we can remove it in onDestroy
    private ValueEventListener assignedTripListener;
    private DatabaseReference assignedTripRef;

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
            if (currentTrip != null) {
                Intent intent = new Intent(this, Ticketing.class);
                intent.putExtra("ROUTE_CODE", currentTrip.getRouteCode());
                intent.putExtra("COMPANY_NAME", userFranchise);
                intent.putExtra("EMPLOYEE_ID", employeeNumericId);
                startActivity(intent);
            }
        });

        btnSetting.setOnClickListener(v ->
                startActivity(new Intent(this, SettingAdminDriCon.class)));
    }

    private void initViews() {
        tvStart      = findViewById(R.id.tvDriverStart);
        tvEnd        = findViewById(R.id.tvDriverEnd);
        tvPlate      = findViewById(R.id.tvDriverPlate);
        tvVehicle    = findViewById(R.id.tvDriverVehicle);
        tvTime       = findViewById(R.id.tvDriverDeparture);
        tvFranchise  = findViewById(R.id.tvDriverFranchise);
        tvDriver     = findViewById(R.id.tvDriverName);
        tvConductor  = findViewById(R.id.tvConductorName);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        map          = findViewById(R.id.mapOSM);
        btnDashboardNav = findViewById(R.id.btndriverorconddashoard);
        btnTicket    = findViewById(R.id.btnticketing);
        btnSetting   = findViewById(R.id.btnsetting);
    }

    private void setupOSM() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
    }

    private void checkInitialPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
            }, 101);
        }
    }

    private void fetchUserProfile() {
        FirebaseDatabase.getInstance(DB_URL).getReference("employee")
                .orderByChild("email").equalTo(userEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            userName          = ds.child("name").getValue(String.class);
                            userFranchise     = ds.child("Franchise").getValue(String.class);
                            employeeNumericId = ds.child("id").getValue(String.class);
                            if (userFranchise != null) fetchAssignedTrip();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchAssignedTrip() {
        assignedTripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips").child(userFranchise);

        // FIX 2: keep addValueEventListener so the dashboard refreshes in real-time
        // when the trip is swapped by StartEndTrip — but now we store the reference
        // so it can be cleaned up in onDestroy and doesn't leak.
        // The key insight: this listener correctly re-fires after the swap, which is
        // GOOD for the dashboard (it shows the new terminal info). The old bug was in
        // StartEndTrip re-reading and re-reversing stale data — that's already fixed there.
        assignedTripListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTrip = null; // reset before searching
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Trip trip = ds.getValue(Trip.class);
                    if (trip != null) {
                        trip.setTripId(ds.getKey());
                        if (userName != null &&
                                (userName.equalsIgnoreCase(trip.getDriverName()) ||
                                        userName.equalsIgnoreCase(trip.getConductorName()))) {
                            currentTrip = trip;
                            displayTripData(trip);
                            // FIX 3: center map on the TRIP's current T1_Coords,
                            // not the routes table — so it updates after each swap.
                            centerMapOnTrip(trip);
                            break;
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        assignedTripRef.addValueEventListener(assignedTripListener);
    }

    private void displayTripData(Trip trip) {
        tvStart.setText("From: "      + trip.getTerminal1());
        tvEnd.setText("To: "          + trip.getTerminal2());
        tvPlate.setText("Plate: "     + trip.getPlateNumber());
        tvTime.setText("Departure: "  + trip.getDepartureTime());
        tvDriver.setText("Driver: "   + trip.getDriverName());
        tvConductor.setText("Conductor: " + trip.getConductorName());
    }

    /**
     * FIX 3: Centers the map on the trip's CURRENT T1_Coords from the trips table.
     * The old fetchRouteCoordinates() read from routes/{routeCode}/T1_Coords which
     * never changes — so the map always showed the original terminal even after a swap.
     */
    private void centerMapOnTrip(Trip trip) {
        String t1Coords = trip.getT1Coords();
        if (t1Coords == null || t1Coords.isEmpty() || t1Coords.equals("0,0")) return;
        try {
            String[] parts = t1Coords.split(",");
            GeoPoint start = new GeoPoint(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()));
            map.getController().setCenter(start);
            map.getOverlays().removeIf(o -> o instanceof Marker);
            Marker m = new Marker(map);
            m.setPosition(start);
            m.setTitle(trip.getTerminal1());
            map.getOverlays().add(m);
            map.invalidate();
        } catch (Exception e) {
            // bad coords — leave map as-is
        }
    }

    private void checkLocationSettingsAndStart() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build())
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
        if (currentTrip == null) return;

        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("TRIP_ID", currentTrip.getTripId());
        serviceIntent.putExtra("FRANCHISE", userFranchise);
        ContextCompat.startForegroundService(this, serviceIntent);

        Intent intent = new Intent(this, StartEndTrip.class);
        intent.putExtra("COMPANY_NAME", userFranchise);
        intent.putExtra("EMPLOYEE_ID", employeeNumericId);
        intent.putExtra("TRIP_ID", currentTrip.getTripId());
        startActivity(intent);
    }

    // FIX 4: remove the persistent listener when the activity is destroyed
    // to prevent memory leaks and ghost callbacks after the user logs out
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (assignedTripRef != null && assignedTripListener != null) {
            assignedTripRef.removeEventListener(assignedTripListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}