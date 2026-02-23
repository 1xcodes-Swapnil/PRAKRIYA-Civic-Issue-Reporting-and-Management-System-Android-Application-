package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AuthorityDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AuthorityDashboard";

    private TextView headerAuthorityName;
    private BottomNavigationView bottomNavigation;
    private RecyclerView issuesRecyclerView;
    private ProgressBar progressBar;

    // =========================================================================
    // === CRITICAL FIX #1: Use the correct adapter type                    ===
    // =========================================================================
    private AuthorityIssueAdapter adapter; // This was the main bug.
    // =========================================================================

    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authority_dashboard);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupRecyclerView(); // This will now use the correct adapter
        setupBottomNavigation();
        loadAuthorityInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        // Fetch issues every time the dashboard is shown
        fetchVerifiedIssues();
    }

    private void initializeViews() {
        headerAuthorityName = findViewById(R.id.headerAuthorityName);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        issuesRecyclerView = findViewById(R.id.issuesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        // Assuming your layout has a map button
        Button btnMaps = findViewById(R.id.btnMaps);
        btnMaps.setOnClickListener(v -> startActivity(new Intent(this, AuthorityMapsActivity.class)));
    }

    private void loadAuthorityInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            headerAuthorityName.setText(name != null ? "Hello, " + name : "Hello, Authority");
                        }
                    })
                    .addOnFailureListener(e -> {
                        headerAuthorityName.setText("Hello, Authority");
                        Log.e(TAG, "Failed to load authority info", e);
                    });
        }
    }

    // =========================================================================
    // === CRITICAL FIX #2: Initialize the correct adapter                  ===
    // =========================================================================
    private void setupRecyclerView() {
        // Use the AuthorityIssueAdapter, which contains all our fixes.
        adapter = new AuthorityIssueAdapter(this, postList);
        issuesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        issuesRecyclerView.setAdapter(adapter);
    }
    // =========================================================================


    private void fetchVerifiedIssues() {
        progressBar.setVisibility(View.VISIBLE);
        issuesRecyclerView.setVisibility(View.GONE);

        db.collection("posts")
                .whereEqualTo("status", "Verified") // Query for "Verified" status
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            post.setPostId(document.getId());
                            postList.add(post);
                        }
                        adapter.notifyDataSetChanged();
                        issuesRecyclerView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Successfully displayed " + postList.size() + " verified posts.");
                        if (postList.isEmpty()) {
                            Toast.makeText(this, "No new issues to review.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AuthorityDashboardActivity.this, "Failed to load issues.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching verified posts: ", task.getException());
                    }
                });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            Intent intent = null;
            if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AuthorityAddNoticeActivity.class);
            } else if (itemId == R.id.nav_accepted) {
                intent = new Intent(this, AcceptedActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, AuthorityProfileActivity.class);
            }
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish(); // finish() to prevent deep back-stack
                return true;
            }
            return false;
        });
    }
}
