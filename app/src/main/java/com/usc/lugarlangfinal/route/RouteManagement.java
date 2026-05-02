package com.usc.lugarlangfinal.route;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.adapters.RouteAdapter;
import com.usc.lugarlangfinal.models.Route;

import java.util.ArrayList;
import java.util.List;

public class RouteManagement extends AppCompatActivity {

    private LinearLayout btnRouteDashboard, btnAddnewRoute, btnBack;
    private RecyclerView rvRoutelist;
    private RouteAdapter routeAdapter;
    private List<Route> routeList;
    private List<Route> filteredList;
    private SearchView searchRoute;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private String adminFranchise = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_management);

        initViews();

        routeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRoutelist.setLayoutManager(new LinearLayoutManager(this));
        routeAdapter = new RouteAdapter(filteredList);
        rvRoutelist.setAdapter(routeAdapter);

        fetchAdminFranchiseAndLoadRoutes();

        searchRoute.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterRoutes(newText);
                return true;
            }
        });

        setupNavigation();
    }

    private void initViews() {
        btnRouteDashboard = findViewById(R.id.btnroutedashboard);
        btnAddnewRoute = findViewById(R.id.btnaddnewroute);
        btnBack = findViewById(R.id.btnback);
        rvRoutelist = findViewById(R.id.rvRoutelist);
        searchRoute = findViewById(R.id.searchRoute);
    }

    private void setupNavigation() {
        btnRouteDashboard.setSelected(true);

        btnAddnewRoute.setOnClickListener(v -> {
            startActivity(new Intent(this, AddNewRoute.class));
        });

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboard.class));
        });

    }

    private void fetchAdminFranchiseAndLoadRoutes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(currentUser.getUid());
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadFranchiseRoutes(adminFranchise);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFranchiseRoutes(String franchiseName) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("franchise_routes").child(franchiseName);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Route route = ds.getValue(Route.class);
                    if (route != null && "Active".equalsIgnoreCase(route.getStatus())) {
                        routeList.add(route);
                    }
                }
                filterRoutes(searchRoute.getQuery().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RouteManagement.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRoutes(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();
        if (query.isEmpty()) {
            filteredList.addAll(routeList);
        } else {
            for (Route item : routeList) {
                String rCode = item.getRouteCode() != null ? item.getRouteCode().toLowerCase() : "";
                String t1 = item.getTerminal1() != null ? item.getTerminal1().toLowerCase() : "";
                String t2 = item.getTerminal2() != null ? item.getTerminal2().toLowerCase() : "";
                if (rCode.contains(query) || t1.contains(query) || t2.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        routeAdapter.notifyDataSetChanged();
    }
}
