package com.usc.lugarlangfinal.route;

import android.content.Intent;
import android.location.Location; // Added for distance calculation
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.StopsAdapter;
import com.usc.lugarlangfinal.models.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AddNewRoute extends AppCompatActivity {

    private TextInputEditText edT1, edT2, edDist, edBase, edBands, edAddl;
    private AutoCompleteTextView editRouteCode, spinnerTransport, spinnerStatus;
    private Button btnSave;
    private View btnManageStops;
    private LinearLayout btnBack, btnroutedashboard, btnaddnewroute;

    private String adminFranchise = "";
    private String selectedT1Coords = "", selectedT2Coords = "";
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private List<Route> masterRouteList = new ArrayList<>();
    private List<String> currentStops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_route);

        initViews();
        setupSpinners();
        fetchAdminCompany();
        loadMasterRoutes();

        editRouteCode.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCode = (String) parent.getItemAtPosition(position);
            autoFill(selectedCode);
        });

        btnManageStops.setOnClickListener(v -> showStopsDialog());
        btnSave.setOnClickListener(v -> saveToFranchise());

        btnaddnewroute.setSelected(true);
        btnroutedashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, RouteManagement.class));
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminDashboard.class));
            });
        }
    }

    private void initViews() {
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
        btnBack = findViewById(R.id.btnback);
        btnroutedashboard = findViewById(R.id.btnroutedashboard);
        btnaddnewroute = findViewById(R.id.btnaddnewroute);


        edT1.setEnabled(false);
        edT2.setEnabled(false);
        edDist.setEnabled(false);
    }

    private void setupSpinners() {
        String[] transport = {"Bus", "Jeepney"};
        spinnerTransport.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transport));
        String[] status = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, status));
        spinnerStatus.setText(status[0], false);
    }

    private void autoFill(String code) {
        for (Route r : masterRouteList) {
            if (r.getRouteCode().equals(code)) {
                edT1.setText(r.getTerminal1());
                edT2.setText(r.getTerminal2());
                edBase.setText(String.valueOf(r.getBaseFare()));
                edBands.setText(String.valueOf(r.getDistanceBands()));
                edAddl.setText(String.valueOf(r.getAdditionalFarePerBand()));

                selectedT1Coords = r.getT1_Coords();
                selectedT2Coords = r.getT2_Coords();

                // AUTOMATIC DISTANCE CALCULATION
                calculateAndSetDistance(selectedT1Coords, selectedT2Coords);

                currentStops.clear();
                if (r.getStops() != null && !r.getStops().isEmpty()) {
                    Collections.addAll(currentStops, r.getStops().split(", "));
                }
                break;
            }
        }
    }

    /**
     * Parses coordinates and calculates distance in KM using Haversine-like formula
     */
    private void calculateAndSetDistance(String coords1, String coords2) {
        try {
            if (!TextUtils.isEmpty(coords1) && !TextUtils.isEmpty(coords2)) {
                String[] latLng1 = coords1.split(",");
                String[] latLng2 = coords2.split(",");

                double lat1 = Double.parseDouble(latLng1[0].trim());
                double lon1 = Double.parseDouble(latLng1[1].trim());
                double lat2 = Double.parseDouble(latLng2[0].trim());
                double lon2 = Double.parseDouble(latLng2[1].trim());

                float[] results = new float[1];
                Location.distanceBetween(lat1, lon1, lat2, lon2, results);

                // Convert meters to KM
                double distanceInKm = results[0] / 1000.0;
                edDist.setText(String.format(Locale.getDefault(), "%.2f", distanceInKm));
            }
        } catch (Exception e) {
            edDist.setText("0.00");
        }
    }

    private void saveToFranchise() {
        String code = editRouteCode.getText().toString().trim();
        if (TextUtils.isEmpty(code) || TextUtils.isEmpty(adminFranchise)) {
            Toast.makeText(this, "Please select a Route Code first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Route newRoute = new Route();
            newRoute.setRouteCode(code);
            newRoute.setCompany(adminFranchise);
            newRoute.setTerminal1(edT1.getText().toString());
            newRoute.setTerminal2(edT2.getText().toString());
            newRoute.setStops(TextUtils.join(", ", currentStops));
            newRoute.setT1_Coords(selectedT1Coords);
            newRoute.setT2_Coords(selectedT2Coords);

            String cleanDist = edDist.getText().toString().replaceAll("[^\\d.]", "");
            if (cleanDist.isEmpty()) cleanDist = "0";

            newRoute.setBaseFare(parseDouble(edBase.getText().toString()));
            newRoute.setDistance(Double.parseDouble(cleanDist));
            newRoute.setDistanceBands(parseInt(edBands.getText().toString()));
            newRoute.setAdditionalFarePerBand(parseDouble(edAddl.getText().toString()));

            newRoute.setAssignedTransport(spinnerTransport.getText().toString());
            newRoute.setStatus(spinnerStatus.getText().toString());

            FirebaseDatabase.getInstance(DB_URL).getReference("franchise_routes")
                    .child(adminFranchise).child(code).setValue(newRoute)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddNewRoute.this, "Route saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddNewRoute.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Please check all numeric fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStopsDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_manage_stops, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(v);

        RecyclerView rv = v.findViewById(R.id.rvManageStops);
        Button btnAdd = v.findViewById(R.id.btnAddStop);
        Button btnDone = v.findViewById(R.id.btnDoneStops);

        rv.setLayoutManager(new LinearLayoutManager(this));
        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];

        StopsAdapter adapter = new StopsAdapter(currentStops, viewHolder -> {
            if (touchHelper[0] != null) touchHelper[0].startDrag(viewHolder);
        });

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder src, @NonNull RecyclerView.ViewHolder target) {
                int from = src.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                Collections.swap(currentStops, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {}
        };

        touchHelper[0] = new ItemTouchHelper(callback);
        touchHelper[0].attachToRecyclerView(rv);
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(view -> {
            EditText input = new EditText(this);
            input.setHint("Enter landmark name");
            new AlertDialog.Builder(this)
                    .setTitle("Add Landmark")
                    .setView(input)
                    .setPositiveButton("Add", (d, w) -> {
                        String s = input.getText().toString().trim();
                        if (!s.isEmpty()) {
                            currentStops.add(s);
                            adapter.notifyItemInserted(currentStops.size() - 1);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnDone.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private void fetchAdminCompany() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        if (s.exists()) adminFranchise = s.child("company").getValue(String.class);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadMasterRoutes() {
        FirebaseDatabase.getInstance(DB_URL).getReference("routes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        masterRouteList.clear();
                        List<String> codes = new ArrayList<>();
                        for (DataSnapshot ds : s.getChildren()) {
                            Route r = ds.getValue(Route.class);
                            if (r != null && r.getRouteCode() != null) {
                                masterRouteList.add(r);
                                codes.add(r.getRouteCode());
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AddNewRoute.this, android.R.layout.simple_dropdown_item_1line, codes);
                        editRouteCode.setAdapter(adapter);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }
}