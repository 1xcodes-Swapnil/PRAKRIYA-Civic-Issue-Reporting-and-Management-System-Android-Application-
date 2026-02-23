package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

public class UserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "UserDashboardActivity";

    private RecyclerView postsRecyclerView;
    private BottomNavigationView bottomNavigationView;
    private Button btnMaps;

    private FirebaseFirestore db;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnMaps = findViewById(R.id.btnMaps);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);

        db = FirebaseFirestore.getInstance();
        postList = new ArrayList<>();

        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(postList, this);
        postsRecyclerView.setAdapter(postAdapter);

        setupBottomNavigation();
        btnMaps.setOnClickListener(v -> startActivity(new Intent(UserDashboardActivity.this, MapsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        fetchPosts();
    }

    // =========================================================================
    // === THIS IS THE FINAL AND GUARANTEED CORRECTED FETCH METHOD           ===
    // =========================================================================
    private void fetchPosts() {
        // This query now fetches all posts that have been processed by an admin.
        // The key is checking for the existence of the 'originalReporterIds' field.
        // This field is ONLY added when an admin merges and forwards a post.
        // Therefore, any old, unverified duplicate will NOT have this field and will be excluded.
        db.collection("posts")
                .orderBy("originalReporterIds") // This is a trick to filter for existence
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // First, we check if the document actually has the field.
                            if (document.contains("originalReporterIds")) {
                                Post post = document.toObject(Post.class);
                                post.setPostId(document.getId());

                                // Exclude any posts that might have been marked as Spam later.
                                if (post.getStatus() != null && post.getStatus().equals("Spam")) {
                                    continue; // Skip this post
                                }

                                // Fix for old posts missing the upvotes field
                                if (document.get("upvotes") == null) {
                                    post.setUpvotes(0);
                                }

                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Successfully fetched " + postList.size() + " relevant posts.");
                        if (postList.isEmpty()) {
                            Toast.makeText(UserDashboardActivity.this, "No posts to display.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Error getting documents. You may need a Firestore Index.", task.getException());
                        Toast.makeText(UserDashboardActivity.this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_add_post) {
                intent = new Intent(this, AddPostActivity.class);
            } else if (itemId == R.id.nav_notices) {
                intent = new Intent(this, NoticesActivity.class);
            } else if (itemId == R.id.nav_alerts) {
                intent = new Intent(this, AlertsActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }
}
