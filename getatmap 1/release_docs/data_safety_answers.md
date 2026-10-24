# Google Play Console - Data Safety & Content Rating Form Guide

This document provides exact, step-by-step answers to complete the mandatory **Data Safety** questionnaire and **Content Rating** in the Google Play Console for **GetAtMap** (`com.aistudio.getatmap.vjnr`).

---

## Part 1: Data Safety Questionnaire

### Section 1: Data Collection and Security
1. **Does your app collect or share any of the required user data types?**
   - Answer: **Yes**
2. **Is all of the user data collected by your app encrypted in transit?**
   - Answer: **Yes** (All communication uses HTTPS/TLS, cleartext traffic is disabled)
3. **Do you provide a way for users to request that their data be deleted?**
   - Answer: **Yes** (Users can email `privacy@getatmap.com` to request account/listing deletion)

---

### Section 2: Data Types Breakdown

#### Data Type: Location
Check the box for:
- [x] **Approximate location** (Network location)
- [x] **Precise location** (GPS location)

For both **Approximate location** and **Precise location**, answer the sub-questions as follows:
1. **Is this data collected, shared, or both?**
   - Answer: **Collected** (Do NOT check Shared)
2. **Is this data processed ephemerally?**
   - Answer: **Yes** (Location is used in real-time in memory to calculate proximity to listings and is not perpetually logged without consent)
3. **Is this data required for your app, or can users choose whether it’s collected?**
   - Answer: **Data collection is required** (GetAtMap requires location to function as a hyper-local marketplace)
4. **Why is this user data collected?**
   - Check: **App functionality**
   - (Leave Analytics, Advertising, Fraud prevention unchecked unless you add them later)

#### Data Type: Photos and Videos
Check the box for:
- [x] **Photos**

Answer sub-questions:
1. **Is this data collected, shared, or both?**
   - Answer: **Collected**
2. **Is this data processed ephemerally?**
   - Answer: **No** (Uploaded photos are stored to display with the classified advertisement)
3. **Is this data required or optional?**
   - Answer: **Optional** (Users can choose whether to upload listing photos)
4. **Why is this user data collected?**
   - Check: **App functionality** (For users to attach photos to items they sell)

---

## Part 2: Content Rating Questionnaire (IARC)

When prompted by the International Age Rating Coalition (IARC) in the Play Console:

1. **Category:** Select **Consumer Store, Commercial Streaming, or Utility** (or "All Other App Types").
2. **Violence:** Does the app contain violence? -> **No**
3. **Sexuality / Adult Content:** Does the app contain sexual material? -> **No**
4. **Language:** Does the app contain potentially offensive language? -> **No**
5. **Controlled Substances:** Does the app promote tobacco, alcohol, or illicit drugs? -> **No**
6. **User Interaction / Location Sharing:**
   - **Does the app allow users to interact or exchange content with others?** -> **Yes** (Buyers and sellers can contact each other via call or messaging for classified ads)
   - **Does the app share the user’s current and precise physical location with other users?** -> **No** (The app shows approximate listing distance, but does NOT broadcast live personal user GPS coordinates to other users)
7. **Digital Goods / In-App Purchases:** Does the app sell digital products or loot boxes? -> **No** (Physical goods marketplace)

**Resulting Rating:** Typically **PEGI 3** / **Everyone** / **Teen** (unrestricted), suitable for general audiences.
