# 📱 PRAKRIYA – Civic Issue Tracking & Engagement App

## 🏛️ Overview

**PRAKRIYA** is a multi-role Android application designed to improve civic engagement by enabling citizens to report, track, and monitor public infrastructure issues efficiently. The application bridges the communication gap between **citizens**, **administrative authorities**, and **municipal officials** through a structured digital workflow.

The platform allows real-time reporting and resolution management of civic problems such as:

* 🕳️ Potholes
* 💡 Broken streetlights
* 🗑️ Garbage accumulation
* 🚧 Infrastructure damage
* 📍 Local community issues

Built using **Java**, **XML**, and **Firebase Firestore**, PRAKRIYA provides a scalable and cost-optimized system for smart city governance.

---

## 🎯 Problem Statement

Traditional civic issue reporting systems suffer from:

* Lack of transparency
* Delayed responses
* Poor communication between citizens and authorities
* Duplicate or fake complaints
* No centralized tracking mechanism

PRAKRIYA solves these challenges by introducing a **location-verified**, **role-based**, and **trackable digital reporting system**.

---

## 🚀 Key Features

### 👤 Citizen/User Module

* Report civic issues with photos
* Automatic geotagging using GPS
* Issue category selection
* Real-time issue tracking
* Status updates and notifications
* View previously submitted complaints

### 🛡️ Administrator Module

* Verify submitted issues
* Location authenticity validation
* Duplicate complaint detection
* Approve or reject reports
* Forward verified issues to authorities

### 🏢 Authority Module

* Receive verified civic issues
* Update issue resolution status
* Monitor assigned complaints
* Manage resolution workflow

---

## 🧠 Core Functional Concepts

### 📍 Geotagging Verification

The application extracts GPS metadata from uploaded images and compares it with user-entered location coordinates.

* Distance calculated in meters
* Helps detect fake or misleading reports
* Improves reporting authenticity

---

### 🧾 Duplicate Issue Detection

A smart matching algorithm identifies repeated reports using:

* Issue category
* Location coordinates
* Proximity analysis

This prevents redundant processing and improves administrative efficiency.

---

### 💾 Cost-Optimized Image Storage

Instead of Firebase Storage:

* Images are converted into **Base64 encoded strings**
* Stored directly inside Firebase Firestore
* Decoded during retrieval

✅ Reduces cloud storage costs
✅ Simplifies infrastructure

---

## 🏗️ System Architecture

```
Android Application (Java + XML)
           │
           ▼
Role-Based Access Control
(User / Admin / Authority)
           │
           ▼
Firebase Firestore Database
           │
           ▼
Geolocation & Verification Engine
           │
           ▼
Issue Tracking & Resolution Workflow
```

---

## 🧑‍💻 Technology Stack

| Category          | Technology              |
| ----------------- | ----------------------- |
| Platform          | Android                 |
| Language          | Java                    |
| UI Design         | XML                     |
| Backend           | Firebase Firestore      |
| Authentication    | Firebase Authentication |
| Location Services | Android GPS API         |
| Image Handling    | Base64 Encoding         |
| IDE               | Android Studio          |

---

## 🔐 Role-Based Access Control

| Role      | Permissions                 |
| --------- | --------------------------- |
| Citizen   | Report & track issues       |
| Admin     | Validate and manage reports |
| Authority | Resolve approved issues     |

---

## 📊 Workflow

1. Citizen reports an issue with image + location.
2. Image metadata and GPS coordinates are verified.
3. Admin validates authenticity.
4. Duplicate detection checks existing complaints.
5. Approved issues forwarded to authorities.
6. Authority updates resolution progress.
7. Citizen tracks status in real time.

---

## 📱 Application Modules

* User Authentication
* Issue Reporting
* Image Processing
* Location Validation
* Admin Dashboard
* Authority Dashboard
* Issue Status Tracking

---

## ⚙️ Installation & Setup

### Prerequisites

* Android Studio (Latest Version)
* Java JDK 8+
* Firebase Project
* Android Device / Emulator

---

### Steps

1. Clone the repository:
  
2. Open project in **Android Studio**

3. Connect Firebase:

   * Add `google-services.json` inside:

     ```
     app/
     ```

4. Sync Gradle:

```
File → Sync Project with Gradle Files
```

5. Run the application:

```
Run ▶ app
```

---

## 🔥 Firebase Configuration

Enable the following services:

* Firebase Authentication
* Cloud Firestore Database

Firestore collections example:

```
users/
issues/
verified_issues/
resolution_updates/
```
---

## 🌍 Future Enhancements

* AI-based issue prioritization
* Push notifications
* Government API integration
* Analytics dashboard
* Web admin panel
* Multilingual support

---

## 🎓 Academic / Project Context

This project was developed as a civic-technology solution demonstrating:

* Mobile Application Development
* Cloud Database Integration
* Location-based Services
* Role-based System Design
* Smart Governance Concepts

---
## 👨‍💻 Author
**Swapnil Mukherjee**

