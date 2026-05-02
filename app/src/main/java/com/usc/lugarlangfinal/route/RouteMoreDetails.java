package com.usc.lugarlangfinal.route;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Route;

import java.util.Locale;

public class RouteMoreDetails extends AppCompatActivity {

    // UI Elements
    private TextView tvRouteCode, tvStatus, tvTerminal1, tvTerminal2, tvDistance;
    private TextView tvTransport, tvBaseFare, tvDistanceBand, tvAdditionalFare;
    private ImageButton btnBack;
    private MaterialButton btnEdit; // Changed from ImageButton to MaterialButton

    // Firebase
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private String selectedRouteCode;
    private String adminFranchise = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_more_details);

        // 1. Initialize Views
        initViews();

        // 2. Get Data from Intent
        selectedRouteCode = getIntent().getStringExtra("ROUTE_CODE");
        adminFranchise = getIntent().getStringExtra("COMPANY");

        if (selectedRouteCode == null) {
            Toast.makeText(this, "Error: Route code missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Load Data logic
        if (adminFranchise == null || adminFranchise.isEmpty()) {
            fetchAdminFranchiseAndLoadData();
        } else {
            loadRouteDetails(adminFranchise, selectedRouteCode);
        }

        // 4. Click Listeners
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(RouteMoreDetails.this, EditRoute.class);
            intent.putExtra("ROUTE_CODE", selectedRouteCode);
            intent.putExtra("COMPANY", adminFranchise);
            startActivity(intent);
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRouteCode = findViewById(R.id.tvDetailRouteCode);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvTerminal1 = findViewById(R.id.tvDetailTerminal1);
        tvTerminal2 = findViewById(R.id.tvDetailTerminal2);
        tvDistance = findViewById(R.id.tvDetailDistance);

        tvTransport = findViewById(R.id.tvAssignedTransport);
        tvBaseFare = findViewById(R.id.tvBaseFare);
        tvDistanceBand = findViewById(R.id.tvDistanceBand);
        tvAdditionalFare = findViewById(R.id.tvAdditionalFare);

        btnEdit = findViewById(R.id.btnEditRoute); // Matches the MaterialButton ID in your XML
    }

    private void fetchAdminFranchiseAndLoadData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadRouteDetails(adminFranchise, selectedRouteCode);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RouteMoreDetails", "Database error: " + error.getMessage());
            }
        });
    }

    private void loadRouteDetails(String franchise, String routeCode) {
        DatabaseReference routeRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("franchise_routes")
                .child(franchise)
                .child(routeCode);

        // addValueEventListener keeps the UI updated if data changes in the DB
        routeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Route route = snapshot.getValue(Route.class);
                    if (route != null) {
                        displayData(route);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RouteMoreDetails.this, "Failed to load route details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData(Route route) {
        tvRouteCode.setText(route.getRouteCode() != null ? route.getRouteCode() : "N/A");
        tvStatus.setText(route.getStatus() != null ? route.getStatus() : "N/A");
        tvTerminal1.setText(route.getTerminal1() != null ? route.getTerminal1() : "N/A");
        tvTerminal2.setText(route.getTerminal2() != null ? route.getTerminal2() : "N/A");

        tvDistance.setText(String.format(Locale.getDefault(), "%.2f KM", route.getDistance()));
        tvTransport.setText(route.getAssignedTransport() != null ? route.getAssignedTransport() : "N/A");
        tvBaseFare.setText(String.format(Locale.getDefault(), "₱%.2f", route.getBaseFare()));
        tvDistanceBand.setText(String.format(Locale.getDefault(), "%d KM", route.getDistanceBands()));
        tvAdditionalFare.setText(String.format(Locale.getDefault(), "₱%.2f", route.getAdditionalFarePerBand()));

        // Set status color aesthetic
        if ("Active".equalsIgnoreCase(route.getStatus())) {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.primaryAesthetic));
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        }
    }
}