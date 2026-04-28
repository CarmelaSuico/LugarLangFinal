package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.usc.lugarlangfinal.Transportation.TransportationManagement;
import com.usc.lugarlangfinal.employee.EmployeeManagement;
import com.usc.lugarlangfinal.route.RouteManagement;

public class AdminDashboard extends AppCompatActivity {

    ImageButton btnAdminsettings;
    Button btnEmployee, btnRoute, btnTransportation, btnTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        //finding the ids
        btnAdminsettings = findViewById(R.id.btnadminsettings);
        btnEmployee = findViewById(R.id.btnemployee);
        btnRoute = findViewById(R.id.btnroute);
        btnTransportation = findViewById(R.id.btntransportation);
//        btnTicket = findViewById(R.id.btnticket);

        //setting up the nav bottons
        btnEmployee.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, EmployeeManagement.class);
            startActivity(intent);
        });

        btnRoute.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, RouteManagement.class);
            startActivity(intent);
        });

        btnTransportation.setOnClickListener(v->{
            Intent intent = new Intent(AdminDashboard.this, TransportationManagement.class);
            startActivity(intent);
        });

    }
}