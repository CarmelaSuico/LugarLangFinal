package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class Settings extends AppCompatActivity {
    LinearLayout btnHomePage, btnSearch, btnSetting;

    MaterialButton btnPortal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnHomePage = findViewById(R.id.btnhomepage);
        btnSearch = findViewById(R.id.btnsearch);
        btnSetting = findViewById(R.id.btnsetting);
        btnPortal = findViewById(R.id.btnPortal);

        btnSetting.setSelected(true);

        btnHomePage.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, commuterhome.class));
        });

        btnPortal.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, LoginPage.class));
        });
    }
}