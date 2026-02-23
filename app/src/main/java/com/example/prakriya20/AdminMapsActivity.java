// File: AdminMapsActivity.java
package com.example.prakriya20;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // Import ArrayAdapter
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AdminMapsActivity";

    // Views
    private BottomNavigationView bottomNavigationView;
    private Button btnToggleIssues, btnAllIssues;
    private LinearLayout issuesContentArea, issuesListLayout;
    private Spinner filterSpinner;
    private EditText searchBar;
    private LayoutInflater inflater;

    // Google Map & Data
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private final List<FirestorePost> allPosts = new ArrayList<>();

    private boolean issuesVisible = false;

    private static class FirestorePost {
        Post post;
        Marker marker;
        View cardView;
        FirestorePost(Post post) { this.post = post; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_maps);

        db = FirebaseFirestore.getInstance();
        initializeViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        setupListeners();

        // **THIS IS THE NEW METHOD CALL**
        setupSpinner();
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnToggleIssues = findViewById(R.id.btnToggleIssues);
        issuesContentArea = findViewById(R.id.issuesContentArea);
        btnAllIssues = findViewById(R.id.btnAllIssues);
        issuesListLayout = findViewById(R.id.issuesListLayout);
        filterSpinner = findViewById(R.id.filterSpinner);
        searchBar = findViewById(R.id.searchBar);
        inflater = LayoutInflater.from(this);
    }

    // **THIS IS THE NEW METHOD**
    private void setupSpinner() {
        // Use the custom layouts we created for the spinner to ensure text is visible
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, R.layout.spinner_item_custom);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom);
        filterSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        // Bottom Navigation for ADMIN
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // If user clicks the home button on the nav bar, take them to the dashboard list.
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(AdminMapsActivity.this, AdminDashboardActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AddNoticeActivity.class);
            } else if (itemId == R.id.nav_duplicates) {
                intent = new Intent(this, DuplicatesActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(this, AdminProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });

        // The rest of the listeners are identical to the user's MapsActivity
        btnToggleIssues.setOnClickListener(v -> {
            issuesVisible = !issuesVisible;
            issuesContentArea.setVisibility(issuesVisible ? View.VISIBLE : View.GONE);
            Drawable icon;
            if (issuesVisible) {
                btnToggleIssues.setText("Hide Issues List");
                icon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_down);
            } else {
                btnToggleIssues.setText("Show Issues List");
                icon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_up);
            }
            btnToggleIssues.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        });

        btnAllIssues.setOnClickListener(v -> {
            searchBar.setText("");
            filterSpinner.setSelection(0);
            zoomToFitAllMarkers();
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        fetchPostsFromFirestore();
    }

    private void fetchPostsFromFirestore() {
        db.collection("posts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allPosts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            if (post.getLatitude() != 0 && post.getLongitude() != 0) {
                                allPosts.add(new FirestorePost(post));
                            }
                        }
                        Log.d(TAG, "Fetched " + allPosts.size() + " posts with location.");
                        addMarkersAndCards();
                        zoomToFitAllMarkers();
                        applyFilters();
                    } else {
                        Log.e(TAG, "Error fetching posts: ", task.getException());
                        Toast.makeText(AdminMapsActivity.this, "Failed to load issues.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addMarkersAndCards() {
        if (mMap == null) return;
        mMap.clear();
        issuesListLayout.removeAllViews();

        for (FirestorePost firestorePost : allPosts) {
            Post post = firestorePost.post;
            LatLng location = new LatLng(post.getLatitude(), post.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions().position(location).title(post.getTitle());
            float markerColor;
            switch (post.getStatus()) {
                case "In Progress": markerColor = BitmapDescriptorFactory.HUE_YELLOW; break;
                case "Resolved": markerColor = BitmapDescriptorFactory.HUE_GREEN; break;
                default: markerColor = BitmapDescriptorFactory.HUE_RED; break;
            }
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor));
            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                firestorePost.marker = marker;
            }

            View cardView = inflater.inflate(R.layout.item_map_issue_card, issuesListLayout, false);
            TextView cardTitle = cardView.findViewById(R.id.issue_card_title);
            TextView cardAddress = cardView.findViewById(R.id.issue_card_address);
            TextView cardTime = cardView.findViewById(R.id.issue_card_time);
            ImageView cardStatusDot = cardView.findViewById(R.id.issue_card_status_dot);

            cardTitle.setText(post.getTitle());
            cardAddress.setText(post.getAddress());
            if (post.getTimestamp() != null) {
                cardTime.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(post.getTimestamp()));
            } else {
                cardTime.setText("");
            }

            switch (post.getStatus()) {
                case "In Progress": cardStatusDot.setImageResource(R.drawable.circle_yellow); break;
                case "Resolved": cardStatusDot.setImageResource(R.drawable.circle_green); break;
                default: cardStatusDot.setImageResource(R.drawable.circle_red); break;
            }

            firestorePost.cardView = cardView;
            issuesListLayout.addView(cardView);
        }
    }

    private void applyFilters() {
        if (filterSpinner == null || searchBar == null) return;

        String statusFilter = filterSpinner.getSelectedItem().toString();
        String searchQuery = searchBar.getText().toString().toLowerCase().trim();
        boolean showAllStatuses = statusFilter.equals("All Issues");

        for (FirestorePost firestorePost : allPosts) {
            Post post = firestorePost.post;
            boolean statusMatch = showAllStatuses || post.getStatus().equals(statusFilter);
            boolean searchMatch = searchQuery.isEmpty() || post.getTitle().toLowerCase().contains(searchQuery);

            boolean isVisible = statusMatch && searchMatch;

            if (firestorePost.marker != null) {
                firestorePost.marker.setVisible(isVisible);
            }
            if (firestorePost.cardView != null) {
                firestorePost.cardView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void zoomToFitAllMarkers() {
        if (mMap == null || allPosts.isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        int visibleMarkerCount = 0;
        for (FirestorePost firestorePost : allPosts) {
            if (firestorePost.marker != null && firestorePost.marker.isVisible()) {
                builder.include(firestorePost.marker.getPosition());
                visibleMarkerCount++;
            }
        }
        if (visibleMarkerCount > 0) {
            LatLngBounds bounds = builder.build();
            int padding = 150; // pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }
}
