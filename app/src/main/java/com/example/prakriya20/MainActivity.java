package com.example.prakriya20;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // GPS and Location
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private EditText targetLocationEditText; // To store which EditText to fill

    // Role Toggles
    ToggleButton toggleUser, toggleGov, toggleAdmin;

    // User Layout Components
    LinearLayout userLayout;
    Button btnUserSignIn, btnUserSignUp;
    LinearLayout userSignInForm, userSignUpForm;
    EditText userSignInEmail, userSignInPassword;
    Button btnUserLogin;
    EditText userSignUpName, userSignUpEmail, userSignUpLocation, userSignUpPassword, userSignUpConfirm;
    ImageButton btnGetUserLocation;
    Button btnUserRegister;

    // Gov/Authority Layout Components
    LinearLayout govLayout;
    Button btnGovSignIn, btnGovSignUp;
    LinearLayout govSignInForm, govSignUpForm;
    EditText govSignInEmail, govSignInPassword;
    Button btnGovLogin;
    EditText govSignUpName, govSignUpEmail, govSignUpId, govSignUpLocation, govSignUpPassword;
    ImageButton btnGetGovLocation;
    Button btnGovRegister;

    // Admin Layout Components
    LinearLayout adminLayout;
    Button btnAdminSignIn, btnAdminSignUp;
    LinearLayout adminSignInForm, adminSignUpForm;
    EditText adminSignInEmail, adminSignInPassword;
    Button btnAdminLogin;
    EditText adminSignUpName, adminSignUpEmail, adminSignUpLocation, adminSignUpPassword;
    ImageButton btnGetAdminLocation;
    Button btnAdminRegister;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Location Services and Permissions Launcher
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationPermissionLauncher();

        // Initialize all views from XML
        initializeViews();

        // --- Set up the screen for first launch and handle all clicks ---
        setupInitialUIState();
        setupRoleToggleListeners();
        setupFormToggleListeners();
        setupAuthButtonListeners();
        setupLocationButtonListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in when the app starts.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRoleAndNavigate(currentUser, true); // Pass true for auto-login check
        }
    }

    private void initializeViews() {
        // Role Toggles
        toggleUser = findViewById(R.id.toggleUser);
        toggleGov = findViewById(R.id.toggleGov);
        toggleAdmin = findViewById(R.id.toggleAdmin);

        // User Layout
        userLayout = findViewById(R.id.userLayout);
        btnUserSignIn = findViewById(R.id.btnUserSignIn);
        btnUserSignUp = findViewById(R.id.btnUserSignUp);
        userSignInForm = findViewById(R.id.userSignInForm);
        userSignUpForm = findViewById(R.id.userSignUpForm);
        userSignInEmail = findViewById(R.id.userSignInEmail);
        userSignInPassword = findViewById(R.id.userSignInPassword);
        btnUserLogin = findViewById(R.id.btnUserLogin);
        userSignUpName = findViewById(R.id.userSignUpName);
        userSignUpEmail = findViewById(R.id.userSignUpEmail);
        userSignUpLocation = findViewById(R.id.userSignUpLocation);
        userSignUpPassword = findViewById(R.id.userSignUpPassword);
        userSignUpConfirm = findViewById(R.id.userSignUpConfirm);
        btnUserRegister = findViewById(R.id.btnUserRegister);
        btnGetUserLocation = findViewById(R.id.btnGetUserLocation);

        // Gov/Authority Layout
        govLayout = findViewById(R.id.govLayout);
        btnGovSignIn = findViewById(R.id.btnGovSignIn);
        btnGovSignUp = findViewById(R.id.btnGovSignUp);
        govSignInForm = findViewById(R.id.govSignInForm);
        govSignUpForm = findViewById(R.id.govSignUpForm);
        govSignInEmail = findViewById(R.id.govSignInEmail);
        govSignInPassword = findViewById(R.id.govSignInPassword);
        btnGovLogin = findViewById(R.id.btnGovLogin);
        govSignUpName = findViewById(R.id.govSignUpName);
        govSignUpEmail = findViewById(R.id.govSignUpEmail);
        govSignUpId = findViewById(R.id.govSignUpId);
        govSignUpLocation = findViewById(R.id.govSignUpLocation);
        govSignUpPassword = findViewById(R.id.govSignUpPassword);
        btnGovRegister = findViewById(R.id.btnGovRegister);
        btnGetGovLocation = findViewById(R.id.btnGetGovLocation);

        // Admin Layout
        adminLayout = findViewById(R.id.adminLayout);
        btnAdminSignIn = findViewById(R.id.btnAdminSignIn);
        btnAdminSignUp = findViewById(R.id.btnAdminSignUp);
        adminSignInForm = findViewById(R.id.adminSignInForm);
        adminSignUpForm = findViewById(R.id.adminSignUpForm);
        adminSignInEmail = findViewById(R.id.adminSignInEmail);
        adminSignInPassword = findViewById(R.id.adminSignInPassword);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        adminSignUpName = findViewById(R.id.adminSignUpName);
        adminSignUpEmail = findViewById(R.id.adminSignUpEmail);
        adminSignUpLocation = findViewById(R.id.adminSignUpLocation);
        adminSignUpPassword = findViewById(R.id.adminSignUpPassword);
        btnAdminRegister = findViewById(R.id.btnAdminRegister);
        btnGetAdminLocation = findViewById(R.id.btnGetAdminLocation);
    }

    private void setupInitialUIState() {
        toggleUser.setChecked(true);
        userLayout.setVisibility(View.VISIBLE);
        userSignInForm.setVisibility(View.VISIBLE);
        userSignUpForm.setVisibility(View.GONE);
        govLayout.setVisibility(View.GONE);
        adminLayout.setVisibility(View.GONE);
    }

    private void setupRoleToggleListeners() {
        toggleUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                userLayout.setVisibility(View.VISIBLE);
                userSignInForm.setVisibility(View.VISIBLE);
                userSignUpForm.setVisibility(View.GONE);
                govLayout.setVisibility(View.GONE);
                adminLayout.setVisibility(View.GONE);
                toggleGov.setChecked(false);
                toggleAdmin.setChecked(false);
            } else if (!toggleGov.isChecked() && !toggleAdmin.isChecked()) {
                userLayout.setVisibility(View.GONE);
            }
        });

        toggleGov.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                govLayout.setVisibility(View.VISIBLE);
                govSignInForm.setVisibility(View.VISIBLE);
                govSignUpForm.setVisibility(View.GONE);
                userLayout.setVisibility(View.GONE);
                adminLayout.setVisibility(View.GONE);
                toggleUser.setChecked(false);
                toggleAdmin.setChecked(false);
            } else if (!toggleUser.isChecked() && !toggleAdmin.isChecked()) {
                govLayout.setVisibility(View.GONE);
            }
        });

        toggleAdmin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adminLayout.setVisibility(View.VISIBLE);
                adminSignInForm.setVisibility(View.VISIBLE);
                adminSignUpForm.setVisibility(View.GONE);
                userLayout.setVisibility(View.GONE);
                govLayout.setVisibility(View.GONE);
                toggleUser.setChecked(false);
                toggleGov.setChecked(false);
            } else if (!toggleUser.isChecked() && !toggleGov.isChecked()) {
                adminLayout.setVisibility(View.GONE);
            }
        });
    }

    private void setupFormToggleListeners() {
        // User
        btnUserSignIn.setOnClickListener(v -> {
            userSignInForm.setVisibility(View.VISIBLE);
            userSignUpForm.setVisibility(View.GONE);
        });
        btnUserSignUp.setOnClickListener(v -> {
            userSignUpForm.setVisibility(View.VISIBLE);
            userSignInForm.setVisibility(View.GONE);
        });

        // Gov
        btnGovSignIn.setOnClickListener(v -> {
            govSignInForm.setVisibility(View.VISIBLE);
            govSignUpForm.setVisibility(View.GONE);
        });
        btnGovSignUp.setOnClickListener(v -> {
            govSignUpForm.setVisibility(View.VISIBLE);
            govSignInForm.setVisibility(View.GONE);
        });

        // Admin
        btnAdminSignIn.setOnClickListener(v -> {
            adminSignInForm.setVisibility(View.VISIBLE);
            adminSignUpForm.setVisibility(View.GONE);
        });
        btnAdminSignUp.setOnClickListener(v -> {
            adminSignUpForm.setVisibility(View.VISIBLE);
            adminSignInForm.setVisibility(View.GONE);
        });
    }

    private void setupAuthButtonListeners() {
        // --- User Registration ---
        btnUserRegister.setOnClickListener(v -> {
            String name = userSignUpName.getText().toString().trim();
            String email = userSignUpEmail.getText().toString().trim();
            String location = userSignUpLocation.getText().toString().trim();
            String password = userSignUpPassword.getText().toString().trim();
            String confirmPassword = userSignUpConfirm.getText().toString().trim();

            if (TextUtils.isEmpty(name)) { userSignUpName.setError("Name is required"); return; }
            if (TextUtils.isEmpty(email)) { userSignUpEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(location)) { userSignUpLocation.setError("Location is required"); return; }
            if (password.length() < 6) { userSignUpPassword.setError("Password too short (min 6)"); return; }
            if (!password.equals(confirmPassword)) { userSignUpConfirm.setError("Passwords do not match"); return; }

            createUserAccount(name, email, password, location, "user", null);
        });

        // --- User Login ---
        btnUserLogin.setOnClickListener(v -> {
            String email = userSignInEmail.getText().toString().trim();
            String password = userSignInPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) { userSignInEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(password)) { userSignInPassword.setError("Password is required"); return; }
            signInUser(email, password);
        });

        // --- Gov/Authority Registration ---
        btnGovRegister.setOnClickListener(v -> {
            String name = govSignUpName.getText().toString().trim();
            String email = govSignUpEmail.getText().toString().trim();
            String govId = govSignUpId.getText().toString().trim();
            String location = govSignUpLocation.getText().toString().trim();
            String password = govSignUpPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) { govSignUpName.setError("Name is required"); return; }
            if (TextUtils.isEmpty(email)) { govSignUpEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(govId)) { govSignUpId.setError("Gov ID is required"); return; }
            if (TextUtils.isEmpty(location)) { govSignUpLocation.setError("Location is required"); return; }
            if (password.length() < 6) { govSignUpPassword.setError("Password too short (min 6)"); return; }

            createUserAccount(name, email, password, location, "authority", govId);
        });

        // --- Gov/Authority Login ---
        btnGovLogin.setOnClickListener(v -> {
            String email = govSignInEmail.getText().toString().trim();
            String password = govSignInPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) { govSignInEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(password)) { govSignInPassword.setError("Password is required"); return; }
            signInUser(email, password);
        });

        // --- Admin Registration ---
        btnAdminRegister.setOnClickListener(v -> {
            String name = adminSignUpName.getText().toString().trim();
            String email = adminSignUpEmail.getText().toString().trim();
            String location = adminSignUpLocation.getText().toString().trim();
            String password = adminSignUpPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) { adminSignUpName.setError("Name is required"); return; }
            if (TextUtils.isEmpty(email)) { adminSignUpEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(location)) { adminSignUpLocation.setError("Location is required"); return; }
            if (password.length() < 6) { adminSignUpPassword.setError("Password too short (min 6)"); return; }

            createUserAccount(name, email, password, location, "admin", null);
        });

        // --- Admin Login ---
        btnAdminLogin.setOnClickListener(v -> {
            String email = adminSignInEmail.getText().toString().trim();
            String password = adminSignInPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) { adminSignInEmail.setError("Email is required"); return; }
            if (TextUtils.isEmpty(password)) { adminSignInPassword.setError("Password is required"); return; }
            signInUser(email, password);
        });
    }

    private void setupLocationButtonListeners() {
        btnGetUserLocation.setOnClickListener(v -> {
            targetLocationEditText = userSignUpLocation;
            checkLocationPermissionAndFetch();
        });

        btnGetGovLocation.setOnClickListener(v -> {
            targetLocationEditText = govSignUpLocation;
            checkLocationPermissionAndFetch();
        });

        btnGetAdminLocation.setOnClickListener(v -> {
            targetLocationEditText = adminSignUpLocation;
            checkLocationPermissionAndFetch();
        });
    }

    private void setupLocationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                fetchLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Feature unavailable.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && targetLocationEditText != null) {
                        getAddressFromLocation(location);
                    } else {
                        Toast.makeText(this, "Could not get location. Make sure GPS is enabled on your device.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String fullAddress = address.getAddressLine(0);

                if (city != null) {
                    targetLocationEditText.setText(city);
                } else if (fullAddress != null) {
                    targetLocationEditText.setText(fullAddress);
                } else {
                    Toast.makeText(this, "Could not determine city from location.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Address not found for this location.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available", e);
            Toast.makeText(this, "Could not get address. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createUserAccount(String name, String email, String password, String location, final String role, final String govId) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, role + " createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserDetails(firebaseUser.getUid(), name, email, role, location, govId, firebaseUser);
                        }
                    } else {
                        Log.w(TAG, role + " createUserWithEmail:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDetails(String userId, String name, String email, String role, String location, String govId, FirebaseUser userToSignOut) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("location", location);

        if ("authority".equals(role) && govId != null) {
            userMap.put("govId", govId);
        }

        db.collection("users").document(userId).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    String roleCapitalized = role.substring(0, 1).toUpperCase() + role.substring(1);
                    Toast.makeText(MainActivity.this, roleCapitalized + " registration successful! Please log in.", Toast.LENGTH_LONG).show();

                    if (userToSignOut != null) {
                        mAuth.signOut();
                    }

                    if ("user".equals(role)) {
                        userSignUpForm.setVisibility(View.GONE);
                        userSignInForm.setVisibility(View.VISIBLE);
                    } else if ("authority".equals(role)) {
                        govSignUpForm.setVisibility(View.GONE);
                        govSignInForm.setVisibility(View.VISIBLE);
                    } else if ("admin".equals(role)) {
                        adminSignUpForm.setVisibility(View.GONE);
                        adminSignInForm.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving user details", e);
                    Toast.makeText(MainActivity.this, "Failed to save details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (userToSignOut != null) {
                        mAuth.signOut();
                    }
                });
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            fetchUserRoleAndNavigate(firebaseUser, false); // Pass false for manual login
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndNavigate(FirebaseUser firebaseUser, boolean isAutoLogin) {
        DocumentReference userDocRef = db.collection("users").document(firebaseUser.getUid());
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String role = document.getString("role");
                    if (role != null) {
                        // For auto-login, navigate directly without checking the UI toggle state.
                        // For manual login, verify the role against the selected toggle.
                        if (isAutoLogin ||
                                (toggleUser.isChecked() && "user".equals(role)) ||
                                (toggleGov.isChecked() && "authority".equals(role)) ||
                                (toggleAdmin.isChecked() && "admin".equals(role))) {
                            navigateToDashboard(firebaseUser, role);
                        } else {
                            Toast.makeText(MainActivity.this, "Incorrect role selected for this account.", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Sign out if trying to log into the wrong role section
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "User role not found in database.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "User data not found. Please register.", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                }
            } else {
                Log.w(TAG, "Error getting user document: ", task.getException());
                Toast.makeText(MainActivity.this, "Failed to retrieve user data.", Toast.LENGTH_LONG).show();
                mAuth.signOut();
            }
        });
    }

    // =========================================================================================
    // === THIS IS THE ONLY METHOD THAT HAS BEEN CHANGED.                                    ===
    // === It now correctly navigates for all three user roles.                            ===
    // =========================================================================================
    private void navigateToDashboard(FirebaseUser user, String role) {
        Intent intent;
        switch (role) {
            case "user":
                intent = new Intent(MainActivity.this, UserDashboardActivity.class);
                break;
            case "authority":
                intent = new Intent(MainActivity.this, AuthorityDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                break;
            default:
                Log.e(TAG, "Unknown role for navigation: " + role);
                mAuth.signOut();
                return; // Do not proceed
        }

        String welcomeMessage = "Welcome " + role.substring(0, 1).toUpperCase() + role.substring(1) + "!";
        Toast.makeText(MainActivity.this, welcomeMessage, Toast.LENGTH_SHORT).show();

        // Pass user information to the next activity
        if (user != null) {
            intent.putExtra("USER_UID", user.getUid());
            intent.putExtra("USER_EMAIL", user.getEmail());
        }
        intent.putExtra("USER_ROLE", role);

        // Start the new activity and finish this one
        startActivity(intent);
        finish(); // Finish MainActivity so the user cannot go back to it
    }
}
