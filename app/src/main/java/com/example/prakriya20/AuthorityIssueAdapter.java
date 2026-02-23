package com.example.prakriya20;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AuthorityIssueAdapter extends RecyclerView.Adapter<AuthorityIssueAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AuthorityIssueAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_authority_issue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post == null) return;

        // ===================================================================
        // === FIX #1: CORRECTLY DISPLAYING THE MERGED POST TITLE          ===
        // ===================================================================
        List<String> reporters = post.getOriginalReporterIds();
        if (reporters != null && reporters.size() > 1) {
            // This IS a merged post.
            String mergedTitle = reporters.size() + " users reported this";
            holder.userName.setText(mergedTitle);
            holder.postTitle.setText(post.getTitle()); // Use the actual title of the issue
        } else {
            // This is a SINGLE post.
            holder.userName.setText(post.getUserName());
            holder.postTitle.setText(post.getIssueType());
        }
        holder.postDescription.setText(post.getDescription());
        // ===================================================================

        // Load user avatar and post image
        PostManager.loadImageFromBase64(post.getUserProfileImageString(), holder.userAvatar, context);
        if (post.getImageDataString() != null && !post.getImageDataString().isEmpty()) {
            byte[] imageBytes = android.util.Base64.decode(post.getImageDataString(), android.util.Base64.DEFAULT);
            Glide.with(context).load(imageBytes).into(holder.postImage);
            holder.postImage.setVisibility(View.VISIBLE);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Set the status UI
        holder.statusText.setText("Verified");
        holder.statusDot.setBackgroundResource(R.drawable.status_dot_red);

        // Set the listener for the accept button
        holder.btnAccept.setOnClickListener(v -> acceptIssue(post, holder.getAdapterPosition()));
    }

    // =================================================================================
    // === FIX #2: CORRECTLY SENDING NOTIFICATIONS TO ALL USERS ON "ACCEPT"        ===
    // =================================================================================
    private void acceptIssue(Post post, int position) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Accepting issue...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DocumentReference postRef = db.collection("posts").document(post.getPostId());

        postRef.update("status", "In Progress")
                .addOnSuccessListener(aVoid -> {
                    // --- SEND NOTIFICATION TO ALL REPORTERS ---
                    String issueTitle = post.getTitle();
                    String notificationTitle = "Issue In Progress";
                    String notificationMessage = "The issue you reported, '" + issueTitle + "', has been accepted and is now in progress.";

                    // Use the 'originalReporterIds' list from the post object.
                    // This list contains ALL the user IDs from the merge.
                    if (post.getOriginalReporterIds() != null && !post.getOriginalReporterIds().isEmpty()) {
                        for (String reporterId : post.getOriginalReporterIds()) {
                            NotificationManager.sendNotification(
                                    reporterId,          // The ID of the user to notify
                                    post.getPostId(),      // The ID of the main merged post
                                    notificationTitle,
                                    notificationMessage,
                                    "In Progress"        // The type for the notification
                            );
                        }
                    }
                    // --- END OF NOTIFICATION LOGIC ---

                    progressDialog.dismiss();
                    Toast.makeText(context, "Issue Accepted. Find it in the 'Accepted' tab.", Toast.LENGTH_LONG).show();

                    // Remove the item from this adapter's list and update the UI
                    postList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, postList.size());
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to accept issue.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar, postImage;
        TextView userName, postTitle, postDescription, statusText;
        Button btnAccept;
        View statusDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            postImage = itemView.findViewById(R.id.post_image);
            userName = itemView.findViewById(R.id.user_name);
            postTitle = itemView.findViewById(R.id.post_title);
            postDescription = itemView.findViewById(R.id.post_description);
            statusText = itemView.findViewById(R.id.status_text);
            statusDot = itemView.findViewById(R.id.status_dot);
            btnAccept = itemView.findViewById(R.id.btnAcceptIssue);
        }
    }
}
