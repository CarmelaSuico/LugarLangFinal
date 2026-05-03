package com.usc.lugarlangfinal.driverconductor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import java.text.SimpleDateFormat;
import java.util.*;

public class StartEndTrip extends AppCompatActivity {

    private MapView map;
    private TextView txtT1, txtT2;
    private Button btnEndTrip;
    private String routeCode, companyName, employeeId, tripId;
    private MyLocationNewOverlay mLocationOverlay;
    private Polyline currentRoadOverlay;
    private ArrayList<GeoPoint> allPoints = new ArrayList<>();
    private ArrayList<String> allNames = new ArrayList<>();
    private int currentStopIndex = 0;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_end_trip);

        initViews();
        setupMap();

        routeCode = getIntent().getStringExtra("ROUTE_CODE");
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        employeeId = getIntent().getStringExtra("EMPLOYEE_ID");
        tripId = getIntent().getStringExtra("TRIP_ID");

        if (tripId != null) fetchTripData();
        btnEndTrip.setOnClickListener(v -> saveAndPrepareNextTrip());
    }

    private void initViews() {
        map = findViewById(R.id.mapStartEnd);
        txtT1 = findViewById(R.id.txtterminal1);
        txtT2 = findViewById(R.id.txtterminal2);
        btnEndTrip = findViewById(R.id.btnEndTrip);
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void fetchTripData() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("trips")
                .child(companyName).child(tripId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    allPoints.clear(); allNames.clear(); map.getOverlays().clear();
                    if (mLocationOverlay != null) map.getOverlays().add(mLocationOverlay);

                    String t1Coords = snapshot.child("t1_Coords").getValue(String.class);
                    String t1Name = snapshot.child("terminal1").getValue(String.class);
                    if (t1Coords != null) {
                        GeoPoint p = parseGeoPoint(t1Coords); allPoints.add(p); allNames.add(t1Name);
                        addMarker(p, "START: " + t1Name, "#00BF63");
                        map.getController().setCenter(p);
                    }

                    String stopCoords = snapshot.child("stops_Coords").getValue(String.class);
                    if (stopCoords != null && !stopCoords.isEmpty()) {
                        String[] coords = stopCoords.split("\\|");
                        String stopsStr = snapshot.child("stops").getValue(String.class);
                        String[] names = (stopsStr != null) ? stopsStr.split(", ") : new String[0];
                        for (int i = 0; i < coords.length; i++) {
                            GeoPoint p = parseGeoPoint(coords[i]); allPoints.add(p);
                            String name = (i < names.length) ? names[i] : "Stop";
                            allNames.add(name); addMarker(p, name, "#703042");
                        }
                    }

                    String t2Coords = snapshot.child("t2_Coords").getValue(String.class);
                    String t2Name = snapshot.child("terminal2").getValue(String.class);
                    if (t2Coords != null) {
                        GeoPoint p = parseGeoPoint(t2Coords); allPoints.add(p); allNames.add(t2Name);
                        addMarker(p, "END: " + t2Name, "#0078FF");
                    }
                    if (allPoints.size() >= 2) drawRoute(allPoints);
                    updateHeaderUI();
                }
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
                if (snapshot.exists()) {
                    // 1. Capture current data for the history log
                    String t1 = snapshot.child("terminal1").getValue(String.class);
                    String t2 = snapshot.child("terminal2").getValue(String.class);
                    String driver = snapshot.child("driverName").getValue(String.class);
                    String depTime = snapshot.child("departureTime").getValue(String.class);

                    // 2. Capture coordinates for reversal
                    String t1C = snapshot.child("t1_Coords").getValue(String.class);
                    String t2C = snapshot.child("t2_Coords").getValue(String.class);
                    String s = snapshot.child("stops").getValue(String.class);
                    String sc = snapshot.child("stops_Coords").getValue(String.class);

                    // 3. Prepare Log Entry (History)
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("T1", t1);
                    historyEntry.put("T2", t2);
                    historyEntry.put("drivername", driver);
                    historyEntry.put("departuretime", depTime);
                    historyEntry.put("timestamp", ServerValue.TIMESTAMP); // Adds time of completion

                    // 4. Prepare Reversed Route for next leg[cite: 7]
                    Map<String, Object> nextLeg = new HashMap<>();
                    nextLeg.put("terminal1", t2);
                    nextLeg.put("terminal2", t1);
                    nextLeg.put("t1_Coords", t2C);
                    nextLeg.put("t2_Coords", t1C);
                    nextLeg.put("stops", reverseString(s, ", "));
                    nextLeg.put("stops_Coords", reverseString(sc, "\\|"));
                    nextLeg.put("status", "Scheduled");

                    // 5. Generate a unique key for THIS specific trip in history
                    String historyKey = rootRef.child("trip_logs").child(companyName)
                            .child(employeeId).push().getKey();

                    // 6. Atomic Update: Add to history AND prepare return leg[cite: 7]
                    Map<String, Object> finalUpdates = new HashMap<>();
                    finalUpdates.put("trip_logs/" + companyName + "/" + employeeId + "/" + historyKey, historyEntry);
                    finalUpdates.put("trips/" + companyName + "/" + tripId, nextLeg);

                    rootRef.updateChildren(finalUpdates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(StartEndTrip.this, "Trip History Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String reverseString(String s, String d) {
        if (s == null || s.isEmpty()) return "";
        String[] p = s.split(java.util.regex.Pattern.quote(d.trim()));
        List<String> l = new ArrayList<>(Arrays.asList(p));
        Collections.reverse(l);
        return android.text.TextUtils.join(d.trim(), l);
    }

    private void addMarker(GeoPoint p, String t, String c) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(t);
        map.getOverlays().add(m);
    }

    private void updateHeaderUI() {
        if (allNames.size() < 2) return;
        txtT1.setText(allNames.get(currentStopIndex));
        txtT2.setText(allNames.get(allNames.size() - 1));
    }

    private GeoPoint parseGeoPoint(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) {
            return new GeoPoint(0.0, 0.0);
        }
    }

    private void drawRoute(ArrayList<GeoPoint> w) {
        new AsyncTask<Void, Void, Road>() {
            @Override
            protected Road doInBackground(Void... v) {
                OSRMRoadManager m = new OSRMRoadManager(StartEndTrip.this, getPackageName());
                m.setMean(OSRMRoadManager.MEAN_BY_CAR);
                return m.getRoad(w);
            }
            @Override
            protected void onPostExecute(Road r) {
                if (r != null && r.mStatus == Road.STATUS_OK) {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = RoadManager.buildRoadOverlay(r);
                    map.getOverlays().add(0, currentRoadOverlay);
                    map.invalidate();
                }
            }
        }.execute();
    }
}