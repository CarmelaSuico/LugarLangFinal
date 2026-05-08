package com.usc.lugarlangfinal.Transportation;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.usc.lugarlangfinal.R;

public class TranportationMoreDetails extends AppCompatActivity {

    // Header Views
    private TextView tvRouteCode, tvVehicleCode;
    private ImageButton btnBack;

    // Card Details Views
    private TextView tvStatus, tvTerminals, tvTransportType, tvPlateNumber;
    private TextView tvDriver, tvConductor, tvDeparture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tranportation_more_details);

        initViews();
        displayTripData();

        // Standard Back Navigation
        btnBack.setOnClickListener(v -> finish());

    }

    private void initViews() {
        // Match these IDs with the XML provided earlier
        btnBack = findViewById(R.id.btnBack);
        tvRouteCode = findViewById(R.id.tvDetailRouteCode);
        tvVehicleCode = findViewById(R.id.tvDetailVehicleCode);

        tvStatus = findViewById(R.id.tvDetailStatus);
        tvTerminals = findViewById(R.id.tvDetailTerminals);
        tvTransportType = findViewById(R.id.tvDetailTransportType);
        tvPlateNumber = findViewById(R.id.tvDetailPlateNumber);
        tvDriver = findViewById(R.id.tvDetailDriver);
        tvConductor = findViewById(R.id.tvDetailConductor);
        tvDeparture = findViewById(R.id.tvDetailDeparture);
    }

    private void displayTripData() {
        // Retrieve strings using the keys from your tripData HashMap
        String routeCode = getIntent().getStringExtra("RouteCode");
        String vehicleCode = getIntent().getStringExtra("VehicleCode");
        String transportType = getIntent().getStringExtra("AssignedTransport");
        String plateNumber = getIntent().getStringExtra("PlateNumber");
        String terminal1 = getIntent().getStringExtra("Terminal1");
        String terminal2 = getIntent().getStringExtra("Terminal2");
        String departureTime = getIntent().getStringExtra("departureTime");
        String driverName = getIntent().getStringExtra("driverName");
        String conductorName = getIntent().getStringExtra("conductorName");
        String status = getIntent().getStringExtra("status");

        // Set Data to Header
        tvRouteCode.setText(routeCode != null ? routeCode : "ROUTE");
        tvVehicleCode.setText(vehicleCode != null ? vehicleCode : "V-000");

        // Set Data to Details Card
        tvStatus.setText("Status: " + (status != null ? status : "Active"));

        if (terminal1 != null && terminal2 != null) {
            tvTerminals.setText(terminal1 + " ➔ " + terminal2);
        }

        tvTransportType.setText(transportType != null ? transportType : "N/A");
        tvPlateNumber.setText(plateNumber != null ? plateNumber : "N/A");
        tvDriver.setText(driverName != null ? driverName : "Unassigned");
        tvConductor.setText(conductorName != null ? conductorName : "Unassigned");
        tvDeparture.setText(departureTime != null ? departureTime : "--:-- --");
    }
}