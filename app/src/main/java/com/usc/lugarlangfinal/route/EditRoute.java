package com.usc.lugarlangfinal.route;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.StopsAdapter;
import com.usc.lugarlangfinal.models.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EditRoute extends AppCompatActivity {

    // Material 3 UI Components
    private TextInputEditText editRouteCode, edT1, edT2, edDist, edBase, edBands, edAddl;
    private AutoCompleteTextView spinnerTransport, spinnerStatus;
    private MaterialButton btnSaveEdit, btnManageStops;
    private ImageButton btnBack;

    private String adminFranchise = "";
    private String selectedRouteCode = "";
    private String t1Coords = "", t2Coords = "";
    private List<String> currentStops = new ArrayList<>();
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_route);

        initViews();

        selectedRouteCode = getIntent().getStringExtra("ROUTE_CODE");
        if (selectedRouteCode == null) {
            Toast.makeText(this, "Error: Route data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupDropdowns();
        fetchAdminAndLoadData();

        btnBack.setOnClickListener(v -> finish());
        btnManageStops.setOnClickListener(v -> showStopsDialog());
        btnSaveEdit.setOnClickListener(v -> saveChanges());
    }

    private void initViews() {
        editRouteCode = findViewById(R.id.editroutecode);
        editRouteCode.setEnabled(false); // Code is the primary key; don't change it here

        edT1 = findViewById(R.id.edterminal1);
        edT2 = findViewById(R.id.edterminal2);
        edDist = findViewById(R.id.eddistance);
        edBase = findViewById(R.id.edbasefare);
        edBands = findViewById(R.id.eddistancebands);
        edAddl = findViewById(R.id.edadditionalfare);

        spinnerTransport = findViewById(R.id.spinnerassignedtranport);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        btnSaveEdit = findViewById(R.id.btneditroute);
        btnBack = findViewById(R.id.btnback);
        btnManageStops = findViewById(R.id.btnAddStopField);
    }

    private void setupDropdowns() {
        String[] transport = {"Bus", "Jeepney", "Modern Jeepney"};
        spinnerTransport.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transport));

        String[] status = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, status));
    }

    private void fetchAdminAndLoadData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            adminFranchise = snapshot.child("company").getValue(String.class);
                            if (adminFranchise != null) loadExistingRouteData();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadExistingRouteData() {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("franchise_routes")
                .child(adminFranchise)
                .child(selectedRouteCode);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Route route = snapshot.getValue(Route.class);
                if (route != null) {
                    editRouteCode.setText(route.getRouteCode());
                    edT1.setText(route.getTerminal1());
                    edT2.setText(route.getTerminal2());
                    edDist.setText(String.format(Locale.getDefault(), "%.2f", route.getDistance()));
                    edBase.setText(String.valueOf(route.getBaseFare()));
                    edBands.setText(String.valueOf(route.getDistanceBands()));
                    edAddl.setText(String.valueOf(route.getAdditionalFarePerBand()));

                    spinnerTransport.setText(route.getAssignedTransport(), false);
                    spinnerStatus.setText(route.getStatus(), false);

                    t1Coords = route.getT1_Coords();
                    t2Coords = route.getT2_Coords();

                    currentStops.clear();
                    if (route.getStops() != null && !route.getStops().isEmpty()) {
                        Collections.addAll(currentStops, route.getStops().split(", "));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveChanges() {
        if (adminFranchise.isEmpty()) return;

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                    .getReference("franchise_routes")
                    .child(adminFranchise)
                    .child(selectedRouteCode);

            Route updatedRoute = new Route();
            updatedRoute.setRouteCode(selectedRouteCode);
            updatedRoute.setCompany(adminFranchise);
            updatedRoute.setTerminal1(edT1.getText().toString());
            updatedRoute.setTerminal2(edT2.getText().toString());
            updatedRoute.setStops(TextUtils.join(", ", currentStops));
            updatedRoute.setT1_Coords(t1Coords);
            updatedRoute.setT2_Coords(t2Coords);

            updatedRoute.setBaseFare(Double.parseDouble(edBase.getText().toString()));
            updatedRoute.setDistance(Double.parseDouble(edDist.getText().toString().replaceAll("[^\\d.]", "")));
            updatedRoute.setDistanceBands(Integer.parseInt(edBands.getText().toString()));
            updatedRoute.setAdditionalFarePerBand(Double.parseDouble(edAddl.getText().toString()));
            updatedRoute.setAssignedTransport(spinnerTransport.getText().toString());
            updatedRoute.setStatus(spinnerStatus.getText().toString());

            ref.setValue(updatedRoute).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Please check all numeric fields", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStopsDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_manage_stops, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(v);

        RecyclerView rv = v.findViewById(R.id.rvManageStops);
        rv.setLayoutManager(new LinearLayoutManager(this));

        final ItemTouchHelper[] touchHelper = new ItemTouchHelper[1];
        StopsAdapter adapter = new StopsAdapter(currentStops, vh -> {
            if (touchHelper[0] != null) touchHelper[0].startDrag(vh);
        });

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder src, @NonNull RecyclerView.ViewHolder target) {
                Collections.swap(currentStops, src.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                adapter.notifyItemMoved(src.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder v, int d) {}
        };

        touchHelper[0] = new ItemTouchHelper(callback);
        touchHelper[0].attachToRecyclerView(rv);
        rv.setAdapter(adapter);

        // Add Landmark in Dialog
        v.findViewById(R.id.btnAddStop).setOnClickListener(view -> {
            EditText input = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Add Landmark")
                    .setView(input)
                    .setPositiveButton("Add", (d, w) -> {
                        String s = input.getText().toString().trim();
                        if (!s.isEmpty()) {
                            currentStops.add(s);
                            adapter.notifyItemInserted(currentStops.size() - 1);
                        }
                    }).show();
        });

        v.findViewById(R.id.btnDoneStops).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }
}
