package com.example.prakriya20;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminIssueAdapter extends RecyclerView.Adapter<AdminIssueAdapter.IssueViewHolder> {

    private final List<Post> postList;
    private final Context context;
    private String userRole = "Admin";

    public AdminIssueAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    public AdminIssueAdapter(Context context, List<Post> postList, String userRole) {
        this.context = context;
        this.postList = postList;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_issue_card, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        Post currentPost = postList.get(position);

        holder.reporterName.setText(currentPost.getUserName());
        holder.issueDescription.setText(currentPost.getDescription());
        holder.issueLocation.setText(currentPost.getAddress());
        if (currentPost.getTimestamp() != null) {
            holder.issueDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(currentPost.getTimestamp()));
        }
        PostManager.loadImageFromBase64(currentPost.getUserProfileImageString(), holder.reporterProfileImage, context);
        holder.issueStatus.setText("● " + currentPost.getStatus());
        switch (currentPost.getStatus()) {
            case "In Progress":
                holder.issueStatus.setTextColor(context.getResources().getColor(R.color.status_progress));
                break;
            case "Resolved":
                holder.issueStatus.setTextColor(context.getResources().getColor(R.color.status_resolved));
                break;
            default:
                holder.issueStatus.setTextColor(context.getResources().getColor(R.color.status_unresolved));
                break;
        }
        String imageDataString = currentPost.getImageDataString();
        if (imageDataString != null && !imageDataString.isEmpty()) {
            holder.issueImage.setVisibility(View.VISIBLE);
            try {
                byte[] imageBytes = Base64.decode(imageDataString, Base64.DEFAULT);
                Glide.with(context).load(imageBytes).centerCrop().placeholder(R.color.gray_light).into(holder.issueImage);
            } catch (Exception e) {
                holder.issueImage.setVisibility(View.GONE);
            }
        } else {
            holder.issueImage.setVisibility(View.GONE);
        }

        if ("Authority".equals(userRole)) {
            // Logic for Authority Dashboard
            holder.verifyButton.setText("Accept");
            holder.verifyButton.setOnClickListener(v -> {
                // When Authority clicks "Accept", update status to "In Progress"
                FirebaseFirestore.getInstance().collection("posts").document(currentPost.getPostId())
                        .update("status", "In Progress")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Issue Accepted. Status is now In Progress.", Toast.LENGTH_SHORT).show();

                            // ====================================================================
                            // === NOTIFICATION LOGIC - THIS IS THE ONLY ADDITION             ===
                            // ====================================================================
                            String originalPosterId = currentPost.getUserId();
                            String postId = currentPost.getPostId();
                            String issueTitle = currentPost.getTitle();
                            String title = "Issue In Progress";
                            String message = "Your report '" + issueTitle + "' has been accepted and is now under progress for resolution.";
                            NotificationManager.sendNotification(originalPosterId, postId, title, message, "In Progress");
                            // ====================================================================

                            postList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, postList.size());
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to accept issue.", Toast.LENGTH_SHORT).show());
            });

        } else {
            // Logic for Admin Dashboard
            holder.verifyButton.setText("Verify & Take Action");
            holder.verifyButton.setOnClickListener(v -> {
                if (context instanceof AppCompatActivity) {
                    FragmentManager fm = ((AppCompatActivity) context).getSupportFragmentManager();
                    VerificationDialog.OnVerificationCompleteListener listener = (postId, newStatus) -> {
                        postList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, postList.size());
                    };
                    VerificationDialog dialog = VerificationDialog.newInstance(currentPost, listener);
                    dialog.show(fm, "VerificationDialog");
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class IssueViewHolder extends RecyclerView.ViewHolder {
        ImageView reporterProfileImage, issueImage;
        TextView reporterName, issueStatus, issueDescription, issueLocation, issueDate;
        Button verifyButton;

        public IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            reporterProfileImage = itemView.findViewById(R.id.reporterProfileImageView);
            issueImage = itemView.findViewById(R.id.issueImageView);
            reporterName = itemView.findViewById(R.id.reporterNameTextView);
            issueStatus = itemView.findViewById(R.id.issueStatusTextView);
            issueDescription = itemView.findViewById(R.id.issueDescriptionTextView);
            issueLocation = itemView.findViewById(R.id.issueLocationTextView);
            issueDate = itemView.findViewById(R.id.issueDateTextView);
            verifyButton = itemView.findViewById(R.id.btnVerifyIssue);
        }
    }
}
