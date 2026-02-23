package com.example.prakriya20;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    private String userId;
    private String userName;
    private String text;

    @ServerTimestamp
    private Date timestamp;

    // Required for Firestore
    public Comment() {}

    public Comment(String userId, String userName, String text) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getText() { return text; }
    public Date getTimestamp() { return timestamp; }
}
