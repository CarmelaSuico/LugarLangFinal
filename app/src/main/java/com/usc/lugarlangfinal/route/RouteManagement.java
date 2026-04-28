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

    LinearLayout btnRouteDashboard, btnAddnewRoute, btnBack;
    RecyclerView rvRoutelist;
    RouteAdapter routeAdapter;
    List<Route> routeList;
    List<Route> filteredList;
    SearchView searchRoute;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private String adminFranchise = ""; // To store the admin's company

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_management);

        btnRouteDashboard = findViewById(R.id.btnroutedashboard);
        btnAddnewRoute = findViewById(R.id.btnaddnewroute);
        btnBack = findViewById(R.id.btnback);
        rvRoutelist = findViewById(R.id.rvRoutelist);
        searchRoute = findViewById(R.id.searchRoute);

        routeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRoutelist.setLayoutManager(new LinearLayoutManager(this));
        routeAdapter = new RouteAdapter(filteredList);
        rvRoutelist.setAdapter(routeAdapter);

        // 1. First, find out which franchise this admin belongs to
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

        // Navigation
        btnRouteDashboard.setSelected(true);
        btnAddnewRoute.setOnClickListener(v -> startActivity(new Intent(this, AddNewRoute.class)));
        btnBack.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboard.class)));
    }

    private void fetchAdminFranchiseAndLoadRoutes() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(uid);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        // 2. Now that we have the franchise name, load the routes
                        loadFranchiseRoutes(adminFranchise);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFranchiseRoutes(String franchiseName) {
        // Pointing to franchise_routes -> Ceres (or whatever the company is)
        DatabaseReference franchiseRouteRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("franchise_routes")
                .child(franchiseName);

        franchiseRouteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Route route = ds.getValue(Route.class);

                    if (route != null) {
                        // 3. Filter: Only show if Status is "Active"
                        if ("Active".equalsIgnoreCase(route.getStatus())) {
                            routeList.add(route);
                        }
                    }
                }
                filterRoutes(searchRoute.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RouteManagement.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRoutes(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(routeList);
        } else {
            String query = text.toLowerCase().trim();
            for (Route item : routeList) {
                if (item.getRouteCode().toLowerCase().contains(query) ||
                        item.getTerminal1().toLowerCase().contains(query) ||
                        item.getTerminal2().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        routeAdapter.notifyDataSetChanged();
    }
}