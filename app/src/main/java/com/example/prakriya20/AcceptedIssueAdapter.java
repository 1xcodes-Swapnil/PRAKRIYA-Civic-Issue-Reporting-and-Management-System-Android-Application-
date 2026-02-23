package com.example.prakriya20;

import android.app.ProgressDialog;
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
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AcceptedIssueAdapter extends RecyclerView.Adapter<AcceptedIssueAdapter.ViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AcceptedIssueAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_accepted_issue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post == null) return;

        // UI Logic to handle both single and merged posts
        List<String> reporters = post.getOriginalReporterIds();
        if (reporters != null && reporters.size() > 1) {
            String mergedTitle = reporters.size() + " users reported this";
            holder.userName.setText(mergedTitle);
            holder.postTitle.setText(post.getTitle());
        } else {
            holder.userName.setText(post.getUserName());
            holder.postTitle.setText(post.getIssueType());
        }

        holder.postDescription.setText(post.getDescription());
        holder.statusText.setText("In Progress");
        holder.statusDot.setBackgroundResource(R.drawable.status_dot_yellow);

        PostManager.loadImageFromBase64(post.getUserProfileImageString(), holder.userAvatar, context);
        if (post.getImageDataString() != null && !post.getImageDataString().isEmpty()) {
            byte[] imageBytes = Base64.decode(post.getImageDataString(), Base64.DEFAULT);
            Glide.with(context).load(imageBytes).into(holder.postImage);
        }

        holder.btnResolveIssue.setOnClickListener(v -> resolveIssue(post, holder.getAdapterPosition()));
    }

    private void resolveIssue(Post post, int position) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Resolving issue...");
        progressDialog.show();

        DocumentReference postRef = db.collection("posts").document(post.getPostId());

        postRef.update("status", "Resolved")
                .addOnSuccessListener(aVoid -> {
                    // Fetch the latest post data to get the full list of reporters
                    postRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Post updatedPost = documentSnapshot.toObject(Post.class);
                            if (updatedPost != null && updatedPost.getOriginalReporterIds() != null) {
                                String issueTitle = updatedPost.getTitle();
                                String notificationTitle = "Issue Resolved!";
                                String notificationMessage = "The issue you reported, '" + issueTitle + "', has been successfully resolved. Thank you for your contribution!";

                                // Loop through every original reporter and send them a notification.
                                for (String reporterId : updatedPost.getOriginalReporterIds()) {
                                    NotificationManager.sendNotification(
                                            reporterId,
                                            post.getPostId(), // Use the main post's ID
                                            notificationTitle,
                                            notificationMessage,
                                            "Resolved"
                                    );
                                }
                            }
                        }
                    });

                    progressDialog.dismiss();
                    Toast.makeText(context, "Issue marked as Resolved!", Toast.LENGTH_SHORT).show();

                    postList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, postList.size());
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to resolve issue.", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar, postImage;
        TextView userName, postTitle, postDescription, statusText;
        Button btnResolveIssue;
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
            btnResolveIssue = itemView.findViewById(R.id.btnResolveIssue);
        }
    }
}
