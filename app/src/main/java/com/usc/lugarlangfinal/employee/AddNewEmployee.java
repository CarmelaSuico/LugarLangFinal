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
import java.util.List;

public class AddNewEmployee extends AppCompatActivity {

    AutoCompleteTextView editEmployeeID;
    EditText editFulName, editEmail, editLicense, editContact, editAddress, editDefaultPassword, editFranchise;
    Spinner spinnerRole, spinnerUnit, spinnerStatus;
    Button btnAdd;
    //navigation buttons
    LinearLayout btnEmployeeDashboard, btnAddnewEmployee, btnBack;

    // Fixed DB_URL to match EmployeeManagement.java
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private List<Employee> fullEmployeeList = new ArrayList<>(); // Stores employees for searching
    private List<String> idSuggestions = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_employee);

        //finding the ids
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

        //nav bottom buttons
        btnAddnewEmployee.setSelected(true);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddNewEmployee.this, AdminDashboard.class);
            startActivity(intent);
        });
        btnEmployeeDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(AddNewEmployee.this, EmployeeManagement.class);
            startActivity(intent);
        });

        // Load Admin Franchise and sync IDs
        fetchAdminFranchiseAndSyncIDs();

        // Automation: When an ID is selected from the dropdown
        editEmployeeID.setOnItemClickListener((parent, view, position, id) -> {
            String selectedID = (String) parent.getItemAtPosition(position);
            autoFillForm(selectedID);
        });

        //spinner input
        String[] statusOptions = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions));
        String[] roles = {"Driver", "Conductor", "Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));
        String[] units = {"Office", "Bus", "Jeepney"};
        spinnerUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units));

        btnAdd.setOnClickListener(v -> saveOrUpdateEmployee());

    }

    private void fetchAdminFranchiseAndSyncIDs() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminUID = currentUser.getUid();
        DatabaseReference adminRef = FirebaseDatabase.getInstance(DB_URL).getReference("admins").child(adminUID);

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String franchise = snapshot.child("company").getValue(String.class);
                    if (franchise != null) {
                        loadEmployeesForSuggestions(franchise);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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

                    // CRITICAL CHECK: Does this record already have a password?
                    boolean hasPassword = ds.hasChild("password");

                    if (emp != null && emp.getId() != null) {
                        // Only show in the "Add New" list if they don't have a password yet
                        if (!hasPassword) {
                            fullEmployeeList.add(emp);
                            idSuggestions.add(emp.getId());
                        }
                    }
                }

                // Update the AutoComplete Adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddNewEmployee.this,
                        android.R.layout.simple_dropdown_item_1line, idSuggestions);
                editEmployeeID.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddNewEmployee.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

                Toast.makeText(this, "Data Loaded for ID: " + id, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void saveOrUpdateEmployee() {
        String id = editEmployeeID.getText().toString().trim();
        String password = editDefaultPassword.getText().toString().trim();

        // Validation to ensure Admin doesn't save empty data
        if (id.isEmpty()) {
            editEmployeeID.setError("Please search and select an ID");
            return;
        }

        if (password.isEmpty()) {
            editDefaultPassword.setError("Please set a temporary password");
            return;
        }

        // Pointing to the specific employee ID in the database
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference("employee").child(id);

        // Updating the profile
        ref.child("role").setValue(spinnerRole.getSelectedItem().toString());
        ref.child("AssignedUnit").setValue(spinnerUnit.getSelectedItem().toString());
        ref.child("status").setValue(spinnerStatus.getSelectedItem().toString());
        ref.child("password").setValue(password)
                .addOnSuccessListener(aVoid -> {
                    // SUCCESS: This is where you notify the Admin
                    Toast.makeText(this, "Employee Profile Updated!", Toast.LENGTH_SHORT).show();

                    // Clear the form for the next entry
                    clearFormFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Function to reset the UI after adding
    private void clearFormFields() {
        editEmployeeID.setText("");
        editFulName.setText("");
        editEmail.setText("");
        editLicense.setText("");
        editContact.setText("");
        editAddress.setText("");
        editFranchise.setText("");
        editDefaultPassword.setText("");

        // Reset Spinners to first item
        spinnerRole.setSelection(0);
        spinnerUnit.setSelection(0);
        spinnerStatus.setSelection(0);

        // Move focus back to ID search
        editEmployeeID.requestFocus();
    }
}
