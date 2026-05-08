package com.usc.lugarlangfinal.commuter;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Trip;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripMoreDetails extends AppCompatActivity {
    private MapView map;
    private Trip trip;
    private int pickupIdx, dropoffIdx;
    private double calculatedFare;
    private float pickupDist, dropoffDist;
    private String userOrigin, userDest;
    private double oLat, oLng;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_trip_more_details);

        // Retrieve data from Intent
        trip = (Trip) getIntent().getSerializableExtra("TRIP_DATA");
        pickupIdx = getIntent().getIntExtra("PICKUP_INDEX", 0);
        dropoffIdx = getIntent().getIntExtra("DROPOFF_INDEX", 0);
        calculatedFare = getIntent().getDoubleExtra("CALCULATED_FARE", 0.0);
        pickupDist = getIntent().getFloatExtra("PICKUP_DIST", 0f);
        dropoffDist = getIntent().getFloatExtra("DROPOFF_DIST", 0f);
        userOrigin = getIntent().getStringExtra("USER_ORIGIN");
        userDest = getIntent().getStringExtra("USER_DEST");

        // Origins needed for the "Walk from here" icon
        oLat = getIntent().getDoubleExtra("ORIGIN_LAT", 0);
        oLng = getIntent().getDoubleExtra("ORIGIN_LNG", 0);

        if (trip == null) {
            finish();
            return;
        }

        initViews();
        setupMapAndPath();
    }

    private void initViews() {
        map = findViewById(R.id.mapDetails);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStartTracking).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartTracking.class);
            intent.putExtra("TRIP_ID", trip.getTripId());
            intent.putExtra("FRANCHISE", trip.getFranchise());
            intent.putExtra("USER_ORIGIN", userOrigin);
            intent.putExtra("USER_DEST", userDest);

            // ADD THESE:
            intent.putExtra("PLATE_NUMBER", trip.getPlateNumber()); // Pass the actual plate
            intent.putExtra("TRANSPORT_TYPE", trip.getAssignedTransport()); // Get if it's "Jeepney" or "Bus"

            startActivity(intent);
        });

        TextView tvHeader = findViewById(R.id.tvTerminals);
        tvHeader.setText((userOrigin != null && userDest != null) ?
                userOrigin + " ❯ " + userDest : trip.getTerminal1() + " ❯ " + trip.getTerminal2());

        ((TextView) findViewById(R.id.tvPlate)).setText("Plate Number: " + trip.getPlateNumber());
        ((TextView) findViewById(R.id.tvDeparture)).setText("Departure: " + trip.getDepartureTime());

        String boardStop = SuggestedRoutes.getStopName(trip, pickupIdx);
        String alightStop = SuggestedRoutes.getStopName(trip, dropoffIdx);

        ((TextView) findViewById(R.id.tvArrival)).setText(
                "🟢 Board: " + boardStop + " (" + formatDist(pickupDist) + " walk)\n"
                        + "🔴 Alight: " + alightStop + " (" + formatDist(dropoffDist) + " walk)");

        double discountedFare = calculatedFare * 0.80; // 20% discount
        ((TextView) findViewById(R.id.tvFare)).setText(
                String.format("Regular: ₱%.2f   |   Discounted: ₱%.2f", calculatedFare, discountedFare));
    }

    private void setupMapAndPath() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(16.0);

        List<GeoPoint> waypoints = new ArrayList<>();
        String raw = trip.getStopCoords();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] segments = raw.split("\\|");
            for (int i = pickupIdx; i <= dropoffIdx && i < segments.length; i++) {
                waypoints.add(parseCoord(segments[i].trim()));
            }
        }

        if (waypoints.size() < 2) return;

        // --- FETCH REAL ROAD (Fixes the "broken" line cutting through buildings) ---
        executor.execute(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
            roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR);
            Road road = roadManager.getRoad(new ArrayList<>(waypoints));

            mainHandler.post(() -> {
                if (road.mStatus == Road.STATUS_OK) {
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.getOutlinePaint().setColor(Color.parseColor("#00C853"));
                    roadOverlay.getOutlinePaint().setStrokeWidth(14.0f);
                    map.getOverlays().add(roadOverlay);
                }

                // Add the Icons for the whole journey
                addJourneyIcons(waypoints);
                map.getController().animateTo(waypoints.get(0));
            });
        });
    }

    private void addJourneyIcons(List<GeoPoint> rideSegment) {
        // 1. "Board Here" Icon (Green Circle) at the start of the ride
        if (!rideSegment.isEmpty()) {
            addMarker(rideSegment.get(0), "Board Here", R.drawable.trip_origin_24px);

            // 2. "Alight Here" Icon (Red Pin) at the end of the ride
            addMarker(rideSegment.get(rideSegment.size() - 1), "Alight Here", R.drawable.location_on_24px);
        }

        // Walking path logic and userStart coordinates have been removed

        map.invalidate();
    }

    private void addMarker(GeoPoint point, String title, int iconRes) {
        Marker m = new Marker(map);
        m.setPosition(point);
        m.setTitle(title);
        try {
            m.setIcon(ContextCompat.getDrawable(this, iconRes));
        } catch (Exception e) {
            Log.e("MARKER_ERROR", "Icon not found");
        }
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    private GeoPoint parseCoord(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) { return new GeoPoint(0.0, 0.0); }
    }

    private String formatDist(float m) {
        return m < 1000 ? Math.round(m) + " m" : String.format("%.1f km", m / 1000f);
    }

    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause() { super.onPause(); map.onPause(); }
}