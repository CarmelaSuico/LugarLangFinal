package com.usc.lugarlangfinal.driverconductor;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;

public class StartEndTrip extends AppCompatActivity {

    private MapView map;
    private TextView txtT1, txtT2;
    private Button btnEndTrip;
    private String routeCode;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_start_end_trip);

        initViews();

        routeCode = getIntent().getStringExtra("ROUTE_CODE");
        String t1Name = getIntent().getStringExtra("TERMINAL_1");
        String t2Name = getIntent().getStringExtra("TERMINAL_2");

        txtT1.setText(t1Name != null ? t1Name : "Terminal 1");
        txtT2.setText(t2Name != null ? t2Name : "Terminal 2");

        setupMap();

        if (routeCode != null) {
            fetchFullRouteData();
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
        map.getController().setZoom(15.0);
    }

    private void fetchFullRouteData() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ArrayList<GeoPoint> waypoints = new ArrayList<>();
                    map.getOverlays().clear();

                    // 1. Terminal 1 (Start)
                    String t1Coords = snapshot.child("T1_Coords").getValue(String.class);
                    if (t1Coords != null) {
                        GeoPoint p1 = parseGeoPoint(t1Coords);
                        waypoints.add(p1);
                        // Using a default marker for the Start Terminal
                        addCustomMarker(p1, "START: " + txtT1.getText().toString(), 0);

                        map.getController().setZoom(17.0);
                        map.getController().setCenter(p1);
                    }

                    // 2. Intermediate Stops - Using trip_origin_24px
                    String stopCoords = snapshot.child("Stop_Coords").getValue(String.class);
                    String stopNames = snapshot.child("Stops").getValue(String.class);

                    if (stopCoords != null && !stopCoords.isEmpty()) {
                        String[] coordArray = stopCoords.split("\\|");
                        String[] nameArray = (stopNames != null) ? stopNames.split(", ") : null;

                        for (int i = 0; i < coordArray.length; i++) {
                            GeoPoint sp = parseGeoPoint(coordArray[i].trim());
                            if (sp.getLatitude() != 0) {
                                waypoints.add(sp);

                                String label = (nameArray != null && i < nameArray.length)
                                        ? nameArray[i] : "Stop " + (i + 1);

                                // UPDATED: Pass your drawable here
                                addCustomMarker(sp, label, R.drawable.circle_24px);
                            }
                        }
                    }

                    // 3. Terminal 2 (End)
                    String t2Coords = snapshot.child("T2_Coords").getValue(String.class);
                    if (t2Coords != null) {
                        GeoPoint p2 = parseGeoPoint(t2Coords);
                        waypoints.add(p2);
                        addCustomMarker(p2, "END: " + txtT2.getText().toString(), 0);
                    }

                    if (waypoints.size() >= 2) {
                        drawRoute(waypoints);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private GeoPoint parseGeoPoint(String coordString) {
        try {
            String[] parts = coordString.split(",");
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
                RoadManager roadManager = new OSRMRoadManager(StartEndTrip.this, "OSM_Routing");
                return roadManager.getRoad(waypoints);
            }

            @Override
            protected void onPostExecute(Road road) {
                if (road != null && road.mStatus == Road.STATUS_OK) {
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.setColor(0xFF00FF00); // Green
                    roadOverlay.setWidth(14.0f);
                    map.getOverlays().add(roadOverlay);
                    map.invalidate();
                }
            }
        }.execute();
    }

    // UPDATED: Method to support custom drawable icons
    private void addCustomMarker(GeoPoint point, String title, int iconRes) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setTitle(title);

        if (iconRes != 0) {
            Drawable icon = getResources().getDrawable(iconRes, getTheme());

            // --- START DARK RED COLOR LOGIC ---
            // Dark Red Hex Code is #8B0000
            icon.setTint(android.graphics.Color.parseColor("#8B0000"));
            // --- END DARK RED COLOR LOGIC ---

            marker.setIcon(icon);
        }

        map.getOverlays().add(marker);
    }
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}