package com.example.prakriya20;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notice {
    private String title;
    private String description;
    private String location;
    private String dateRange;
    private String noticeId;

    // --- FIELD TO ADD ---
    private String userId; // This will store the UID of the user who posted the notice.

    @ServerTimestamp
    private Date timestamp;

    // Required empty constructor for Firestore
    public Notice() {}

    public Notice(String title, String description, String location, String dateRange) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.dateRange = dateRange;
    }

    // --- GETTERS ---
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getDateRange() { return dateRange; }
    public String getNoticeId() { return noticeId; }
    public Date getTimestamp() { return timestamp; }

    // --- NEW GETTER ---
    public String getUserId() { return userId; }

    // --- SETTERS ---
    public void setNoticeId(String noticeId) { this.noticeId = noticeId; }

    // --- NEW SETTER ---
    public void setUserId(String userId) { this.userId = userId; }
}
