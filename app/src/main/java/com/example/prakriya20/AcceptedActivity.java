package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AcceptedActivity extends AppCompatActivity {

    private RecyclerView acceptedIssuesRecyclerView;
    private ProgressBar progressBar;
    private AcceptedIssueAdapter adapter;
    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAcceptedIssues();
    }

    private void initializeViews() {
        acceptedIssuesRecyclerView = findViewById(R.id.acceptedIssuesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new AcceptedIssueAdapter(this, postList);
        acceptedIssuesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        acceptedIssuesRecyclerView.setAdapter(adapter);
    }

    // =========================================================================
    // === THIS IS THE ONLY METHOD THAT HAS BEEN CHANGED                     ===
    // === It now ONLY fetches "In Progress" issues.                       ===
    // =========================================================================
    private void loadAcceptedIssues() {
        progressBar.setVisibility(View.VISIBLE);
        acceptedIssuesRecyclerView.setVisibility(View.GONE);

        // THE FIX: This query now ONLY looks for posts with the status "In Progress".
        // It will no longer fetch "Resolved" posts.
        db.collection("posts")
                .whereEqualTo("status", "In Progress") // This is the corrected line
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
                        acceptedIssuesRecyclerView.setVisibility(View.VISIBLE);
                        if (postList.isEmpty()) {
                            Toast.makeText(this, "No issues are currently in progress.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load issues.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_accepted);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_accepted) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, AuthorityDashboardActivity.class);
            } else if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AuthorityAddNoticeActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, AuthorityProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
