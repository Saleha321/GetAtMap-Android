# GetAtMap - Google Play Release & Deployment Checklist

Follow this checklist step-by-step to build, sign, and publish **GetAtMap** (`com.aistudio.getatmap.vjnr`) to the Google Play Store.

---

## Checklist Summary
- [ ] 1. Keystore Creation (Upload Key)
- [ ] 2. Build Signed Android App Bundle (AAB)
- [ ] 3. Retrieve SHA-256 Fingerprint from Google Play Console
- [ ] 4. Deploy `assetlinks.json` on `getatmap.com`
- [ ] 5. Test Android App Links & Deep Linking
- [ ] 6. Upload AAB & Submit Release for Review

---

## Step 1: Generate Release Keystore
Run the following terminal command on your workstation:

```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

- When prompted, enter a strong password (keep this password safe!).
- Enter your developer details.
- This creates `my-upload-key.jks`.

---

## Step 2: Build Signed Android App Bundle (.aab)
Set the environment variables with your keystore path and passwords, then execute:

```bash
export KEYSTORE_PATH="/path/to/your/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"

gradle :app:bundleRelease
```

Your production App Bundle will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

*(Note: ProGuard / R8 minification and resource shrinking are automatically executed during this step, with JavaScript interfaces safely retained).*

---

## Step 3: Set Up App in Google Play Console
1. Log in to [Google Play Console](https://play.google.com/console).
2. Click **Create app**:
   - **App name:** `GetAtMap - Local Marketplace`
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
3. Complete the **Set up your app** tasks on the Dashboard:
   - **Privacy Policy:** Link to `https://getatmap.com/privacy` (host `release_docs/privacy_policy.html`).
   - **App access:** All functionality is available without special credentials.
   - **Ads:** Select "No, my app does not contain ads".
   - **Content rating:** Complete questionnaire per `release_docs/data_safety_answers.md`.
   - **Target audience:** Ages 18 and over (or 16+).
   - **Data safety:** Complete form per `release_docs/data_safety_answers.md`.
   - **Government apps:** No.
   - **Financial features:** None.

---

## Step 4: Retrieve SHA-256 Fingerprint for Deep Links
1. In Play Console, go to **Release > Setup > App Integrity**.
2. Under **Play App Signing**, find the **App signing key certificate**.
3. Copy the **SHA-256 certificate fingerprint** (e.g., `FA:C6:17:...`).
4. Open `/release_docs/assetlinks.json` and replace `PASTE_YOUR_RELEASE_KEY_SHA256_FINGERPRINT_HERE` with this fingerprint.
5. Upload this file to your web server at:
   `https://getatmap.com/.well-known/assetlinks.json`
   *(Ensure HTTP header `Content-Type: application/json` and HTTPS is active).*

---

## Step 5: Test Deep Links & App Links
Once `assetlinks.json` is live on your server, verify that links open directly in GetAtMap:

```bash
# Test HTTPS App Link
adb shell am start -a android.intent.action.VIEW \
  -d "https://getatmap.com/listing/123" com.aistudio.getatmap.vjnr

# Test Custom URL Scheme
adb shell am start -a android.intent.action.VIEW \
  -d "getatmap://listing/123" com.aistudio.getatmap.vjnr
```

The app will launch immediately and open the targeted page inside the WebView.

---

## Step 6: Create Production Release & Submit
1. In Play Console, navigate to **Production** (or **Internal Testing** first).
2. Click **Create new release**.
3. Upload `app-release.aab`.
4. Enter Release notes:
   ```
   Initial release 1.0 of GetAtMap:
   - Hyper-local classifieds and map discovery.
   - Real-time GPS distance filtering.
   - Deep linking support.
   ```
5. Click **Next**, review any warnings, and click **Save and publish**.
