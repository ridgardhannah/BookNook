<h1 align="center">BookNook</h1>

<p align="center">
  <strong>A community-driven Android application for discovering, mapping, and sharing free local book-swap locations.</strong><br>
  Built with Material Design 3, Firebase, CameraX, and the Google Books API.
</p>


![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=flat-square)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square)
![Material Design 3](https://img.shields.io/badge/Design-Material_Design_3-757575?style=flat-square)
![Google APIs](https://img.shields.io/badge/APIs-Google_Books_%26_Maps-4285F4?style=flat-square)

---

## Overview

With public library closures and restricted opening hours reducing local access to physical books across the UK, **BookNook** turns everyday spaces—from converted phone boxes and park benches to supermarket community shelves—into a shared, user-mapped reading network.

Rather than acting solely as a static map or a personal reading log, BookNook combines **location discovery**, **ISBN barcode logging**, and **proximity-based wishlist alerts** into a cohesive book-sharing ecosystem designed around Material Design 3 and inclusive accessibility principles.

---

## Key Features

<p align="center">
  <img src="https://github.com/user-attachments/assets/40a6fb13-be75-4060-a189-39a70c8d80ea" width="250" alt="Map Screen">
  <img src="https://github.com/user-attachments/assets/edfd4c22-43da-40d8-a920-ccebd938c7d4" width="250" alt="Book ISBN Scanning">
  <img src="https://github.com/user-attachments/assets/75af3d73-8917-41cc-94e8-623f08abfa68" width="250" alt="Achievements Screen">
</p>


* **Interactive Nook Discovery & Filtering:** Explore nearby Book Nooks and Reading Spots on an interactive map, inspect real-time inventory and accessibility tags (e.g. wheelchair access, well-lit areas), filter by location type, or pin new community Nooks with photos and GPS coordinates.
* **ISBN Barcode Scanner & Manual Fallback:** Donate or pick up books using a native **CameraX** barcode scanner integrated with the **Google Books REST API** to automatically retrieve titles, authors, and cover art—with a manual ISBN entry fallback when camera permissions are unavailable.
* **Proximity Wishlist Alerts:** Maintain a personal book wishlist; when another user donates a matching title within your chosen notification radius, **Firebase Cloud Functions and Cloud Messaging** trigger a push alert.
* **Gamified Reading Journey & Achievements:** Track every book donated, book discovered, and Nook visited in a personal activity log while unlocking hexagonal achievement badges and equippable profile titles.
* **Inclusive & Accessible UI:** Built to **Material Design 3** standards featuring scalable font sizing (up to 200%), high-contrast map pins and search controls, warm low-glare background tones, and graceful permission handling.

---

## System Architecture & Tech Stack

| Layer | Technologies Used |
| :--- | :--- |
| **Mobile Client** | Native Android (Java), Gradle (Kotlin DSL), Material Design 3 |
| **Hardware & Media** | Android CameraX API (ISBN barcode scanning & Nook photo uploads) |
| **Location & Maps** | Google Maps SDK, GeoPoints, native navigation intent integration |
| **External Data** | Google Books REST API (automated book metadata & cover lookup) |
| **Backend & Cloud** | Firebase Authentication, Cloud Firestore, Cloud Storage, Cloud Functions, Cloud Messaging (FCM) |

---

## Iterative User Testing & Design Evolution

BookNook was developed and refined across three structured evaluation phases:

1. **Usability & M3 Heuristic Testing (Stage 1):** Evaluated contrast, cognitive load, and navigation flow, resulting in high-contrast outlined map pins, a dedicated `Add a Nook` extended FAB, split `Donate` and `Find` actions to reduce decision friction, and global Settings access.
2. **Error Handling & State Persistence Testing (Stage 2):** Verified offline/aeroplane-mode data retention, lifecycle state preservation across backgrounding, invalid barcode handling, and dynamic profile customisation.
3. **Target Demographic Focus Group (Book Club & Neurodiversity Review):** Evaluated real-world discoverability, gamification retention, and readability with dyslexic and avid readers, confirming low visual clutter and strong multimodal icon clarity.

---

## Getting Started

1. Clone this repository using `git clone [https://github.com/ridgardhannah/BookNook.git](https://github.com/ridgardhannah/BookNook.git)`
2. Open the project in **Android Studio**.
3. Add your own `google-services.json` (from Firebase Console) into the `app/` directory and configure your Google Maps API key in `local.properties`.
4. Sync Gradle and run on an Android device or emulator (API 26+).
