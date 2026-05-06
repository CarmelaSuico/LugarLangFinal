package com.usc.lugarlangfinal.driverconductor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
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
        // FIX: Point to "trips" node so we get the dynamic Stop_Coords[cite: 1, 2]
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("trips").child(companyName).child(tripId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Clear map of old markers before redrawing
                allPoints.clear();
                map.getOverlays().removeIf(o -> o instanceof Marker);

                String t1C = snapshot.child("t1_Coords").getValue(String.class);
                String t2C = snapshot.child("t2_Coords").getValue(String.class);
                String stops = snapshot.child("Stops").getValue(String.class);
                String stopCoords = snapshot.child("Stop_Coords").getValue(String.class);

                // Add Terminal 1
                if (t1C != null) {
                    GeoPoint p = parseGeoPoint(t1C);
                    allPoints.add(p);
                    addMarker(p, "Start", R.drawable.trip_origin_24px);
                }

                // Add Intermediate Stops
                if (stops != null && stopCoords != null) {
                    String[] names = stops.split(",\\s*");
                    String[] coords = stopCoords.split("\\|"); // pipe split
                    for (int i = 0; i < coords.length; i++) {
                        GeoPoint p = parseGeoPoint(coords[i]);
                        if (p.getLatitude() != 0) {
                            allPoints.add(p);
                            String stopLabel = (i < names.length) ? names[i] : "Stop " + (i + 1);
                            addMarker(p, stopLabel, R.drawable.vd_vector);
                        }
                    }
                }

                // Add Terminal 2
                if (t2C != null) {
                    GeoPoint p = parseGeoPoint(t2C);
                    allPoints.add(p);
                    addMarker(p, "End", R.drawable.location_on_24px);
                }

                if (allPoints.size() >= 2) drawRoute(new ArrayList<>(allPoints));
                map.invalidate();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveAndPrepareNextTrip() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();
        DatabaseReference tripRef = rootRef.child("trips").child(companyName).child(tripId);

        tripRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // 1. Capture ALL current data so it isn't lost
                String t1N = snapshot.child("terminal1").getValue(String.class);
                String t2N = snapshot.child("terminal2").getValue(String.class);
                String t1C = snapshot.child("t1_Coords").getValue(String.class);
                String t2C = snapshot.child("t2_Coords").getValue(String.class);
                String s   = snapshot.child("Stops").getValue(String.class);
                String sc  = snapshot.child("Stop_Coords").getValue(String.class);

                // Capture identifying info to PREVENT DELETION
                Object plate = snapshot.child("PlateNumber").getValue();
                Object vCode = snapshot.child("vehicleCode").getValue();
                Object dName = snapshot.child("driverName").getValue();
                Object cName = snapshot.child("conductorName").getValue();
                Object rCode = snapshot.child("routeCode").getValue();
                Object fran  = snapshot.child("franchise").getValue();
                Object dTime = snapshot.child("departureTime").getValue();

                // 2. Prepare reversal data (Swapping Terminals and Coords)
                Map<String, Object> nextLeg = new HashMap<>();
                nextLeg.put("terminal1",    t2N); // T2 becomes the new Start
                nextLeg.put("terminal2",    t1N); // T1 becomes the new End
                nextLeg.put("t1_Coords",    t2C);
                nextLeg.put("t2_Coords",    t1C);
                nextLeg.put("Stops",        reverseString(s,  ", "));
                nextLeg.put("Stop_Coords",  reverseString(sc, "|"));
                nextLeg.put("status",       "Scheduled");
                nextLeg.put("currentLocation", ""); // Clear location for the new trip

                // 3. RE-ADD identifying info so it stays in the database
                nextLeg.put("PlateNumber",    plate);
                nextLeg.put("vehicleCode",    vCode);
                nextLeg.put("driverName",     dName);
                nextLeg.put("conductorName",  cName);
                nextLeg.put("routeCode",      rCode);
                nextLeg.put("franchise",      fran);
                nextLeg.put("departureTime",  dTime);
                nextLeg.put("tripId",         tripId);

                // 4. Prepare History Log entry
                String logKey = rootRef.child("trip_logs").child(companyName).child(employeeId).push().getKey();
                Map<String, Object> log = new HashMap<>();
                log.put("T1", t1N);
                log.put("T2", t2N);
                log.put("PlateNumber", plate);
                log.put("drivername", dName);
                log.put("timestamp", ServerValue.TIMESTAMP);

                // 5. Atomic Update
                Map<String, Object> updates = new HashMap<>();
                updates.put("trips/" + companyName + "/" + tripId, nextLeg);
                if (logKey != null) {
                    updates.put("trip_logs/" + companyName + "/" + employeeId + "/" + logKey, log);
                }

                rootRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    stopService(new Intent(StartEndTrip.this, LocationService.class));
                    Toast.makeText(StartEndTrip.this, "Trip reversed and logged!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addMarker(GeoPoint p, String t, int icon) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(t);
        Drawable d = ContextCompat.getDrawable(this, icon);
        if (d != null) m.setIcon(d);
        map.getOverlays().add(m);
    }

    private GeoPoint parseGeoPoint(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) { return new GeoPoint(0, 0); }
    }

    private String reverseString(String s, String d) {
        if (s == null || s.isEmpty()) return "";
        String[] parts = s.split(java.util.regex.Pattern.quote(d.trim()));
        List<String> list = new ArrayList<>(Arrays.asList(parts));
        Collections.reverse(list);
        return android.text.TextUtils.join(d.trim(), list);
    }

    private void drawRoute(ArrayList<GeoPoint> w) {
        executor.execute(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
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
}