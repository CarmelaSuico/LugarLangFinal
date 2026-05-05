package com.usc.lugarlangfinal.driverconductor;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private String companyName, employeeId, tripId;
    private MyLocationNewOverlay mLocationOverlay;
    private Polyline currentRoadOverlay;
    private ArrayList<GeoPoint> allPoints = new ArrayList<>();
    private ArrayList<String> allNames = new ArrayList<>();
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_end_trip);

        initViews();
        setupMap();

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

                    String t1Name = snapshot.child("terminal1").getValue(String.class);
                    String t1Coords = snapshot.child("t1_Coords").getValue(String.class);
                    if (t1Coords != null) {
                        GeoPoint p = parseGeoPoint(t1Coords); allPoints.add(p); allNames.add(t1Name);
                        addMarker(p, "Start: " + t1Name);
                        map.getController().setCenter(p);
                    }

                    String t2Name = snapshot.child("terminal2").getValue(String.class);
                    String t2Coords = snapshot.child("t2_Coords").getValue(String.class);
                    if (t2Coords != null) {
                        GeoPoint p = parseGeoPoint(t2Coords); allPoints.add(p); allNames.add(t2Name);
                        addMarker(p, "End: " + t2Name);
                    }
                    if (allPoints.size() >= 2) drawRoute(allPoints);
                    txtT1.setText(t1Name);
                    txtT2.setText(t2Name);
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
                    // 1. Capture EVERY field from the snapshot to prevent deletion
                    // CRITICAL: Matches the uppercase 'P' seen in your database image_10133c.png
                    String plate = snapshot.child("PlateNumber").getValue(String.class);
                    String tId = snapshot.child("tripId").getValue(String.class);
                    String vCode = snapshot.child("vehicleCode").getValue(String.class);
                    String rCode = snapshot.child("routeCode").getValue(String.class);
                    String franchise = snapshot.child("franchise").getValue(String.class);

                    String t1Name = snapshot.child("terminal1").getValue(String.class);
                    String t2Name = snapshot.child("terminal2").getValue(String.class);
                    String t1C = snapshot.child("t1_Coords").getValue(String.class);
                    String t2C = snapshot.child("t2_Coords").getValue(String.class);
                    String stops = snapshot.child("stops").getValue(String.class);
                    String stopsCoords = snapshot.child("stops_Coords").getValue(String.class);

                    String driver = snapshot.child("driverName").getValue(String.class);
                    String conductor = snapshot.child("conductorName").getValue(String.class);
                    String oldDepTime = snapshot.child("departureTime").getValue(String.class);

                    // 2. Calculate +1 Hour for the next trip leg
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                    String nextDeparture = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.getTime());

                    // 3. Prepare the reversed leg map
                    Map<String, Object> nextLeg = new HashMap<>();

                    // SWAP the route locations
                    nextLeg.put("terminal1", t2Name);
                    nextLeg.put("terminal2", t1Name);
                    nextLeg.put("t1_Coords", t2C);
                    nextLeg.put("t2_Coords", t1C);
                    nextLeg.put("stops", reverseString(stops, ", "));
                    nextLeg.put("stops_Coords", reverseString(stopsCoords, "\\|"));

                    // RETAIN ALL IDENTIFIERS (This prevents the deletion seen in image_044cd3.png)
                    nextLeg.put("PlateNumber", plate); // Re-saved with correct casing
                    nextLeg.put("tripId", tId);
                    nextLeg.put("vehicleCode", vCode);
                    nextLeg.put("routeCode", rCode);
                    nextLeg.put("franchise", franchise);
                    nextLeg.put("driverName", driver);
                    nextLeg.put("conductorName", conductor);

                    // UPDATE status and time
                    nextLeg.put("departureTime", nextDeparture);
                    nextLeg.put("status", "Scheduled");
                    nextLeg.put("currentLocation", "");

                    // 4. Atomic Database Update
                    Map<String, Object> finalUpdates = new HashMap<>();
                    finalUpdates.put("trips/" + companyName + "/" + tripId, nextLeg);

                    // Log history with history-specific keys
                    String logKey = rootRef.child("trip_logs").child(companyName).child(employeeId).push().getKey();
                    Map<String, Object> log = new HashMap<>();
                    log.put("T1", t1Name);
                    log.put("T2", t2Name);
                    log.put("PlateNumber", plate);
                    log.put("drivername", driver);
                    log.put("departuretime", oldDepTime);
                    log.put("timestamp", ServerValue.TIMESTAMP);
                    finalUpdates.put("trip_logs/" + companyName + "/" + employeeId + "/" + logKey, log);

                    rootRef.updateChildren(finalUpdates).addOnSuccessListener(aVoid -> {
                        stopService(new Intent(StartEndTrip.this, LocationService.class));
                        Toast.makeText(StartEndTrip.this, "Trip Reversed! Next: " + nextDeparture, Toast.LENGTH_LONG).show();
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

    private void addMarker(GeoPoint p, String t) {
        Marker m = new Marker(map); m.setPosition(p); m.setTitle(t); map.getOverlays().add(m);
    }

    private GeoPoint parseGeoPoint(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) { return new GeoPoint(0.0, 0.0); }
    }

    private void drawRoute(ArrayList<GeoPoint> w) {
        new AsyncTask<Void, Void, Road>() {
            @Override protected Road doInBackground(Void... v) {
                OSRMRoadManager m = new OSRMRoadManager(StartEndTrip.this, getPackageName());
                m.setMean(OSRMRoadManager.MEAN_BY_CAR);
                return m.getRoad(w);
            }
            @Override protected void onPostExecute(Road r) {
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