package com.example.prakriya20;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuplicateGroupAdapter extends RecyclerView.Adapter<DuplicateGroupAdapter.ViewHolder> {

    private final Context context;
    private final List<List<Post>> duplicateGroups;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public DuplicateGroupAdapter(Context context, List<List<Post>> duplicateGroups) {
        this.context = context;
        this.duplicateGroups = duplicateGroups;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_duplicate_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<Post> group = duplicateGroups.get(position);
        if (group == null || group.isEmpty()) return;

        Post representativePost = group.get(0);
        String title = String.format("%s (%d reports)", representativePost.getIssueType(), group.size());
        holder.tvGroupTitle.setText(title);

        IndividualPostAdapter individualAdapter = new IndividualPostAdapter(context, group);
        holder.rvIndividualIssues.setLayoutManager(new LinearLayoutManager(context));
        holder.rvIndividualIssues.setAdapter(individualAdapter);

        individualAdapter.setOnItemHandledListener(() -> checkGroupCompletion(holder, group));
        checkGroupCompletion(holder, group);
        holder.btnMergeAndForward.setOnClickListener(v -> mergeAndForward(group, holder.getAdapterPosition()));
    }

    private void checkGroupCompletion(ViewHolder holder, List<Post> group) {
        if (group == null || group.isEmpty()) {
            holder.btnMergeAndForward.setEnabled(false);
            return;
        }
        boolean allHandled = true;
        for (Post post : group) {
            if (!"Verified".equals(post.getStatus()) && !"Spam".equals(post.getStatus())) {
                allHandled = false;
                break;
            }
        }
        holder.btnMergeAndForward.setEnabled(allHandled);
    }

    private void mergeAndForward(List<Post> group, int adapterPosition) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Processing Group...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        List<Post> verifiedPosts = group.stream()
                .filter(p -> "Verified".equals(p.getStatus()))
                .collect(Collectors.toList());

        List<Post> spamPosts = group.stream()
                .filter(p -> "Spam".equals(p.getStatus()))
                .collect(Collectors.toList());

        if (verifiedPosts.isEmpty()) {
            // Case: No posts were verified, only spam. Just delete them.
            if (!spamPosts.isEmpty()) {
                WriteBatch deleteBatch = db.batch();
                for (Post spamPost : spamPosts) {
                    deleteBatch.delete(db.collection("posts").document(spamPost.getPostId()));
                }
                deleteBatch.commit().addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Spam posts removed.", Toast.LENGTH_SHORT).show();
                    removeGroup(adapterPosition);
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to remove spam posts.", Toast.LENGTH_SHORT).show();
                });
            } else {
                progressDialog.dismiss();
            }
            return;
        }

        // --- NEW "PROMOTE-AND-DELETE" LOGIC ---
        // 1. Promote the FIRST verified post to be the main one.
        Post mainPost = verifiedPosts.get(0);
        DocumentReference mainPostRef = db.collection("posts").document(mainPost.getPostId());

        // 2. Prepare the list of all other posts in the group to delete.
        List<Post> postsToDelete = new ArrayList<>();
        postsToDelete.addAll(spamPosts);
        for (int i = 1; i < verifiedPosts.size(); i++) {
            postsToDelete.add(verifiedPosts.get(i)); // Add all other verified posts (but not the main one)
        }

        // 3. Calculate merged data
        int totalUpvotes = verifiedPosts.stream().mapToInt(Post::getUpvotes).sum();
        List<String> allReporterIds = verifiedPosts.stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 4. Create a batch write to do everything at once.
        WriteBatch batch = db.batch();

        // Operation 1: Update the main post with merged data.
        batch.update(mainPostRef, "upvotes", totalUpvotes);
        batch.update(mainPostRef, "originalReporterIds", allReporterIds);
        batch.update(mainPostRef, "status", "Verified"); // Ensure its status is ready for the authority.

        // Operation 2: Delete all other posts.
        for (Post postToDelete : postsToDelete) {
            batch.delete(db.collection("posts").document(postToDelete.getPostId()));
        }

        // 5. Commit the batch and handle the result.
        batch.commit().addOnSuccessListener(aVoid -> {
            progressDialog.dismiss();
            Toast.makeText(context, "Group merged and forwarded!", Toast.LENGTH_LONG).show();

            // Send notifications to ALL reporters
            String notificationTitle = "Issue Merged & Forwarded";
            String notificationMessage = "Your report '" + mainPost.getTitle() + "' has been merged with " + (allReporterIds.size() - 1) + " other similar reports and forwarded to the authority.";
            for (String reporterId : allReporterIds) {
                NotificationManager.sendNotification(
                        reporterId,
                        mainPost.getPostId(), // Use the ID of the ONE post that survived.
                        notificationTitle,
                        notificationMessage,
                        "Verified"
                );
            }
            removeGroup(adapterPosition);

        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(context, "Error processing group: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("DuplicateGroupAdapter", "Error in merge batch commit", e);
        });
    }

    private void removeGroup(int position) {
        if (position >= 0 && position < duplicateGroups.size()) {
            duplicateGroups.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
    }

    @Override
    public int getItemCount() {
        return duplicateGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupTitle;
        RecyclerView rvIndividualIssues;
        Button btnMergeAndForward;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            rvIndividualIssues = itemView.findViewById(R.id.rvIndividualIssues);
            btnMergeAndForward = itemView.findViewById(R.id.btnMergeAndForward);
        }
    }
}
