package com.usc.lugarlangfinal.logmonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.BaseActivity;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.LogTripAdapter;
import com.usc.lugarlangfinal.models.LogTrip;
import com.usc.lugarlangfinal.driverconductor.Ticketing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogMonitoring extends BaseActivity {

    private RecyclerView rvTrips;
    private SearchView searchView;
    private LogTripAdapter adapter;
    private List<LogTrip> fullLogList;
    private DatabaseReference dbRef;

    // Data to keep context across activities
    private String adminFranchise, employeeId, routeCode;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_monitoring);

        // 1. Initialize data lists first
        fullLogList = new ArrayList<>();

        // 2. Map UI components
        initViews();
        setupRecyclerView();

        // 3. Logic: Get the Admin's franchise then load logs
        getAdminFranchiseAndLoadLogs();

        // 4. Setup interaction
        setupSearch();
        setupNavigation();
    }

    private void initViews() {
        rvTrips = findViewById(R.id.rvtrip);
        searchView = findViewById(R.id.searchdriverid);

        LinearLayout btnTrips = findViewById(R.id.btntrips);
        if (btnTrips != null) btnTrips.setSelected(true);
    }

    private void setupRecyclerView() {
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogTripAdapter(fullLogList);
        rvTrips.setAdapter(adapter);
    }

    private void getAdminFranchiseAndLoadLogs() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetch company name to lock logs to this specific admin's franchise
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadFranchiseSpecificLogs(adminFranchise);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Admin fetch failed: " + error.getMessage());
            }
        });
    }

    private void loadFranchiseSpecificLogs(String adminFranchise) {
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("trip_log").child(adminFranchise);

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Use a HashMap to ensure unique Employee IDs
                Map<String, LogTrip> uniqueDrivers = new HashMap<>();

                for (DataSnapshot employeeSnapshot : snapshot.getChildren()) {
                    String empId = employeeSnapshot.getKey();

                    for (DataSnapshot tripSnapshot : employeeSnapshot.getChildren()) {
                        LogTrip log = tripSnapshot.getValue(LogTrip.class);
                        if (log != null) {
                            log.setEmployeeId(empId);
                            log.setFranchise(adminFranchise);
                            // Putting it in the map by ID ensures each ID appears only once
                            uniqueDrivers.put(empId, log);
                        }
                    }
                }

                fullLogList.clear();
                fullLogList.addAll(uniqueDrivers.values());

                filter(searchView.getQuery().toString());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", error.getMessage());
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });
    }

    private void filter(String text) {
        List<LogTrip> filtered = new ArrayList<>();
        String query = text.toLowerCase().trim();

        if (query.isEmpty()) {
            filtered.addAll(fullLogList);
        } else {
            for (LogTrip trip : fullLogList) {
                // Multi-search: Check Driver Name, Employee ID, or Vehicle Code
                if ((trip.getDriverName() != null && trip.getDriverName().toLowerCase().contains(query)) ||
                        (trip.getEmployeeId() != null && trip.getEmployeeId().toLowerCase().contains(query)) ||
                        (trip.getVehicleCode() != null && trip.getVehicleCode().toLowerCase().contains(query))) {
                    filtered.add(trip);
                }
            }
        }
        adapter.updateList(filtered);
    }

    private void setupNavigation() {
        LinearLayout btnTrips = findViewById(R.id.btntrips);
        LinearLayout btnTicketing = findViewById(R.id.btnticketing);
        LinearLayout btnBack = findViewById(R.id.btnback);

        btnTrips.setOnClickListener(v -> rvTrips.smoothScrollToPosition(0));

        btnTicketing.setOnClickListener(v -> {
            Intent intent = new Intent(this, Ticketing.class);
            intent.putExtra("COMPANY_NAME", adminFranchise);
            // These might be null if not passed from dashboard, so we use the loaded franchise
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }
}