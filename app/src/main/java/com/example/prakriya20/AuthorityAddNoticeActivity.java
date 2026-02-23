package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
// =================== IMPORT THESE TWO CLASSES ===================
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// ================================================================
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthorityAddNoticeActivity extends AppCompatActivity {

    private EditText etNoticeTitle, etNoticeDescription, etNoticeLocation, etNoticeDateRange;
    private Button btnPostNotice;
    private FirebaseFirestore db;
    // =================== ADD THIS VARIABLE ===================
    private FirebaseAuth mAuth;
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We reuse the same layout file as the admin's "Add Notice" screen
        setContentView(R.layout.activity_add_notice);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        // =================== INITIALIZE FIREBASE AUTH ===================
        mAuth = FirebaseAuth.getInstance();
        // ================================================================

        // Initialize Views from the layout
        etNoticeTitle = findViewById(R.id.etNoticeTitle);
        etNoticeDescription = findViewById(R.id.etNoticeDescription);
        etNoticeLocation = findViewById(R.id.etNoticeLocation);
        etNoticeDateRange = findViewById(R.id.etNoticeDateRange);
        btnPostNotice = findViewById(R.id.btnPostNotice);

        // Handle Authority Bottom Navigation
        setupBottomNavigation();

        // Set listener for the Post Notice button
        btnPostNotice.setOnClickListener(v -> postNotice());
    }

    // ==========================================================================================
    // =================== THIS IS THE FULLY CORRECTED postNotice() METHOD ======================
    // ==========================================================================================
    private void postNotice() {
        String title = etNoticeTitle.getText().toString().trim();
        String description = etNoticeDescription.getText().toString().trim();
        String location = etNoticeLocation.getText().toString().trim();
        String dateRange = etNoticeDateRange.getText().toString().trim();

        // 1. Get the currently logged-in user from FirebaseAuth
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 2. Perform a safety check. If no user is logged in, we can't proceed.
        if (currentUser == null) {
            Toast.makeText(this, "Authentication Error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Get the unique User ID (UID) of the logged-in authority.
        String authorityUserId = currentUser.getUid();

        // Perform validation on the input fields
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the Notice object
        Notice notice = new Notice(title, description, location, dateRange);

        // 4. THIS IS THE MOST IMPORTANT STEP: Stamp the notice with the authority's ID.
        notice.setUserId(authorityUserId);

        // Now, save the complete notice object to Firestore
        db.collection("notices")
                .add(notice)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AuthorityAddNoticeActivity.this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AuthorityAddNoticeActivity.this, AuthorityDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AuthorityAddNoticeActivity.this, "Error posting notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    // ==========================================================================================
    // ================================= END OF CORRECTED METHOD =================================
    // ==========================================================================================


    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // This part you have is correct for displaying the right menu
        bottomNavigationView.getMenu().clear();
        bottomNavigationView.inflateMenu(R.menu.authority_bottom_nav_menu);

        bottomNavigationView.setSelectedItemId(R.id.nav_add_notice);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), AuthorityDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_add_notice) {
                return true;
            } else if (itemId == R.id.nav_accepted) {
                startActivity(new Intent(getApplicationContext(), AcceptedActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), AuthorityProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
