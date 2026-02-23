package com.example.prakriya20;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creates and saves a notification to the 'notifications' collection in Firestore.
     *
     * @param userId  The ID of the user who should receive the notification.
     * @param postId  The ID of the related post.
     * @param title   The title of the notification (e.g., "Issue Verified").
     * @param message The detailed message for the notification.
     * @param type    A category for the notification (e.g., "Verified", "Spam").
     */
    public static void sendNotification(String userId, String postId, String title, String message, String type) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot send notification: userId is null or empty.");
            return;
        }

        Notification notification = new Notification(userId, postId, title, message, type);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Notification sent successfully to user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send notification", e));
    }
}
