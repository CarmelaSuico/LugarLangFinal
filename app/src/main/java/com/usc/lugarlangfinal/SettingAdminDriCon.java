package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingAdminDriCon extends AppCompatActivity {

    private MaterialButton btnBack, btnLogout;
    private LinearLayout btnAccount, btnDisplay, btnLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_admin_dri_con);

        btnBack = findViewById(R.id.btnadminback);
        btnLogout = findViewById(R.id.btnlogout);
        btnAccount = findViewById(R.id.btnSettingAccount);
        btnDisplay = findViewById(R.id.btnSettingDisplay);
        btnLanguage = findViewById(R.id.btnSettingLanguage);



        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(SettingAdminDriCon.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // IMPORTANT: Ensure "LoginPage" is the correct name of your login activity
            Intent intent = new Intent(SettingAdminDriCon.this, LoginPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //
            startActivity(intent);
            finish();
        });
    }
}