package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.usc.lugarlangfinal.Transportation.TransportationManagement;
import com.usc.lugarlangfinal.driverconductor.Ticketing;
import com.usc.lugarlangfinal.employee.EmployeeManagement;
import com.usc.lugarlangfinal.route.RouteManagement;

public class AdminDashboard extends AppCompatActivity {

    private MaterialButton btnSettings, btnEmployee, btnRoute, btnTransportation, btnTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Views
        btnSettings = findViewById(R.id.btnadminsettings);
        btnEmployee = findViewById(R.id.btnemployee);
        btnRoute = findViewById(R.id.btnroute);
        btnTransportation = findViewById(R.id.btntransportation);
        btnTicket = findViewById(R.id.btnticket);

        // --- Navigation Logic ---

        // 1. Employee Management
        btnEmployee.setOnClickListener(v -> {
             Intent intent = new Intent(AdminDashboard.this, EmployeeManagement.class);
             startActivity(intent);
            showToast("Opening Employee Management...");
        });

        // 2. Route Management
        btnRoute.setOnClickListener(v -> {
             Intent intent = new Intent(AdminDashboard.this, RouteManagement.class);
             startActivity(intent);
            showToast("Opening Route Management...");
        });

        // 3. Transportation Management (Fleet/Units)
        btnTransportation.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, TransportationManagement.class);
            startActivity(intent);
            showToast("Opening Transportation Management...");
        });

//        btnTicket.setOnClickListener(v -> {
//             Intent intent = new Intent(AdminDashboard.this, Ticketing.class);
//             startActivity(intent);
//            showToast("Opening Loging Management...");
//        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, SettingAdminDriCon.class);
            startActivity(intent);
            showToast("Opening Admin Settings...");
        });
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}