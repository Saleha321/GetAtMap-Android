# GetAtMap - Google Play Store Publishing & Release Guide

## 1. Project Specifications
- **App Name:** GetAtMap
- **Application ID:** `com.aistudio.getatmap.vjnr`
- **Version:** `1.0` (VersionCode: `1`, VersionName: `"1.0"`)
- **Min SDK:** `24` (Android 7.0 Nougat)
- **Target SDK:** `36` (Complies with latest Google Play requirements)

---

## 2. Generating Release Keystore & Builds

### Step A: Generate a Production Keystore
Run the following standard `keytool` command in your terminal to create your release signing key:

```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```
Store this keystore file safely and note the password.

### Step B: Build Android App Bundle (AAB) for Google Play
Google Play requires `.aab` format for all new app releases:

```bash
gradle :app:bundleRelease
```
The output bundle will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

### Step C: Build Signed Standalone APK (For Direct Distribution)
If you also want to distribute an APK directly to testers or clients:

```bash
gradle :app:assembleRelease
```
The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 3. Deep Linking & Android App Links Setup

The app is pre-configured with Android App Links (`android:autoVerify="true"`):
- `https://getatmap.com/*`
- `https://www.getatmap.com/*`
- `getatmap://*`

### Hosting `assetlinks.json` (Required for Auto-Verification)
To make links open directly inside the app without a browser prompt, host the following JSON file at:
`https://getatmap.com/.well-known/assetlinks.json`

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.aistudio.getatmap.vjnr",
      "sha256_cert_fingerprints": [
        "YOUR_RELEASE_KEY_SHA256_FINGERPRINT_HERE"
      ]
    }
  }
]
```

*(You can retrieve your SHA-256 fingerprint in the Google Play Console under **Release > Setup > App Integrity**).*

---

## 4. Google Play Console Data Safety Form

Because GetAtMap requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` to display hyper-local marketplace listings and calculate distances:

### Data Safety Answers:
1. **Does your app collect or share any of the required user data types?** -> **Yes**
2. **Is all the user data collected by your app encrypted in transit?** -> **Yes** (HTTPS/TLS)
3. **Do you provide a way for users to request data deletion?** -> **Yes**
4. **Data Type: Location**
   - **Approximate location**: Collected -> **Yes**, Shared -> **No**
   - **Precise location**: Collected -> **Yes**, Shared -> **No**
   - **Purpose**: Select **App functionality** (e.g., finding nearby items and map pins)
   - **Is this data processed ephemerally?**: Select **Yes** (used in real-time to compute nearest listings, not stored perpetually without consent).

---

## 5. Sample Privacy Policy Text for Location

> **Privacy Policy for GetAtMap**
> 
> *Effective Date: October 2026*
> 
> **Overview**
> GetAtMap ("we", "our", or "us") provides a hyper-local map-based classifieds and marketplace application. We take your privacy seriously and handle data with full transparency.
> 
> **Location Information**
> GetAtMap requires access to your device's GPS and network location (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`) solely while the app is active in the foreground. This location data is used to:
> 1. Show classified listings, products, vehicles, and services situated closest to you.
> 2. Calculate approximate distance to sellers.
> 3. Populate native map markers and navigation directions upon your explicit request.
> 
> We do NOT track your background location when the app is closed. We do NOT sell, rent, or trade your precise location data to third-party data brokers or marketing services.
> 
> **Media & Camera Uploads**
> When you choose to post an ad, you may voluntarily select photos from your device gallery. Only images you specifically select are uploaded.
> 
> **Security**
> All network communication between the app and our web servers is encrypted using industry-standard TLS (HTTPS).
> 
> **Contact Us**
> If you have questions regarding this policy, contact our privacy team at:
> `privacy@getatmap.com` or visit `https://getatmap.com/privacy`.
