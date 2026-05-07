package com.usc.lugarlangfinal.driverconductor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
    private String companyName, employeeId, tripId;
    private MyLocationNewOverlay mLocationOverlay;
    private Polyline currentRoadOverlay;

    private final ArrayList<GeoPoint> allPoints = new ArrayList<>();
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_end_trip);

        map = findViewById(R.id.mapStartEnd);
        txtT1 = findViewById(R.id.txtterminal1);
        txtT2 = findViewById(R.id.txtterminal2);
        btnEndTrip = findViewById(R.id.btnEndTrip);

        companyName = getIntent().getStringExtra("COMPANY_NAME");
        employeeId  = getIntent().getStringExtra("EMPLOYEE_ID");
        tripId      = getIntent().getStringExtra("TRIP_ID");

        setupMap();
        if (tripId != null) fetchTripData();

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
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("trips").child(companyName).child(tripId);
        ref.addValueEventListener(new ValueEventListener() { // Changed to addValueEventListener for live header updates
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // CLEAN SLATE FIRST: Clear markers but keep blue dot
                allPoints.clear();
                map.getOverlays().removeIf(o -> o instanceof Marker || (o instanceof Polyline && o != mLocationOverlay));

                // FIXED: Use Capitalized Keys for Terminals to match your Firebase JSON
                String t1N = snapshot.child("Terminal1").getValue(String.class);
                String t2N = snapshot.child("Terminal2").getValue(String.class);
                String t1C = snapshot.child("T1_Coords").getValue(String.class);
                String t2C = snapshot.child("T2_Coords").getValue(String.class);
                String stopsStr = snapshot.child("Stops").getValue(String.class);
                String stopCoordsStr = snapshot.child("Stop_Coords").getValue(String.class);

                // Update the Header UI
                txtT1.setText(t1N != null ? t1N : "Origin");
                txtT2.setText(t2N != null ? t2N : "Destination");

                // 1. ORIGIN MARKER
                if (t1C != null) {
                    GeoPoint p = parseGeoPoint(t1C);
                    allPoints.add(p);
                    addMarker(p, "Start: " + t1N, R.drawable.trip_origin_24px);
                    map.getController().animateTo(p);
                }

                // 2. INTERMEDIATE STOPS MARKERS
                if (stopCoordsStr != null && !stopCoordsStr.isEmpty()) {
                    String[] coords = stopCoordsStr.split("\\|");
                    String[] names = (stopsStr != null) ? stopsStr.split(",\\s*") : new String[0];

                    for (int i = 0; i < coords.length; i++) {
                        if (coords[i].trim().isEmpty()) continue;
                        GeoPoint p = parseGeoPoint(coords[i]);
                        allPoints.add(p);
                        String label = (i < names.length) ? names[i] : "Stop " + (i + 1);
                        addMarker(p, label, R.drawable.vd_vector);
                    }
                }

                // 3. DESTINATION MARKER
                if (t2C != null) {
                    GeoPoint p = parseGeoPoint(t2C);
                    allPoints.add(p);
                    addMarker(p, "End: " + t2N, R.drawable.location_on_24px);
                }

                // Draw the blue line
                if (allPoints.size() >= 2) {
                    drawRoute(new ArrayList<>(allPoints));
                }
                map.invalidate();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void drawRoute(ArrayList<GeoPoint> w) {
        executor.execute(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
            roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR);
            Road road = roadManager.getRoad(w);
            mainHandler.post(() -> {
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = RoadManager.buildRoadOverlay(road);
                    currentRoadOverlay.getOutlinePaint().setColor(Color.parseColor("#2979FF"));
                    currentRoadOverlay.getOutlinePaint().setStrokeWidth(14.0f);
                    map.getOverlays().add(0, currentRoadOverlay);
                    map.invalidate();
                }
            });
        });
    }

    private void addMarker(GeoPoint p, String t, int iconRes) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(t);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            Drawable d = ContextCompat.getDrawable(this, iconRes);
            if (d != null) m.setIcon(d);
        } catch (Exception e) {
            Log.e("MarkerIcon", "Icon not found");
        }
        map.getOverlays().add(m);
    }

    private GeoPoint parseGeoPoint(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) { return new GeoPoint(0, 0); }
    }

    private void saveAndPrepareNextTrip() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();
        DatabaseReference tripRef = rootRef.child("trips").child(companyName).child(tripId);

        tripRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Reversing Capitalized keys for the next leg
                String t1N = snapshot.child("Terminal1").getValue(String.class);
                String t2N = snapshot.child("Terminal2").getValue(String.class);
                String t1C = snapshot.child("T1_Coords").getValue(String.class);
                String t2C = snapshot.child("T2_Coords").getValue(String.class);
                String s   = snapshot.child("Stops").getValue(String.class);
                String sc  = snapshot.child("Stop_Coords").getValue(String.class);

                Map<String, Object> nextLeg = new HashMap<>();
                nextLeg.put("Terminal1", t2N); // Swap
                nextLeg.put("Terminal2", t1N); // Swap
                nextLeg.put("T1_Coords", t2C);
                nextLeg.put("T2_Coords", t1C);
                nextLeg.put("Stops", reverseString(s, ", "));
                nextLeg.put("Stop_Coords", reverseString(sc, "|"));
                nextLeg.put("status", "Scheduled");
                nextLeg.put("currentLocation", "");

                // Keep everything else as is
                nextLeg.put("PlateNumber", snapshot.child("PlateNumber").getValue());
                nextLeg.put("vehicleCode", snapshot.child("vehicleCode").getValue());
                nextLeg.put("driverName", snapshot.child("driverName").getValue());
                nextLeg.put("conductorName", snapshot.child("conductorName").getValue());
                nextLeg.put("routeCode", snapshot.child("routeCode").getValue());
                nextLeg.put("franchise", snapshot.child("franchise").getValue());
                nextLeg.put("departureTime", snapshot.child("departureTime").getValue());
                nextLeg.put("AssignedTransport", snapshot.child("AssignedTransport").getValue());
                nextLeg.put("tripId", tripId);

                rootRef.child("trips").child(companyName).child(tripId).updateChildren(nextLeg)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(StartEndTrip.this, "Trip Swapped for Return!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String reverseString(String s, String d) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split(java.util.regex.Pattern.quote(d.trim()));
        List<String> list = new ArrayList<>(Arrays.asList(parts));
        Collections.reverse(list);
        return android.text.TextUtils.join(d.trim(), list);
    }
}