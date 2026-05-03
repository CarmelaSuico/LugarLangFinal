package com.usc.lugarlangfinal.driverconductor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.DriverOrConductoerDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.SettingAdminDriCon;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Ticketing extends AppCompatActivity {
    private Spinner spOrigin, spDestination, spPassengerType;
    private TextView tvTicketT1, tvTicketT2;
    private Button btnSubmit;
    private LinearLayout btnDashboardNav, btnTicket, btnSetting;

    private String routeCode, companyName, employeeId;
    private ArrayList<String> stopNames = new ArrayList<>();
    private ArrayList<GeoPoint> stopPoints = new ArrayList<>();
    private double baseFare, additionalFarePerBand;
    private int distanceBands;
    private final double DISCOUNT_RATE = 0.20;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticketing);

        initViews();
        setupNavigation();

        routeCode = getIntent().getStringExtra("ROUTE_CODE");
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        employeeId = getIntent().getStringExtra("EMPLOYEE_ID");

        if (routeCode == null) {
            Toast.makeText(this, "Error: No route assigned for ticketing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupPassengerTypeSpinner();
        fetchRouteData();

        btnSubmit.setOnClickListener(v -> calculateAndSaveTicket());
    }

    private void initViews() {
        spOrigin = findViewById(R.id.spinnerOrigin);
        spDestination = findViewById(R.id.spinnerDestination);
        spPassengerType = findViewById(R.id.spinnerPassengerType);
        tvTicketT1 = findViewById(R.id.tvTicketT1);
        tvTicketT2 = findViewById(R.id.tvTicketT2);
        btnSubmit = findViewById(R.id.btnSubmitTicket);

        // Navigation buttons from included layout
        btnDashboardNav = findViewById(R.id.btndriverorconddashoard);
        btnTicket = findViewById(R.id.btnticketing);
        btnSetting = findViewById(R.id.btnsetting);
    }

    private void setupNavigation() {
        btnTicket.setSelected(true);

        if (btnDashboardNav != null) {
            btnDashboardNav.setOnClickListener(v -> {
                Intent intent = new Intent(this, DriverOrConductoerDashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (btnSetting != null) {
            btnSetting.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingAdminDriCon.class);
                startActivity(intent);
            });
        }


    }

    private void setupPassengerTypeSpinner() {
        String[] types = {"Regular", "Student", "PWD", "Senior"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spPassengerType.setAdapter(adapter);
    }

    private void fetchRouteData() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("routes").child(routeCode);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    baseFare = snapshot.child("BaseFare").getValue(Double.class) != null ?
                            snapshot.child("BaseFare").getValue(Double.class) : 0.0;

                    additionalFarePerBand = snapshot.child("AdditionalFarePerBand").getValue(Double.class) != null ?
                            snapshot.child("AdditionalFarePerBand").getValue(Double.class) : 0.0;

                    distanceBands = snapshot.child("DistanceBands").getValue(Integer.class) != null ?
                            snapshot.child("DistanceBands").getValue(Integer.class) : 0;

                    stopNames.clear();
                    stopPoints.clear();

                    String t1Name = snapshot.child("Terminal1").getValue(String.class);
                    tvTicketT1.setText(t1Name);
                    stopNames.add(t1Name);
                    stopPoints.add(parseCoords(snapshot.child("T1_Coords").getValue(String.class)));

                    String stops = snapshot.child("Stops").getValue(String.class);
                    String coords = snapshot.child("Stop_Coords").getValue(String.class);
                    if (stops != null && coords != null && !stops.isEmpty()) {
                        String[] sNames = stops.split(", ");
                        String[] sCoords = coords.split("\\|");
                        for (int i = 0; i < Math.min(sNames.length, sCoords.length); i++) {
                            stopNames.add(sNames[i]);
                            stopPoints.add(parseCoords(sCoords[i]));
                        }
                    }

                    String t2Name = snapshot.child("Terminal2").getValue(String.class);
                    tvTicketT2.setText(t2Name);
                    stopNames.add(t2Name);
                    stopPoints.add(parseCoords(snapshot.child("T2_Coords").getValue(String.class)));

                    updateSpinners();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ERROR", error.getMessage());
            }
        });
    }

    private void updateSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stopNames);
        spOrigin.setAdapter(adapter);
        spDestination.setAdapter(adapter);
    }

    private void calculateAndSaveTicket() {
        int originIdx = spOrigin.getSelectedItemPosition();
        int destIdx = spDestination.getSelectedItemPosition();
        String type = spPassengerType.getSelectedItem().toString();

        if (originIdx == destIdx) {
            Toast.makeText(this, "Origin and Destination cannot be the same", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint start = stopPoints.get(originIdx);
        GeoPoint end = stopPoints.get(destIdx);
        double distanceKm = start.distanceToAsDouble(end) / 1000.0;

        double regularFare = baseFare;
        if (distanceKm > distanceBands) {
            double extraDistance = distanceKm - distanceBands;
            regularFare += Math.ceil(extraDistance) * additionalFarePerBand;
        }

        double discount = 0;
        if (!type.equalsIgnoreCase("Regular")) {
            discount = regularFare * DISCOUNT_RATE;
        }
        double totalFare = regularFare - discount;

        showReceiptDialog(stopNames.get(originIdx), stopNames.get(destIdx), type, regularFare, discount, totalFare);
    }

    private void showReceiptDialog(String start, String end, String type, double reg, double disc, double total) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ticket_receipt, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((TextView) dialogView.findViewById(R.id.resOrigin)).setText(start);
        ((TextView) dialogView.findViewById(R.id.resDestination)).setText(end);
        ((TextView) dialogView.findViewById(R.id.resType)).setText(type);
        ((TextView) dialogView.findViewById(R.id.resRegPrice)).setText(String.format("Php %.2f", reg));
        ((TextView) dialogView.findViewById(R.id.resDiscount)).setText(String.format("Php -%.2f", disc));
        ((TextView) dialogView.findViewById(R.id.resTotal)).setText(String.format("Php %.2f", total));

        dialogView.findViewById(R.id.btnConfirmTicket).setOnClickListener(v -> {
            saveTicketToFirebase(start, end, type, reg, disc, total);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveTicketToFirebase(String start, String end, String type, double reg, double disc, double total) {
        if (companyName == null || employeeId == null) {
            Toast.makeText(this, "Error: User context missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to ticket_logs instead of trip_logs to separate data
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ticket_logs")
                .child(companyName)
                .child(employeeId.replace(".", ","));

        // NEW: Create a human-readable timestamp
        // Format example: "May 02, 2026 11:30 AM"
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
        String readableTimestamp = sdf.format(new java.util.Date());

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("origin", start);
        ticket.put("destination", end);
        ticket.put("passenger_type", type);
        ticket.put("regular_fare", Math.round(reg * 100.0) / 100.0);
        ticket.put("discount", Math.round(disc * 100.0) / 100.0);
        ticket.put("total_fare", Math.round(total * 100.0) / 100.0);
        ticket.put("timestamp", readableTimestamp); // Now saves as a readable string
        ticket.put("route_code", routeCode);

        String ticketKey = ticketsRef.push().getKey();
        if (ticketKey != null) {
            ticketsRef.child(ticketKey).setValue(ticket).addOnSuccessListener(aVoid -> {
                Toast.makeText(Ticketing.this, "Ticket Issued: ₱" + total, Toast.LENGTH_LONG).show();
            });
        }
    }

    private GeoPoint parseCoords(String s) {
        if (s == null || s.isEmpty()) return new GeoPoint(0.0, 0.0);
        try {
            String[] p = s.split(",");
            return new GeoPoint(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()));
        } catch (Exception e) {
            return new GeoPoint(0.0, 0.0);
        }
    }
}