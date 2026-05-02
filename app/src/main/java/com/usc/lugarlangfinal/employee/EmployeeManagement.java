package com.usc.lugarlangfinal.employee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.usc.lugarlangfinal.adapters.EmployeeAdapter;
import com.usc.lugarlangfinal.models.Employee;

import java.util.ArrayList;
import java.util.List;

public class EmployeeManagement extends AppCompatActivity {

    private LinearLayout btnAddnewEmployee, btnBack, BtnEmployeeDashboard;
    private RecyclerView rvEmployeeList;
    private EmployeeAdapter employeeAdapter;
    private List<Employee> employeeList;
    private List<Employee> filteredList;
    private SearchView searchEmployee;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_management);

        initViews();

        employeeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Initialize adapter
        employeeAdapter = new EmployeeAdapter(filteredList);
        rvEmployeeList.setLayoutManager(new LinearLayoutManager(this));
        rvEmployeeList.setAdapter(employeeAdapter);

        setupSearch();
        getAdminFranchiseAndLoadEmployees();

        // 1. Set Navigation State
        BtnEmployeeDashboard.setSelected(true);

        // 2. Navigation Listeners
        btnBack.setOnClickListener(v -> finish()); // Clean back navigation

        btnAddnewEmployee.setOnClickListener(v -> {
            startActivity(new Intent(this, AddNewEmployee.class));
            // Don't finish here, so user can press 'back' to return to list
        });
    }

    private void initViews() {
        btnAddnewEmployee = findViewById(R.id.btnaddnewemployee);
        btnBack = findViewById(R.id.btnback);
        rvEmployeeList = findViewById(R.id.rvEmployeeList);
        searchEmployee = findViewById(R.id.searchEmployee);
        BtnEmployeeDashboard = findViewById(R.id.btnemployeedashboard);
    }

    private void setupSearch() {
        searchEmployee.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void getAdminFranchiseAndLoadEmployees() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("admins").child(uid);

        adminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadAdminAuthorizedEmployees(adminFranchise);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Admin fetch failed: " + error.getMessage());
            }
        });
    }

    private void loadAdminAuthorizedEmployees(String adminFranchise) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("employee");

        // Query by "Franchise" key as defined in your Model/Database
        ref.orderByChild("Franchise").equalTo(adminFranchise)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        employeeList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Employee emp = ds.getValue(Employee.class);

                            // Optional: If you want to show ALL employees (Active & Deactive),
                            // remove the getStatus().equalsIgnoreCase check.
                            if (emp != null && emp.getStatus() != null &&
                                    emp.getStatus().equalsIgnoreCase("Active")) {
                                employeeList.add(emp);
                            }
                        }
                        filter(searchEmployee.getQuery().toString());
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error: " + error.getMessage());
                    }
                });
    }

    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();

        if (query.isEmpty()) {
            filteredList.addAll(employeeList);
        } else {
            for (Employee item : employeeList) {
                String name = item.getName() != null ? item.getName().toLowerCase() : "";
                String id = item.getId() != null ? item.getId().toLowerCase() : "";

                if (name.contains(query) || id.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        employeeAdapter.notifyDataSetChanged();
    }
}