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

        // 4. Ticket Management (Revenue/Sales)
//        btnTicket.setOnClickListener(v -> {
//             Intent intent = new Intent(AdminDashboard.this, Ticketing.class);
//             startActivity(intent);
//            showToast("Opening Loging Management...");
//        });

        // 5. Admin Settings / Logout
//        btnSettings.setOnClickListener(v -> {
//            // Usually, admins want a quick way to logout or change password
//            showLogoutDialog();
//        });
    }

//    private void showLogoutDialog() {
//        new androidx.appcompat.app.AlertDialog.Builder(this)
//                .setTitle("Admin Settings")
//                .setMessage("Do you want to logout of the system?")
//                .setPositiveButton("Logout", (dialog, which) -> {
//                    // FirebaseAuth.getInstance().signOut();
//                    Intent intent = new Intent(AdminDashboard.this, LoginPage.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//                })
//                .setNegativeButton("Cancel", null)
//                .show();
//    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}