package com.example.prakriya20;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {

    private String notificationId;
    private String userId; // The ID of the user who will receive the notification
    private String postId; // The ID of the post this notification is about
    private String message;
    private String title;
    private String type; // e.g., "Verified", "Resolved", "Spam"
    @ServerTimestamp
    private Date timestamp;

    // Must have a public no-argument constructor for Firestore
    public Notification() {}

    public Notification(String userId, String postId, String title, String message, String type) {
        this.userId = userId;
        this.postId = postId;
        this.title = title;
        this.message = message;
        this.type = type;
    }

    // --- Getters and Setters ---
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
