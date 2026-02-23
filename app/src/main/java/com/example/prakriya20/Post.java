package com.example.prakriya20;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Post implements Serializable {

    // Existing fields
    private String title;
    private String description;
    private String issueType;
    private String imageDataString;
    private String userId;
    private String userName;
    private String userProfileImageString;
    private double latitude;
    private double longitude;
    private String address;
    private double imageGeoLat;
    private double imageGeoLng;
    private String status;
    @ServerTimestamp
    private Date timestamp;
    private String postId;
    private int upvotes = 0;
    private List<String> upvotedBy = new ArrayList<>();

    // =============================================================
    // === NEW FIELD: This tracks all users who reported the issue ===
    // =============================================================
    private List<String> originalReporterIds = new ArrayList<>();
    // =============================================================


    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    // Your existing constructor is fine and does not need to be changed.
    public Post(String title, String description, String issueType, String imageDataString, String userId, String userName, String userProfileImageString, double latitude, double longitude, String address, String status, double imageGeoLat, double imageGeoLng) {
        this.title = title;
        this.description = description;
        this.issueType = issueType;
        this.imageDataString = imageDataString;
        this.userId = userId;
        this.userName = userName;
        this.userProfileImageString = userProfileImageString;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status;
        this.imageGeoLat = imageGeoLat;
        this.imageGeoLng = imageGeoLng;
    }

    // Getters and Setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getImageDataString() { return imageDataString; }
    public void setImageDataString(String imageDataString) { this.imageDataString = imageDataString; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserProfileImageString() { return userProfileImageString; }
    public void setUserProfileImageString(String userProfileImageString) { this.userProfileImageString = userProfileImageString; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getImageGeoLat() { return imageGeoLat; }
    public void setImageGeoLat(double imageGeoLat) { this.imageGeoLat = imageGeoLat; }
    public double getImageGeoLng() { return imageGeoLng; }
    public void setImageGeoLng(double imageGeoLng) { this.imageGeoLng = imageGeoLng; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public List<String> getUpvotedBy() { return upvotedBy; }
    public void setUpvotedBy(List<String> upvotedBy) { this.upvotedBy = upvotedBy; }

    // =============================================================
    // === NEW GETTER/SETTER for the new field                   ===
    // =============================================================
    public List<String> getOriginalReporterIds() {
        return originalReporterIds;
    }
    public void setOriginalReporterIds(List<String> originalReporterIds) {
        this.originalReporterIds = originalReporterIds;
    }
    // =============================================================
}
