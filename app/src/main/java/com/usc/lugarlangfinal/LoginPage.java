package com.usc.lugarlangfinal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginPage extends AppCompatActivity {

    private Button btnLogin;
    private EditText edusername, edpassword;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    // Your specific database URL
    private final String DB_URL = "https://lugarlangfinal-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("employee");

        // Finding the IDs
        btnLogin = findViewById(R.id.btnlogin);
        edusername = findViewById(R.id.edusername);
        edpassword = findViewById(R.id.edpassword);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String username = edusername.getText().toString().trim();
        String inputPassword = edpassword.getText().toString().trim();

        // Basic Validation
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(inputPassword)) {
            Toast.makeText(this, "Please enter both name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Search for the employee by name in the database
        dbRef.orderByChild("name").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {

                        // Handle password as either String or Number from DB
                        Object dbPassObj = ds.child("password").getValue();
                        String dbPassword = (dbPassObj != null) ? String.valueOf(dbPassObj) : "";

                        String email = ds.child("email").getValue(String.class);
                        String role = ds.child("role").getValue(String.class);

                        // 2. Compare the passwords
                        if (dbPassword.equals(inputPassword)) {
                            if (email != null) {
                                // 3. If password matches, sign in to Firebase Auth session
                                signInWithFirebase(email, inputPassword, role);
                            } else {
                                showErrorDialog("Login Error", "No email found for this user in the database.");
                            }
                        } else {
                            showErrorDialog("Login Failed", "Incorrect password. Please try again.");
                            edpassword.setText("");
                        }
                        return; // Exit after first match
                    }
                } else {
                    showErrorDialog("Access Denied", "User not found in our records.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showErrorDialog("Database Error", error.getMessage());
            }
        });
    }

    private void signInWithFirebase(String email, String password, String role) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 4. Redirect based on Role
                        Intent intent;
                        if ("Admin".equalsIgnoreCase(role)) {
                            intent = new Intent(LoginPage.this, AdminDashboard.class);
                        } else {
                            intent = new Intent(LoginPage.this, MainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        // 5. Use the pop-up box for Auth errors
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        showErrorDialog("Authentication Failed", errorMsg);
                    }
                });
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(LoginPage.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }
}