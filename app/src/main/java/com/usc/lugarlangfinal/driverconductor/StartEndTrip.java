package com.usc.lugarlangfinal.driverconductor;

import android.Manifest;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StartEndTrip extends AppCompatActivity {

    private MapView map;
    private TextView txtT1, txtT2;
    private Button btnEndTrip;
    private String routeCode, companyName, employeeId;

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
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_start_end_trip);

        checkGpsPermissions();
        initViews();

        routeCode = getIntent().getStringExtra("ROUTE_CODE");
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        employeeId = getIntent().getStringExtra("EMPLOYEE_ID");

        setupMap();

        if (routeCode != null) {
            fetchFullRouteData();
        } else {
            Toast.makeText(this, "Error: Route Data Missing", Toast.LENGTH_LONG).show();
        }

        btnEndTrip.setOnClickListener(v -> saveAndPrepareNextTrip());
    }

    private void checkGpsPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
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

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map) {
            @Override
            public void onLocationChanged(android.location.Location location, org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                super.onLocationChanged(location, source);
                if (location != null) {
                    GeoPoint myLoc = new GeoPoint(location.getLatitude(), location.getLongitude());
                    runOnUiThread(() -> checkProximity(myLoc));
                }
            }
        };

        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        mLocationOverlay.setDrawAccuracyEnabled(false);
        map.getOverlays().add(mLocationOverlay);
    }

    private void fetchFullRouteData() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    allPoints.clear();
                    allNames.clear();
                    map.getOverlays().clear();

                    if (mLocationOverlay != null) map.getOverlays().add(mLocationOverlay);

                    String t1Coords = snapshot.child("T1_Coords").getValue(String.class);
                    String t1Name = snapshot.child("Terminal1").getValue(String.class);
                    if (isValidCoord(t1Coords)) {
                        GeoPoint p1 = parseGeoPoint(t1Coords);
                        allPoints.add(p1);
                        allNames.add(t1Name);
                        addCustomMarker(p1, "START: " + t1Name, R.drawable.location_on_24px, "#00BF63");
                        map.getController().setCenter(p1);
                    }

                    String stopCoords = snapshot.child("Stop_Coords").getValue(String.class);
                    if (stopCoords != null && !stopCoords.trim().isEmpty()) {
                        String[] coordArray = stopCoords.split("\\|");
                        String stopNamesStr = snapshot.child("Stops").getValue(String.class);
                        String[] nameArray = (stopNamesStr != null) ? stopNamesStr.split(", ") : null;

                        for (int i = 0; i < coordArray.length; i++) {
                            GeoPoint sp = parseGeoPoint(coordArray[i].trim());
                            if (sp.getLatitude() != 0) {
                                allPoints.add(sp);
                                String label = (nameArray != null && i < nameArray.length) ? nameArray[i] : "Stop";
                                allNames.add(label);
                                addCustomMarker(sp, label, R.drawable.vd_vector, "#703042");
                            }
                        }
                    }

                    String t2Coords = snapshot.child("T2_Coords").getValue(String.class);
                    String t2Name = snapshot.child("Terminal2").getValue(String.class);
                    if (isValidCoord(t2Coords)) {
                        GeoPoint p2 = parseGeoPoint(t2Coords);
                        allPoints.add(p2);
                        allNames.add(t2Name);
                        addCustomMarker(p2, "END: " + t2Name, R.drawable.location_on_24px, "#0078FF");
                    }

                    if (allPoints.size() >= 2) {
                        drawRoute(allPoints);
                        updateHeaderUI();
                    }
                    map.invalidate();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCustomMarker(GeoPoint point, String title, int iconRes, String hexColor) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);

        if (iconRes != 0) {
            try {
                Drawable icon = ContextCompat.getDrawable(this, iconRes);
                if (icon != null) {
                    Drawable tintedIcon = icon.mutate();
                    tintedIcon.setTint(android.graphics.Color.parseColor(hexColor));
                    marker.setIcon(tintedIcon);
                }
            } catch (Exception e) {
                Log.e("MARKER_ERROR", "Icon error");
            }
        }
        map.getOverlays().add(marker);
    }

    private void saveAndPrepareNextTrip() {
        if (companyName == null || employeeId == null) {
            Toast.makeText(this, "System Error: Missing User Context", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference routeRef = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        DatabaseReference logsRef = FirebaseDatabase.getInstance(DB_URL).getReference("trip_logs")
                .child(companyName)
                .child(employeeId.replace(".", ","));

        routeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String oldT1 = snapshot.child("Terminal1").getValue(String.class);
                    String oldT2 = snapshot.child("Terminal2").getValue(String.class);
                    String oldT1Coords = snapshot.child("T1_Coords").getValue(String.class);
                    String oldT2Coords = snapshot.child("T2_Coords").getValue(String.class);
                    String stopCoords = snapshot.child("Stop_Coords").getValue(String.class);
                    String stops = snapshot.child("Stops").getValue(String.class);

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.HOUR, 1);
                    String nextDepartureTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime());

                    Map<String, Object> logEntry = new HashMap<>();
                    logEntry.put("terminal1", oldT1);
                    logEntry.put("terminal2", oldT2);
                    logEntry.put("route_code", routeCode);
                    logEntry.put("departuretime", snapshot.child("Departure_Time").getValue(String.class));
                    logEntry.put("status", "Completed");

                    Map<String, Object> nextTripUpdate = new HashMap<>();
                    nextTripUpdate.put("Terminal1", oldT2);
                    nextTripUpdate.put("Terminal2", oldT1);
                    nextTripUpdate.put("T1_Coords", oldT2Coords);
                    nextTripUpdate.put("T2_Coords", oldT1Coords);
                    nextTripUpdate.put("Stop_Coords", reverseString(stopCoords, "\\|"));
                    nextTripUpdate.put("Stops", reverseString(stops, ", "));
                    nextTripUpdate.put("Departure_Time", nextDepartureTime);

                    String tripId = logsRef.push().getKey();
                    if (tripId != null) {
                        logsRef.child(tripId).setValue(logEntry).addOnSuccessListener(aVoid -> {
                            routeRef.updateChildren(nextTripUpdate).addOnSuccessListener(unused -> {
                                Toast.makeText(StartEndTrip.this, "Trip Logged! Next trip: " + nextDepartureTime, Toast.LENGTH_LONG).show();
                                finish();
                            });
                        });
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String reverseString(String original, String delimiter) {
        if (original == null || !original.contains(delimiter.replace("\\", ""))) return original;
        String[] parts = original.split(delimiter);
        StringBuilder builder = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            builder.append(parts[i].trim()).append(i == 0 ? "" : delimiter.replace("\\", ""));
        }
        return builder.toString();
    }

    private void checkProximity(GeoPoint currentPos) {
        if (allPoints.isEmpty() || currentStopIndex >= allPoints.size() - 1) return;
        GeoPoint nextStop = allPoints.get(currentStopIndex + 1);
        double distance = currentPos.distanceToAsDouble(nextStop);
        if (distance < 50) {
            currentStopIndex++;
            updateHeaderUI();
            Toast.makeText(this, "Arrived at: " + allNames.get(currentStopIndex), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHeaderUI() {
        if (allNames.size() < 2 || currentStopIndex >= allNames.size() - 1) return;
        txtT1.setText(allNames.get(currentStopIndex));
        txtT2.setText(allNames.get(currentStopIndex + 1));
    }

    private boolean isValidCoord(String s) { return s != null && !s.isEmpty(); }

    private GeoPoint parseGeoPoint(String coordString) {
        try {
            String[] parts = coordString.split(",");
            return new GeoPoint(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (Exception e) { return new GeoPoint(0.0, 0.0); }
    }

    private void drawRoute(ArrayList<GeoPoint> waypoints) {
        new AsyncTask<Void, Void, Road>() {
            @Override
            protected Road doInBackground(Void... voids) {
                OSRMRoadManager roadManager = new OSRMRoadManager(StartEndTrip.this, getPackageName());
                roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR);
                try { return roadManager.getRoad(waypoints); } catch (Exception e) { return null; }
            }
            @Override
            protected void onPostExecute(Road road) {
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = RoadManager.buildRoadOverlay(road);
                    currentRoadOverlay.getOutlinePaint().setColor(0xFF00FF00);
                    currentRoadOverlay.getOutlinePaint().setStrokeWidth(12.0f);
                    map.getOverlays().add(0, currentRoadOverlay);
                    
                    // Automatically zoom map to show the entire route
                    map.zoomToBoundingBox(road.mBoundingBox, true, 100);
                    map.invalidate();
                }
            }
        }.execute();
    }

    @Override public void onResume() { super.onResume(); map.onResume(); if (mLocationOverlay != null) { mLocationOverlay.enableMyLocation(); mLocationOverlay.enableFollowLocation(); } }
    @Override public void onPause() { super.onPause(); map.onPause(); if (mLocationOverlay != null) { mLocationOverlay.disableMyLocation(); mLocationOverlay.disableFollowLocation(); } }
}