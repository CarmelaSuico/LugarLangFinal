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
    private String companyName, tripId;
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
        tripId      = getIntent().getStringExtra("TRIP_ID");

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
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("trips").child(companyName).child(tripId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                allPoints.clear();
                List<org.osmdroid.views.overlay.Overlay> overlays = map.getOverlays();
                overlays.removeIf(o -> o instanceof Marker || o instanceof Polyline);

                String t1N = snapshot.child("Terminal1").getValue(String.class);
                String t2N = snapshot.child("Terminal2").getValue(String.class);
                String t1C = snapshot.child("T1_Coords").getValue(String.class);
                String t2C = snapshot.child("T2_Coords").getValue(String.class);
                String stopsStr = snapshot.child("Stops").getValue(String.class);
                String stopCoordsStr = snapshot.child("Stop_Coords").getValue(String.class);

                txtT1.setText(t1N != null ? t1N : "Origin");
                txtT2.setText(t2N != null ? t2N : "Destination");

                if (t1C != null && t1C.contains(",")) {
                    GeoPoint p = parseGeoPoint(t1C);
                    allPoints.add(p);
                    addMarker(p, "Start: " + t1N, R.drawable.trip_origin_24px);
                    map.getController().animateTo(p);
                }

                if (stopCoordsStr != null && !stopCoordsStr.isEmpty()) {
                    String[] coords = stopCoordsStr.split("\\|");
                    String[] names = (stopsStr != null) ? stopsStr.split(",") : new String[0];

                    for (int i = 0; i < coords.length; i++) {
                        String coordStr = coords[i].trim();
                        if (!coordStr.contains(",")) continue;

                        GeoPoint p = parseGeoPoint(coordStr);
                        allPoints.add(p);
                        String label = (i < names.length) ? names[i].trim() : "Stop " + (i + 1);
                        addMarker(p, label, R.drawable.vd_vector);
                    }
                }

                if (t2C != null && t2C.contains(",")) {
                    GeoPoint p = parseGeoPoint(t2C);
                    allPoints.add(p);
                    addMarker(p, "End: " + t2N, R.drawable.location_on_24px);
                }

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
                    currentRoadOverlay = RoadManager.buildRoadOverlay(road);
                    currentRoadOverlay.getOutlinePaint().setColor(Color.parseColor("#2979FF"));
                    currentRoadOverlay.getOutlinePaint().setStrokeWidth(14.0f);
                    map.getOverlays().add(0, currentRoadOverlay);
                    map.invalidate();
                } else {
                    Log.e("RoadManager", "Failed to fetch road: " + (road != null ? road.mStatus : "null"));
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
            m.setIcon(ContextCompat.getDrawable(this, iconRes));
        } catch (Exception e) {
            Log.e("Marker", "Icon resource not found: " + iconRes);
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
        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();
        DatabaseReference tripRef = db.child("trips").child(companyName).child(tripId);
        DatabaseReference logRef = db.child("trip_log").child(companyName).push();

        tripRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Map<String, Object> currentTripData = (Map<String, Object>) snapshot.getValue();
                if (currentTripData != null) {
                    currentTripData.put("tripEndTime", ServerValue.TIMESTAMP);
                    logRef.setValue(currentTripData);
                }

                Map<String, Object> nextLeg = new HashMap<>();
                nextLeg.put("Terminal1", snapshot.child("Terminal2").getValue());
                nextLeg.put("Terminal2", snapshot.child("Terminal1").getValue());
                nextLeg.put("T1_Coords", snapshot.child("T2_Coords").getValue());
                nextLeg.put("T2_Coords", snapshot.child("T1_Coords").getValue());

                String s = snapshot.child("Stops").getValue(String.class);
                String sc = snapshot.child("Stop_Coords").getValue(String.class);

                nextLeg.put("Stops", reverseString(s, ","));
                nextLeg.put("Stop_Coords", reverseString(sc, "|"));
                nextLeg.put("status", "Scheduled");

                tripRef.updateChildren(nextLeg)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(StartEndTrip.this, "Trip logged and swapped for return leg!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(StartEndTrip.this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String reverseString(String s, String d) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split(java.util.regex.Pattern.quote(d));
        List<String> list = new ArrayList<>();
        for(String p : parts) if(!p.trim().isEmpty()) list.add(p.trim());
        Collections.reverse(list);
        return android.text.TextUtils.join(d, list);
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