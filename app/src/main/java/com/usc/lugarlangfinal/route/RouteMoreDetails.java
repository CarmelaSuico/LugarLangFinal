package com.usc.lugarlangfinal.route;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Route;

public class RouteMoreDetails extends AppCompatActivity {

    // UI Elements
    private TextView tvRouteCode, tvStatus, tvTerminal1, tvTerminal2, tvDistance;
    private TextView tvTransport, tvBaseFare, tvDistanceBand, tvAdditionalFare;
    private ImageButton btnBack, btnEdit;

    // Firebase
    private DatabaseReference routeRef;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private String selectedRouteCode;
    private String adminFranchise = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_more_details);

        // 1. Initialize Views
        initViews();

        // 2. Get Data from Intent (Passed from RouteManagement)
        selectedRouteCode = getIntent().getStringExtra("ROUTE_CODE");
        adminFranchise = getIntent().getStringExtra("COMPANY"); // Optional: if passed via intent

        if (selectedRouteCode == null) {
            Toast.makeText(this, "Error: Route code not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Load Data from Firebase
        if (adminFranchise == null || adminFranchise.isEmpty()) {
            fetchAdminFranchiseAndLoadData();
        } else {
            loadRouteDetails(adminFranchise, selectedRouteCode);
        }

        // 4. Click Listeners
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            // Logic to go to an Edit Page (pass the current route details)
            Intent intent = new Intent(RouteMoreDetails.this, EditRoute.class);
            intent.putExtra("ROUTE_CODE", selectedRouteCode);
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
        tvBaseFare = findViewById(R.id.tvBasefar);
        tvDistanceBand = findViewById(R.id.tvDistanceBand);
        tvAdditionalFare = findViewById(R.id.tvAddtionalFareperBand);

        btnEdit = findViewById(R.id.imgedit);
    }

    private void fetchAdminFranchiseAndLoadData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    loadRouteDetails(adminFranchise, selectedRouteCode);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRouteDetails(String franchise, String routeCode) {
        routeRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("franchise_routes")
                .child(franchise)
                .child(routeCode);

        routeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Route route = snapshot.getValue(Route.class);
                if (route != null) {
                    displayData(route);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RouteMoreDetails.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayData(Route route) {
        tvRouteCode.setText(route.getRouteCode());
        tvStatus.setText("Status: " + route.getStatus());
        tvTerminal1.setText("Terminal 1 (Starting Point): " + route.getTerminal1());
        tvTerminal2.setText("Terminal 2 (Ending Point): " + route.getTerminal2());
        tvDistance.setText(String.format("Distance: %.2f km", route.getDistance()));

        tvTransport.setText("Assigned Transportation: " + route.getAssignedTransport());
        tvBaseFare.setText(String.format("Base Fare: ₱%.2f", route.getBaseFare()));
        tvDistanceBand.setText("Distance Band: " + route.getDistanceBands() + " km");
        tvAdditionalFare.setText(String.format("Additional Fare Per Band: ₱%.2f", route.getAdditionalFarePerBand()));
    }
}