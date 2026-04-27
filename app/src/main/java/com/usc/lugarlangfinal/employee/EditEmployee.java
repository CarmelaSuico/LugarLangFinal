package com.usc.lugarlangfinal.employee;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.usc.lugarlangfinal.R;
import com.usc.lugarlangfinal.models.Employee;

public class EditEmployee extends AppCompatActivity {

    private EditText editEmployeeID, editFulName, editEmail, editLicense, editContact, editAddress, editFranchise;
    private Spinner spinnerRole, spinnerUnit, spinnerStatus;
    private Button btnSaveEdit;
    private ImageButton btnBack;

    private String employeeID;
    private DatabaseReference ref;

    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_employee);

        // 1. Initialize Views
        initViews();

        // 2. Get ID from Intent
        employeeID = getIntent().getStringExtra("EMPLOYEE_ID");

        if (employeeID == null) {
            Toast.makeText(this, "Error: Employee ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSpinners();

        // 3. Setup Firebase Reference (Singular "employee" to match your DB)
        ref = FirebaseDatabase.getInstance(DB_URL).getReference("employee").child(employeeID);

        // 4. Load Existing Data
        loadEmployeeData();

        // 5. Button Listeners
        btnSaveEdit.setOnClickListener(v -> saveOrUpdateEmployee());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        editEmployeeID = findViewById(R.id.editEmployeeID);
        editFulName = findViewById(R.id.edfullname);
        editEmail = findViewById(R.id.editEmail);
        editLicense = findViewById(R.id.editLicense);
        editContact = findViewById(R.id.editContact);
        editAddress = findViewById(R.id.editAddress);
        editFranchise = findViewById(R.id.edfranchise);

        // Make ID and Franchise read-only during edit to maintain database integrity
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
                        editEmployeeID.setText(employee.getId());
                        editFulName.setText(employee.getName());
                        editEmail.setText(employee.getEmail());
                        editLicense.setText(employee.getLicenseNumber());
                        editContact.setText(employee.getContactNumber());
                        editAddress.setText(employee.getAddress());
                        editFranchise.setText(employee.getFranchise());

                        setSpinnerValue(spinnerRole, employee.getRole());
                        setSpinnerValue(spinnerUnit, employee.getAssignedUnit());
                        setSpinnerValue(spinnerStatus, employee.getStatus());
                    }
                } else {
                    Toast.makeText(EditEmployee.this, "No data found for this ID", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorDialog("Database Error", error.getMessage());
            }
        });
    }

    private void saveOrUpdateEmployee(){
        // Validation: Ensure full name isn't empty
        if(editFulName.getText().toString().trim().isEmpty()){
            editFulName.setError("Name is required");
            return;
        }

        // Creating the updated object with ALL 11 fields (including password)
        Employee updateEmployee = new Employee(
                editEmployeeID.getText().toString(),
                editFulName.getText().toString(),
                spinnerRole.getSelectedItem().toString(),
                spinnerUnit.getSelectedItem().toString(),
                editFranchise.getText().toString(),
                editEmail.getText().toString(),
                editLicense.getText().toString(),
                editContact.getText().toString(),
                editAddress.getText().toString(),
                spinnerStatus.getSelectedItem().toString(),
                "" // Empty password
        );

        // Save back to Firebase
        ref.setValue(updateEmployee).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            showErrorDialog("Update Failed", e.getMessage());
        });
    }

    private void setupSpinners() {
        String[] statusOptions = {"Active", "Deactive"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions));

        String[] roles = {"Driver", "Conductor", "Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));

        String[] units = {"Office", "Bus", "Jeepney"};
        spinnerUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units));
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}