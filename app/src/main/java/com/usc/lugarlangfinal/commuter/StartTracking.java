package com.usc.lugarlangfinal.commuter;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.usc.lugarlangfinal.models.Trip;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class StartTracking extends AppCompatActivity {

    private MapView map;
    private Marker driverMarker;
    private DatabaseReference tripRef;

    private String tripId, franchise, originName, destName, transportType;
    private TextView tvOrigin, tvDest, tvPlate, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_start_tracking);

        // 1. Get Data from Intent
        tripId = getIntent().getStringExtra("TRIP_ID");
        franchise = getIntent().getStringExtra("FRANCHISE");
        originName = getIntent().getStringExtra("USER_ORIGIN");
        destName = getIntent().getStringExtra("USER_DEST");
        transportType = getIntent().getStringExtra("TRANSPORT_TYPE"); // Received from intent
        String plateNumber = getIntent().getStringExtra("PLATE_NUMBER"); // Now correctly received

        if (tripId == null || franchise == null) {
            Toast.makeText(this, "Tracking Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews(plateNumber);
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

        // Fix: check for null before setting text
        tvPlate.setText("Plate Number: " + (plate != null ? plate : "N/A"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStopTracking).setOnClickListener(v -> finish());

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);
    }

    private void setupFirebase() {
        tripRef = FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(franchise)
                .child(tripId);

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String locStr = snapshot.child("currentLocation").getValue(String.class); //

                if (locStr != null && !locStr.isEmpty()) {
                    updateDriverMarker(parseCoord(locStr));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StartTracking.this, "Connection lost", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDriverMarker(GeoPoint position) {
        if (driverMarker == null) {
            driverMarker = new Marker(map);

            // LOGIC: Check transport type to set correct icon
            if (transportType != null && transportType.equalsIgnoreCase("Bus")) {
                driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.airport_shuttle_24px));
                driverMarker.setTitle("Bus Location");
            } else {
                // Default to Jeepney
                driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.airport_shuttle_24px));
                driverMarker.setTitle("Jeepney Location");
            }

            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlays().add(driverMarker);
        }

        driverMarker.setPosition(position);
        map.getController().animateTo(position);
        map.invalidate();
    }

    private GeoPoint parseCoord(String s) {
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) {
            return new GeoPoint(0.0, 0.0);
        }
    }

    @Override
    protected void onResume() { super.onResume(); map.onResume(); }
    @Override
    protected void onPause() { super.onPause(); map.onPause(); }
}