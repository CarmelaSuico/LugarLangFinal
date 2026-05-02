package com.usc.lugarlangfinal.employee;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddNewEmployee extends AppCompatActivity {

    // UI Components
    private AutoCompleteTextView editEmployeeID, spinnerRole, spinnerUnit, spinnerStatus;
    private TextInputEditText editFulName, editEmail, editLicense, editContact, editAddress, editDefaultPassword, editFranchise;
    private Button btnAdd;
    private LinearLayout btnEmployeeDashboard, btnAddnewEmployee, btnBack;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private List<Employee> fullEmployeeList = new ArrayList<>();
    private List<String> idSuggestions = new ArrayList<>();
    private FirebaseAuth mAuth;
    private String adminFranchise = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_employee);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSpinners();
        fetchAdminFranchiseAndSyncIDs();

        // 1. Navbar State
        btnAddnewEmployee.setSelected(true);

        // 2. ID Selection Listener
        editEmployeeID.setOnItemClickListener((parent, view, position, id) -> {
            String selectedID = (String) parent.getItemAtPosition(position);
            autoFillForm(selectedID);
        });

        // 3. Save Button Listener
        btnAdd.setOnClickListener(v -> saveOrUpdateEmployee());

        // 4. Navigation Listeners
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboard.class));
            finish();
        });

        btnEmployeeDashboard.setOnClickListener(v -> {
            // Adjust this if your management activity is named differently
            startActivity(new Intent(this, EmployeeManagement.class));
            finish();
        });
    }

    private void initViews() {
        editEmployeeID = findViewById(R.id.editEmployeeID);
        editFulName = findViewById(R.id.edfullname);
        editEmail = findViewById(R.id.editEmail);
        editLicense = findViewById(R.id.editLicense);
        editContact = findViewById(R.id.editContact);
        editAddress = findViewById(R.id.editAddress);
        editDefaultPassword = findViewById(R.id.editDefaultPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        editFranchise = findViewById(R.id.edfranchise);
        btnAdd = findViewById(R.id.btnAddEmployee);

        btnEmployeeDashboard = findViewById(R.id.btnemployeedashboard);
        btnAddnewEmployee = findViewById(R.id.btnaddnewemployee);
        btnBack = findViewById(R.id.btnback);

        editFranchise.setEnabled(false);
    }

    private void setupSpinners() {
        String[] statusArr = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statusArr));
        spinnerStatus.setText(statusArr[0], false); // Default to Active

        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Driver", "Conductor"}));
        spinnerUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"None", "Bus", "Jeepney"}));
    }

    private void fetchAdminFranchiseAndSyncIDs() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(user.getUid());
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    adminFranchise = snapshot.child("company").getValue(String.class);
                    if (adminFranchise != null) {
                        editFranchise.setText(adminFranchise);
                        loadDeactiveOrNewSuggestions(adminFranchise);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadDeactiveOrNewSuggestions(String franchise) {
        DatabaseReference empRef = FirebaseDatabase.getInstance(DB_URL).getReference("employee");
        empRef.orderByChild("Franchise").equalTo(franchise).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullEmployeeList.clear();
                idSuggestions.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Employee emp = ds.getValue(Employee.class);
                    if (emp != null) {
                        String status = emp.getStatus();
                        // Suggest only deactive or fresh entries for reactivation/re-registration
                        if (status == null || status.equalsIgnoreCase("Deactive") || status.equalsIgnoreCase("Inactive")) {
                            fullEmployeeList.add(emp);
                            idSuggestions.add(emp.getId());
                        }
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddNewEmployee.this, android.R.layout.simple_dropdown_item_1line, idSuggestions);
                editEmployeeID.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void autoFillForm(String id) {
        for (Employee emp : fullEmployeeList) {
            if (emp.getId().equals(id)) {
                editFulName.setText(emp.getName());
                editEmail.setText(emp.getEmail());
                editLicense.setText(emp.getLicenseNumber());
                editContact.setText(emp.getContactNumber());
                editAddress.setText(emp.getAddress());
                spinnerRole.setText(emp.getRole(), false);
                spinnerStatus.setText("Active", false);
                editDefaultPassword.setText(emp.getPassword());
                btnAdd.setText("Reactivate Employee");
                break;
            }
        }
    }

    private void saveOrUpdateEmployee() {
        String id = editEmployeeID.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editDefaultPassword.getText().toString().trim();
        String role = spinnerRole.getText().toString();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        Employee existing = null;
        for (Employee e : fullEmployeeList) {
            if (e.getId().equals(id)) {
                existing = e;
                break;
            }
        }

        if (existing != null && existing.getAuthUID() != null && !existing.getAuthUID().isEmpty()) {
            updateFirebaseData(id, existing.getAuthUID(), "Employee Reactivated!");
        } else {
            if (password.length() < 6) {
                Toast.makeText(this, "Password too short!", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    updateFirebaseData(id, task.getResult().getUser().getUid(), "Employee Registered!");
                } else {
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFirebaseData(String empID, String authUID, String successMsg) {
        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL).getReference();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", editFulName.getText().toString().trim());
        updates.put("email", editEmail.getText().toString().trim());
        updates.put("role", spinnerRole.getText().toString());
        updates.put("status", spinnerStatus.getText().toString());
        updates.put("licenseNumber", editLicense.getText().toString().trim());
        updates.put("contactNumber", editContact.getText().toString().trim());
        updates.put("address", editAddress.getText().toString().trim());
        updates.put("AssignedUnit", spinnerUnit.getText().toString());
        updates.put("Franchise", adminFranchise);
        updates.put("password", editDefaultPassword.getText().toString());
        updates.put("authUID", authUID);
        updates.put("id", empID);

        root.child("employee").child(empID).updateChildren(updates).addOnSuccessListener(aVoid -> {
            String rolePath = spinnerRole.getText().toString().toLowerCase() + "s";
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("name", editFulName.getText().toString().trim());
            roleMap.put("company", adminFranchise);
            roleMap.put("id", empID);

            root.child(rolePath).child(authUID).updateChildren(roleMap).addOnSuccessListener(unused -> {
                Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}