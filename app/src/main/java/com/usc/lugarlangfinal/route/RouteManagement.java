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
    List<Route> filteredList; // List for searching
    SearchView searchRoute;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private DatabaseReference routeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_management);

        // 1. Finding the IDs
        btnRouteDashboard = findViewById(R.id.btnroutedashboard);
        btnAddnewRoute = findViewById(R.id.btnaddnewroute);
        btnBack = findViewById(R.id.btnback);
        rvRoutelist = findViewById(R.id.rvRoutelist);
        searchRoute = findViewById(R.id.searchRoute);

        // 2. Setting up the Lists and Adapter
        routeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        rvRoutelist.setLayoutManager(new LinearLayoutManager(this));
        // We pass the filteredList to the adapter so search works
        routeAdapter = new RouteAdapter(filteredList);
        rvRoutelist.setAdapter(routeAdapter);

        // 3. Initialize Firebase Reference (Shared 'routes' node)
        routeRef = FirebaseDatabase.getInstance(DB_URL).getReference("routes");

        // 4. Load Data
        loadSharedRoutes();

        // 5. Search Logic
        searchRoute.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRoutes(newText);
                return true;
            }
        });

        // 6. Navigation Buttons
        btnRouteDashboard.setSelected(true);
        btnAddnewRoute.setOnClickListener(v -> {
            Intent intent = new Intent(RouteManagement.this, AdminDashboard.class);
            startActivity(intent);
        });

//        btnAddnewRoute.setOnClickListener(v -> {
//            Intent intent = new Intent(RouteManagement.this, AddNewRoute.class);
//            startActivity(intent);
//        });
    }

    private void loadSharedRoutes() {
        // Listen to the shared 'routes' node
        routeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // 1. Map the data to our Route model
                    Route route = ds.getValue(Route.class);

                    if (route != null) {
                        // 2. Only add to the list if the status is "Active"
                        // We use equalsIgnoreCase to handle "active" or "Active"
                        if ("Active".equalsIgnoreCase(route.getStatus())) {
                            routeList.add(route);
                        }
                    }
                }
                // 3. Sync with the search filter so the UI updates
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
                // Search by Route Code, Terminal 1, or Terminal 2
                if (item.getCode().toLowerCase().contains(query) ||
                        item.getTerminal1().toLowerCase().contains(query) ||
                        item.getTerminal2().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        routeAdapter.notifyDataSetChanged();
    }
}