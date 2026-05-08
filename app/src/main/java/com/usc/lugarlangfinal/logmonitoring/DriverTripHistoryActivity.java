package com.usc.lugarlangfinal.logmonitoring;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.BaseActivity;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.LogHistoryAdapter; // Corrected Import
import com.usc.lugarlangfinal.models.LogTrip;

import java.util.ArrayList;
import java.util.List;

public class DriverTripHistoryActivity extends BaseActivity {

    private RecyclerView rvHistory;
    private LogHistoryAdapter adapter; // Corrected Variable Type
    private List<LogTrip> tripHistoryList;
    private TextView tvDriverIdHeader;
    private ImageButton btnBack;

    private String selectedEmployeeId, franchiseName;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_trip_history);

        // Get data passed from LogMonitoring click listener
        selectedEmployeeId = getIntent().getStringExtra("SELECTED_ID");
        franchiseName = getIntent().getStringExtra("FRANCHISE");

        if (selectedEmployeeId == null || franchiseName == null) {
            Toast.makeText(this, "Error: Driver data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        fetchDriverSpecificLogs();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rvtrip);
        tvDriverIdHeader = findViewById(R.id.tvDetailDriverId);
        btnBack = findViewById(R.id.btnBack);

        // Set the header to show "Driver ID: 2001"
        tvDriverIdHeader.setText(selectedEmployeeId);
    }

    private void setupRecyclerView() {
        tripHistoryList = new ArrayList<>();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Use the LogHistoryAdapter to match the history layout
        adapter = new LogHistoryAdapter(tripHistoryList);
        rvHistory.setAdapter(adapter);
    }

    private void fetchDriverSpecificLogs() {
        // Direct Path: trip_log -> Sugbo Transit -> 2001
        DatabaseReference dbRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("trip_log")
                .child(franchiseName)
                .child(selectedEmployeeId);

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tripHistoryList.clear();

                for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                    LogTrip log = tripSnapshot.getValue(LogTrip.class);
                    if (log != null) {
                        log.setEmployeeId(selectedEmployeeId);
                        tripHistoryList.add(log);
                    }
                }

                adapter.notifyDataSetChanged();

                if (tripHistoryList.isEmpty()) {
                    Toast.makeText(DriverTripHistoryActivity.this, "No history found for this driver", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HISTORY_FIREBASE", "Error: " + error.getMessage());
            }
        });
    }
}