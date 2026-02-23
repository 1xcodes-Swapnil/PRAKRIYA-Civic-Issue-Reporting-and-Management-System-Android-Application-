package com.example.prakriya20;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    private static final String TAG = "AddPostActivity";

    // Views
    private ImageView btnBack, selectedImage;
    private View uploadPlaceholder;
    private Button btnTakePhoto, btnUploadPhoto, btnUseGPS, btnSubmit;
    private EditText titleInput, descriptionInput, locationInput;
    private Spinner issueTypeSpinner;
    private BottomNavigationView bottomNavigationView;

    // State & Data
    private Location lastFetchedLocation;
    private Uri selectedImageUri;
    private ProgressDialog progressDialog;
    private boolean isAddressManuallyChanged = false; // The crucial flag to fix the bug

    // Clients & Launchers
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupSpinner();
        setupResultLaunchers();
        setupListeners();
        bottomNavigationView.setSelectedItemId(R.id.nav_add_post);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.issue_types, R.layout.spinner_item_custom);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom);
        issueTypeSpinner.setAdapter(adapter);
    }

    // =========================================================================
    // === THIS IS THE METHOD WITH THE CORRECTED SUBMISSION LOGIC            ===
    // =========================================================================
    private void submitReport() {
        String title = titleInput.getText().toString().trim();
        String issueType = issueTypeSpinner.getSelectedItem().toString();
        String description = descriptionInput.getText().toString().trim();
        String address = locationInput.getText().toString().trim();

        if (selectedImageUri == null || title.isEmpty() || issueType.equals("Select Issue Type") || description.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select an image.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Posting Issue...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String userName = userDoc.getString("name");
                String userProfileImageString = userDoc.getString("profileImageString");

                // THIS IS THE CRITICAL FIX
                // If the address was typed manually, OR if we never used GPS, we MUST get new coordinates.
                if (isAddressManuallyChanged || lastFetchedLocation == null) {
                    getCoordinatesFromAddress(title, issueType, description, address, userName, userProfileImageString);
                } else {
                    // Otherwise, we can trust the GPS location we already have.
                    createPost(title, issueType, description, address, lastFetchedLocation.getLatitude(), lastFetchedLocation.getLongitude(), userName, userProfileImageString);
                }
            } else {
                progressDialog.dismiss();
                Toast.makeText(AddPostActivity.this, "Error: Could not find user profile.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(AddPostActivity.this, "Failed to get user data.", Toast.LENGTH_SHORT).show();
        });
    }

    private void getCoordinatesFromAddress(String title, String issueType, String description, String address, String userName, String userProfileImageString) {
        progressDialog.setMessage("Verifying Address...");
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address foundAddress = addresses.get(0);
                double latitude = foundAddress.getLatitude();
                double longitude = foundAddress.getLongitude();

                createPost(title, issueType, description, address, latitude, longitude, userName, userProfileImageString);
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Could not find coordinates for the typed address.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Network error. Could not verify address.", Toast.LENGTH_SHORT).show();
        }
    }

    // This method now correctly includes the 'originalReporterIds' field on creation
    private void createPost(String title, String issueType, String description, String address, double latitude, double longitude, String userName, String userProfileImageString) {
        progressDialog.setMessage("Processing Image...");
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", mAuth.getCurrentUser().getUid());
        postData.put("userName", userName);
        postData.put("userProfileImageString", userProfileImageString);
        postData.put("title", title);
        postData.put("issueType", issueType);
        postData.put("description", description);
        postData.put("address", address);
        postData.put("latitude", latitude);
        postData.put("longitude", longitude);
        postData.put("status", "Unresolved");
        postData.put("timestamp", FieldValue.serverTimestamp());
        postData.put("upvotes", 0);
        postData.put("upvotedBy", new ArrayList<String>());

        // Add the current user to the list of original reporters by default
        List<String> reporterIds = new ArrayList<>();
        reporterIds.add(mAuth.getCurrentUser().getUid());
        postData.put("originalReporterIds", reporterIds);

        try {
            Bitmap correctlyOrientedBitmap = getCorrectlyOrientedBitmap(selectedImageUri);
            if (correctlyOrientedBitmap == null) { throw new IOException("Failed to decode bitmap from URI."); }

            String postImageDataString = convertImageToBase64(correctlyOrientedBitmap);
            if (postImageDataString == null) { throw new IOException("Failed to convert image to Base64"); }

            postData.put("imageDataString", postImageDataString);

            // Get EXIF Geo-location AFTER processing the image, just in case
            try (InputStream exifInputStream = getContentResolver().openInputStream(selectedImageUri)) {
                if (exifInputStream != null) {
                    ExifInterface exifInterface = new ExifInterface(exifInputStream);
                    double[] latLong = exifInterface.getLatLong();
                    if (latLong != null) {
                        postData.put("imageGeoLat", latLong[0]);
                        postData.put("imageGeoLng", latLong[1]);
                    } else {
                        postData.put("imageGeoLat", 0.0);
                        postData.put("imageGeoLng", 0.0);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Could not read EXIF data. Defaulting to 0,0.", ex);
                postData.put("imageGeoLat", 0.0);
                postData.put("imageGeoLng", 0.0);
            }

            progressDialog.setMessage("Finalizing Post...");
            db.collection("posts")
                    .add(postData)
                    .addOnSuccessListener(documentReference -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Issue posted successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, UserDashboardActivity.class));
                        finishAffinity();
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error posting issue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to process image file.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error processing image URI", e);
        }
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnBack = findViewById(R.id.btnBack);
        selectedImage = findViewById(R.id.selectedImage);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnUseGPS = findViewById(R.id.btnUseGPS);
        titleInput = findViewById(R.id.titleInput);
        issueTypeSpinner = findViewById(R.id.issueTypeSpinner);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        btnSubmit = findViewById(R.id.btnSubmit);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    // --- THIS LISTENER IS NOW A CRITICAL PART OF THE FIX ---
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnUploadPhoto.setOnClickListener(v -> checkStoragePermission());
        btnUseGPS.setOnClickListener(v -> checkLocationPermissionAndFetch());
        btnSubmit.setOnClickListener(v -> submitReport());

        locationInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When the user starts typing in the address box, we set the flag to true.
                isAddressManuallyChanged = true;
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        setupBottomNavigation();
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                this.lastFetchedLocation = location;
                getAddressFromLocation(location);
                // CRITICAL FIX: Reset the flag because we just used GPS.
                // The user has not manually changed the text since this update.
                isAddressManuallyChanged = false;
            } else {
                Toast.makeText(this, "Could not get location. Ensure GPS is on.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                locationInput.setText(addresses.get(0).getAddressLine(0));
            } else {
                Toast.makeText(this, "Address not found for this location.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed to get address.", e);
            Toast.makeText(this, "Could not retrieve address.", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertImageToBase64(Bitmap bitmapToConvert) {
        if (bitmapToConvert == null) return null;
        try {
            int originalWidth = bitmapToConvert.getWidth();
            int originalHeight = bitmapToConvert.getHeight();
            int maxWidth = 800;
            if (originalWidth <= maxWidth && originalHeight <= maxWidth) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmapToConvert.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
            }
            float ratio = Math.min((float) maxWidth / originalWidth, (float) maxWidth / originalHeight);
            int newWidth = Math.round(originalWidth * ratio);
            int newHeight = Math.round(originalHeight * ratio);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapToConvert, newWidth, newHeight, true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Could not convert bitmap to Base64", e);
            return null;
        }
    }

    private Bitmap getCorrectlyOrientedBitmap(Uri imageUri) throws IOException {
        try (InputStream exifInputStream = getContentResolver().openInputStream(imageUri)) {
            if (exifInputStream == null) throw new IOException("Cannot open input stream for URI");
            ExifInterface exifInterface = new ExifInterface(exifInputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            try (InputStream bitmapInputStream = getContentResolver().openInputStream(imageUri)) {
                if (bitmapInputStream == null) throw new IOException("Cannot open input stream for URI");
                Bitmap originalBitmap = BitmapFactory.decodeStream(bitmapInputStream);
                if (originalBitmap == null) throw new IOException("Cannot decode bitmap from stream");

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                    case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                    case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
                    default: return originalBitmap;
                }
                return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            }
        }
    }

    private void setupResultLaunchers() {
        requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) fetchLastLocation(); else Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
        });
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openCamera(); else Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
        });
        requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openGallery(); else Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && selectedImageUri != null) {
                displayImageFromUri(selectedImageUri);
            } else {
                Toast.makeText(this, "Camera cancelled.", Toast.LENGTH_SHORT).show();
            }
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    displayImageFromUri(selectedImageUri);
                }
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera();
        else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Prakriya_Issue_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image taken for Prakriya app issue report");
        selectedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void checkStoragePermission() {
        String permission = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) openGallery();
        else requestStoragePermissionLauncher.launch(permission);
    }

    private void openGallery() {
        galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void displayImageFromUri(Uri imageUri) {
        selectedImage.setImageURI(imageUri);
        selectedImage.setVisibility(View.VISIBLE);
        uploadPlaceholder.setVisibility(View.GONE);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_add_post) return true;
            Intent intent = null;
            if (itemId == R.id.nav_home) intent = new Intent(this, UserDashboardActivity.class);
            else if (itemId == R.id.nav_notices) intent = new Intent(this, NoticesActivity.class);
            else if (itemId == R.id.nav_alerts) intent = new Intent(this, AlertsActivity.class);
            else if (itemId == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
            return false;
        });
    }
}
