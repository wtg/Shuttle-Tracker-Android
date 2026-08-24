---
title: Installation
permalink: /installation/
---

# Installation

This guide walks you through setting up the development environment for the RCOS Shuttle Tracker Android app, including Git, Android Studio, and Google Maps API configuration.

## 1. Prerequisites

### 1.1 Install Android Studio
Download Android Studio here: https://developer.android.com/studio.

During setup:
- Click Next on the setup pages unless you want to change default settings
- Click Finish and allow the installation to complete (this may take some time)

### 1.2 Install Git
Install [Git](https://git-scm.com/downloads) for your operating system. WSL is optional on Windows; Android Studio and Git work directly in Windows.

Next, sign in to https://www.github.com, or sign up if you do not have an account. Remember the username you choose; you will use it below.

Configure Git with your username and email:
```shell
git config --global user.name "your_username"
git config --global user.email "your_emailid"
```
These commands will link your username and email to your computer, so GitHub knows who authored your commits.

## 2. Git & Repository Setup

### 2.1 Clone the repository
Run the following command in your terminal:

```shell
git clone https://github.com/wtg/Shuttle-Tracker-Android.git
```

The code will be downloaded into a folder named <b>Shuttle-Tracker-Android</b>.

### 2.2 Open the project in Android Studio
1. Open Android Studio
2. Click Open and navigate to and select the Shuttle-Tracker-Android folder

## 3. Google Maps API Key Setup

### 3.1 Generate a Google Maps API key
Create a key with the Maps SDK for Android enabled by following Google's [Maps SDK for Android key guide](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

<details markdown="1">
<summary>If you have trouble obtaining your debug SHA-1 this might help</summary>

Run the Gradle signing report and copy the debug variant's SHA-1:

```shell
./gradlew signingReport
```

On Windows without WSL, use `gradlew.bat signingReport`.
</details>

### 3.2 Insert your API key

Open `local.properties` in the project root and add:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

## 4. Running the Virtual Android Device
1. In Android Studio, open the **Device Manager**, click the **+** button, and select **Create Virtual Device**.
2. Choose a recent Pixel and an **API 34 or newer** system image with the **Play Store** icon. The Play Store image includes the Google Play services needed by Maps and Firebase.
3. Select the emulator as your run configuration.
4. Click **Run**.

After completing these steps, the virtual Android device should launch and Google Maps should load correctly.
Once that's all done, congrats. You are now ready to start development for the RCOS Shuttle Tracker Android app.

If you're new to the codebase, [Architecture](ARCHITECTURE.md) is a good next read - it maps out where things live before you start changing code.

## 5. Troubleshooting (optional)

The project targets JVM 17. If Gradle reports a Java version error, open **Settings → Build, Execution, Deployment → Build Tools → Gradle** in Android Studio and select a JDK 17-compatible Gradle JDK, then sync the project again.
