package com.usc.lugarlangfinal.route;

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
import com.usc.lugarlangfinal.models.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddNewRoute extends AppCompatActivity {

    AutoCompleteTextView editRouteCode;
    EditText edT1, edT2, edDist, edBase, edBands, edAddl;
    Spinner spinnerTransport, spinnerStatus;
    Button btnSave;
    ImageButton btnManageStops;
    LinearLayout btnRouteDashboard, btnBack;

    private String adminFranchise = "";
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private List<Route> masterRouteList = new ArrayList<>();
    private List<String> currentStops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_route);

        // Initialize UI
        editRouteCode = findViewById(R.id.editroutecode);
        edT1 = findViewById(R.id.edterminal1);
        edT2 = findViewById(R.id.edterminal2);
        edDist = findViewById(R.id.eddistance);
        edBase = findViewById(R.id.edbasefare);
        edBands = findViewById(R.id.eddistancebands);
        edAddl = findViewById(R.id.edadditionalfare);
        spinnerTransport = findViewById(R.id.spinnerassignedtranport);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSave = findViewById(R.id.btnaddroute);
        btnManageStops = findViewById(R.id.btnAddStopField);
        btnRouteDashboard = findViewById(R.id.btnroutedashboard);
        btnBack = findViewById(R.id.btnback);

        setupSpinners();
        fetchAdminCompany();
        loadMasterRoutes();

        // Autocomplete Logic
        editRouteCode.setOnTouchListener((v, event) -> { editRouteCode.showDropDown(); return false; });
        editRouteCode.setOnItemClickListener((parent, view, position, id) -> autoFill((String) parent.getItemAtPosition(position)));

        btnManageStops.setOnClickListener(v -> showStopsDialog());
        btnSave.setOnClickListener(v -> saveToFranchise());

        // Nav
        btnBack.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboard.class)));
        btnRouteDashboard.setOnClickListener(v -> startActivity(new Intent(this, RouteManagement.class)));
    }

    private void fetchAdminCompany() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) adminFranchise = snapshot.child("company").getValue(String.class);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadMasterRoutes() {
        FirebaseDatabase.getInstance(DB_URL).getReference("routes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        masterRouteList.clear();
                        List<String> codes = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Route r = ds.getValue(Route.class);
                            if (r != null) { masterRouteList.add(r); codes.add(r.getRouteCode()); }
                        }
                        editRouteCode.setAdapter(new ArrayAdapter<>(AddNewRoute.this, android.R.layout.simple_dropdown_item_1line, codes));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void autoFill(String code) {
        for (Route r : masterRouteList) {
            if (r.getRouteCode().equals(code)) {
                edT1.setText(r.getTerminal1());
                edT2.setText(r.getTerminal2());
                edBase.setText(String.valueOf(r.getBaseFare()));
                edBands.setText(String.valueOf(r.getDistanceBands()));
                edAddl.setText(String.valueOf(r.getAdditionalFarePerBand()));

                try {
                    String[] t1 = r.getT1_Coords().split(",");
                    String[] t2 = r.getT2_Coords().split(",");

                    double lat1 = Double.parseDouble(t1[0].trim());
                    double lon1 = Double.parseDouble(t1[1].trim());
                    double lat2 = Double.parseDouble(t2[0].trim());
                    double lon2 = Double.parseDouble(t2[1].trim());

                    double finalDistance = calculateDistance(lat1, lon1, lat2, lon2);

                    // --- FIXED: Format to 2 decimal places ---
                    String formattedDistance = String.format("%.2f", finalDistance);
                    edDist.setText(formattedDistance + " km");

                } catch (Exception e) {
                    // Fallback to the distance value in the DB if coords fail
                    // Also formatting the fallback just in case
                    String fallbackDist = String.format("%.2f", r.getDistance());
                    edDist.setText(fallbackDist + " km");
                }

                currentStops.clear();
                if (r.getStops() != null) Collections.addAll(currentStops, r.getStops().split(", "));
                break;
            }
        }
    }

    private void showStopsDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_manage_stops, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(v);

        RecyclerView rv = v.findViewById(R.id.rvManageStops);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 1. Define the ItemTouchHelper first (we use a wrapper to avoid the "variable might not be initialized" error)
        final ItemTouchHelper[] itemTouchHelperWrapper = new ItemTouchHelper[1];

        // 2. Initialize the Adapter with the new Constructor
        // We pass 'itemTouchHelperWrapper[0]::startDrag' as the second argument
        StopsAdapter adapter = new StopsAdapter(currentStops, viewHolder -> {
            if (itemTouchHelperWrapper[0] != null) {
                itemTouchHelperWrapper[0].startDrag(viewHolder);
            }
        });

        // 3. Set up the Drag & Drop Callback
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();

                // Swap the items in your data list
                java.util.Collections.swap(currentStops, fromPosition, toPosition);
                // Notify the adapter of the change
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        };

        // 4. Create the actual Helper and attach it
        itemTouchHelperWrapper[0] = new ItemTouchHelper(callback);
        itemTouchHelperWrapper[0].attachToRecyclerView(rv);

        rv.setAdapter(adapter);

        // "Done" Button logic (if you have one in your dialog layout)
        Button btnDone = v.findViewById(R.id.btnDoneStops);
        if (btnDone != null) {
            btnDone.setOnClickListener(view -> dialog.dismiss());
        }
        dialog.show();
    }

    private void saveToFranchise() {
        String code = editRouteCode.getText().toString().trim();
        if (code.isEmpty() || adminFranchise.isEmpty()) {
            Toast.makeText(this, "Complete the form and ensure you are logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Route newRoute = new Route();
        newRoute.setRouteCode(code);
        newRoute.setCompany(adminFranchise);
        newRoute.setTerminal1(edT1.getText().toString());
        newRoute.setTerminal2(edT2.getText().toString());
        newRoute.setStops(String.join(", ", currentStops));
        newRoute.setBaseFare(Double.parseDouble(edBase.getText().toString()));
        newRoute.setDistance(Double.parseDouble(edDist.getText().toString().replaceAll("[^\\d.]", "")));
        newRoute.setDistanceBands(Integer.parseInt(edBands.getText().toString().replaceAll("[^\\d]", "")));
        newRoute.setAdditionalFarePerBand(Double.parseDouble(edAddl.getText().toString()));
        newRoute.setAssignedTransport(spinnerTransport.getSelectedItem().toString());
        newRoute.setStatus(spinnerStatus.getSelectedItem().toString());

        FirebaseDatabase.getInstance(DB_URL).getReference("franchise_routes")
                .child(adminFranchise).child(code).setValue(newRoute)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Route claimed for " + adminFranchise, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupSpinners() {
        spinnerTransport.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Bus", "Jeepney"}));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Active", "Deactive"}));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);

        // Convert to Kilometers (using 1.609344 for miles to km)
        return dist * 60 * 1.1515 * 1.609344;
    }
}