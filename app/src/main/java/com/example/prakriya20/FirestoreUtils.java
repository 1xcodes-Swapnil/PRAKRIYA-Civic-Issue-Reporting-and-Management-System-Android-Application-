package com.example.prakriya20;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class FirestoreUtils {

    private static final String TAG = "FirestoreUtils";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Listener to report back when deletion is complete
    public interface OnDeleteCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Deletes a post and all associated notifications in a single transaction.
     * @param postId The ID of the post to delete.
     * @param listener A callback to notify the calling activity of success or failure.
     */
    public static void deletePostAndNotifications(String postId, OnDeleteCompleteListener listener) {
        if (postId == null || postId.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Post ID cannot be null or empty."));
            }
            return;
        }

        // Step 1: Find all notifications with the matching postId
        db.collection("notifications")
                .whereEqualTo("postId", postId)
                .get()
                .addOnCompleteListener(notificationTask -> {
                    if (!notificationTask.isSuccessful()) {
                        if (listener != null) {
                            listener.onFailure(notificationTask.getException());
                        }
                        return;
                    }

                    // Step 2: Prepare a batch write to delete everything at once
                    WriteBatch batch = db.batch();

                    // Add the main post to the delete batch
                    batch.delete(db.collection("posts").document(postId));

                    // Add every found notification to the delete batch
                    for (QueryDocumentSnapshot document : notificationTask.getResult()) {
                        batch.delete(db.collection("notifications").document(document.getId()));
                    }

                    // Step 3: Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully deleted post and all related notifications for postId: " + postId);
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting post and notifications for postId: " + postId, e);
                                if (listener != null) {
                                    listener.onFailure(e);
                                }
                            });
                });
    }
}
