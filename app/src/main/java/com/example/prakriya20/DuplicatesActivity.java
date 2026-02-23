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

import java.util.ArrayList; // Make sure ArrayList is imported
import java.util.List;

public class DuplicatesActivity extends AppCompatActivity {

    private RecyclerView rvDuplicateGroups;
    private ProgressBar progressBar;
    private DuplicateGroupAdapter adapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicates);

        rvDuplicateGroups = findViewById(R.id.rvDuplicateGroups);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        rvDuplicateGroups.setLayoutManager(new LinearLayoutManager(this));
        setupBottomNavigation();

        PostManager postManager = PostManager.getInstance();

        progressBar.setVisibility(View.VISIBLE);
        rvDuplicateGroups.setVisibility(View.GONE);

        if (postManager.isDataReady()) {
            // Data is ready, get the groups from the manager.
            List<List<Post>> originalDuplicateGroups = postManager.getDuplicatePostGroups();
            progressBar.setVisibility(View.GONE);

            if (originalDuplicateGroups != null && !originalDuplicateGroups.isEmpty()) {
                rvDuplicateGroups.setVisibility(View.VISIBLE);

                // =========================================================================
                // === THIS IS THE CRITICAL FIX: CREATE A DEEP COPY OF THE LIST          ===
                // =========================================================================
                // We create a new list for the adapter so it has its own independent copy.
                // We iterate through the original groups and create a new ArrayList for each one.
                List<List<Post>> adapterGroups = new ArrayList<>();
                for (List<Post> originalGroup : originalDuplicateGroups) {
                    // Create a new list containing all the posts from the original group.
                    adapterGroups.add(new ArrayList<>(originalGroup));
                }
                // =========================================================================

                // Pass the NEW, independent list to the adapter.
                adapter = new DuplicateGroupAdapter(this, adapterGroups);
                rvDuplicateGroups.setAdapter(adapter);

            } else {
                // If there are no duplicates, show a message.
                Toast.makeText(this, "No duplicate issues found.", Toast.LENGTH_LONG).show();
            }
        } else {
            // Data is not ready, show an error and hide progress bar.
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Data not available. Please go back and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_duplicates);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_duplicates) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, AdminDashboardActivity.class);
            } else if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AddNoticeActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, AdminProfileActivity.class);
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
