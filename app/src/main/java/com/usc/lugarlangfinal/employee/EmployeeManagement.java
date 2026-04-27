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

    LinearLayout btnEmployeeDashboard, btnAddnewEmployee, btnBack;
    RecyclerView rvEmployeeList;
    EmployeeAdapter employeeAdapter;
    List<Employee> employeeList;
    SearchView searchEmployee;
    List<Employee> filteredList;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_management);

        // UI IDs
        btnEmployeeDashboard = findViewById(R.id.btnemployeedashboard);
        btnAddnewEmployee = findViewById(R.id.btnaddnewemployee);
        btnBack = findViewById(R.id.btnback);
        rvEmployeeList = findViewById(R.id.rvEmployeeList);
        searchEmployee = findViewById(R.id.searchEmployee);

        // RecyclerView Setup
        rvEmployeeList.setLayoutManager(new LinearLayoutManager(this));
        employeeList = new ArrayList<>();
        employeeAdapter = new EmployeeAdapter(employeeList);
        rvEmployeeList.setAdapter(employeeAdapter);
        filteredList = new ArrayList<>();

        //search function
        employeeAdapter = new EmployeeAdapter(filteredList);
        rvEmployeeList.setAdapter(employeeAdapter);

        // Search Logic
        searchEmployee.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        // Start the Relational Mapping Chain
        getAdminFranchiseAndLoadEmployees();

        // Nav buttons
        btnEmployeeDashboard.setSelected(true);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeManagement.this, AdminDashboard.class));
            finish();
        });
        btnAddnewEmployee.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeManagement.this, AddNewEmployee.class));
        });
    }

    private void getAdminFranchiseAndLoadEmployees() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(adminUID);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        loadAdminAuthorizedEmployees(adminFranchise);
                    }
                } else {
                    Log.e("FirebaseError", "Admin profile not found in database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAdminAuthorizedEmployees(String adminFranchise) {
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("employee");

        ref.orderByChild("Franchise").equalTo(adminFranchise).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                employeeList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.hasChild("password")) {
                        String status = ds.child("status").getValue(String.class);
                        if ("Active".equalsIgnoreCase(status)) {
                            Employee emp = ds.getValue(Employee.class);
                            if (emp != null) {
                                employeeList.add(emp);
                            }
                        }
                    }
                }
                // Sync the filtered list with the new data
                filter(searchEmployee.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void filter(String text) {
        filteredList.clear();

        if (text.isEmpty()) {
            filteredList.addAll(employeeList);
        } else {
            String query = text.toLowerCase().trim();
            for (Employee item : employeeList) {
                // Search by Name or ID
                if (item.getName().toLowerCase().contains(query) ||
                        item.getId().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        employeeAdapter.notifyDataSetChanged();
    }
}