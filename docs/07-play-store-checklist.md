# PlacementOS — Google Play Store Readiness Checklist

This document details the exact configuration, operational procedures, and compliance items required to prepare and publish the PlacementOS native Android application on the Google Play Console.

---

## 1. Application Identity

| Attribute | Specification | Verification |
|---|---|---|
| **App Name** | PlacementOS | Configured in `app.json` |
| **Package Name (Application ID)** | `com.placementos.app` | Configured in `app.json` (`android.package`) |
| **Slug** | `placementos` | Configured in `app.json` |
| **Version Name** | `1.0.0` | Semantic versioning |
| **Version Code** | `1` | Incremented with each release build |
| **Category** | Education / Productivity | Set in Google Play Console |
| **Default Language** | English (United States) | Configured |

---

## 2. Android SDK & Platform Compliance

| Requirement | Target | Status / Notes |
|---|---|---|
| **Compile SDK** | `36` (Android 16) | Configured in `app.json` & Android Gradle build |
| **Target SDK** | `36` (Android 16) | Complies with latest Google Play target API requirements |
| **Min SDK** | `24` (Android 7.0) | Supported by React Native / Expo New Architecture |
| **Build Tools** | `36.1.0` | Installed locally and verified |
| **Architecture** | New Architecture (`newArchEnabled: true`) | Enabled in `app.json` |

---

## 3. Release Signing Configuration

Google Play requires all production release builds to be signed with an upload key, which Google Play App Signing uses to generate the final distribution APKs.

### 3.1 Keystore Generation (Do NOT commit keystore to Git)

```bash
keytool -genkeypair -v -keystore placementos-upload-key.jks \
  -alias placementos-upload \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

### 3.2 Environment Variables (CI / Local Release)

Configure these variables securely in your build environment (e.g., EAS Secrets, GitHub Secrets, or local secure shell):

- `RELEASE_STORE_FILE`: Path to `placementos-upload-key.jks`
- `RELEASE_KEY_ALIAS`: `placementos-upload`
- `RELEASE_STORE_PASSWORD`: Keystore password
- `RELEASE_KEY_PASSWORD`: Key password

> [!CAUTION]
> Never commit `.jks` files, keystore passwords, or Google Play service account JSON files to the Git repository. Keep `.jks` in `.gitignore`.

---

## 4. Build Artifacts

### 4.1 Android App Bundle (AAB) — Production Submission

Google Play strictly requires the **Android App Bundle (.aab)** format for new app submissions:

```bash
# Via EAS:
eas build --platform android --profile production

# Or via Gradle:
cd mobile/android
./gradlew bundleRelease
```

Output: `mobile/android/app/build/outputs/bundle/release/app-release.aab`

### 4.2 Release APK — Direct Testing & Distribution

For direct sideloading and internal testing without Google Play:

```bash
# Via EAS:
eas build --platform android --profile preview

# Or via Gradle:
cd mobile/android
./gradlew assembleRelease
```

Output: `mobile/android/app/build/outputs/apk/release/app-release.apk`

---

## 5. Google Play Account Deletion Requirement

Google Play requires apps that support account creation to provide both an **in-app account deletion flow** and a **web-based deletion URL**.

### 5.1 In-App Deletion Flow
- Path: `mobile/app/(app)/delete-account.tsx` (accessible via Profile → Danger Zone).
- Requires explicit user confirmation (typing `DELETE`).
- Authenticated endpoint: `DELETE /api/v1/auth/account`.
- Clears local tokens and session from `SecureStore`.

### 5.2 Deletion Semantics & Data Retention
- **Anonymised:** Student personal name, phone number, gender, specialization, user account email (`deleted_{id}@placementos.invalid`), password hash cleared.
- **Unlinked & Removed:** Telegram identities and link tokens.
- **Cancelled:** Active reminder tasks and pending outbox notifications.
- **Retained (Institutional Records):** Registration number, NeoPAT ID, degree, branch, batch, CGPA, placement drives, applications, and shortlist matching entries are retained as university system of record.

### 5.3 Web Deletion URL
- URL: `https://<YOUR_DEPLOYED_DOMAIN>/profile` (and public info at `https://<YOUR_DEPLOYED_DOMAIN>/privacy`).

---

## 6. Privacy Policy & Data Safety Declarations

### 6.1 Privacy Policy URL
- Web URL: `https://<YOUR_DEPLOYED_DOMAIN>/privacy`
- In-App Route: `/privacy` (publicly accessible without authentication).

### 6.2 Data Safety Questionnaire Declarations

| Data Type | Collected? | Shared? | Purpose | Retention / Deletion |
|---|---|---|---|---|
| **Email Address** | Yes | No | Account management, authentication | Anonymised on account deletion |
| **Name** | Yes | No | App functionality, student identification | Anonymised on account deletion |
| **Phone Number** | Optional | No | Contact information for placement recruiters | Cleared on account deletion |
| **Academic Identifiers** (Reg No, NeoPAT ID, CGPA, Branch, Batch) | Yes | No | Placement eligibility evaluation, shortlist matching | Retained as university institutional record |
| **Telegram Chat ID** | Optional | No | Automated push notifications & reminders | Permanently deleted on account deletion or unlink |
| **Application & Placement History** | Yes | No | Application tracking, university placement statistics | Retained as university institutional record |

---

## 7. Store Listing & Visual Assets

| Asset | Specifications | Status |
|---|---|---|
| **App Icon** | 512 × 512 px, 32-bit PNG, max 1024 KB | Configured in `assets/icon.png` |
| **Feature Graphic** | 1024 × 500 px, JPG or 24-bit PNG (no alpha) | To be uploaded in Console |
| **Phone Screenshots** | Min 2, max 8 screenshots; 16:9 or 9:16 aspect ratio; min 1080 px | Capture from running app |
| **Short Description** | Max 80 characters | "Placement tracking, deterministic eligibility, and instant Telegram alerts." |
| **Full Description** | Max 4000 characters | Comprehensive feature breakdown |

---

## 8. Reviewer / Demo Credentials

Provide test credentials in Google Play Console under **App access**:

- **Account Type:** All functionality is available without restrictions.
- **Username / Email:** `demo.student@placementos.edu` (pre-provisioned test account)
- **Password:** Configure a secure demo password for Google Play review team
- **Notes for Reviewer:** Include step-by-step instructions to view drives, check eligibility, and submit an application.

---

## 9. Release Track Workflow

1. **Internal Testing Track:** Upload initial AAB, invite internal QA testers.
2. **Closed Testing (Alpha):** Test with real student devices across various Android versions (Android 7.0 through Android 16).
3. **Open Testing / Production:** Promote to production after review approval.
