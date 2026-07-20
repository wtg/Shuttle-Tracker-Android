# Installation

This guide walks you through setting up the development environment for the RCOS Shuttle Tracker Android app, including Git, Android Studio, and Google Maps API configuration.

## 1. Prerequisites

### 1.1 Install Android Studio
Download Android Studio here: https://developer.android.com/studio.

During setup:
- Click Next on the setup pages unless you want to change default settings
- Click Finish and allow the installation to complete (this may take some time)

### 1.2 Set up WSL (Windows only)
If you are on Windows, install Windows Subsystem for Linux (WSL):
https://docs.microsoft.com/en-us/windows/wsl/install

### 1.3 Install Git
Open your Ubuntu / WSL terminal and run:
```apt-get install git``` or if prompted, use: ```sudo apt-get install git```

Next, sign in to https://www.github.com, or sign up if you do not have an account. Make sure to remember the username you give, this will 
be important for later.  

Configure Git with your username and email:
```
git config --global user.name "your_username"
git config --global user.email "your_emailid"
```
These commands will link your username and email to your computer, so GitHub knows who authored your commits.

## 2. Git & Repository Setup

### 2.1 Clone the repository
Run the following command in your terminal:

```git clone https://github.com/wtg/Shuttle-Tracker-Android.git```

The code will be downloaded into a folder named <b>Shuttle-Tracker-Android</b>.

### 2.2 Open the project in Android Studio
1. Open Android Studio
2. Click Open and navigate to and select the Shuttle-Tracker-Android folder

## 3. Google Maps API Key Setup

### 3.1 Generate a Google Maps API key
Get your own custom google maps API key. Here's a guide that details how to get one for yourself: [Here](https://developers.google.com/maps/documentation/javascript/get-api-key).

<details>
    <summary>If you have trouble obtaining your debug SHA-1 this might help</summary>

    Run this in a terminal (WSL users on Windows):

    keytool -list -v \
    -keystore /mnt/c/Users/<YOUR_WINDOWS_USERNAME>/.android/debug.keystore \
    -alias androiddebugkey \
    -storepass android \
    -keypass android
</details>

### 3.2 Insert your API key

Open the **local.properties** in your project level directory, and then add the following code.

```MAPS_API_KEY=YOUR_API_KEY```

## 4. Running the Virtual Android Device
1. In Android Studio, open the **Device Manager**, click the **+** button, and select **Create Virtual Device**.
2. Choose **Pixel 5**. When prompted to select a system image, choose **API level 31 (Android S)**.
3. Select the Pixel 5 emulator as your run configuration.
4. Click **Run**.

After completing these steps, the virtual Android device should launch and Google Maps should load correctly.
Once that's all done, congrats. You are now ready to start development for the RCOS Shuttle Tracker Android app.

If you're new to the codebase, [Architecture](architecture.md) is a good next read - it maps out where things live before you start changing code.

## 5. Troubleshooting (optional)
If you complete steps 1 through 4 and you are receiving the build tools is corrupted issue, use this link to Stack Overflow to solve the problem and put yourself back on track because of an error on Google's part: [Here](https://stackoverflow.com/a/68430992).

If you have an error that looks like this,

<img width="494" height="131" alt="Screenshot 2026-04-29 165126" src="https://github.com/user-attachments/assets/cdfde006-f502-4464-91f5-130f3af651a4" />

This project is not compatible with higher versions of java.
Do not click project update recommended popup, it will fix the problem but then no longer be compatible with other branches/main project.

If you have a higher version of JDK installed, manually set JAVA_HOME to be jdk-17(must be at least this).

Can use the following commands to install and set JAVA_HOME to jdk-17(run these in the Ubuntu Terminal):
```
sudo apt update 
sudo apt install -y openjdk-17-jdk t
readlink -f $(which javac)
```
You will see something like this: /usr/lib/jvm/java-17-openjdk-amd64/bin/javac and java home should be: /usr/lib/jvm/java-17-openjdk-amd64
```
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 
export PATH=$JAVA_HOME/bin:$PATH
```
