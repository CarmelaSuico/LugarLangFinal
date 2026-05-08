package com.usc.lugarlangfinal.commuter;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

public class StartTracking extends AppCompatActivity {

    private MapView map;
    private Marker driverMarker;
    private DatabaseReference tripRef;

    // Add the DB URL consistency
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private MyLocationNewOverlay mLocationOverlay;
    private Polyline connectionLine;

    private String tripId, franchise, originName, destName, transportType;
    private TextView tvOrigin, tvDest, tvPlate, tvStatus;
    private BottomSheetBehavior<MaterialCardView> sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_tracking);

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
        // Optional: uncomment if you want the map to follow the user automatically
        // mLocationOverlay.enableFollowLocation();
        map.getOverlays().add(mLocationOverlay);

        connectionLine = new Polyline();
        connectionLine.getOutlinePaint().setColor(Color.GRAY);
        connectionLine.getOutlinePaint().setStrokeWidth(5f);
        connectionLine.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        map.getOverlays().add(connectionLine);

        // Ensure map focuses on user as soon as GPS is found
        mLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (mLocationOverlay.getMyLocation() != null) {
                map.getController().animateTo(mLocationOverlay.getMyLocation());
            }
        }));
    }

    private void setupFirebase() {
        // FIX: Added DB_URL to avoid null instance on non-US projects
        tripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips")
                .child(franchise)
                .child(tripId);

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Get the coordinates (format: "lat,lng")
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
            List<GeoPoint> linePoints = new ArrayList<>();
            linePoints.add(myPos);
            linePoints.add(driverPos);
            connectionLine.setPoints(linePoints);
            connectionLine.setVisible(true);
        } else {
            // Keep line hidden until GPS fix is acquired
            connectionLine.setVisible(false);
        }

        map.invalidate();
    }

    private void setupBottomSheet() {
        MaterialCardView trackingCard = findViewById(R.id.trackingCard);
        sheetBehavior = BottomSheetBehavior.from(trackingCard);
        sheetBehavior.setPeekHeight(300);
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
            return new GeoPoint(10.3157, 123.8854); // Default to Cebu instead of 0,0
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) {
            mLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) {
            mLocationOverlay.disableMyLocation();
        }
    }
}