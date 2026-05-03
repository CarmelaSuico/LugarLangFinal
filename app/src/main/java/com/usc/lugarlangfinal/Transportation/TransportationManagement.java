package com.usc.lugarlangfinal.Transportation;

import android.content.Intent;
import android.os.Bundle;
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

        // 1. Initialize UI Elements
        rvTrips = findViewById(R.id.rvEmployeeList); // Matching your XML ID
        searchView = findViewById(R.id.searchassigntransport);

        // 2. Setup Lists and Adapter
        tripList = new ArrayList<>();
        filteredList = new ArrayList<>();
        rvTrips.setLayoutManager(new LinearLayoutManager(this));

        // Pass the filteredList to the adapter
        tripAdapter = new TripAdapter(filteredList);
        rvTrips.setAdapter(tripAdapter);

        //nav bottons
        btnTransportdashboard = findViewById(R.id.btntransportdashboard);
        btnAssinedriver = findViewById(R.id.btnassinedriver);
        btnBack = findViewById(R.id.btnback);

        btnTransportdashboard.setSelected(true);
        btnAssinedriver.setOnClickListener(v -> {
            startActivity(new Intent(TransportationManagement.this, AssignedDriverConductor.class));
        });
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(TransportationManagement.this, AdminDashboard.class));
            finish();
        });


        // 3. Fetch Admin Identity and Load Trips
        fetchAdminAndLoadTrips();

        // 4. Setup Search Logic
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
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadFranchiseTrips();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransportationManagement.this, "Auth Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFranchiseTrips() {
        DatabaseReference tripRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trips")
                .child(adminFranchise);

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tripList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Trip trip = ds.getValue(Trip.class);
                    if (trip != null) {
                        // Optionally set the trip ID from the key if needed for details
                        trip.tripId = ds.getKey();
                        tripList.add(trip);
                    }
                }
                // Sync search and update UI
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
        if (text.isEmpty()) {
            filteredList.addAll(tripList);
        } else {
            String query = text.toLowerCase().trim();
            for (Trip item : tripList) {
                // Search by Route Code, Driver Name, or Vehicle Code
                if (item.routeCode.toLowerCase().contains(query) ||
                        item.driverName.toLowerCase().contains(query) ||
                        item.vehicleCode.toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        tripAdapter.notifyDataSetChanged();
    }
}