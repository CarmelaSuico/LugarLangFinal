package com.usc.lugarlangfinal.commuter;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.BaseActivity;
import com.usc.lugarlangfinal.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StartTracking extends BaseActivity {

    private MapView map;
    private Marker driverMarker;
    private DatabaseReference tripRef;

    // Database URL
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private MyLocationNewOverlay mLocationOverlay;
    private Polyline connectionLine;

    private String tripId, franchise, originName, destName, transportType;
    private TextView tvOrigin, tvDest, tvPlate, tvStatus, tvLiveETA;
    private BottomSheetBehavior<MaterialCardView> sheetBehavior;

    // ETA Calculation Constants
    private static final double AVG_SPEED_KMH = 20.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Inherits Light Mode lock from BaseActivity

        // Load OSM Configuration with User Agent
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_start_tracking);

        // Retrieve Data from Intent
        tripId = getIntent().getStringExtra("TRIP_ID");
        franchise = getIntent().getStringExtra("FRANCHISE");
        originName = getIntent().getStringExtra("USER_ORIGIN");
        destName = getIntent().getStringExtra("USER_DEST");
        transportType = getIntent().getStringExtra("TRANSPORT_TYPE");
        String plateNumber = getIntent().getStringExtra("PLATE_NUMBER");

        if (tripId == null || franchise == null) {
            Toast.makeText(this, "Tracking Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews(plateNumber);
        setupGPS();
        setupBottomSheet();
        setupFirebase();
    }

    private void initViews(String plate) {
        map = findViewById(R.id.mapTracking);
        tvOrigin = findViewById(R.id.tvTrackingOrigin);
        tvDest = findViewById(R.id.tvTrackingDest);
        tvPlate = findViewById(R.id.tvTrackingPlate);
        tvStatus = findViewById(R.id.tvStatusHeader);
        tvLiveETA = findViewById(R.id.tvLiveETA);

        tvOrigin.setText(originName);
        tvDest.setText(destName);
        tvPlate.setText("Plate Number: " + (plate != null ? plate : "N/A"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStopTracking).setOnClickListener(v -> finish());

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);
    }

    private void setupGPS() {
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

        // Dotted connection line between user and driver
        connectionLine = new Polyline();
        connectionLine.getOutlinePaint().setColor(Color.GRAY);
        connectionLine.getOutlinePaint().setStrokeWidth(5f);
        connectionLine.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        map.getOverlays().add(connectionLine);

        mLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (mLocationOverlay.getMyLocation() != null) {
                map.getController().animateTo(mLocationOverlay.getMyLocation());
            }
        }));
    }

    private void setupFirebase() {
        tripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips")
                .child(franchise)
                .child(tripId);

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String locStr = snapshot.child("currentLocation").getValue(String.class);
                if (locStr != null && !locStr.isEmpty()) {
                    GeoPoint driverPos = parseCoord(locStr);
                    updateTrackingUI(driverPos);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateTrackingUI(GeoPoint driverPos) {
        // Update Driver Marker
        if (driverMarker == null) {
            driverMarker = new Marker(map);
            int iconRes = (transportType != null && transportType.equalsIgnoreCase("Bus"))
                    ? R.drawable.airport_shuttle_24px : R.drawable.airport_shuttle_24px;
            driverMarker.setIcon(ContextCompat.getDrawable(this, iconRes));
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(driverPos);

        GeoPoint myPos = mLocationOverlay.getMyLocation();
        if (myPos != null) {
            // Update connection line
            List<GeoPoint> linePoints = new ArrayList<>();
            linePoints.add(myPos);
            linePoints.add(driverPos);
            connectionLine.setPoints(linePoints);
            connectionLine.setVisible(true);

            // Calculate ETA
            calculateAndDisplayETA(myPos, driverPos);
        } else {
            connectionLine.setVisible(false);
            tvLiveETA.setText("Estimated Arrival: Waiting for GPS...");
        }

        map.invalidate();
    }

    private void calculateAndDisplayETA(GeoPoint userPos, GeoPoint driverPos) {
        double distanceMeters = userPos.distanceToAsDouble(driverPos);
        double distanceKm = distanceMeters / 1000.0;

        // Time (Minutes) = (Distance / Speed) * 60
        double timeHours = distanceKm / AVG_SPEED_KMH;
        int timeMinutes = (int) (timeHours * 60);

        if (distanceMeters < 50) {
            tvLiveETA.setText("Status: Your ride has arrived!");
        } else {
            String etaText = String.format(Locale.getDefault(),
                    "Estimated Arrival: %d mins (%.1f km away)",
                    timeMinutes > 0 ? timeMinutes : 1,
                    distanceKm);
            tvLiveETA.setText(etaText);
        }
    }

    private void setupBottomSheet() {
        MaterialCardView trackingCard = findViewById(R.id.trackingCard);
        sheetBehavior = BottomSheetBehavior.from(trackingCard);
        sheetBehavior.setPeekHeight(350);
        sheetBehavior.setHideable(false);

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                String vehicle = (transportType != null && transportType.equalsIgnoreCase("Bus")) ? "Bus" : "Jeepney";
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    tvStatus.setText(vehicle + " Details");
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    tvStatus.setText("Waiting for the " + vehicle);
                }
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private GeoPoint parseCoord(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) {
            return new GeoPoint(10.3157, 123.8854);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }
}