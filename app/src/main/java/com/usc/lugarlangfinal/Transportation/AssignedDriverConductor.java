package com.usc.lugarlangfinal.Transportation;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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

    // UI Components - Correct types to match Material 3 XML
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
        setupConstraints();
        fetchAdminCompany();

        etTime.setOnClickListener(v -> showTimePicker());
        btnAddTrip.setOnClickListener(v -> performAddTrip());

        // Bottom Navigation Logic
        if (btnAssignDriver != null) btnAssignDriver.setSelected(true);
        if (btnNavBack != null) btnNavBack.setOnClickListener(v -> finish());
        if (btnTransportDashboard != null) {
            btnTransportDashboard.setOnClickListener(v -> {
                startActivity(new Intent(this, TransportationManagement.class));
                finish();
            });
        }

        setupDropdownTouchListeners();
    }

    private void initViews() {
        acRoute = findViewById(R.id.editroutecode);
        etT1 = findViewById(R.id.etTerminal1);
        etT2 = findViewById(R.id.etTerminal2);

        acVehicle = findViewById(R.id.autoVehicleCode);
        etPlate = findViewById(R.id.etPlateNumber);
        acVehicleType = findViewById(R.id.spinnerVehicleType);
        etFranchise = findViewById(R.id.etFranchise);

        acDriver = findViewById(R.id.eddriver);
        acConductor = findViewById(R.id.edconductor);
        etContact = findViewById(R.id.etContact);
        etTime = findViewById(R.id.eddeparturetime);
        acStatus = findViewById(R.id.spinnerStatus);

        btnAddTrip = findViewById(R.id.btnAddTrip);
        btnTransportDashboard = findViewById(R.id.btntransportdashboard);
        btnAssignDriver = findViewById(R.id.btnassinedriver);
        btnNavBack = findViewById(R.id.btnback);

        setupStaticDropdowns();
    }

    private void setupConstraints() {
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

    private void setupDropdownTouchListeners() {
        View.OnTouchListener listener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ((AutoCompleteTextView) v).showDropDown();
            }
            return false;
        };
        acRoute.setOnTouchListener(listener);
        acVehicle.setOnTouchListener(listener);
        acDriver.setOnTouchListener(listener);
        acConductor.setOnTouchListener(listener);
        acStatus.setOnTouchListener(listener);
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

        // Load Routes
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
                        acRoute.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_dropdown_item_1line, codes));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Load Vehicles
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
                        acVehicle.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_dropdown_item_1line, vCodes));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Load Staff
        root.child("employee").orderByChild("Franchise").equalTo(adminFranchise)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> drivers = new ArrayList<>();
                        List<String> conductors = new ArrayList<>();
                        driverContactMap.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String role = ds.child("role").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);
                            String name = ds.child("name").getValue(String.class);
                            String contact = ds.child("contactNumber").getValue(String.class);
                            if ("Active".equalsIgnoreCase(status)) {
                                if ("Driver".equalsIgnoreCase(role)) {
                                    drivers.add(name);
                                    driverContactMap.put(name, contact);
                                } else if ("Conductor".equalsIgnoreCase(role)) {
                                    conductors.add(name);
                                }
                            }
                        }
                        acDriver.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_dropdown_item_1line, drivers));
                        acConductor.setAdapter(new ArrayAdapter<>(AssignedDriverConductor.this, android.R.layout.simple_dropdown_item_1line, conductors));
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
        // 1. Check if Franchise is loaded
        if (TextUtils.isEmpty(adminFranchise)) {
            Toast.makeText(this, "Company data still loading. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String rCode = acRoute.getText().toString().trim();
            String vCode = acVehicle.getText().toString().trim();

            if (TextUtils.isEmpty(rCode) || TextUtils.isEmpty(vCode)) {
                Toast.makeText(this, "Select Route and Vehicle", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();
            String tripKey = db.child("trips").child(adminFranchise).push().getKey();

            if (tripKey == null) {
                Toast.makeText(this, "Failed to generate Trip Key", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Find selected route
            Route selectedRoute = null;
            for (Route r : activeRoutes) {
                if (rCode.equals(r.getRouteCode())) {
                    selectedRoute = r;
                    break;
                }
            }

            if (selectedRoute == null) {
                Toast.makeText(this, "Route details not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Construct Trip object
            Trip trip = new Trip();
            trip.tripId = tripKey;
            trip.routeCode = rCode;
            trip.vehicleCode = vCode;
            trip.setPlateNumber(etPlate.getText().toString());
            trip.terminal1 = etT1.getText().toString();
            trip.terminal2 = etT2.getText().toString();
            trip.driverName = acDriver.getText().toString();
            trip.conductorName = acConductor.getText().toString();
            trip.departureTime = etTime.getText().toString();
            trip.status = acStatus.getText().toString();
            trip.franchise = adminFranchise;

            // Sync coordinates/stops from Route
            trip.t1_Coords = selectedRoute.getT1_Coords();
            trip.t2_Coords = selectedRoute.getT2_Coords();
            trip.stops = selectedRoute.getStops();
            trip.stops_Coords = selectedRoute.getStops_Coords();

            // 4. Perform Atomic Update
            Map<String, Object> updates = new HashMap<>();
            updates.put("trips/" + adminFranchise + "/" + tripKey, trip);
            updates.put("vehicles/" + adminFranchise + "/" + vCode + "/Status", "Unavailable");

            db.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AssignedDriverConductor.this, "Trip Assigned Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // This will show you exactly why Firebase rejected the save
                        Log.e("FIREBASE_SAVE_ERROR", "Reason: " + e.getMessage());
                        Toast.makeText(AssignedDriverConductor.this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            // This catches coding errors (like NullPointerExceptions)
            Log.e("TRIP_CODE_ERROR", "Exception: ", e);
            Toast.makeText(this, "System Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
