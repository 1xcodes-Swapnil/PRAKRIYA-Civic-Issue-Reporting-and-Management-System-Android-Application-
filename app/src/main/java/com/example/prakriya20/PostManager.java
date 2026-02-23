package com.example.prakriya20;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PostManager {

    private static final String TAG = "PostManager";
    private static volatile PostManager instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Data lists
    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> uniquePosts = new ArrayList<>();
    private final List<List<Post>> duplicatePostGroups = new ArrayList<>();

    // State
    private boolean isDataReady = false;

    // Listener Interface
    public interface OnDataReadyListener {
        void onDataReady();
        void onError(Exception e);
    }

    private PostManager() {
        // Private constructor
    }

    public static PostManager getInstance() {
        if (instance == null) {
            synchronized (PostManager.class) {
                if (instance == null) {
                    instance = new PostManager();
                }
            }
        }
        return instance;
    }

    // THIS IS THE VERSION THAT WORKS WITH YOUR AdminDashboardActivity
    // It fetches all posts and allows the dashboard to filter them.
    public void fetchAllPosts(final OnDataReadyListener listener) {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allPosts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            post.setPostId(document.getId());
                            allPosts.add(post);
                        }
                        processPosts(); // Process all fetched data
                        isDataReady = true;
                        if (listener != null) {
                            listener.onDataReady();
                        }
                    } else {
                        isDataReady = false;
                        if (listener != null) {
                            listener.onError(task.getException());
                        }
                        Log.e(TAG, "Error fetching posts for PostManager: ", task.getException());
                    }
                });
    }

    // All other methods are exactly as they were in your working version.
    private void processPosts() {
        findDuplicateGroups();
        findUniquePosts();
    }

    private void findDuplicateGroups() {
        duplicatePostGroups.clear();
        Map<String, List<Post>> groups = new HashMap<>();

        for (Post post : allPosts) {
            // This is the correct logic for your app, which now needs to filter.
            // We will filter statuses in the AdminDashboardActivity, not here.
            String status = post.getStatus();
            if (status != null && !status.equals("Verified") && !status.equals("Spam") && !status.equals("Resolved") && !status.equals("In Progress")) {

                if (post.getLatitude() == 0 && post.getLongitude() == 0) continue;

                String issueIdentifier = post.getIssueType();
                if (issueIdentifier == null || issueIdentifier.isEmpty() || issueIdentifier.equals("Select Issue Type")) {
                    issueIdentifier = post.getTitle().toLowerCase().trim();
                }

                String groupKey = issueIdentifier + "_" + Math.round(post.getLatitude() * 1000.0) / 1000.0 + "_" + Math.round(post.getLongitude() * 1000.0) / 1000.0;

                if (!groups.containsKey(groupKey)) {
                    groups.put(groupKey, new ArrayList<>());
                }
                groups.get(groupKey).add(post);
            }
        }

        for (Map.Entry<String, List<Post>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatePostGroups.add(entry.getValue());
            }
        }
    }

    private void findUniquePosts() {
        uniquePosts.clear();
        Set<String> duplicatePostIds = new HashSet<>();
        for (List<Post> group : duplicatePostGroups) {
            for (Post post : group) {
                duplicatePostIds.add(post.getPostId());
            }
        }
        for (Post post : allPosts) {
            if (!duplicatePostIds.contains(post.getPostId())) {
                uniquePosts.add(post);
            }
        }
    }

    public List<Post> getAllPosts() { return allPosts; }
    public List<Post> getUniquePosts() { return uniquePosts; }
    public List<List<Post>> getDuplicatePostGroups() { return duplicatePostGroups; }
    public boolean isDataReady() { return isDataReady; }

    public static void loadImageFromBase64(String base64String, ImageView imageView, Context context) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
                Glide.with(context).load(imageBytes).circleCrop().placeholder(R.drawable.ic_user_placeholder).into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_user_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_user_placeholder);
        }
    }
}
