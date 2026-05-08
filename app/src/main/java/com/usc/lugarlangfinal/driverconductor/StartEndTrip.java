package com.usc.lugarlangfinal.driverconductor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.*;
import com.usc.lugarlangfinal.DriverOrConductoerDashboard;
import com.usc.lugarlangfinal.R;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartEndTrip extends AppCompatActivity {

    private MapView map;
    private TextView txtT1, txtT2;
    private Button btnEndTrip;
    private String companyName, tripId, employeeId;
    private MyLocationNewOverlay mLocationOverlay;
    private Polyline currentRoadOverlay;

    // FIX 1: track the listener so we can remove it before writing
    private ValueEventListener tripListener;
    private DatabaseReference tripRef;

    private final ArrayList<GeoPoint> allPoints = new ArrayList<>();
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_end_trip);

        map     = findViewById(R.id.mapStartEnd);
        txtT1   = findViewById(R.id.txtterminal1);
        txtT2   = findViewById(R.id.txtterminal2);
        btnEndTrip = findViewById(R.id.btnEndTrip);

        companyName = getIntent().getStringExtra("COMPANY_NAME");
        tripId      = getIntent().getStringExtra("TRIP_ID");
        employeeId = getIntent().getStringExtra("EMPLOYEE_ID");

        setupMap();

        if (companyName != null && tripId != null) {
            fetchTripData();
        } else {
            Toast.makeText(this, "Error: Missing Trip Data", Toast.LENGTH_SHORT).show();
        }

        btnEndTrip.setOnClickListener(v -> saveAndPrepareNextTrip());
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(16.0);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void fetchTripData() {
        tripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips").child(companyName).child(tripId);

        // FIX 2: use addListenerForSingleValueEvent instead of addValueEventListener.
        // The persistent listener was firing again every time saveAndPrepareNextTrip
        // wrote to tripRef, causing a race condition / crash on the second end-trip.
        tripListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                allPoints.clear();
                map.getOverlays().removeIf(o -> o instanceof Marker || o instanceof Polyline);

                String t1N = snapshot.child("Terminal1").getValue(String.class);
                String t2N = snapshot.child("Terminal2").getValue(String.class);
                String t1C = snapshot.child("T1_Coords").getValue(String.class);
                String t2C = snapshot.child("T2_Coords").getValue(String.class);
                String stopsStr     = snapshot.child("Stops").getValue(String.class);
                String stopCoordsStr= snapshot.child("Stop_Coords").getValue(String.class);

                txtT1.setText(t1N != null ? t1N : "Origin");
                txtT2.setText(t2N != null ? t2N : "Destination");

                // FIX 3: Build allPoints from Stop_Coords ONLY (it already contains T1 and T2).
                // Previously the code added T1 + Stop_Coords + T2, duplicating the terminal
                // points and sending malformed waypoints to OSRM on every trip swap.
                if (stopCoordsStr != null && !stopCoordsStr.isEmpty()) {
                    String[] coords = stopCoordsStr.split("\\|");
                    String[] names  = (stopsStr != null) ? stopsStr.split(",") : new String[0];

                    for (int i = 0; i < coords.length; i++) {
                        String coordStr = coords[i].trim();
                        if (!coordStr.contains(",")) continue;
                        GeoPoint p = parseGeoPoint(coordStr);
                        allPoints.add(p);

                        // First entry = T1, last entry = T2, middle = intermediate stops
                        if (i == 0) {
                            addMarker(p, "Start: " + (t1N != null ? t1N : "Origin"),
                                    R.drawable.trip_origin_24px);
                            map.getController().animateTo(p);
                        } else if (i == coords.length - 1) {
                            addMarker(p, "End: " + (t2N != null ? t2N : "Destination"),
                                    R.drawable.location_on_24px);
                        } else {
                            // Middle stop — use the name from Stops field if available
                            // Stops has 13 names; Stop_Coords has 15 (T1 + 13 + T2)
                            // so middle stop at coord index i maps to Stops name at index (i-1)
                            int nameIdx = i - 1;
                            String label = (nameIdx >= 0 && nameIdx < names.length)
                                    ? names[nameIdx].trim() : "Stop " + i;
                            addMarker(p, label, R.drawable.vd_vector);
                        }
                    }
                } else {
                    // Fallback: no Stop_Coords, use T1/T2 directly
                    if (t1C != null && t1C.contains(",")) {
                        GeoPoint p = parseGeoPoint(t1C);
                        allPoints.add(p);
                        addMarker(p, "Start: " + t1N, R.drawable.trip_origin_24px);
                        map.getController().animateTo(p);
                    }
                    if (t2C != null && t2C.contains(",")) {
                        GeoPoint p = parseGeoPoint(t2C);
                        allPoints.add(p);
                        addMarker(p, "End: " + t2N, R.drawable.location_on_24px);
                    }
                }

                if (allPoints.size() >= 2) {
                    drawRoute(new ArrayList<>(allPoints));
                }
                map.invalidate();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StartEndTrip", "fetchTripData cancelled: " + error.getMessage());
            }
        };

        tripRef.addListenerForSingleValueEvent(tripListener);
    }

    private void drawRoute(ArrayList<GeoPoint> waypoints) {
        executor.execute(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
            roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR);
            Road road = roadManager.getRoad(waypoints);
            mainHandler.post(() -> {
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = RoadManager.buildRoadOverlay(road);
                    currentRoadOverlay.getOutlinePaint().setColor(Color.parseColor("#2979FF"));
                    currentRoadOverlay.getOutlinePaint().setStrokeWidth(14.0f);
                    map.getOverlays().add(0, currentRoadOverlay);
                    map.invalidate();
                } else {
                    Log.e("RoadManager", "Road fetch failed: " + (road != null ? road.mStatus : "null"));
                }
            });
        });
    }

    private void addMarker(GeoPoint p, String title, int iconRes) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(title);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            m.setIcon(ContextCompat.getDrawable(this, iconRes));
        } catch (Exception e) {
            Log.e("Marker", "Icon not found: " + iconRes);
        }
        map.getOverlays().add(m);
    }

    private GeoPoint parseGeoPoint(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) { return new GeoPoint(0.0, 0.0); }
    }

    private void saveAndPrepareNextTrip() {
        btnEndTrip.setEnabled(false);

        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();

        // 1. Generate a UNIQUE key for this specific log entry
        // This allows one employee to have multiple trip logs under their ID.
        // Structure: trip_log -> franchise -> employeeid -> [unique_log_id]
        String logEntryId = db.child("trip_log").child(companyName).child(employeeId).push().getKey();

        if (logEntryId == null) {
            btnEndTrip.setEnabled(true);
            return;
        }

        DatabaseReference logRef = db.child("trip_log")
                .child(companyName)
                .child(employeeId)
                .child(logEntryId); // Use the unique key instead of just tripId

        db.child("trips").child(companyName).child(tripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            btnEndTrip.setEnabled(true);
                            return;
                        }

                        // 2. Archive the current trip data
                        Map<String, Object> currentTripData = (Map<String, Object>) snapshot.getValue();
                        if (currentTripData != null) {
                            currentTripData.put("tripEndTime", ServerValue.TIMESTAMP);
                            // Optional: Store the original tripId inside the log object for reference
                            currentTripData.put("originalTripId", tripId);
                            logRef.setValue(currentTripData);
                        }

                        // 3. Prepare the return leg (Swap Logic)
                        Map<String, Object> nextLeg = new HashMap<>();
                        nextLeg.put("Terminal1",   snapshot.child("Terminal2").getValue());
                        nextLeg.put("Terminal2",   snapshot.child("Terminal1").getValue());
                        nextLeg.put("T1_Coords",   snapshot.child("T2_Coords").getValue());
                        nextLeg.put("T2_Coords",   snapshot.child("T1_Coords").getValue());
                        nextLeg.put("Stops",       reverseDelimited(
                                snapshot.child("Stops").getValue(String.class), ","));
                        nextLeg.put("Stop_Coords", reverseDelimited(
                                snapshot.child("Stop_Coords").getValue(String.class), "|"));
                        nextLeg.put("status", "Scheduled");

                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.add(java.util.Calendar.HOUR_OF_DAY, 1);
                        String nextDeparture = new java.text.SimpleDateFormat(
                                "hh:mm a", java.util.Locale.getDefault()).format(cal.getTime());
                        nextLeg.put("departureTime", nextDeparture);

                        // 4. Update the LIVE trip reference for the next journey
                        db.child("trips").child(companyName).child(tripId)
                                .updateChildren(nextLeg)
                                .addOnSuccessListener(aVoid -> {
                                    stopService(new Intent(StartEndTrip.this, LocationService.class));
                                    Toast.makeText(StartEndTrip.this, "Trip logged!", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(StartEndTrip.this, DriverOrConductoerDashboard.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(StartEndTrip.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    btnEndTrip.setEnabled(true);
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnEndTrip.setEnabled(true);
                    }
                });
    }

    /**
     * Splits by delimiter, reverses the parts, rejoins.
     * Handles trailing delimiters and whitespace safely.
     */
    private String reverseDelimited(String s, String delimiter) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split(java.util.regex.Pattern.quote(delimiter));
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.trim().isEmpty()) list.add(p.trim());
        }
        Collections.reverse(list);
        return android.text.TextUtils.join(delimiter, list);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }
}