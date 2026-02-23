package com.example.prakriya20;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // All your views and variables
    private BottomNavigationView bottomNavigationView;
    private Button btnLogout, btnEditProfile;
    private LinearLayout myReportsContainer;
    private TextView profileName, profileEmail, profileLocation, headerProfileName;
    private ImageView profileImage;
    private LayoutInflater inflater;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private EditText locationEditTextInDialog;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ImageView dialogProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initializeViews();
        setupResultLaunchers();
        setupListeners();
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserProfile();
        loadUserReports();
    }

    // =========================================================================
    // === THIS IS THE FINAL AND CORRECT `loadUserReports` IMPLEMENTATION    ===
    // =========================================================================
    private void loadUserReports() {
        myReportsContainer.removeAllViews();
        if (currentUser == null) return;
        String currentUserId = currentUser.getUid();

        // 1. GET DISMISSED POSTS
        db.collection("users").document(currentUserId).collection("dismissedPosts").get()
                .addOnSuccessListener(dismissedSnapshot -> {
                    Set<String> dismissedIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : dismissedSnapshot) {
                        dismissedIds.add(doc.getId());
                    }

                    // 2. GET MERGED POSTS (where user is in the array)
                    Task<QuerySnapshot> mergedPostsTask = db.collection("posts")
                            .whereArrayContains("originalReporterIds", currentUserId)
                            .get();

                    // 3. GET LEGACY POSTS (where user is the primary ID)
                    Task<QuerySnapshot> legacyPostsTask = db.collection("posts")
                            .whereEqualTo("userId", currentUserId)
                            .get();

                    // 4. COMBINE AND PROCESS AFTER BOTH QUERIES COMPLETE
                    Tasks.whenAllSuccess(mergedPostsTask, legacyPostsTask).addOnSuccessListener(results -> {
                        myReportsContainer.removeAllViews();
                        Map<String, Post> userPostsMap = new HashMap<>(); // Use a Map to automatically handle duplicates

                        // Process MERGED posts
                        for (QueryDocumentSnapshot document : (QuerySnapshot) results.get(0)) {
                            if (!dismissedIds.contains(document.getId())) {
                                Post post = document.toObject(Post.class);
                                post.setPostId(document.getId());
                                userPostsMap.put(document.getId(), post);
                            }
                        }

                        // Process LEGACY posts
                        for (QueryDocumentSnapshot document : (QuerySnapshot) results.get(1)) {
                            if (!dismissedIds.contains(document.getId())) {
                                Post post = document.toObject(Post.class);
                                post.setPostId(document.getId());
                                userPostsMap.put(document.getId(), post);
                            }
                        }

                        // 5. DISPLAY THE FINAL, UNIQUE LIST
                        if (userPostsMap.isEmpty()) {
                            showNoReportsMessage();
                        } else {
                            List<Post> sortedList = new ArrayList<>(userPostsMap.values());
                            sortedList.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                            for (Post report : sortedList) {
                                addReportCardToView(report);
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user post lists.", e);
                        Toast.makeText(ProfileActivity.this, "Could not load your reports.", Toast.LENGTH_SHORT).show();
                        showNoReportsMessage();
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Non-critical error: Could not fetch dismissed post list.", e);
                    Toast.makeText(ProfileActivity.this, "Could not load your reports.", Toast.LENGTH_SHORT).show();
                    showNoReportsMessage();
                });
    }

    private void showNoReportsMessage() {
        myReportsContainer.removeAllViews();
        TextView noReportsView = new TextView(this);
        noReportsView.setText("You have not posted any reports yet.");
        noReportsView.setPadding(0, 16, 0, 16);
        myReportsContainer.addView(noReportsView);
    }

    private void addReportCardToView(Post report) {
        View reportCard = inflater.inflate(R.layout.item_my_report, myReportsContainer, false);
        TextView reportTitle = reportCard.findViewById(R.id.reportTitle);
        TextView reportStatus = reportCard.findViewById(R.id.reportStatus);
        Button deleteButton = reportCard.findViewById(R.id.btnDeleteReport);

        reportTitle.setText(report.getTitle());
        reportStatus.setText(report.getStatus());

        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to remove this report from your profile?")
                    .setPositiveButton("Delete", (dialog, which) -> handlePostDeletion(report.getPostId(), reportCard))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        myReportsContainer.addView(reportCard);
    }

    private void handlePostDeletion(String postId, View reportCardView) {
        String currentUserId = currentUser.getUid();
        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference userDismissRef = db.collection("users").document(currentUserId).collection("dismissedPosts").document(postId);

        myReportsContainer.removeView(reportCardView);
        if (myReportsContainer.getChildCount() == 0) {
            showNoReportsMessage();
        }
        Toast.makeText(this, "Report removed.", Toast.LENGTH_SHORT).show();

        WriteBatch batch = db.batch();
        Map<String, Object> dismissedData = new HashMap<>();
        dismissedData.put("dismissed", true);

        batch.set(userDismissRef, dismissedData);
        batch.update(postRef, "originalReporterIds", FieldValue.arrayRemove(currentUserId));

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                postRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Post post = documentSnapshot.toObject(Post.class);
                        if (post != null && (post.getOriginalReporterIds() == null || post.getOriginalReporterIds().isEmpty())) {
                            postRef.delete().addOnSuccessListener(aVoid -> Log.d(TAG, "Post " + postId + " permanently deleted."));
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Failed to save removal. Report may reappear.", Toast.LENGTH_SHORT).show();
                loadUserReports();
            }
        });
    }

    // --- ALL OTHER METHODS BELOW ARE UNCHANGED AND CORRECT ---

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        myReportsContainer = findViewById(R.id.myReportsContainer);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profileLocation = findViewById(R.id.profileLocation);
        headerProfileName = findViewById(R.id.headerProfileName);
        profileImage = findViewById(R.id.profileImage);
        inflater = LayoutInflater.from(this);
    }
    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) return true;
            Intent intent = null;
            if (itemId == R.id.nav_home) intent = new Intent(this, UserDashboardActivity.class);
            else if (itemId == R.id.nav_notices) intent = new Intent(this, NoticesActivity.class);
            else if (itemId == R.id.nav_add_post) intent = new Intent(this, AddPostActivity.class);
            else if (itemId == R.id.nav_alerts) intent = new Intent(this, AlertsActivity.class);
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
    private void showEditProfileDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        final EditText editNameDialog = dialog.findViewById(R.id.editName);
        final EditText editEmailDialog = dialog.findViewById(R.id.editEmail);
        locationEditTextInDialog = dialog.findViewById(R.id.editLocation);
        dialogProfileImage = dialog.findViewById(R.id.dialogProfileImage);
        Button btnChangePicture = dialog.findViewById(R.id.btnChangePicture);
        ImageButton btnUpdateGPS = dialog.findViewById(R.id.btnUpdateGPS);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        editNameDialog.setText(profileName.getText());
        editEmailDialog.setText(profileEmail.getText());
        String currentLocation = profileLocation.getText().toString();
        locationEditTextInDialog.setText(currentLocation.equals("Location not set") ? "" : currentLocation);
        dialogProfileImage.setImageDrawable(profileImage.getDrawable());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnChangePicture.setOnClickListener(v -> checkStoragePermissionAndLaunchGallery());
        btnUpdateGPS.setOnClickListener(v -> checkLocationPermissionAndFetch());
        btnSave.setOnClickListener(v -> {
            String newName = editNameDialog.getText().toString().trim();
            String newLocation = locationEditTextInDialog.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = ((BitmapDrawable) dialogProfileImage.getDrawable()).getBitmap();
            String profileImageString = convertImageToBase64(bitmap);
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("name", newName);
            updatedData.put("location", newLocation);
            updatedData.put("profileImageString", profileImageString);
            db.collection("users").document(currentUser.getUid()).update(updatedData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUserProfile();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
        });
        dialog.show();
    }
    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
    private void fetchLastLocation() {
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                getAddressFromLocation(location);
            } else {
                Toast.makeText(this, "Could not get location. Ensure GPS is on and try again.", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String addressLine = addresses.get(0).getAddressLine(0);
                if (locationEditTextInDialog != null) {
                    locationEditTextInDialog.setText(addressLine);
                }
            } else {
                Toast.makeText(this, "Address not found for this location.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed to get address.", e);
            Toast.makeText(this, "Could not retrieve address.", Toast.LENGTH_SHORT).show();
        }
    }
    private void setupResultLaunchers() {
        requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchGallery();
            } else {
                Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
            }
        });
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null && dialogProfileImage != null) {
                    dialogProfileImage.setImageURI(imageUri);
                }
            }
        });
        requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                fetchLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkStoragePermissionAndLaunchGallery() {
        String permission = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            requestStoragePermissionLauncher.launch(permission);
        }
    }
    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
    private String convertImageToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    private void loadUserProfile() {
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                profileName.setText(doc.getString("name"));
                profileEmail.setText(doc.getString("email"));
                headerProfileName.setText("Hello, " + doc.getString("name"));
                String location = doc.getString("location");
                profileLocation.setText(location != null && !location.isEmpty() ? location : "Location not set");
                String imageString = doc.getString("profileImageString");
                if (imageString != null && !imageString.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        profileImage.setImageResource(R.drawable.ic_user_large);
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading user profile", e));
    }
    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
