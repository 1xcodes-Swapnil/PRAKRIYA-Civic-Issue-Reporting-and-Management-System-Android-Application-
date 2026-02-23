package com.example.prakriya20;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> postList;
    private final Context context;

    public PostAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post); // Pass only the post object
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.postListener != null) {
            holder.postListener.remove();
        }
    }

    // ==============================================================
    // === REMOVED THE 'static' KEYWORD HERE - THIS IS THE ENTIRE FIX ===
    // ==============================================================
    class PostViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "PostAdapter";

        // All your views
        ImageView postImageView, imgUpvote, btnShare, userProfileImageView;
        TextView userNameTextView, postStatusTextView, postDescriptionTextView, postLocationTextView, postDateTextView, tvUpvoteCount, tvCommentCount;
        LinearLayout btnUpvote, btnComment, commentSection, commentsContainer;
        EditText etComment;
        Button btnPostComment;

        private final FirebaseFirestore db = FirebaseFirestore.getInstance();
        private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        private ListenerRegistration postListener;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // All your findViewById calls
            postImageView = itemView.findViewById(R.id.postImageView);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            postStatusTextView = itemView.findViewById(R.id.postStatusTextView);
            postDescriptionTextView = itemView.findViewById(R.id.postDescriptionTextView);
            postLocationTextView = itemView.findViewById(R.id.postLocationTextView);
            postDateTextView = itemView.findViewById(R.id.postDateTextView);
            btnUpvote = itemView.findViewById(R.id.btnUpvote);
            imgUpvote = itemView.findViewById(R.id.imgUpvote);
            tvUpvoteCount = itemView.findViewById(R.id.tvUpvoteCount);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnShare = itemView.findViewById(R.id.btnShare);
            commentSection = itemView.findViewById(R.id.commentSection);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            etComment = itemView.findViewById(R.id.etComment);
            btnPostComment = itemView.findViewById(R.id.btnPostComment);
        }

        // The context is now available from the outer class
        public void bind(final Post post) {
            final String currentUserId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "";
            if (post.getPostId() == null || post.getPostId().isEmpty()) {
                Log.e(TAG, "Post ID is null or empty, cannot bind view.");
                return;
            }
            final DocumentReference postRef = db.collection("posts").document(post.getPostId());

            if (postListener != null) {
                postListener.remove();
            }

            postListener = postRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot == null || !snapshot.exists()) {
                    Log.d(TAG, "Post document does not exist anymore: " + post.getPostId());
                    return;
                }

                Post updatedPost = snapshot.toObject(Post.class);
                if (updatedPost == null) return;

                // Bind all UI elements
                postLocationTextView.setText(updatedPost.getAddress());
                loadImage(updatedPost.getUserProfileImageString(), userProfileImageView, R.drawable.ic_user_placeholder);
                loadImage(updatedPost.getImageDataString(), postImageView, 0);

                if (updatedPost.getTimestamp() != null) {
                    postDateTextView.setText(new SimpleDateFormat("dd MMM", Locale.getDefault()).format(updatedPost.getTimestamp()));
                }

                // Logic to change the title for merged posts
                List<String> reporters = updatedPost.getOriginalReporterIds();
                if (reporters != null && reporters.size() > 1) {
                    String mergedTitle = reporters.size() + " users reported this";
                    userNameTextView.setText(mergedTitle);
                    postDescriptionTextView.setText(updatedPost.getTitle());
                } else {
                    userNameTextView.setText(updatedPost.getUserName());
                    postDescriptionTextView.setText(updatedPost.getDescription());
                }

                long upvotes = snapshot.contains("upvotes") ? snapshot.getLong("upvotes") : 0;
                tvUpvoteCount.setText(String.valueOf(upvotes));

                List<String> upvotedBy = updatedPost.getUpvotedBy() != null ? updatedPost.getUpvotedBy() : new ArrayList<>();
                if (upvotedBy.contains(currentUserId)) {
                    imgUpvote.setColorFilter(ContextCompat.getColor(context, R.color.prakriya_teal));
                } else {
                    imgUpvote.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
                }

                updateStatusUI(updatedPost.getStatus());
            });

            // Set Listeners
            btnUpvote.setOnClickListener(v -> {
                if (currentUserId.isEmpty()) {
                    Toast.makeText(context, "You must be logged in.", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.runTransaction(transaction -> {
                    DocumentReference docRef = db.collection("posts").document(post.getPostId());
                    Post latestPost = transaction.get(docRef).toObject(Post.class);
                    if (latestPost == null) return null;

                    List<String> currentUpvotedBy = latestPost.getUpvotedBy() != null ? latestPost.getUpvotedBy() : new ArrayList<>();
                    if (currentUpvotedBy.contains(currentUserId)) {
                        transaction.update(docRef, "upvotes", FieldValue.increment(-1));
                        transaction.update(docRef, "upvotedBy", FieldValue.arrayRemove(currentUserId));
                    } else {
                        transaction.update(docRef, "upvotes", FieldValue.increment(1));
                        transaction.update(docRef, "upvotedBy", FieldValue.arrayUnion(currentUserId));
                    }
                    return null;
                }).addOnFailureListener(err -> Log.w(TAG, "Upvote transaction failed.", err));
            });

            loadComments(post.getPostId());
            btnComment.setOnClickListener(v -> commentSection.setVisibility(commentSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            btnPostComment.setOnClickListener(v -> postNewComment(postRef, currentUserId));
            btnShare.setOnClickListener(v -> sharePost(post.getDescription()));
        }

        private void updateStatusUI(String status) {
            if (status == null) status = "Unresolved";
            int statusColor;
            String statusTextWithDot;
            switch (status) {
                case "Verified":
                    statusColor = ContextCompat.getColor(context, R.color.status_progress);
                    statusTextWithDot = "● Verified";
                    break;
                case "In Progress":
                    statusColor = ContextCompat.getColor(context, R.color.status_progress);
                    statusTextWithDot = "● In Progress";
                    break;
                case "Resolved":
                    statusColor = ContextCompat.getColor(context, R.color.status_resolved);
                    statusTextWithDot = "● Resolved";
                    break;
                default:
                    statusColor = ContextCompat.getColor(context, R.color.status_unresolved);
                    statusTextWithDot = "● Unresolved";
                    break;
            }
            postStatusTextView.setText(statusTextWithDot);
            postStatusTextView.setTextColor(statusColor);
        }

        private void loadImage(String base64String, ImageView imageView, int placeholderResId) {
            if (base64String != null && !base64String.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
                    Glide.with(context)
                            .load(imageBytes)
                            .placeholder(placeholderResId)
                            .error(placeholderResId)
                            .into(imageView);
                    imageView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    if (placeholderResId != 0) imageView.setImageResource(placeholderResId);
                    else imageView.setVisibility(View.GONE);
                }
            } else {
                if (placeholderResId != 0) imageView.setImageResource(placeholderResId);
                else imageView.setVisibility(View.GONE);
            }
        }

        private void sharePost(String description) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this issue on Prakriya: " + description);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        }

        private void postNewComment(DocumentReference postRef, String currentUserId) {
            String commentText = etComment.getText().toString().trim();
            if (commentText.isEmpty() || currentUserId.isEmpty()) return;
            db.collection("users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
                String currentUserName = userDoc.exists() ? userDoc.getString("name") : "Anonymous";
                Comment newComment = new Comment(currentUserId, currentUserName, commentText);
                postRef.collection("comments").add(newComment).addOnSuccessListener(ref -> {
                    Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show();
                    etComment.setText("");
                });
            });
        }

        private void loadComments(String postId) {
            db.collection("posts").document(postId).collection("comments")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null || snapshots == null) { return; }
                        commentsContainer.removeAllViews();
                        tvCommentCount.setText(String.valueOf(snapshots.size()));
                        if (!snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Comment comment = doc.toObject(Comment.class);
                                View commentView = LayoutInflater.from(context).inflate(R.layout.comment_item, commentsContainer, false);
                                ((TextView) commentView.findViewById(R.id.tvCommentUsername)).setText(comment.getUserName());
                                ((TextView) commentView.findViewById(R.id.tvCommentText)).setText(comment.getText());
                                commentsContainer.addView(commentView);
                            }
                        }
                    });
        }
    }
}
