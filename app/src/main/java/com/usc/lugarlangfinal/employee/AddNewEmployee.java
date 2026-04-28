package com.usc.lugarlangfinal.employee;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.AdminDashboard;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddNewEmployee extends AppCompatActivity {

    AutoCompleteTextView editEmployeeID;
    EditText editFulName, editEmail, editLicense, editContact, editAddress, editDefaultPassword, editFranchise;
    Spinner spinnerRole, spinnerUnit, spinnerStatus;
    Button btnAdd;
    LinearLayout btnEmployeeDashboard, btnAddnewEmployee, btnBack;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private List<Employee> fullEmployeeList = new ArrayList<>();
    private List<String> idSuggestions = new ArrayList<>();

    // Auth instance for creating accounts
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_employee);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSpinners();

        // Bottom Navigation
        btnAddnewEmployee.setSelected(true);
        btnBack.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboard.class)));
        btnEmployeeDashboard.setOnClickListener(v -> finish());
        fetchAdminFranchiseAndSyncIDs();

        editEmployeeID.setOnItemClickListener((parent, view, position, id) -> {
            String selectedID = (String) parent.getItemAtPosition(position);
            autoFillForm(selectedID);
        });

        btnAdd.setOnClickListener(v -> saveOrUpdateEmployee());
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
    }

    private void setupSpinners() {
        String[] statusOptions = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions));
        String[] roles = {"Driver", "Conductor"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));
        String[] units = {"Office", "Bus", "Jeepney"};
        spinnerUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units));
    }

    private void fetchAdminFranchiseAndSyncIDs() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(currentUser.getUid());
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String franchise = snapshot.child("company").getValue(String.class);
                    if (franchise != null) loadEmployeesForSuggestions(franchise);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadEmployeesForSuggestions(String franchise) {
        DatabaseReference empRef = FirebaseDatabase.getInstance(DB_URL).getReference("employee");
        Query query = empRef.orderByChild("Franchise").equalTo(franchise);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullEmployeeList.clear();
                idSuggestions.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Employee emp = ds.getValue(Employee.class);
                    // Only suggest if they don't have a role assigned yet (to avoid double registration)
                    if (emp != null && !ds.hasChild("role")) {
                        fullEmployeeList.add(emp);
                        idSuggestions.add(emp.getId());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddNewEmployee.this,
                        android.R.layout.simple_dropdown_item_1line, idSuggestions);
                editEmployeeID.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void autoFillForm(String id) {
        for (Employee emp : fullEmployeeList) {
            if (emp.getId() != null && emp.getId().equals(id)) {
                editFulName.setText(emp.getName());
                editEmail.setText(emp.getEmail());
                editLicense.setText(emp.getLicenseNumber());
                editContact.setText(emp.getContactNumber());
                editAddress.setText(emp.getAddress());
                editFranchise.setText(emp.getFranchise());
                break;
            }
        }
    }

    private void saveOrUpdateEmployee() {
        String id = editEmployeeID.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editDefaultPassword.getText().toString().trim();
        String name = editFulName.getText().toString().trim();
        String franchise = editFranchise.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (id.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in ID, Email, and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        // STEP 1: Create the User in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userUID = task.getResult().getUser().getUid();
                        updateEmployeeDatabase(id, userUID, name, email, franchise, role, password);
                    } else {
                        Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmployeeDatabase(String empID, String authUID, String name, String email, String franchise, String role, String password) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance(DB_URL).getReference();

        // Data for the 'employee' node
        Map<String, Object> empUpdates = new HashMap<>();
        empUpdates.put("role", role);
        empUpdates.put("AssignedUnit", spinnerUnit.getSelectedItem().toString());
        empUpdates.put("status", spinnerStatus.getSelectedItem().toString());
        empUpdates.put("password", password);
        empUpdates.put("authUID", authUID); // Link them to their Auth ID

        // Data for the 'drivers' or 'conductors' node
        Map<String, Object> roleData = new HashMap<>();
        roleData.put("name", name);
        roleData.put("email", email);
        roleData.put("company", franchise);

        // Path based on role: "drivers" or "conductors"
        String rolePath = role.toLowerCase() + "s";

        // Run multi-path update
        rootRef.child("employee").child(empID).updateChildren(empUpdates);
        rootRef.child(rolePath).child(authUID).setValue(roleData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, role + " Registered Successfully!", Toast.LENGTH_SHORT).show();
                    clearFormFields();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFormFields() {
        editEmployeeID.setText("");
        editFulName.setText("");
        editEmail.setText("");
        editLicense.setText("");
        editContact.setText("");
        editAddress.setText("");
        editFranchise.setText("");
        editDefaultPassword.setText("");
        editEmployeeID.requestFocus();
    }
}