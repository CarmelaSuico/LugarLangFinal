package com.usc.lugarlangfinal.Transportation;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.StopsAdapter;
import com.usc.lugarlangfinal.models.*;

import java.util.*;

public class AssignedDriverConductor extends AppCompatActivity {

    // Navigation and UI
    private LinearLayout btnTransportdashboard, btnAssinedriver, btnBack;
    private AutoCompleteTextView acRoute, acVehicle, acDriver, acConductor;
    private EditText etT1, etT2, etPlate, etFranchise, etContact, etTime;
    private Spinner spinnerVehicleType, spinnerStatus;
    private Button btnAddTrip;
    private ImageButton btnAddStopField;

    // Data Storage
    private String adminFranchise = "";
    private List<String> currentStops = new ArrayList<>();
    private List<Route> activeRoutes = new ArrayList<>();
    private List<Vehicle> availableVehicles = new ArrayList<>();
    private Map<String, String> driverContactMap = new HashMap<>();

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_driver_conductor);

        initViews();
        lockFields();
        fetchAdminCompany();

        // Departure Time Picker
        etTime.setOnClickListener(v -> showTimePicker());

        // Listeners
        btnAddStopField.setOnClickListener(v -> showStopsDialog());
        btnAddTrip.setOnClickListener(v -> performAddTrip());

        // Navigation Logic
        btnAssinedriver.setSelected(true);
        btnTransportdashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, TransportationManagement.class));
        });
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboard.class));
            finish();
        });
    }

    private void initViews() {
        acRoute = findViewById(R.id.editroutecode);
        acVehicle = findViewById(R.id.autoVehicleCode);
        acDriver = findViewById(R.id.eddriver);
        acConductor = findViewById(R.id.edconductor);

        etT1 = findViewById(R.id.etTerminal1);
        etT2 = findViewById(R.id.etTerminal2);
        etPlate = findViewById(R.id.etPlateNumber);
        etFranchise = findViewById(R.id.etFranchise);
        etContact = findViewById(R.id.etContact);
        etTime = findViewById(R.id.eddeparturetime);

        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnAddTrip = findViewById(R.id.btnAddTrip);
        btnAddStopField = findViewById(R.id.btnAddStopField);

        btnTransportdashboard = findViewById(R.id.btntransportdashboard);
        btnAssinedriver = findViewById(R.id.btnassinedriver);
        btnBack = findViewById(R.id.btnback);

        setupSpinners();
    }

    private void lockFields() {
        etFranchise.setEnabled(false);
        spinnerVehicleType.setEnabled(false);
        etTime.setFocusable(false); // Prevents keyboard from opening
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, selectedMinute) -> {
                    String amPm = (hourOfDay < 12) ? "AM" : "PM";
                    int displayHour = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                    String time = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, selectedMinute, amPm);
                    etTime.setText(time);
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void fetchAdminCompany() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        adminFranchise = snapshot.child("company").getValue(String.class);
                        if (adminFranchise != null) {
                            etFranchise.setText(adminFranchise);
                            loadFilteredData();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadFilteredData() {
        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL).getReference();

        // 1. Load Active Routes
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

        // 2. Load Available Vehicles
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

        // 3. Load Active Drivers & Conductors with role filtering
        root.child("employee").orderByChild("franchise").equalTo(adminFranchise)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> drivers = new ArrayList<>();
                        List<String> conductors = new ArrayList<>();
                        driverContactMap.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String role = ds.child("role").getValue(String.class);
                            String name = ds.child("name").getValue(String.class);
                            String status = ds.child("status").getValue(String.class);
                            String phone = ds.child("phone").getValue(String.class);

                            if ("Active".equalsIgnoreCase(status)) {
                                if ("Driver".equalsIgnoreCase(role)) {
                                    drivers.add(name);
                                    driverContactMap.put(name, phone);
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

        // Auto-fill Listeners
        acRoute.setOnItemClickListener((parent, view, pos, id) -> {
            String selected = (String) parent.getItemAtPosition(pos);
            for (Route r : activeRoutes) {
                if (r.getRouteCode().equals(selected)) {
                    etT1.setText(r.getTerminal1());
                    etT2.setText(r.getTerminal2());
                    currentStops.clear();
                    if (r.getStops() != null) Collections.addAll(currentStops, r.getStops().split(", "));
                    break;
                }
            }
        });

        acVehicle.setOnItemClickListener((p, v, pos, id) -> {
            String selected = (String) p.getItemAtPosition(pos);
            for (Vehicle veh : availableVehicles) {
                if (veh.getVehicleCode().equals(selected)) {
                    etPlate.setText(veh.getPlateNumber());
                    setSpinnerValue(spinnerVehicleType, veh.getVehicleType());
                    break;
                }
            }
        });

        acDriver.setOnItemClickListener((p, v, pos, id) -> {
            String selectedName = (String) p.getItemAtPosition(pos);
            etContact.setText(driverContactMap.get(selectedName));
        });
    }

    private void performAddTrip() {
        String rCode = acRoute.getText().toString();
        String vCode = acVehicle.getText().toString();
        String dName = acDriver.getText().toString();
        String plateNum = etPlate.getText().toString(); // Get the plate number from EditText

        if (rCode.isEmpty() || vCode.isEmpty() || dName.isEmpty()) {
            Toast.makeText(this, "Route, Vehicle, and Driver are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance(DB_URL).getReference();
        String tripKey = db.child("trips").child(adminFranchise).push().getKey();

        Trip trip = new Trip();
        trip.tripId = tripKey;
        trip.routeCode = rCode;
        trip.vehicleCode = vCode;
        trip.plateNumber = plateNum; // ADDED: Assign plate number to the trip object
        trip.terminal1 = etT1.getText().toString();
        trip.terminal2 = etT2.getText().toString();
        trip.driverName = dName;
        trip.conductorName = acConductor.getText().toString();
        trip.departureTime = etTime.getText().toString();
        trip.status = spinnerStatus.getSelectedItem().toString();
        trip.franchise = adminFranchise;

        Map<String, Object> updates = new HashMap<>();
        updates.put("/trips/" + adminFranchise + "/" + tripKey, trip);
        updates.put("/vehicles/" + adminFranchise + "/" + vCode + "/Status", "Unavailable");

        db.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Trip Added Successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showStopsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_manage_stops, null);
        dialog.setContentView(v);
        RecyclerView rv = v.findViewById(R.id.rvManageStops);
        rv.setLayoutManager(new LinearLayoutManager(this));
        final ItemTouchHelper[] helper = new ItemTouchHelper[1];
        StopsAdapter adapter = new StopsAdapter(currentStops, vHolder -> {
            if (helper[0] != null) helper[0].startDrag(vHolder);
        });
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                Collections.swap(currentStops, vh.getBindingAdapterPosition(), t.getBindingAdapterPosition());
                adapter.notifyItemMoved(vh.getBindingAdapterPosition(), t.getBindingAdapterPosition());
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int d) {}
        };
        helper[0] = new ItemTouchHelper(callback);
        helper[0].attachToRecyclerView(rv);
        rv.setAdapter(adapter);
        dialog.show();
    }

    private void setupSpinners() {
        spinnerVehicleType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Bus", "Jeepney"}));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Scheduled", "Ongoing"}));
    }

    private void setSpinnerValue(Spinner s, String val) {
        ArrayAdapter a = (ArrayAdapter) s.getAdapter();
        int p = a.getPosition(val);
        if (p >= 0) s.setSelection(p);
    }
}