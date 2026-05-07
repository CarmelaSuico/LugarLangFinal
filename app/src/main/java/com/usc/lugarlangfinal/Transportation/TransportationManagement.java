package com.usc.lugarlangfinal.Transportation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.TripAdapter;
import com.usc.lugarlangfinal.models.Trip;

import java.util.ArrayList;
import java.util.List;

public class TransportationManagement extends AppCompatActivity {
    LinearLayout btnTransportdashboard, btnAssinedriver, btnBack;

    private RecyclerView rvTrips;
    private TripAdapter tripAdapter;
    private List<Trip> tripList;
    private List<Trip> filteredList;
    private SearchView searchView;

    private String adminFranchise = "";
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transportation_management);

        // 1. Init UI
        rvTrips = findViewById(R.id.rvEmployeeList);
        searchView = findViewById(R.id.searchassigntransport);
        btnTransportdashboard = findViewById(R.id.btntransportdashboard);
        btnAssinedriver = findViewById(R.id.btnassinedriver);
        btnBack = findViewById(R.id.btnback);

        // 2. Setup RecyclerView
        tripList = new ArrayList<>();
        filteredList = new ArrayList<>();
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(filteredList);
        rvTrips.setAdapter(tripAdapter);

        // 3. Navigation
        btnTransportdashboard.setSelected(true);
        btnAssinedriver.setOnClickListener(v -> {
            startActivity(new Intent(this, AssignedDriverConductor.class));
        });
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboard.class));
            finish();
        });

        // 4. Start Data Load
        fetchAdminAndLoadTrips();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterTrips(newText);
                return true;
            }
        });
    }

    private void fetchAdminAndLoadTrips() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // FIX: Get the company name as a String, NOT a Trip object
                adminFranchise = snapshot.child("company").getValue(String.class);

                if (adminFranchise != null && !adminFranchise.isEmpty()) {
                    // Once we have the company name, we can safely load the trips
                    loadFranchiseTrips();
                } else {
                    Toast.makeText(TransportationManagement.this, "Company not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransportationManagement.this, "Auth Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFranchiseTrips() {
        // Pointing to: trips -> Sugbo Transit (or whatever the franchise name is)
        DatabaseReference tripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips")
                .child(adminFranchise);

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tripList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        // This uses the @PropertyName mapping from your Trip.java
                        Trip trip = ds.getValue(Trip.class);
                        if (trip != null) {
                            trip.setTripId(ds.getKey());
                            tripList.add(trip);
                        }
                    } catch (Exception e) {
                        Log.e("FirebaseError", "Crash prevented: " + e.getMessage());
                    }
                }
                filterTrips(searchView.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransportationManagement.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTrips(String text) {
        filteredList.clear();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(tripList);
        } else {
            String query = text.toLowerCase().trim();
            for (Trip item : tripList) {
                String route = item.getRouteCode() != null ? item.getRouteCode().toLowerCase() : "";
                String driver = item.getDriverName() != null ? item.getDriverName().toLowerCase() : "";
                String vehicle = item.getVehicleCode() != null ? item.getVehicleCode().toLowerCase() : "";

                if (route.contains(query) || driver.contains(query) || vehicle.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        tripAdapter.notifyDataSetChanged();
    }
}