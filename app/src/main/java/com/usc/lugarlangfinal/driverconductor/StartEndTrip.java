package com.usc.lugarlangfinal.driverconductor;

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

import java.util.ArrayList;

public class StartEndTrip extends AppCompatActivity {

    private MapView map;
    private TextView txtT1, txtT2;
    private Button btnEndTrip;
    private String routeCode;

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

        initViews();
        routeCode = getIntent().getStringExtra("ROUTE_CODE");

        setupMap();

        if (routeCode != null) {
            fetchFullRouteData();
        } else {
            Toast.makeText(this, "Error: Route Code Missing", Toast.LENGTH_LONG).show();
        }

        btnEndTrip.setOnClickListener(v -> {
            Toast.makeText(this, "Trip Ended", Toast.LENGTH_SHORT).show();
            finish();
        });
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

                    // 1. Terminal 1
                    String t1Coords = snapshot.child("T1_Coords").getValue(String.class);
                    if (t1Coords != null && !t1Coords.isEmpty()) {
                        GeoPoint p1 = parseGeoPoint(t1Coords);
                        if (p1.getLatitude() != 0) {
                            allPoints.add(p1);
                            allNames.add(snapshot.child("Terminal1").getValue(String.class));
                            addCustomMarker(p1, "START", 0);
                        }
                    }


                    // 2. Intermediate Stops
                    String stopCoords = snapshot.child("Stop_Coords").getValue(String.class);
                    if (stopCoords != null && !stopCoords.trim().isEmpty()) {
                        String[] coordArray = stopCoords.split("\\|");
                        String stopNames = snapshot.child("Stops").getValue(String.class);
                        String[] nameArray = (stopNames != null) ? stopNames.split(", ") : null;

                        for (int i = 0; i < coordArray.length; i++) {
                            GeoPoint sp = parseGeoPoint(coordArray[i].trim());
                            if (sp.getLatitude() != 0) {
                                allPoints.add(sp);
                                String label = (nameArray != null && i < nameArray.length) ? nameArray[i] : "Stop";
                                allNames.add(label);
                                addCustomMarker(sp, label, R.drawable.vd_vector);
                            }
                        }
                    }

                    // 3. Terminal 2
                    String t2Coords = snapshot.child("T2_Coords").getValue(String.class);
                    if (t2Coords != null && !t2Coords.isEmpty()) {
                        GeoPoint p2 = parseGeoPoint(t2Coords);
                        if (p2.getLatitude() != 0) {
                            allPoints.add(p2);
                            allNames.add(snapshot.child("Terminal2").getValue(String.class));
                            addCustomMarker(p2, "END", 0);
                        }
                    }

                    if (allPoints.size() >= 2) {
                        drawRoute(allPoints);
                        updateHeaderUI();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkProximity(GeoPoint currentPos) {
        if (currentPos == null || allPoints.isEmpty() || currentStopIndex >= allPoints.size() - 1) return;

        GeoPoint nextStop = allPoints.get(currentStopIndex + 1);
        double distance = currentPos.distanceToAsDouble(nextStop);

        if (distance < 50) {
            currentStopIndex++;
            updateHeaderUI();
        }
    }

    private void updateHeaderUI() {
        if (allNames.size() < 2 || currentStopIndex >= allNames.size() - 1) return;
        txtT1.setText(allNames.get(currentStopIndex));
        txtT2.setText(allNames.get(currentStopIndex + 1));
    }

    private GeoPoint parseGeoPoint(String coordString) {
        try {
            String[] parts = coordString.split(",");
            if (parts.length < 2) return new GeoPoint(0.0, 0.0);
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return new GeoPoint(lat, lon);
        } catch (Exception e) {
            return new GeoPoint(0.0, 0.0);
        }
    }

    private void drawRoute(ArrayList<GeoPoint> waypoints) {
        new AsyncTask<Void, Void, Road>() {
            @Override
            protected Road doInBackground(Void... voids) {
                OSRMRoadManager roadManager = new OSRMRoadManager(StartEndTrip.this, getPackageName());
                // Use the built-in OSRM profile (default is fastest)
                try {
                    return roadManager.getRoad(waypoints);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Road road) {
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    if (currentRoadOverlay != null) map.getOverlays().remove(currentRoadOverlay);
                    currentRoadOverlay = RoadManager.buildRoadOverlay(road);
                    currentRoadOverlay.getOutlinePaint().setColor(0xFF00FF00); // Green for route
                    currentRoadOverlay.getOutlinePaint().setStrokeWidth(12.0f);
                    map.getOverlays().add(0, currentRoadOverlay);
                    map.invalidate();
                } else {
                    String status = (road != null) ? String.valueOf(road.mStatus) : "Null";
                    Toast.makeText(StartEndTrip.this, "Route Error: " + status, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void addCustomMarker(GeoPoint point, String title, int iconRes) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setTitle(title);

        if (iconRes != 0) {
            try {
                Drawable icon = ContextCompat.getDrawable(this, iconRes);
                if (icon != null) {
                    Drawable tintedIcon = icon.mutate();
                    tintedIcon.setTint(android.graphics.Color.parseColor("#8B0000"));
                    marker.setIcon(tintedIcon);
                }
            } catch (Exception e) {
                Log.e("MARKER_ERROR", "Icon resource not found");
            }
        }
        map.getOverlays().add(marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) {
            mLocationOverlay.enableMyLocation();
            mLocationOverlay.enableFollowLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) {
            mLocationOverlay.disableMyLocation();
            mLocationOverlay.disableFollowLocation();
        }
    }
}
