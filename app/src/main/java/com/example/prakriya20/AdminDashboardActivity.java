package com.example.prakriya20;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
// ===============================================
// === THIS IS ONE OF THE TWO LINES I ADDED      ===
import android.widget.Button;
// ===============================================
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private RecyclerView issuesRecyclerView;
    private ProgressBar progressBar;
    private TextView headerAdminName;
    private BottomNavigationView bottomNavigationView;
    private AdminIssueAdapter adapter;
    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private PostManager postManager;
    private Button btnMaps; // This is the other line I added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        db = FirebaseFirestore.getInstance();
        postManager = PostManager.getInstance();
        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadAdminInfo();

        // ========================================================
        // === THIS IS THE FIX: MAKING THE MAPS BUTTON CLICKABLE ===
        // ========================================================
        btnMaps.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminMapsActivity.class));
        });
        // ========================================================
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataAndFilterDuplicates();
    }

    // This is your existing, correct data loading logic. It is unchanged.
    private void loadDataAndFilterDuplicates() {
        progressBar.setVisibility(View.VISIBLE);
        issuesRecyclerView.setVisibility(View.GONE);
        postManager.fetchAllPosts(new PostManager.OnDataReadyListener() {
            @Override
            public void onDataReady() {
                Set<String> duplicatePostIds = new HashSet<>();
                for (List<Post> group : postManager.getDuplicatePostGroups()) {
                    for (Post p : group) {
                        duplicatePostIds.add(p.getPostId());
                    }
                }
                db.collection("posts")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful() && task.getResult() != null) {
                                postList.clear();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Post post = document.toObject(Post.class);
                                    post.setPostId(document.getId());
                                    if ("Unresolved".equals(post.getStatus()) && !duplicatePostIds.contains(post.getPostId())) {
                                        postList.add(post);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                issuesRecyclerView.setVisibility(View.VISIBLE);
                                if (postList.isEmpty()) {
                                    Toast.makeText(AdminDashboardActivity.this, "No unique unresolved issues found.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(AdminDashboardActivity.this, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this, "Could not initialize data. Please restart.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initializeViews() {
        issuesRecyclerView = findViewById(R.id.issuesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        headerAdminName = findViewById(R.id.headerAdminName);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnMaps = findViewById(R.id.btnMaps); // I added this line
    }

    // All other methods are unchanged and correct.
    private void setupRecyclerView() {
        adapter = new AdminIssueAdapter(this, postList);
        issuesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        issuesRecyclerView.setAdapter(adapter);
    }

    private void loadAdminInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
                if (doc.exists() && doc.getString("name") != null) {
                    headerAdminName.setText("Hello, " + doc.getString("name"));
                }
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            }
            if (itemId == R.id.nav_duplicates) {
                if (postManager.isDataReady()) {
                    startActivity(new Intent(this, DuplicatesActivity.class));
                } else {
                    Toast.makeText(this, "Data is still loading, please wait a moment.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AddNoticeActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, AdminProfileActivity.class);
            }
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            return true;
        });
    }
}
