package com.example.prakriya20;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class VerificationDialog extends DialogFragment {

    public interface OnVerificationCompleteListener {
        void onComplete(String postId, String newStatus);
    }

    private static final String ARG_POST = "post_object";
    private Post post;
    private FirebaseFirestore db;
    private OnVerificationCompleteListener listener;

    private ImageView dialogReporterAvatar, dialogIssueImage;
    private TextView dialogReporterName, dialogIssueTitle, dialogIssueDescription,
            dialogGeotaggedLocation, dialogUserEnteredLocation, dialogDistance;
    private Button btnSpam, btnVerified;

    public static VerificationDialog newInstance(Post post, OnVerificationCompleteListener listener) {
        VerificationDialog fragment = new VerificationDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST, post);
        fragment.setArguments(args);
        fragment.setListener(listener);
        return fragment;
    }

    public void setListener(OnVerificationCompleteListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = (Post) getArguments().getSerializable(ARG_POST);
        }
        db = FirebaseFirestore.getInstance();
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_verification, container, false);
        findViews(view);
        populateViews();
        btnVerified.setOnClickListener(v -> updatePostStatus("Verified"));
        btnSpam.setOnClickListener(v -> updatePostStatus("Spam"));
        return view;
    }

    private void updatePostStatus(String newStatus) {
        if (post.getPostId() == null || post.getPostId().isEmpty()) {
            Toast.makeText(getContext(), "Error: Post ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("posts").document(post.getPostId()).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post marked as " + newStatus, Toast.LENGTH_SHORT).show();

                    // ====================================================================
                    // === NOTIFICATION LOGIC - THIS IS THE ONLY ADDITION             ===
                    // ====================================================================
                    String originalPosterId = post.getUserId();
                    String postId = post.getPostId();
                    String issueTitle = post.getTitle();

                    if ("Verified".equals(newStatus)) {
                        String title = "Issue Verified";
                        String message = "Your report '" + issueTitle + "' has been verified and forwarded to the relevant authority.";
                        NotificationManager.sendNotification(originalPosterId, postId, title, message, "Verified");
                    } else if ("Spam".equals(newStatus)) {
                        String title = "Suspected Spam";
                        String message = "Your report '" + issueTitle + "' has been flagged as suspected spam and is under review.";
                        NotificationManager.sendNotification(originalPosterId, postId, title, message, "Spam");
                    }
                    // ====================================================================

                    if (listener != null) {
                        listener.onComplete(post.getPostId(), newStatus);
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update status.", Toast.LENGTH_SHORT).show());
    }

    // All other methods below are UNCHANGED.
    private void findViews(View view) {
        dialogReporterAvatar = view.findViewById(R.id.dialogReporterAvatar);
        dialogIssueImage = view.findViewById(R.id.dialogIssueImage);
        dialogReporterName = view.findViewById(R.id.dialogReporterName);
        dialogIssueTitle = view.findViewById(R.id.dialogIssueTitle);
        dialogIssueDescription = view.findViewById(R.id.dialogIssueDescription);
        dialogGeotaggedLocation = view.findViewById(R.id.dialogGeotaggedLocation);
        dialogUserEnteredLocation = view.findViewById(R.id.dialogUserEnteredLocation);
        dialogDistance = view.findViewById(R.id.dialogDistance);
        btnSpam = view.findViewById(R.id.btnSpam);
        btnVerified = view.findViewById(R.id.btnVerified);
    }

    private void populateViews() {
        if (post == null) return;
        dialogReporterName.setText(post.getUserName());
        loadImageFromBase64(post.getUserProfileImageString(), dialogReporterAvatar, R.drawable.ic_user_placeholder);
        dialogIssueTitle.setText(post.getTitle());
        dialogIssueDescription.setText(post.getDescription());
        loadImageFromBase64(post.getImageDataString(), dialogIssueImage, R.drawable.pothole_image);
        dialogUserEnteredLocation.setText(post.getAddress());
        checkGeoTagAndSetLocation();
    }

    private void loadImageFromBase64(String base64String, ImageView imageView, int placeholderResId) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
                Glide.with(this).load(imageBytes).placeholder(placeholderResId).error(placeholderResId).into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(placeholderResId);
            }
        } else {
            imageView.setImageResource(placeholderResId);
        }
    }

    private void checkGeoTagAndSetLocation() {
        if (post.getImageGeoLat() != 0 && post.getImageGeoLng() != 0) {
            Location imageLocation = new Location("Image");
            imageLocation.setLatitude(post.getImageGeoLat());
            imageLocation.setLongitude(post.getImageGeoLng());
            Location postLocation = new Location("Post");
            postLocation.setLatitude(post.getLatitude());
            postLocation.setLongitude(post.getLongitude());
            float distance = imageLocation.distanceTo(postLocation);
            dialogDistance.setText(String.format(Locale.US, "%.2f meters", distance));
            if (distance < 200) {
                dialogDistance.setTextColor(getResources().getColor(R.color.status_resolved));
            } else {
                dialogDistance.setTextColor(getResources().getColor(R.color.status_progress));
            }
            if (getContext() != null) {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(post.getImageGeoLat(), post.getImageGeoLng(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        dialogGeotaggedLocation.setText(addresses.get(0).getAddressLine(0));
                    } else {
                        dialogGeotaggedLocation.setText("Address not found");
                    }
                } catch (IOException e) {
                    dialogGeotaggedLocation.setText("Error finding address");
                }
            }
        } else {
            dialogGeotaggedLocation.setText("Not available in image");
            dialogDistance.setText("N/A");
            dialogDistance.setTextColor(getResources().getColor(R.color.status_unresolved));
        }
    }
}
