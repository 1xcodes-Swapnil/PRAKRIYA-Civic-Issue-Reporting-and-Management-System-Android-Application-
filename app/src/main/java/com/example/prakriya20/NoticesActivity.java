package com.example.prakriya20;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class NoticesActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView noticesRecyclerView;
    private NoticeAdapter noticeAdapter;
    private List<Notice> noticeList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // Add FirebaseAuth
    private ProgressBar progressBar;
    private TextView tvNoNotices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);

        // Initialize Firebase Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        noticesRecyclerView = findViewById(R.id.noticesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvNoNotices = findViewById(R.id.tvNoNotices);

        // Setup UI
        setupBottomNavigation();
        setupRecyclerView();

        // Fetch notices using the new, correct method
        fetchNoticesForCurrentUser();
    }

    private void setupRecyclerView() {
        noticeList = new ArrayList<>();
        noticeAdapter = new NoticeAdapter(this, noticeList);
        noticesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        noticesRecyclerView.setAdapter(noticeAdapter);

        // This stays the same, but will call the new method in the adapter
        setupSwipeToDismiss();
    }

    // =================================================================================
    // === NEW METHOD: fetchNoticesForCurrentUser() - Replaces the old fetchNotices() ===
    // =================================================================================
    private void fetchNoticesForCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);
        noticesRecyclerView.setVisibility(View.GONE);
        tvNoNotices.setVisibility(View.GONE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressBar.setVisibility(View.GONE);
            tvNoNotices.setText("Please log in to see notices.");
            tvNoNotices.setVisibility(View.VISIBLE);
            return;
        }
        String userId = currentUser.getUid();

        // Step 1: Get the list of dismissed notice IDs for the current user.
        Task<QuerySnapshot> dismissedTask = db.collection("users").document(userId)
                .collection("dismissedNotices").get();

        // Step 2: Get all recent notices from the main "notices" collection.
        Task<QuerySnapshot> allNoticesTask = db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(100).get();

        // Step 3: When both tasks are complete, process the results.
        Tasks.whenAllSuccess(dismissedTask, allNoticesTask).addOnSuccessListener(results -> {
            progressBar.setVisibility(View.GONE);

            // Process the list of dismissed IDs
            Set<String> dismissedNoticeIds = new HashSet<>();
            QuerySnapshot dismissedSnapshot = (QuerySnapshot) results.get(0);
            for (QueryDocumentSnapshot document : dismissedSnapshot) {
                dismissedNoticeIds.add(document.getId());
            }

            // Process all notices and filter out the dismissed ones
            noticeList.clear();
            QuerySnapshot allNoticesSnapshot = (QuerySnapshot) results.get(1);
            for (QueryDocumentSnapshot document : allNoticesSnapshot) {
                // If the notice's ID is NOT in the dismissed set, add it to the list to be displayed
                if (!dismissedNoticeIds.contains(document.getId())) {
                    Notice notice = document.toObject(Notice.class);
                    notice.setNoticeId(document.getId()); // Store the document ID
                    noticeList.add(notice);
                }
            }

            // Update the UI
            if (noticeList.isEmpty()) {
                tvNoNotices.setText("No new notices.");
                tvNoNotices.setVisibility(View.VISIBLE);
            } else {
                noticesRecyclerView.setVisibility(View.VISIBLE);
                noticeAdapter.notifyDataSetChanged();
            }

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            tvNoNotices.setVisibility(View.VISIBLE);
            tvNoNotices.setText("Failed to load notices.");
            Toast.makeText(NoticesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // ================================================================================
    // === METHOD UPDATED: setupSwipeToDismiss() now calls dismissItem()            ===
    // ================================================================================
    private void setupSwipeToDismiss() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                // Call the new dismissItem() method instead of deleteItem()
                noticeAdapter.dismissItem(position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(NoticesActivity.this, R.color.red))
                        .addActionIcon(R.drawable.ic_delete_swipe)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(noticesRecyclerView);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_notices);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(NoticesActivity.this, UserDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_notices) {
                return true;
            } else if (itemId == R.id.nav_add_post) {
                startActivity(new Intent(NoticesActivity.this, AddPostActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(NoticesActivity.this, AlertsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(NoticesActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
