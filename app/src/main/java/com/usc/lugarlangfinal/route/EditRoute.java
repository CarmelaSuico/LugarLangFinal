package com.usc.lugarlangfinal.route;

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
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.StopsAdapter;
import com.usc.lugarlangfinal.models.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditRoute extends AppCompatActivity {

    private AutoCompleteTextView editRouteCode;
    private EditText edT1, edT2, edDist, edBase, edBands, edAddl;
    private Spinner spinnerTransport, spinnerStatus;
    private Button btnSaveEdit;
    private ImageButton btnBack, btnManageStops;

    private String adminFranchise = "";
    private String selectedRouteCode = "";
    private List<String> currentStops = new ArrayList<>();
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_route);

        // 1. Initialize UI
        initViews();

        // 2. Get Data from Intent
        selectedRouteCode = getIntent().getStringExtra("ROUTE_CODE");

        // 3. Setup Spinners
        setupSpinners();

        // 4. Fetch Admin Info then Load Data
        fetchAdminAndLoadData();

        // 5. Button Listeners
        btnBack.setOnClickListener(v -> finish());
        btnManageStops.setOnClickListener(v -> showStopsDialog());
        btnSaveEdit.setOnClickListener(v -> saveChanges());
    }

    private void initViews() {
        editRouteCode = findViewById(R.id.editroutecode);
        editRouteCode.setEnabled(false); // Keep Route Code locked during edit
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

    private void fetchAdminAndLoadData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            adminFranchise = snapshot.child("company").getValue(String.class);
                            loadExistingRouteData();
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
                    edDist.setText(String.format("%.2f", route.getDistance()));
                    edBase.setText(String.valueOf(route.getBaseFare()));
                    edBands.setText(String.valueOf(route.getDistanceBands()));
                    edAddl.setText(String.valueOf(route.getAdditionalFarePerBand()));

                    setSpinnerValue(spinnerTransport, route.getAssignedTransport());
                    setSpinnerValue(spinnerStatus, route.getStatus());

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

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("franchise_routes")
                .child(adminFranchise)
                .child(selectedRouteCode);

        // Update object
        Route updatedRoute = new Route();
        updatedRoute.setRouteCode(selectedRouteCode);
        updatedRoute.setCompany(adminFranchise);
        updatedRoute.setTerminal1(edT1.getText().toString());
        updatedRoute.setTerminal2(edT2.getText().toString());
        updatedRoute.setStops(String.join(", ", currentStops));
        updatedRoute.setBaseFare(Double.parseDouble(edBase.getText().toString()));
        updatedRoute.setDistance(Double.parseDouble(edDist.getText().toString().replace(" km", "")));
        updatedRoute.setDistanceBands(Integer.parseInt(edBands.getText().toString()));
        updatedRoute.setAdditionalFarePerBand(Double.parseDouble(edAddl.getText().toString()));
        updatedRoute.setAssignedTransport(spinnerTransport.getSelectedItem().toString());
        updatedRoute.setStatus(spinnerStatus.getSelectedItem().toString());

        ref.setValue(updatedRoute).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Route Updated Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showStopsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_manage_stops, null);
        dialog.setContentView(v);

        RecyclerView rv = v.findViewById(R.id.rvManageStops);
        rv.setLayoutManager(new LinearLayoutManager(this));

        final ItemTouchHelper[] itemTouchHelperWrapper = new ItemTouchHelper[1];

        StopsAdapter adapter = new StopsAdapter(currentStops, viewHolder -> {
            if (itemTouchHelperWrapper[0] != null) itemTouchHelperWrapper[0].startDrag(viewHolder);
        });

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                Collections.swap(currentStops, vh.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                adapter.notifyItemMoved(vh.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {}
        };

        itemTouchHelperWrapper[0] = new ItemTouchHelper(callback);
        itemTouchHelperWrapper[0].attachToRecyclerView(rv);
        rv.setAdapter(adapter);
        dialog.show();
    }

    private void setupSpinners() {
        String[] transport = {"Bus", "Jeepney"};
        String[] status = {"Active", "Deactive"};
        spinnerTransport.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, transport));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, status));
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int pos = adapter.getPosition(value);
        spinner.setSelection(pos);
    }
}