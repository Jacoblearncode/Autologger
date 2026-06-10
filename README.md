# AutoCare – Vehicle Service & Maintenance Tracker

AutoCare is an Android mobile app built to help vehicle owners track maintenance records, service history, and expenses effortlessly. Developed with Kotlin, Firebase, and Cloudinary, it follows MVVM architecture and offers a full suite of features for managing multiple vehicles, generating reports, and staying on top of upcoming service and document deadlines.

---

## Features

### Vehicle Management
- Add, edit, and delete multiple vehicles with brand, model, registration, year, and mileage
- Upload a vehicle photo via camera or gallery (Cloudinary, circle-cropped display)
- Health badges on each vehicle card: **GOOD / SOON / OVERDUE** based on km since last service (threshold respects the user-configured service interval)
- Star-marked default vehicle sorted to the top of the list
- Swipe-to-delete with Snackbar **UNDO**
- Search/filter vehicles by registration number in real time
- Side-by-side **vehicle comparison** (service cost, service count, fuel cost, total spend, last odometer) with the better value highlighted

### Service Records
- Log service date, odometer reading, service type, cost, checklist items, and notes
- Dynamic service checklists for 5 000 km / 40 000 km / 100 000 km intervals
- Upload and view service-bill photos (Cloudinary)
- Swipe-to-delete records with Snackbar UNDO
- Timeline tab sorted newest-first
- Export options: **Service PDF**, **Service CSV**, or **Full Vehicle Report PDF** (vehicle details, financial summary, all records)

### Fuel Log
- Log fuel fills with date, odometer, litres, and total cost
- Average fuel efficiency calculated from consecutive odometer readings

### Trip Log
- Log trip entries per vehicle
- One-tap button to search for nearby auto workshops in Google Maps

### Parts & Warranty
- Track replaced parts with warranty expiry dates
- Warranty expiry reminders via WorkManager notifications

### Vehicle Documents
- Store road tax, insurance, and fitness-certificate expiry dates
- Expiry reminders via WorkManager (configurable lead time)

### Statistics & Charts (Summary tab)
- Bar chart: monthly service + fuel spend (last 12 months, side-by-side bars)
- Line chart: fuel efficiency trend (cubic-bezier curve)
- Pie chart: service vs. fuel cost breakdown
- Stat cards: total service cost, total fuel cost, combined total, record count, average efficiency, cost per km, km until next service
- **Annual spending** breakdown by year
- **Monthly budget tracker** with progress bar (red tint when ≥ 90 % used)

### Notifications
- WorkManager reminders for upcoming service (based on estimated km/week)
- Configurable notification toggles per category (service / documents / warranty)
- Configurable alert lead time (7 / 14 / 30 / 60 days before expiry)

### Settings
- **Currency** selector (MYR, USD, SGD, GBP, EUR, AUD, JPY) — all cost displays update instantly on return
- **Default vehicle** selector (vehicle sorted to top of home list and marked with ★)
- **Service interval** selector (3 000 / 5 000 / 8 000 / 10 000 km) — used for next-service calculation and health badges
- **Monthly budget** input — powers the budget tracker card in Stats
- **Fingerprint / biometric unlock** toggle (requires enrolled fingerprint on device)
- **Profile photo** — take a photo or choose from gallery; stored via Cloudinary and loaded into a circular avatar
- Notification toggles + lead-time selector

### Authentication & Onboarding
- Firebase email/password sign-up and login
- Password reset by email
- **3-slide onboarding walkthrough** shown on first launch (ViewPager2 with dot indicators)
- **Biometric authentication** on subsequent launches when enabled in Settings
- Account deletion with confirmation dialog (removes all Firebase data including vehicles, services, fuel logs, and the Auth record)

### Home Screen
- Greeting with the user's display name
- **Recent Activity feed** showing the 5 most-recent service and fuel events across all vehicles (sorted by actual date)
- Offline banner when no internet connection; auto-hides on reconnect
- Pull-to-refresh
- Quick-action buttons: Add Vehicle, Add Service, Fuel Log

---

## Architecture

| Layer | Implementation |
|---|---|
| UI | Activities + Fragments (MVVM) |
| State | `ViewModel` + `LiveData` / `MediatorLiveData` |
| Persistence | Firebase Realtime Database (UID-scoped paths) |
| Auth | Firebase Authentication |
| Background | WorkManager (`ServiceReminderWorker`, `DocumentExpiryWorker`) |
| Image upload | Cloudinary `MediaManager` |
| Charts | MPAndroidChart (Bar, Line, Pie) |
| PDF export | iText7 |
| Settings | `SharedPreferences` via `SettingsManager` singleton |
| Biometrics | `androidx.biometric.BiometricPrompt` |

---

## Technologies

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | XML layouts, ViewPager2, RecyclerView, SwipeRefreshLayout |
| Backend | Firebase Authentication, Firebase Realtime Database |
| Cloud storage | Cloudinary |
| PDF / CSV | iText7, FileProvider sharing |
| Charts | MPAndroidChart |
| Background tasks | WorkManager |
| Image loading | Glide |
| Biometrics | AndroidX Biometric |

---

## Installation

1. Clone the repository:
```bash
git clone https://github.com/jacoblearncode/autologger.git
```

2. Open in Android Studio and let Gradle sync.

3. Add your `google-services.json` (Firebase project config) to `app/`.

4. Set your Cloudinary cloud name and upload preset in `CloudinaryConfig.kt`.

5. Run on an emulator (API 26+) or physical device.

---

## Project Structure

```
app/src/main/java/com/nibm/autocare/
├── Authentication/         Login, Signup, ForgotPassword
├── Home/                   HomeActivity, VehicleViewModel
├── Vehicle/                AddVehicleActivity
├── ServiceRecord/          ServiceRecordActivity, ServiceViewModel,
│                           StatsFragment, ServicesFragment
├── Reminder/               ReminderScheduler, ServiceReminderWorker,
│                           DocumentExpiryWorker
├── SplashScreen/           SplashActivity, OnboardingActivity
├── PDFGenerator/           PdfGenerator
├── adapter/                VehicleAdapter, ServiceRecordAdapter,
│                           FuelLogAdapter, TripLogAdapter
├── model/                  Vehicle, ServiceRecord, FuelLog, Trip
├── CompareVehiclesActivity.kt
├── SettingsActivity.kt
├── SettingsManager.kt
├── FuelLogActivity.kt
├── TripLogActivity.kt
├── PartsWarrantyActivity.kt
└── VehicleDocumentsActivity.kt
```
