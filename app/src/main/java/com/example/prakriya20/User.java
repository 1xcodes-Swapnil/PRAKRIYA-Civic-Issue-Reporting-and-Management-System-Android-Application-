// File: User.java
// Location: app/src/main/java/com/example/prakriya20/User.java

package com.example.prakriya20;

public class User {
    private String name;
    private String email;
    private String location;
    private String profileImageString; // We'll store the profile image as a Base64 string

    // Required for Firebase
    public User() {}

    public User(String name, String email, String location) {
        this.name = name;
        this.email = email;
        this.location = location;
        this.profileImageString = ""; // Default empty
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getLocation() { return location; }
    public String getProfileImageString() { return profileImageString; }
}

