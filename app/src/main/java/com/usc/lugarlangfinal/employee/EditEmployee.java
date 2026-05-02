package com.usc.lugarlangfinal.employee;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Employee;

import java.util.HashMap;
import java.util.Map;

public class EditEmployee extends AppCompatActivity {

    private AutoCompleteTextView editEmployeeID;
    private TextInputEditText editFulName, editEmail, editLicense, editContact, editAddress, editFranchise;
    private AutoCompleteTextView spinnerRole, spinnerUnit, spinnerStatus;
    private Button btnSaveEdit;
    private ImageButton btnBack;

    private String employeeID;
    private DatabaseReference ref;
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_employee);

        initViews();
        setupSpinners();

        employeeID = getIntent().getStringExtra("EMPLOYEE_ID");

        if (employeeID == null) {
            Toast.makeText(this, "Error: Employee ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ref = FirebaseDatabase.getInstance(DB_URL).getReference("employee").child(employeeID);
        loadEmployeeData();

        btnSaveEdit.setOnClickListener(v -> saveOrUpdateEmployee());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        // Matching IDs and Material 3 components from activity_edit_employee.xml
        editEmployeeID = findViewById(R.id.editEmployeeID);
        editFulName = findViewById(R.id.edfullname);
        editEmail = findViewById(R.id.editEmail);
        editLicense = findViewById(R.id.editLicense);
        editContact = findViewById(R.id.editContact);
        editAddress = findViewById(R.id.editAddress);
        editFranchise = findViewById(R.id.edfranchise);

        // Lock fields that shouldn't be edited here
        editEmployeeID.setEnabled(false);
        editFranchise.setEnabled(false);

        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSaveEdit = findViewById(R.id.btnsaveedit);
        btnBack = findViewById(R.id.btnback);
    }

    private void loadEmployeeData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Employee employee = snapshot.getValue(Employee.class);
                    if(employee != null){
                        // Use setText(text, false) for AutoCompleteTextView to avoid dropdown popups
                        editEmployeeID.setText(employee.getId(), false);
                        editFulName.setText(employee.getName());
                        editEmail.setText(employee.getEmail());
                        editLicense.setText(employee.getLicenseNumber());
                        editContact.setText(employee.getContactNumber());
                        editAddress.setText(employee.getAddress());
                        editFranchise.setText(employee.getFranchise());

                        spinnerRole.setText(employee.getRole(), false);
                        spinnerUnit.setText(employee.getAssignedUnit(), false);
                        spinnerStatus.setText(employee.getStatus(), false);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveOrUpdateEmployee(){
        String name = editFulName.getText().toString().trim();
        if(name.isEmpty()){
            editFulName.setError("Name is required");
            return;
        }

        // Use a Map to update ONLY specific fields. This prevents deleting the password.
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", editEmail.getText().toString().trim());
        updates.put("licenseNumber", editLicense.getText().toString().trim());
        updates.put("contactNumber", editContact.getText().toString().trim());
        updates.put("address", editAddress.getText().toString().trim());
        updates.put("role", spinnerRole.getText().toString());
        updates.put("AssignedUnit", spinnerUnit.getText().toString());
        updates.put("status", spinnerStatus.getText().toString());

        ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Employee updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            showErrorDialog("Update Failed", e.getMessage());
        });
    }

    private void setupSpinners() {
        // Updated to use ArrayAdapter with AutoCompleteTextView for Material 3 dropdowns
        String[] statusOptions = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statusOptions));

        String[] roles = {"Driver", "Conductor", "Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles));

        String[] units = {"Office", "Bus", "Jeepney"};
        spinnerUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, units));
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
