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
import android.text.format.DateUtils;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuthorityProfileActivity extends AppCompatActivity {

    private static final String TAG = "AuthorityProfile";

    // Views
    private BottomNavigationView bottomNavigationView;
    private Button btnLogout, btnEditProfile;
    private TextView profileName, profileEmail, profileLocation, headerProfileName;
    private ImageView profileImage;

    // --- VIEWS FOR NOTICES SECTION ---
    private LinearLayout myNoticesContainer;
    private LayoutInflater inflater;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // GPS, Permissions, etc.
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private EditText locationEditTextInDialog;
    private ImageView dialogProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authority_profile);

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        loadUserProfile();
        loadMyNotices();
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profileLocation = findViewById(R.id.profileLocation);
        headerProfileName = findViewById(R.id.headerProfileName);
        profileImage = findViewById(R.id.profileImage);

        myNoticesContainer = findViewById(R.id.myNoticesContainer);
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
            if (itemId == R.id.nav_home) {
                intent = new Intent(this, AuthorityDashboardActivity.class);
            } else if (itemId == R.id.nav_add_notice) {
                intent = new Intent(this, AuthorityAddNoticeActivity.class);
            } else if (itemId == R.id.nav_accepted) {
                intent = new Intent(this, AcceptedActivity.class);
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

    private void loadMyNotices() {
        if (myNoticesContainer == null) {
            Log.e(TAG, "myNoticesContainer is null. Check your activity_authority_profile.xml layout file.");
            return;
        }
        myNoticesContainer.removeAllViews();

        db.collection("notices")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            TextView noNoticesView = new TextView(this);
                            noNoticesView.setText("You have not posted any notices.");
                            noNoticesView.setPadding(16, 16, 16, 16);
                            noNoticesView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                            myNoticesContainer.addView(noNoticesView);
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Notice notice = document.toObject(Notice.class);
                                addNoticeCardToView(notice, document.getId());
                            }
                        }
                    } else {
                        Log.e(TAG, "Error fetching notices: ", task.getException());
                        Toast.makeText(AuthorityProfileActivity.this, "Could not load notices.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addNoticeCardToView(Notice notice, String noticeId) {
        View noticeCard = inflater.inflate(R.layout.item_my_notice, myNoticesContainer, false);

        TextView noticeTitle = noticeCard.findViewById(R.id.myNoticeTitle);
        TextView noticeTimestamp = noticeCard.findViewById(R.id.myNoticeTimestamp);
        Button deleteButton = noticeCard.findViewById(R.id.btnDeleteNotice);

        noticeTitle.setText(notice.getTitle());

        if (notice.getTimestamp() != null) {
            CharSequence ago = DateUtils.getRelativeTimeSpanString(notice.getTimestamp().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            noticeTimestamp.setText("Posted " + ago);
        } else {
            noticeTimestamp.setText("");
        }

        deleteButton.setOnClickListener(v -> {
            // ========================= THIS IS THE FIX =========================
            // Use the custom dialog theme to ensure a white background
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_CustomDialog);
            // ===================================================================

            View dialogView = inflater.inflate(R.layout.dialog_custom_delete, null);
            builder.setView(dialogView);

            final AlertDialog dialog = builder.create();

            Button btnCancel = dialogView.findViewById(R.id.dialog_button_cancel);
            Button btnDelete = dialogView.findViewById(R.id.dialog_button_delete);
            TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
            TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);

            dialogTitle.setText("Delete Notice");
            dialogMessage.setText("Are you sure you want to permanently delete this notice?");

            btnCancel.setOnClickListener(view -> dialog.dismiss());
            btnDelete.setOnClickListener(view -> {
                dialog.dismiss();
                db.collection("notices").document(noticeId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AuthorityProfileActivity.this, "Notice deleted", Toast.LENGTH_SHORT).show();
                            loadMyNotices();
                        })
                        .addOnFailureListener(e -> Toast.makeText(AuthorityProfileActivity.this, "Failed to delete notice.", Toast.LENGTH_SHORT).show());
            });

            dialog.show();
        });

        myNoticesContainer.addView(noticeCard);
    }

    // --- All other standard profile methods below (unchanged) ---

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

    private void showEditProfileDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_authority_edit_profile);
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
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update profile. Check logs.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Profile update failed", e);
                    });
        });

        dialog.show();
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

    private String convertImageToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
