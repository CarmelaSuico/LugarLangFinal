package com.usc.lugarlangfinal.Transportation;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.*;

import java.util.*;

public class AssignedDriverConductor extends AppCompatActivity {

    private AutoCompleteTextView acRoute, acVehicle, acDriver, acConductor, acVehicleType, acStatus;
    private TextInputEditText etT1, etT2, etPlate, etFranchise, etContact, etTime;
    private MaterialButton btnAddTrip;
    private LinearLayout btnTransportDashboard, btnAssignDriver, btnNavBack;

    private String adminFranchise = "";
    private List<Route> activeRoutes = new ArrayList<>();
    private List<Vehicle> availableVehicles = new ArrayList<>();
    private Map<String, String> driverContactMap = new HashMap<>();

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_driver_conductor);

        initViews();
        setupLockedFields();
        fetchAdminCompany();

        etTime.setOnClickListener(v -> showTimePicker());
        btnAddTrip.setOnClickListener(v -> performAddTrip());

        // Navigation
        if (btnAssignDriver != null) btnAssignDriver.setSelected(true);
        if (btnNavBack != null) btnNavBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboard.class));
            finish();
        });

        if (btnTransportDashboard != null) {
            btnTransportDashboard.setOnClickListener(v -> {
                startActivity(new Intent(this, TransportationManagement.class));
                finish();
            });
        }
    }

    private void initViews() {
        acRoute = findViewById(R.id.editroutecode);
        acVehicle = findViewById(R.id.autoVehicleCode);
        acDriver = findViewById(R.id.eddriver);
        acConductor = findViewById(R.id.edconductor);
        acVehicleType = findViewById(R.id.spinnerVehicleType);
        acStatus = findViewById(R.id.spinnerStatus);

        etT1 = findViewById(R.id.etTerminal1);
        etT2 = findViewById(R.id.etTerminal2);
        etPlate = findViewById(R.id.etPlateNumber);
        etFranchise = findViewById(R.id.etFranchise);
        etContact = findViewById(R.id.etContact);
        etTime = findViewById(R.id.eddeparturetime);

        btnAddTrip = findViewById(R.id.btnAddTrip);

        btnTransportDashboard = findViewById(R.id.btntransportdashboard);
        btnAssignDriver = findViewById(R.id.btnassinedriver);
        btnNavBack = findViewById(R.id.btnback);

        setupStaticDropdowns();
    }

    private void setupLockedFields() {
        etFranchise.setEnabled(false);
        etPlate.setEnabled(false);
        etT1.setEnabled(false);
        etT2.setEnabled(false);
        acVehicleType.setEnabled(false);
        etTime.setFocusable(false);
    }

    private void setupStaticDropdowns() {
        String[] statuses = {"Scheduled", "Ongoing", "Completed"};
        acStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses));
        acStatus.setText(statuses[0], false);
    }

    private void fetchAdminCompany() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        adminFranchise = snapshot.child("company").getValue(String.class);
                        if (adminFranchise != null) {
                            etFranchise.setText(adminFranchise);
                            loadFirebaseData();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadFirebaseData() {
        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL).getReference();

        // 1. Load Active Routes - Using PascalCase "Status"
        root.child("franchise_routes").child(adminFranchise).orderByChild("Status").equalTo("Active")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        activeRoutes.clear();
                        List<String> codes = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Route r = ds.getValue(Route.class);
                            if (r != null) { activeRoutes.add(r); codes.add(r.getRouteCode()); }
                        }
                        acRoute.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_list_item_1, codes));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 2. Load Available Vehicles - Using PascalCase "Status"
        root.child("vehicles").child(adminFranchise).orderByChild("Status").equalTo("Available")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        availableVehicles.clear();
                        List<String> vCodes = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) { availableVehicles.add(v); vCodes.add(v.getVehicleCode()); }
                        }
                        acVehicle.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_list_item_1, vCodes));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // 3. Load Staff - Ensure keys match your 'employee' node
        root.child("employee").orderByChild("franchise").equalTo(adminFranchise)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> drivers = new ArrayList<>();
                        List<String> conductors = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String role = ds.child("role").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);
                            String name = ds.child("name").getValue(String.class);
                            if ("Active".equalsIgnoreCase(status)) {
                                if ("Driver".equalsIgnoreCase(role)) {
                                    drivers.add(name);
                                    driverContactMap.put(name, ds.child("phone").getValue(String.class));
                                } else if ("Conductor".equalsIgnoreCase(role)) {
                                    conductors.add(name);
                                }
                            }
                        }
                        acDriver.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_list_item_1, drivers));
                        acConductor.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_list_item_1, conductors));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Auto-fill Logic
        acRoute.setOnItemClickListener((p, v, pos, id) -> {
            String selected = (String) p.getItemAtPosition(pos);
            for (Route r : activeRoutes) {
                if (selected.equals(r.getRouteCode())) {
                    etT1.setText(r.getTerminal1());
                    etT2.setText(r.getTerminal2());
                    break;
                }
            }
        });

        acVehicle.setOnItemClickListener((p, v, pos, id) -> {
            String selected = (String) p.getItemAtPosition(pos);
            for (Vehicle veh : availableVehicles) {
                if (selected.equals(veh.getVehicleCode())) {
                    etPlate.setText(veh.getPlateNumber());
                    acVehicleType.setText(veh.getVehicleType(), false);
                    break;
                }
            }
        });

        acDriver.setOnItemClickListener((p, v, pos, id) -> {
            String name = (String) p.getItemAtPosition(pos);
            etContact.setText(driverContactMap.get(name));
        });
    }

    private void performAddTrip() {
        if (acRoute.getText().toString().isEmpty() || acVehicle.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select Route and Vehicle", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();
        String tripKey = db.child("trips").child(adminFranchise).push().getKey();

        Trip trip = new Trip();
        trip.tripId = tripKey;
        trip.routeCode = acRoute.getText().toString();
        trip.vehicleCode = acVehicle.getText().toString();
        trip.setPlateNumber(etPlate.getText().toString());
        trip.terminal1 = etT1.getText().toString();
        trip.terminal2 = etT2.getText().toString();
        trip.driverName = acDriver.getText().toString();
        trip.conductorName = acConductor.getText().toString();
        trip.departureTime = etTime.getText().toString();
        trip.status = acStatus.getText().toString();
        trip.franchise = adminFranchise;

        Map<String, Object> updates = new HashMap<>();
        updates.put("/trips/" + adminFranchise + "/" + tripKey, trip);
        updates.put("/vehicles/" + adminFranchise + "/" + acVehicle.getText().toString() + "/Status", "Unavailable");

        db.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Trip Created!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String amPm = (hour < 12) ? "AM" : "PM";
            int h = (hour == 0 || hour == 12) ? 12 : hour % 12;
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }
}