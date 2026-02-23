package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddNoticeActivity extends AppCompatActivity {

    private EditText etNoticeTitle, etNoticeDescription, etNoticeLocation, etNoticeDateRange;
    private Button btnPostNotice;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // Add FirebaseAuth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_notice);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        // Initialize Views
        etNoticeTitle = findViewById(R.id.etNoticeTitle);
        etNoticeDescription = findViewById(R.id.etNoticeDescription);
        etNoticeLocation = findViewById(R.id.etNoticeLocation);
        etNoticeDateRange = findViewById(R.id.etNoticeDateRange);
        btnPostNotice = findViewById(R.id.btnPostNotice);

        // Handle Admin Bottom Navigation
        setupBottomNavigation();

        // Set listener for the Post Notice button
        btnPostNotice.setOnClickListener(v -> postNotice());
    }

    private void postNotice() {
        String title = etNoticeTitle.getText().toString().trim();
        String description = etNoticeDescription.getText().toString().trim();
        String location = etNoticeLocation.getText().toString().trim();
        String dateRange = etNoticeDateRange.getText().toString().trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post a notice.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        // Validate that fields are not empty
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new Notice object
        Notice notice = new Notice(title, description, location, dateRange);

        // --- IMPORTANT: SET THE USER ID ---
        notice.setUserId(currentUserId);

        // Add the notice to a "notices" collection in Firestore
        db.collection("notices")
                .add(notice)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddNoticeActivity.this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate back to the admin home screen after posting
                    startActivity(new Intent(AddNoticeActivity.this, AdminDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddNoticeActivity.this, "Error posting notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_add_notice); // Set current item

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), AdminDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_add_notice) {
                // We are already here
                return true;
            } else if (itemId == R.id.nav_duplicates) {
                startActivity(new Intent(getApplicationContext(), DuplicatesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), AdminProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
