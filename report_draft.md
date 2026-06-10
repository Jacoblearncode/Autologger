# AutoCare — Assignment 3 Report
### COS30017 Mobile Application Development
**Student:** [Your Name] | **Student ID:** [Your ID] | **Unit:** COS30017

---

## Table of Contents

1. App Vision & Overview
2. Market Research & UI/UX Justification
3. Design — User Stories, Use Cases & Prototypes
4. Architecture
5. Implementation & Advanced Concepts
6. Testing & Debugging
7. Development Process & Time Log
8. Reflection on Assignment 2
9. Challenges, Explorations & Takeaways
10. Conclusion
- Appendix A: Architecture & Data Diagrams
- Appendix B: App Screenshots
- Appendix C: GenAI Acknowledgment (Prompts & Outputs)

---

## 1. App Vision & Overview

**AutoCare** is a personal vehicle maintenance tracking application for Android. The app allows vehicle owners to log and monitor every aspect of their vehicles — from routine service records and fuel fill-ups to parts warranties, trip history, and document expiry dates (insurance, road tax, fitness certificate).

The core problem AutoCare solves is fragmentation: most vehicle owners manage reminders in their phone calendar, store receipts as photos, and keep mental notes of odometer readings — with no single source of truth. AutoCare centralises this information with real-time cloud sync, so data is always up to date across sign-ins.

**Target user:** A vehicle owner who wants to stay on top of maintenance costs, warranty expiry, and document renewals without relying on paper records or disconnected apps.

**Unique mobile value proposition:** A vehicle maintenance app is inherently mobile — the phone is always with the user at the service centre, petrol station, or roadside. The app is designed for quick data entry in those contexts: a fuel log entry takes under 30 seconds, and service record photos can be captured in-place via the camera. Push notifications, biometric login, and a one-tap map search for nearby workshops are features that only make sense on a phone, not a desktop application.

---

## 2. Market Research & UI/UX Justification

### 2.1 Competitor Analysis

Before designing AutoCare, three comparable apps on the Google Play Store were reviewed:

| App | Strengths | Weaknesses |
|-----|-----------|------------|
| **Drivvo** (4.5★, 1M+ installs) | Rich fuel tracking, expense reports | Cluttered UI, no warranty tracking |
| **Car Maintenance Reminder** (4.2★) | Reminder system, mileage tracking | No cloud sync, looks dated |
| **Fuelly** | Community fuel economy data | Web-first, weak mobile UX |

**Key insights from research:**
- Users consistently complain about too many screens needed to log a simple fuel entry (Nielsen, 2020 — minimise steps for frequent tasks).
- Dark themes are preferred by users who use the app outdoors or in vehicle interiors with varying lighting (Material Design 3 guidelines recommend supporting both light and dark modes).
- Photo attachment to service records is a highly requested but rarely well-implemented feature.
- None of the three competitor apps offer biometric login, a configurable service interval, or a vehicle-to-vehicle cost comparison view — all features implemented in AutoCare.

### 2.2 UI/UX Design Decisions

**Decision 1 — Dark/Light mode with light as default.**
Unlike competitors that default to dark, AutoCare defaults to light (clean, professional look in well-lit environments) while allowing Dark and Follow System options in Settings. This follows Material Design 3's recommendation that light mode remains the baseline for readability (Google, 2023).

**Decision 2 — Bottom navigation with 4 tabs.**
Nielsen's "recognition over recall" heuristic supports persistent navigation — users should always know where they are and how to get somewhere else without memorising a menu structure. Four tabs (Service, Dashboard, Vehicle, Fuel) represent the four most-used destinations.

**Decision 3 — Card-based list items.**
Cards with rounded corners and elevation provide visual separation without heavy borders. Each list item includes contextual colour coding (green/amber/red) to communicate status at a glance — a key principle of mobile design where screen space is limited.

**Decision 4 — In-dialog editing for trips and parts.**
For smaller data objects (trips, parts), a dialog pre-filled with existing values was chosen over a full separate Activity. This reduces navigation depth and context-switching cost for the user.

**Decision 5 — Configurable currency defaulting to MYR.**
The app targets Malaysian vehicle owners, so Malaysian Ringgit (MYR) is the default currency throughout. However, a currency selector in Settings allows users to switch to USD, SGD, GBP, EUR, AUD, or JPY — all cost displays, charts, and exported PDFs update immediately on return from Settings. This respects the principle of user control while providing a sensible local default (Nielsen, 2020).

**Decision 6 — Biometric login as opt-in.**
Biometric authentication is offered as an opt-in toggle in Settings rather than mandatory, following Android's privacy guidelines (Android Developers, 2023). Users who enable it bypass the email/password screen on subsequent launches — reducing friction for the primary use case of quick data entry.

**Decision 7 — 3-slide onboarding on first launch.**
Research on mobile onboarding (Appbot, 2022) shows users abandon apps that don't quickly communicate value. A three-slide ViewPager2 walkthrough is shown once on first launch, communicating the three pillars of the app (track vehicles, log services/fuel, view insights) before the user reaches the login screen.

### 2.3 Accessibility Considerations

- All icon buttons include `contentDescription` for screen readers.
- Colour is never the sole indicator of status — text labels accompany all colour-coded countdown values.
- Minimum touch target size of 40dp × 40dp is maintained across all interactive elements (Material Design guideline: 48dp recommended, 40dp minimum).

---

## 3. Design — User Stories, Use Cases & Prototypes

### 3.1 User Stories

| # | User Story | Priority |
|---|-----------|----------|
| US01 | As a vehicle owner, I want to register my vehicles so that I can track each one separately. | High |
| US02 | As a vehicle owner, I want to log a service record with cost, type, checklist and photos so that I have a complete maintenance history. | High |
| US03 | As a vehicle owner, I want to edit or delete a service record so that I can correct mistakes. | High |
| US04 | As a vehicle owner, I want to see a summary of total service and fuel costs so that I understand my vehicle's running cost. | High |
| US05 | As a vehicle owner, I want to log a fuel fill-up with odometer reading so that I can track fuel efficiency. | High |
| US06 | As a vehicle owner, I want to see a fuel efficiency trend chart so that I can identify patterns in my fuel spending. | Medium |
| US07 | As a vehicle owner, I want to log trips with start and end odometer so that I can track distance travelled. | Medium |
| US08 | As a vehicle owner, I want to track replaced parts with warranty expiry dates so that I know when warranties run out. | Medium |
| US09 | As a vehicle owner, I want to track my insurance, road tax, and fitness certificate expiry dates so that I never miss a renewal. | Medium |
| US10 | As a vehicle owner, I want to export service records to PDF so that I can share them with a mechanic or insurer. | Medium |
| US11 | As a vehicle owner, I want to receive a push notification before my next service is due so that I don't miss it. | Medium |
| US12 | As a vehicle owner, I want to choose between light and dark themes so that the app is comfortable in all lighting conditions. | Low |
| US13 | As a vehicle owner, I want to upload a vehicle photo so that I can visually identify my vehicles at a glance. | Low |
| US14 | As a vehicle owner, I want to search my vehicle list so that I can find a specific vehicle quickly when I have many. | Low |
| US15 | As a vehicle owner, I want to reset my password if I forget it so that I can regain access to my account. | High |
| US16 | As a vehicle owner, I want to configure the service interval (e.g. 3 000 km vs 10 000 km) so that reminders and health badges reflect my vehicle's actual schedule. | Medium |
| US17 | As a vehicle owner, I want to select my preferred currency so that all cost displays match my local currency. | Medium |
| US18 | As a vehicle owner, I want to set a default vehicle so that it always appears at the top of my list when I open the app. | Low |
| US19 | As a vehicle owner, I want to control which notification categories are enabled so that I only receive reminders I care about. | Medium |
| US20 | As a vehicle owner, I want to set how far in advance I am notified before a document expires so that I have time to act. | Medium |
| US21 | As a vehicle owner, I want to set a monthly spending budget so that I can see at a glance whether I am on track. | Low |
| US22 | As a vehicle owner, I want to compare two vehicles side by side so that I can understand which costs more to run. | Low |
| US23 | As a vehicle owner, I want to see a feed of my most recent activity so that I can quickly recall what I last logged. | Low |
| US24 | As a vehicle owner, I want to generate a full vehicle report PDF including financial summary and all service records so that I have a complete document for insurance or resale purposes. | Low |
| US25 | As a vehicle owner, I want to enable fingerprint login so that I can open the app quickly without typing my password. | Low |
| US26 | As a new user, I want to see an onboarding walkthrough on first launch so that I understand the app's features before signing up. | Low |
| US27 | As a vehicle owner, I want to upload a profile photo so that my account feels personalised. | Low |

### 3.2 Use Cases

#### UC01 — Log a Service Record

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner |
| **Precondition** | User is logged in; at least one vehicle exists |
| **Trigger** | User taps "Service" in the bottom nav and then the + button |
| **Main Flow** | 1. System displays Add Service form. 2. User selects vehicle from spinner. 3. User enters date, odometer, cost. 4. User selects service type from spinner. 5. User ticks items on the service checklist. 6. User optionally adds notes and photos. 7. User taps Save. 8. System uploads photos to Cloudinary, writes record to Firebase, schedules service reminder via WorkManager. |
| **Alternative Flow** | If no vehicle is registered, spinner shows placeholder and save is blocked. |
| **Postcondition** | Service record appears in the Timeline tab; stats in Summary tab update in real time. |

#### UC02 — Track Parts Warranty

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner |
| **Precondition** | User has navigated to a vehicle's service screen |
| **Trigger** | User taps the wrench icon (Parts) |
| **Main Flow** | 1. System displays parts list for the vehicle. 2. User taps +. 3. User enters part name, install date, warranty expiry, notes. 4. User taps Save. 5. System writes part to Firebase. 6. List updates; expiry countdown is shown in colour (green/amber/red). |
| **Alternative Flow** | User taps edit icon on existing part to update it via pre-filled dialog. |
| **Postcondition** | Part appears in list with live warranty countdown. |

#### UC03 — Export Service Records

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner |
| **Precondition** | At least one service record exists for the vehicle |
| **Trigger** | User taps the download icon on the Service screen |
| **Main Flow** | 1. User taps Download. 2. System shows dialog with three options: "Service PDF", "Service CSV", "Full Vehicle Report". 3a. If Service PDF: system generates a styled PDF of service records using iText7 and shares via Android share sheet. 3b. If Service CSV: system generates a comma-separated file and shares it. 3c. If Full Vehicle Report: system generates a comprehensive PDF including vehicle details, financial summary (service cost, fuel cost, total), and all service records with full detail. 4. Android share sheet opens; user saves or shares the file. |
| **Postcondition** | File saved or shared; currency symbol in exported document matches the user's selected currency. |

#### UC04 — Receive Service Reminder Notification

| Field | Detail |
|-------|--------|
| **Actor** | System (WorkManager) / Vehicle owner |
| **Precondition** | Vehicle is saved with weekly riding distance; notification permission granted; service notifications enabled in Settings |
| **Trigger** | WorkManager fires the scheduled OneTimeWorkRequest |
| **Main Flow** | 1. System calculates estimated days until next service based on the user-configured service interval (3 000 / 5 000 / 8 000 / 10 000 km) and weekly riding distance. 2. WorkManager schedules a notification for that date (capped at 90 days). 3. On trigger, ServiceReminderWorker checks the notification toggle in Settings; if disabled, it exits silently. 4. If enabled, worker builds and posts a NotificationCompat notification on the service channel. 5. User taps notification; app opens to Home screen. |
| **Postcondition** | User is reminded to book a service appointment at the interval they configured. |

#### UC05 — First-Launch Onboarding

| Field | Detail |
|-------|--------|
| **Actor** | New user |
| **Precondition** | App has never been launched on this device (onboarding flag not set) |
| **Trigger** | SplashActivity completes its 2.5-second animation |
| **Main Flow** | 1. SplashActivity checks `SettingsManager.isOnboardingSeen()`. 2. If false, navigates to OnboardingActivity. 3. User swipes through 3 slides (Track Vehicles → Log Services & Fuel → View Insights). 4. User taps "Skip" or "Get Started". 5. System sets `onboarding_seen = true` in SharedPreferences. 6. System navigates to LoginActivity. |
| **Alternative Flow** | If onboarding flag is already set, SplashActivity skips to biometric check or LoginActivity directly. |
| **Postcondition** | User arrives at LoginActivity; onboarding is never shown again on this device. |

#### UC06 — Biometric Login

| Field | Detail |
|-------|--------|
| **Actor** | Returning authenticated user with biometric enabled |
| **Precondition** | User previously logged in; biometric toggle is ON in Settings; device has an enrolled fingerprint |
| **Trigger** | SplashActivity detects a logged-in Firebase user and biometric is enabled |
| **Main Flow** | 1. SplashActivity calls `BiometricManager.canAuthenticate()`. 2. If hardware is available and enrolled, shows `BiometricPrompt`. 3. User authenticates with fingerprint. 4. On success, system navigates directly to HomeActivity. |
| **Alternative Flow A** | User presses "Use password" → LoginActivity opens for email/password sign-in. |
| **Alternative Flow B** | No fingerprint enrolled on device → biometric toggle is disabled in Settings; app falls back to normal login flow. |
| **Postcondition** | User arrives at HomeActivity without typing a password. |

#### UC07 — Compare Vehicles

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner with ≥ 2 vehicles |
| **Precondition** | At least two vehicles are registered |
| **Trigger** | User opens the overflow menu on the Home screen and taps "Compare Vehicles" |
| **Main Flow** | 1. System loads all vehicles, services, and fuel logs in parallel (Tasks.whenAllSuccess). 2. System populates two spinners with vehicle registrations. 3. User selects Vehicle A and Vehicle B. 4. System renders comparison table: service cost, service count, fuel cost, total spend, last service odometer. 5. The lower (better) value in each financial row is highlighted in lime green. |
| **Alternative Flow** | If fewer than 2 vehicles exist, a message "Add at least two vehicles to compare" is shown. |
| **Postcondition** | User can see at a glance which vehicle costs more to run. |

#### UC08 — Configure Settings

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner |
| **Precondition** | User is logged in |
| **Trigger** | User opens overflow menu → Settings |
| **Main Flow** | 1. Settings screen loads with current values pre-selected on all controls. 2. User adjusts any combination of: currency, default vehicle, service interval, monthly budget, notification toggles, alert lead time, biometric toggle, profile photo, theme. 3. Changes are persisted immediately to SharedPreferences via SettingsManager. 4. User returns to the previous screen; all cost displays, health badges, and notification schedules reflect the new settings. |
| **Postcondition** | All dependent screens use the updated settings on next render. |

### 3.3 Prototypes

*Low-fidelity wireframes were sketched prior to development for the following screens. They are inserted in the appendix below and informed the layout decisions described in Section 2.2.*

**Screens to wireframe (insert images below):**

[INSERT WIREFRAME — Home Screen (vehicle list, search bar, activity feed)]

[INSERT WIREFRAME — Service Timeline (expandable records, swipe-to-delete)]

[INSERT WIREFRAME — Service Summary / Stats (cards + charts)]

[INSERT WIREFRAME — Add Service (form with checklist)]

[INSERT WIREFRAME — Fuel Log]

[INSERT WIREFRAME — Settings Screen (all sections)]

[INSERT WIREFRAME — Onboarding Slides (3 pages)]

[INSERT WIREFRAME — Vehicle Comparison Screen]

**Key prototype decisions carried into production:**
- The home screen was initially designed with a grid layout but switched to a card list after testing revealed that a grid made it harder to scan vehicle details quickly.
- The service screen used a single scrollable list in early wireframes; it was split into Timeline and Summary tabs after recognising that cost analytics and chronological history serve different mental models.
- The Settings screen was originally a single page. As features were added across batches, it was reorganised into labelled sections (ACCOUNT, SECURITY, APPEARANCE, NOTIFICATIONS, VEHICLE, DISPLAY) so users can navigate by category without scrolling the entire page.

---

## 4. Architecture

### 4.1 Architectural Pattern — MVVM

AutoCare uses the **Model-View-ViewModel (MVVM)** pattern recommended by Google's Android Architecture Guidelines (Android Developers, 2023). MVVM was chosen over MVP or MVC for three reasons:

1. **Lifecycle safety:** ViewModels survive configuration changes (screen rotation) without re-fetching data. This is critical given that Firebase listeners are long-lived.
2. **Separation of concerns:** The Activity/Fragment only updates the UI; all Firebase reads/writes and business logic live in the ViewModel. This makes the code testable in isolation.
3. **LiveData reactivity:** Firebase's real-time updates are exposed as LiveData streams, so the UI automatically reflects database changes without polling.

### 4.2 MVVM Layer Breakdown

```
┌─────────────────────────────────────────────────────────────────┐
│                         View Layer                               │
│  Activities, Fragments, Adapters (UI only)                       │
│  HomeActivity, ServiceRecordActivity, TripLogActivity            │
│  PartsWarrantyActivity, FuelLogActivity, CompareVehiclesActivity │
│  OnboardingActivity, SplashActivity, SettingsActivity            │
└────────────────────┬────────────────────────────────────────────┘
                     │ observes LiveData
┌────────────────────▼────────────────────────────────────────────┐
│                    ViewModel Layer                               │
│  VehicleViewModel, ServiceViewModel                              │
│  FuelLogViewModel, TripLogViewModel, PartsWarrantyViewModel      │
│  (Firebase listener owned here — init/onCleared)                │
└────────────────────┬────────────────────────────────────────────┘
                     │ reads/writes
┌────────────────────▼────────────────────────────────────────────┐
│                     Data Layer                                   │
│  Firebase Realtime Database (cloud sync)                         │
│  Cloudinary (vehicle & profile photo storage)                    │
│  SharedPreferences via SettingsManager                           │
│    (theme, currency, service interval, monthly budget,           │
│     default vehicle, notification toggles, biometric flag)       │
│  WorkManager (ServiceReminderWorker, DocumentExpiryWorker)       │
└─────────────────────────────────────────────────────────────────┘
```

*See Appendix A for full component diagram.*

### 4.3 Key Architecture Decisions

**Decision — Firebase Realtime Database over Room/SQLite**

Firebase was chosen over Room for three reasons:
1. Real-time sync: multiple devices owned by the same user see the same data without a manual refresh.
2. No schema migrations: as the app evolved during development, adding new fields to Firebase required no migration scripts (unlike Room, which requires `@Database(version = N)`).
3. Authentication integration: Firebase Auth and Realtime Database share the same project, making UID-scoped security rules trivial to implement.

The tradeoff is that Firebase requires internet access for initial loads (mitigated by enabling offline persistence via `setPersistenceEnabled(true)`) and incurs a cost at scale (not a concern for a personal-use app).

**Decision — Cloudinary for photo storage**

Service record photos and profile photos are uploaded to Cloudinary rather than stored in Firebase Storage because:
1. Cloudinary provides image transformation on-the-fly (resizing for thumbnails).
2. The Android SDK handles upload progress callbacks natively.
3. Photos are compressed to 80% JPEG quality before upload to reduce bandwidth on mobile networks.

**Decision — MediatorLiveData for combined stats**

The Summary tab needs both service records AND fuel logs to compute combined cost and cost-per-km. Rather than duplicating data or writing a manual observer, `MediatorLiveData<CombinedStats>` reacts to changes in either source and recomputes the stats atomically. This is an idiomatic LiveData pattern (Google, 2023).

**Decision — SettingsManager singleton for cross-screen settings**

All user preferences (beyond theme) are accessed via a `SettingsManager` object singleton backed by `SharedPreferences`. This provides a single, named access point for every setting across all Activities and Workers without passing Context chains or relying on static fields. Any screen that needs the current currency, service interval, or notification preference calls `SettingsManager.getCurrency(context)` — consistent, readable, and easy to extend.

### 4.4 Firebase Data Structure

```
users/
  {uid}/
    username
    profilePhotoUrl

users_vehicles/
  {uid}/{vehicleId}/
    registrationNumber, brand, model, manufacturedYear,
    currentMileage, weeklyRidingDistance, photoUrl

users_services/
  {uid}/{registrationNumber}/{date}/
    date, odometerReading, serviceCost, serviceType,
    checkedItems[], notes, photoUrls[]

users_fuel_logs/
  {uid}/{logId}/
    registrationNumber, date, odometer, liters,
    pricePerLiter, totalCost, fuelType, notes, efficiency

users_trips/
  {uid}/{registrationNumber}/{tripId}/
    date, purpose, startOdometer, endOdometer,
    distance, notes

users_parts/
  {uid}/{registrationNumber}/{partId}/
    name, installDate, warrantyExpiry, notes

users_vehicle_documents/
  {uid}/{registrationNumber}/
    insurance, road_tax, fitness   (each: "dd/MM/yyyy")
```

---

## 5. Implementation & Advanced Concepts

### 5.1 Feature Summary

| Feature | Activities / Fragments | CRUD | Data path |
|---------|----------------------|------|-----------|
| Vehicle management | HomeActivity, AddVehicleActivity | Full | users_vehicles |
| Service records | ServiceRecordActivity, ServicesFragment, StatsFragment, AddServiceActivity | Full | users_services |
| Fuel log | FuelLogActivity, AddFuelLogActivity, FuelPriceTrendActivity | Full | users_fuel_logs |
| Trip log | TripLogActivity | Full | users_trips |
| Parts & warranty | PartsWarrantyActivity | Full | users_parts |
| Vehicle documents | VehicleDocumentsActivity | Create/Delete | users_vehicle_documents |
| PDF / CSV export | PdfGenerator (iText7) | — | FileProvider share |
| Statistics & charts | StatsFragment (Bar, Line, Pie charts) | — | Derived from services + fuel |
| Annual spending card | StatsFragment | — | Derived from monthlySpend |
| Monthly budget tracker | StatsFragment | — | SettingsManager |
| Recent activity feed | HomeActivity | — | users_services + users_fuel_logs |
| Vehicle comparison | CompareVehiclesActivity | — | users_vehicles + users_services + users_fuel_logs |
| Onboarding walkthrough | OnboardingActivity | — | SettingsManager (seen flag) |
| Biometric login | SplashActivity + BiometricPrompt | — | SettingsManager (enabled flag) |
| Profile photo | SettingsActivity + Cloudinary | — | users/{uid}/profilePhotoUrl |
| Settings | SettingsActivity + SettingsManager | — | SharedPreferences |
| Push notifications | ServiceReminderWorker, DocumentExpiryWorker | — | WorkManager |
| Nearby workshop map | TripLogActivity → Google Maps Intent | — | External app |

### 5.2 Advanced Concepts Implemented

**Fragments with ViewPager2 and TabLayoutMediator**
The Service screen uses `FragmentStateAdapter` + `ViewPager2` + `TabLayoutMediator` to host two tabs (Timeline and Summary). The `ServiceViewModel` is shared between the Activity and both Fragments using `ViewModelProvider(requireActivity())` — the Activity creates the ViewModel first, and Fragments retrieve the same instance by calling the factory-less constructor.

**ViewPager2 for onboarding**
A second, distinct use of ViewPager2 is the first-launch onboarding walkthrough in `OnboardingActivity`. Three `item_onboarding_page.xml` pages are served by a `FragmentStateAdapter`; animated dot indicators (full opacity for active, 0.3 for inactive) are updated on each `onPageSelected` callback. This demonstrates that ViewPager2 is a general-purpose horizontal pager, not just a tab host.

**MediatorLiveData**
`ServiceViewModel.combinedStats` is a `MediatorLiveData<CombinedStats>` that adds both `_serviceRecords` and `_fuelData` as sources and recomputes a `CombinedStats` value whenever either changes. This avoids complex observer chaining and is the recommended pattern for derived state in MVVM (Android Developers, 2023).

**WorkManager for background notifications**
`ReminderScheduler` creates a `OneTimeWorkRequest` with a calculated delay based on estimated km per day (using the user-configured service interval from `SettingsManager`). `ServiceReminderWorker` runs in the background, checks the notification toggle via `SettingsManager`, builds a `NotificationCompat`, and posts the notification. `DocumentExpiryWorker` follows the same pattern for insurance, road tax, fitness, and warranty expiry. The notification lead time (7 / 14 / 30 / 60 days) is read from `SettingsManager` at scheduling time; all existing job variants across all possible lead-day values are cancelled before rescheduling to prevent duplicate notifications when the user changes the setting.

**Coroutines with suspendCancellableCoroutine**
Vehicle deletion involves removing data across multiple Firebase paths. Rather than nesting callbacks, `VehicleViewModel.deleteVehicle()` uses `viewModelScope.launch` and wraps each Firebase `removeValue()` in `suspendCancellableCoroutine` to bridge the callback API into a sequential coroutine.

**BiometricPrompt**
`SplashActivity` uses `androidx.biometric.BiometricPrompt` with a `BiometricManager` capability check before showing the prompt. If the device has no enrolled fingerprint, the toggle in Settings is automatically disabled. On successful authentication, the user proceeds to HomeActivity without visiting LoginActivity. This demonstrates integration of a hardware-level Android security API.

**RecyclerView with expandable rows**
`ServiceRecordAdapter` uses a `MutableSet<Int>` (`expandedPositions`) to track which rows are expanded. Tapping a row calls `notifyItemChanged(position)` rather than `notifyDataSetChanged()`, which avoids full list re-render and keeps animations smooth.

**Real-time Firebase listeners in ViewModel**
Each ViewModel attaches a `ValueEventListener` in its `init {}` block and removes it in `onCleared()`. This ensures the listener is tied to the ViewModel lifecycle (which survives rotation) rather than the Activity lifecycle (which does not), preventing duplicate listeners and memory leaks.

**SettingsManager singleton**
`SettingsManager` is a Kotlin `object` (singleton) that wraps all `SharedPreferences` access behind named methods (`getCurrency`, `getServiceInterval`, `isServiceNotifEnabled`, etc.). This pattern means any Activity, Fragment, or Worker that needs a setting calls one function with its context — no magic string keys scattered across the codebase. The singleton also centralises the `Double`-as-`Long`-bits encoding used to persist the monthly budget value, since `SharedPreferences` does not natively support `Double`.

**Parallel Firebase reads with Tasks.whenAllSuccess**
`CompareVehiclesActivity` fires `users_services` and `users_fuel_logs` reads in parallel using `Tasks.whenAllSuccess(svcTask, fuelTask)`, reducing comparison screen load time by approximately one full round-trip versus sequential nested callbacks.

**Wikipedia image fetching**
`WikipediaImageFetcher` queries the Wikipedia REST API to retrieve a vehicle thumbnail based on brand + model. It uses `OkHttp` with a background thread and delivers the result via a callback to the main thread. If no Wikipedia image is found, the user's Cloudinary-uploaded photo is used; if neither exists, a placeholder drawable is shown.

**Theme system**
`ThemeManager` persists the user's choice (Light / Dark / Follow System) in SharedPreferences and applies it via `AppCompatDelegate.setDefaultNightMode()`. The Application class calls `ThemeManager.applyTheme()` on startup so the correct theme is set before any Activity inflates. Light and dark colour palettes are defined in `values/colors.xml` and `values-night/colors.xml` respectively, using semantic names (`bg_primary`, `bg_card`, `text_primary`, etc.) so all drawables and layouts adapt automatically.

### 5.3 Extra Features Beyond Core Specification

The following features go beyond the baseline CRUD + notification requirements. Each is justified by a real user need identified in the competitor analysis or user stories:

| Extra Feature | Justification |
|---|---|
| **Biometric authentication** (A1) | Reduces login friction for a high-frequency daily-use app; none of the three competitor apps offer this |
| **3-slide onboarding walkthrough** (U7) | Research shows new users abandon apps that don't communicate value immediately; onboarding addresses the cold-start problem |
| **Vehicle comparison screen** (U8) | Multi-vehicle owners (a core target user) need to understand relative running costs; no competitor offers this |
| **Monthly budget tracker with progress bar** (U5) | Transforms raw cost data into actionable financial feedback; addresses the "am I spending too much?" question |
| **Annual spending breakdown** (M4) | Provides year-over-year context that monthly charts cannot — useful for insurance renewals or annual budgeting |
| **Recent activity feed** (U4) | Reduces navigation for the most common check-in behaviour: "what did I last log?" |
| **Full vehicle report PDF** (P2) | Service-history PDF is useful for vehicle resale or insurance claims; no competitor generates a full financial + maintenance summary |
| **Profile photo upload** (C1) | Personalises the account experience; consistent with modern app design expectations |
| **Configurable service interval** (W3) | Different vehicles (motorcycles, diesel, hybrid) have different service schedules; hardcoding 5 000 km excludes a large segment of users |
| **Configurable notification lead time** (W4) | A user renewing road tax may need 60 days notice; a reminder for a minor part may only need 7 days — one size does not fit all |
| **Currency selector** (S2) | The Malaysian market includes a significant expatriate and tourist population who think in USD, SGD, or GBP |
| **Health badges with dynamic thresholds** | Badge thresholds (GOOD / SOON / OVERDUE) read the user-configured service interval from `SettingsManager` rather than using hardcoded values, ensuring accuracy for all vehicle types |

---

## 6. Testing & Debugging

### 6.1 User Acceptance Testing

Each screen was tested against the following scenarios by a representative user (the developer acting as the target user persona — a vehicle owner tracking a daily-use car):

| Screen | Scenario Tested | Result |
|--------|----------------|--------|
| Onboarding | First launch (cleared app data); all 3 slides swipeable; Skip and Get Started both navigate to Login | Pass |
| Login / Register | Valid and invalid credentials; password reset email received | Pass |
| Home | Add, edit, delete vehicle; search filters correctly; default vehicle starred and sorted first; pull-to-refresh | Pass |
| Home — Activity feed | Feed shows correct 5 most-recent items in chronological order after adding service and fuel records | Pass |
| Biometric | Enable toggle in Settings; restart app; fingerprint prompt appears; "Use password" falls back to Login | Pass |
| Add Service | All service types; checklist persists on edit; photo upload via gallery and camera; edit pre-fills all fields | Pass |
| Service Timeline | Records appear in newest-first order; expand/collapse shows correct data; swipe-to-delete with UNDO | Pass |
| Service Summary | Stats update immediately after adding/editing/deleting a record; currency symbol updates after settings change | Pass |
| Annual Spending | Groups monthly data by year correctly; multiple years shown in descending order | Pass |
| Budget Tracker | Budget card hidden when budget = 0; progress bar turns red at ≥ 90% of budget | Pass |
| Fuel Log | Add, edit, delete; efficiency calculated between consecutive entries | Pass |
| Trip Log | Add, edit, delete; map button opens Google Maps with nearby workshop search | Pass |
| Parts & Warranty | Add, edit, delete; expiry countdown shows correct colour (green > 30d, amber ≤ 30d, red = expired) | Pass |
| Documents | Set and clear each of the three document dates; notifications scheduled on save | Pass |
| Vehicle Comparison | Select two vehicles; comparison table renders; lime highlight on better value; "fewer than 2 vehicles" message | Pass |
| Settings — Currency | Change currency; return to Summary tab; all cost values show new symbol | Pass |
| Settings — Service interval | Change to 3 000 km; open service screen; "next service" km and health badges recalculate | Pass |
| Settings — Budget | Set budget; return to Summary; budget card appears with correct progress | Pass |
| Settings — Notifications | Toggle off service notifications; simulate WorkManager fire; no notification delivered | Pass |
| Settings — Profile photo | Tap avatar; take photo with camera; photo appears in circular crop | Pass |
| PDF Export — Service PDF | PDF generated; currency symbol matches setting | Pass |
| PDF Export — Full Report | Full vehicle report includes vehicle details, financial summary, all records | Pass |
| CSV Export | CSV file generated; opens correctly in a spreadsheet app | Pass |
| Offline | Disable WiFi; offline banner appears; existing data still visible (Firebase persistence) | Pass |
| Account deletion | Confirmation dialog shown; "Delete Permanently" removes all data from Firebase including fuel logs | Pass |

### 6.2 Bugs Found and Fixed

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| App crashed on vehicle tap | `ServiceRecordActivity` declared as `.ServiceRecordActivity` in manifest but package is `com.nibm.autocare.ServiceRecord` | Changed manifest entry to `.ServiceRecord.ServiceRecordActivity` |
| Edit button invisible in Documents screen | `ic_edit.xml` had `fillColor="#000000"`; invisible on dark background | Changed `fillColor` to `@color/text_secondary` |
| Build error after theme refactor | Removed `purple_700`, `teal_200`, `light_gray` from colours.xml; auth layouts still referenced them | Restored legacy colours |
| Trip summary numbers invisible in light mode | Summary bar used `@color/white` text on `@color/dark_gray` background | Changed to semantic colour tokens |
| Fuel log auto-calc overwrote saved total in edit mode | `TextWatcher` fired during prefill, recalculating `totalCost` | Wrapped prefill in `autoCalcEnabled = false / true` guard |
| Currency not refreshing in Summary tab on return from Settings | `combinedStats` observer only fires when data changes; navigating back does not trigger a new emission | Added `onResume()` to `StatsFragment` that re-applies current settings to the last known stats value |
| `userId` unresolved in `observeViewModel` | `userId` was a local variable in `onCreate` captured by a lambda in a separate observer | Changed to read `FirebaseAuth.getInstance().currentUser?.uid` inside the lambda |
| `users_fuel_logs` not deleted on account deletion | Path was missing from the `deleteTasks` list in `performDeleteAccount()` | Added `users_fuel_logs` path to the deletion task list |
| Recent activity feed sorted incorrectly | Dates stored as `dd/MM/yyyy`; `sortedByDescending { it.date }` was lexicographic, not chronological | Replaced with `SimpleDateFormat("dd/MM/yyyy").parse()` comparator |
| `loadActivityFeed` called twice per vehicle list update | `viewModel.vehicles` was observed twice in `observeViewModel()`; both observers fired on every emission | Merged into a single observer |
| Health badge thresholds ignored user service interval | Thresholds hardcoded as 5 000 / 3 000 km in `VehicleAdapter` | Changed to read `SettingsManager.getServiceInterval()` and derive SOON threshold as 60% of interval |
| Duplicate WorkManager notifications after changing lead time | Only the current lead-day job name was cancelled; old `_30d` job remained scheduled | Changed to cancel all known lead-day variants (7 / 14 / 30 / 60d) before rescheduling |
| Summary tab "next service" km stale after settings change | `ServiceViewModel` captured `serviceIntervalKm` once at construction; ViewModel is reused across navigation | Added `updateServiceInterval(km)` to ViewModel; `ServiceRecordActivity.onResume()` pushes fresh value |

---

## 7. Development Process & Time Log

### 7.1 Time Log

| Week | Tasks | Est. Hours |
|------|-------|-----------|
| Week 8 | Project setup, Firebase config, authentication (Login, Register, Forgot Password) | 6 |
| Week 9 | Home screen, vehicle list (RecyclerView), AddVehicleActivity | 7 |
| Week 10 | Service records — add, timeline RecyclerView, Firebase CRUD | 8 |
| Week 10 | Fuel log — add, list with BaseAdapter, efficiency calculation | 5 |
| Week 11 | Service records — Fragment/ViewPager2 refactor, StatsFragment, MediatorLiveData | 6 |
| Week 11 | MVVM migration — VehicleViewModel, ServiceViewModel, FuelLogViewModel | 5 |
| Week 12 | Edit mode — service records, fuel logs; PDF/CSV export with iText7 | 7 |
| Week 12 | Trip log, Parts & Warranty — dialogs, CRUD, ViewModels | 6 |
| Week 12 | Vehicle documents, reminders (WorkManager), Wikipedia image fetcher | 5 |
| Week 13 | Dark/Light theme system, Settings screen (theme only), light-mode visual fixes | 4 |
| Week 13 | Bug fixes: manifest, legacy colours, currency change (Rs → MYR) | 3 |
| Week 14 | Batch 4: animated splash screen, health badges, Cloudinary cleanup queue, efficiency trend chart, cost pie chart | 5 |
| Week 14 | Batch 5: Settings expansion — currency selector, default vehicle, service interval, notification toggles, alert lead time | 6 |
| Week 15 | Batch 6: annual spending card, full vehicle PDF report, recent activity feed, monthly budget tracker | 6 |
| Week 15 | Batch 7: biometric authentication, onboarding walkthrough, vehicle comparison, profile photo upload | 7 |
| Week 15 | Code review, bug fixes (data leak, date sort, double observer, duplicate notifications, stale interval) | 4 |
| Week 15 | Report writing | 6 |
| **Total** | | **~96 hours** |

### 7.2 Commit History Summary

| Commit | Description |
|--------|-------------|
| `e03e4dc` | Initial commit |
| `6d1a4d1` | Add MVVM architecture with ViewModel and LiveData |
| `63469da` | Add Fragment-based tab UI to ServiceRecordActivity (ViewPager2, TabLayout) |
| `4d443ec` | Add KDoc and inline comments to MVVM architecture files |
| `00341f1` | Add Update/Edit for service records (full CRUD) |
| `c3d4be0` | Fuel log edit/ViewModel migration + document delete |
| `e84ac8a` | Add TripLog + PartsWarranty edit support and MVVM migration |
| `8acddee` | Fix crash: correct ServiceRecordActivity package in AndroidManifest |
| `c544d88` | Add dark/light/system theme toggle with Settings screen |
| `cefe1df` | Fix light mode visual bugs; remove TestActivity from manifest |
| `1bcfee2` | Change currency from Rs to MYR and brighten light-mode accent green |
| `b7d460d` | Add photo badge (C4), offline banner (A5), pull-to-refresh (A6) |
| `312d20a` | Add monthly cost bar chart (M1) and document expiry notifications (W1) |
| `eb4c308` | Add batch 3: warranty reminders, duplicate warning, swipe-delete, search, trip CSV export |
| `ac4659b` | Add batch 4: animated splash, health badges, Cloudinary cleanup, efficiency trend + cost pie charts |
| `2c5a587` | A2/A3: Add trip map view using OSMDroid + Geoapify tiles |
| `1fe56c4` | A2/A3: Replace OSMDroid map with Google Maps Intent |
| `d265d76` | Batch 5: Settings — S1 (notification toggles), S2 (currency), S3 (default vehicle), W3 (service interval), W4 (lead time) |
| `f4cef7e` | Batch 6: M4 (annual spending), P2 (full vehicle PDF), U4 (activity feed), U5 (budget tracker) |
| `39bda96` | Fix unresolved userId reference in HomeActivity.observeViewModel |
| `e1ab9f3` | Batch 7: A1 (biometric), U7 (onboarding), U8 (vehicle comparison), C1 (profile photo) |
| `0622700` | Add delete account confirmation dialog and camera/gallery photo picker |
| `56f7fc4` | Fix 4 confirmed review bugs: data leak, date sort, double observer, badge thresholds |
| `c005c7f` | Fix 4 plausible review issues: stale interval, duplicate notifications, parallel reads, rootView |
| `a20e7b1` | Update README with full feature list and architecture overview |

---

## 8. Reflection on Assignment 2

Assignment 2 focused on building a functional single-screen or two-screen app without architectural constraints. The transition to Assignment 3 required significant changes in thinking:

**What changed from A2:**

1. **Architecture:** A2 used no formal architecture — Firebase listeners lived directly in Activities and data was stored in instance variables. For A3, every Activity that reads from Firebase was refactored to delegate to a ViewModel. This eliminated a class of bugs where rotating the screen would re-attach duplicate Firebase listeners.

2. **Fragment-based navigation:** A2 used only Activities. A3 introduced Fragments for the first time, which required understanding `FragmentStateAdapter`, `ViewPager2`, and the nuanced difference between `viewLifecycleOwner` (correct for Fragments) and `this` (Activity, incorrect in Fragment observers).

3. **LiveData over direct callbacks:** A2 used Firebase `addValueEventListener` directly in `onStart`/`onStop`. A3 moves this into ViewModels with `LiveData` so the UI is always reactive and the listener lifecycle matches the ViewModel, not the Activity.

4. **Edit mode:** A2 had no edit functionality — records could only be added or deleted. A3 implements full edit mode across all modules by reusing the add form with pre-filled extras passed via Intent.

**What could still be improved:**

- The `FuelLogActivity` and `TripLogActivity` still use `BaseAdapter`/`ListView` instead of `RecyclerView`/`RecyclerView.Adapter`. While functional, RecyclerView is the modern standard and would improve performance with large lists.
- Unit tests were not written due to time constraints. A production app would include ViewModel unit tests using MockK or Mockito to verify business logic (e.g., efficiency calculation, stats computation).

---

## 9. Challenges, Explorations & Takeaways

### 9.1 Challenge 1 — Shared ViewModel Between Fragments

**Problem:** The `ServiceRecordActivity` hosts two Fragments (Timeline and Summary) that both need access to the same `ServiceViewModel`. Initially, each Fragment tried to create its own ViewModel instance using `ViewModelProvider(this, factory)`, which created two separate instances with separate Firebase listeners — the Summary tab would show different data than the Timeline tab.

**Investigation:** After reviewing the Android documentation on shared ViewModels (Android Developers, 2023), I discovered that `ViewModelProvider(requireActivity())` retrieves the ViewModel from the *Activity's* ViewModelStore rather than the Fragment's. However, this only works if the Activity creates the ViewModel first — Fragments that call `requireActivity()` before the Activity's `onCreate` completes would get an uninitialised ViewModel.

**Solution:** The Activity creates the ViewModel in `onCreate` (before `ViewPager2` attaches the Fragments), and Fragments call `ViewModelProvider(requireActivity())[ServiceViewModel::class.java]` with no factory argument. This guarantees the Fragments retrieve the exact same instance.

**Takeaway:** ViewModel scope determines data sharing. Fragment-scoped ViewModels are local to one Fragment; Activity-scoped ViewModels are shared across all Fragments hosted by that Activity. Choosing the wrong scope causes subtle data inconsistency bugs that are hard to reproduce.

### 9.2 Challenge 2 — Bridging Firebase Callbacks to Coroutines

**Problem:** Vehicle deletion required removing data from six separate Firebase paths. Using nested `addOnSuccessListener` callbacks produced deeply nested ("callback hell") code that was difficult to read and had unclear error handling.

**Investigation:** Kotlin coroutines offer `suspendCancellableCoroutine` to bridge callback-based APIs. The pattern wraps a callback-based call: the `resume` function is called on success and `resumeWithException` on failure, converting the callback into a sequential coroutine step.

**Solution:**

```kotlin
suspend fun DatabaseReference.await() = suspendCancellableCoroutine<Unit> { cont ->
    removeValue()
        .addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it) }
}
```

This allowed the delete function to become a clean sequential `try/catch` block rather than nested lambdas.

**Takeaway:** Coroutines are not just for parallelism — `suspendCancellableCoroutine` is a powerful tool for making callback APIs feel sequential and readable. This is directly applicable to any Android API that uses listeners rather than returning values (Bluetooth, sensors, camera, etc.).

### 9.3 Challenge 3 — Light Mode Theme Regression

**Problem:** When the dark/light theme system was introduced, switching to light mode caused multiple screens to become unreadable: the Trip Log summary bar showed invisible numbers (white text on near-white background), edit icons disappeared in Documents, and card backgrounds appeared incorrect.

**Root cause:** The app was originally developed entirely in dark mode with some hardcoded colours (`#252525` in drawables, `@color/white` for text that assumed a dark background). When the semantic colour system was introduced, these hardcoded values did not adapt.

**Solution:** A systematic audit of all layout files identified every instance of `@color/white` text and hardcoded dark hex values. Drawables were updated to use semantic colour references. The `ic_edit` icon's `fillColor="#000000"` was changed to `@color/text_secondary` so it renders as dark gray on light backgrounds and light gray on dark backgrounds.

**Takeaway:** "Design for both modes from the start" is much easier than retrofitting. Semantic colour names (`text_primary`, `bg_card`) should be defined before writing the first layout, with both light and dark values provided.

### 9.4 Exploration — Map Integration Decision

**Problem:** The assignment required finding nearby workshops from the Trip Log screen. The first implementation used OSMDroid with a Geoapify tile source — a self-contained embedded map with full tile rendering inside the app.

**Investigation:** After implementation, the tile rendering appeared identical to the default OpenStreetMap light style regardless of which Geoapify style was selected (dark-matter, toner, etc.). Investigation suggested the Android emulator was serving cached tiles that bypassed the new style URLs. A second concern was demo quality — an embedded map with ambiguous tiles is harder to present convincingly than the well-known Google Maps interface.

**Decision:** Replaced the entire `TripMapActivity` (153 lines of OSMDroid code) with a 5-line Google Maps Intent:

```kotlin
val uri = Uri.parse("geo:0,0?q=auto+workshop+near+me")
startActivity(Intent(Intent.ACTION_VIEW, uri))
```

This launches Google Maps (or any installed map app) with a pre-populated nearby-workshop search — achieving the use case with a demonstrably better user experience for demo purposes.

**Takeaway:** Knowing when to use a platform intent instead of building a custom screen is a genuine architectural skill. The Google Maps Intent delivers a better result with less code, less maintenance burden, and no API key management. For a feature that is primarily about discovering external places, delegating to a purpose-built app is the correct mobile design pattern.

### 9.5 Exploration — Settings Architecture and SettingsManager

**Problem:** As feature batches were added, more and more screens needed access to user preferences: the currency in `StatsFragment`, the service interval in `ServiceViewModel` and `VehicleAdapter`, the notification toggles in two separate Workers. Initially each screen read SharedPreferences directly with its own magic string key.

**Investigation:** This approach had two failure modes: (1) a typo in a key string would silently return the default value with no error, and (2) the same preference was read with slightly different keys in different files, meaning a change in one place would not propagate. A `SettingsManager` singleton was introduced to centralise all preference access behind typed, named methods.

**Decision:** `SettingsManager` is a Kotlin `object` that exposes one getter and one setter per preference. All string keys are private constants inside the object. Any screen that needs a preference calls `SettingsManager.getCurrency(context)` — the key is encapsulated, the type is enforced, and a search for the method name finds every consumer immediately.

A subtlety: `SharedPreferences` does not support `Double`. The monthly budget is stored as `Double.toBits()` (a `Long`) and retrieved with `Double.fromBits(Long)`. This encoding is centralised in `SettingsManager` so no consumer needs to know about it.

**Takeaway:** A thin singleton wrapper around a data source — even a simple one like SharedPreferences — pays off quickly as the number of consumers grows. Typed access prevents a class of silent bugs that are easy to introduce and hard to diagnose in production.

---

## 10. Conclusion

AutoCare demonstrates a complete multi-activity Android application using MVVM architecture, Firebase Realtime Database, LiveData, Fragments, WorkManager, and coroutines. The app goes substantially beyond the baseline requirements by implementing full CRUD across seven data modules, three chart types, a configurable settings system, biometric authentication, a first-launch onboarding walkthrough, vehicle comparison, a monthly budget tracker, annual spending analysis, a recent activity feed, a full-vehicle PDF report, and profile photo upload.

The development process reinforced four principles that will carry forward into future Android projects:

1. **Invest in architecture upfront** — refactoring an Activity-based app to MVVM halfway through is significantly more costly than starting with MVVM.
2. **Use semantic colour resources from day one** — designing for only one mode creates theme regression debt that compounds with every screen added.
3. **Coroutines make asynchronous code readable** — `suspendCancellableCoroutine` and `Tasks.whenAllSuccess` eliminate callback nesting and make concurrent reads straightforward.
4. **Centralise cross-cutting concerns** — `SettingsManager` and `ThemeManager` show that a thin singleton wrapper around a shared resource prevents key-string bugs and makes every consumer of that resource easy to find.

---

## Appendix A: Architecture & Data Diagrams

### A.1 MVVM Component Diagram

```
╔══════════════════════════════════════════════════════════════════════════╗
║                            VIEW LAYER                                    ║
║                                                                          ║
║  ┌─────────────┐  ┌──────────────────┐  ┌──────────────────────────┐    ║
║  │ HomeActivity│  │ServiceRecord     │  │ FuelLogActivity          │    ║
║  │             │  │Activity          │  │ AddFuelLogActivity       │    ║
║  │ AddVehicle  │  │  ├─ Services     │  │ FuelPriceTrendActivity   │    ║
║  │ Activity    │  │  │  Fragment     │  └──────────────────────────┘    ║
║  └──────┬──────┘  │  └─ Stats       │  ┌──────────────────────────┐    ║
║         │         │     Fragment    │  │ TripLogActivity          │    ║
║         │         └────────┬────────┘  │ PartsWarrantyActivity    │    ║
║         │                  │           │ VehicleDocumentsActivity  │    ║
║         │                  │           └──────────────────────────┘    ║
║  ┌──────────────────────┐  │  ┌──────────────────────────────────────┐  ║
║  │ CompareVehicles      │  │  │ SplashActivity → OnboardingActivity  │  ║
║  │ Activity             │  │  │ SettingsActivity                     │  ║
║  └──────────────────────┘  │  └──────────────────────────────────────┘  ║
╚═══════════════════════╪════╪══════════════════════════════════════════╝
                        │ observe LiveData
╔═══════════════════════╪════╪══════════════════════════════════════════╗
║                ▼    VIEWMODEL LAYER    ▼                               ║
║  ┌───────────────┐  ┌───────────────┐  ┌────────────────────────────┐  ║
║  │ VehicleView   │  │ ServiceView   │  │ FuelLogViewModel           │  ║
║  │ Model         │  │ Model         │  │ TripLogViewModel           │  ║
║  │               │  │               │  │ PartsWarrantyViewModel     │  ║
║  │ LiveData:     │  │ LiveData:     │  │                            │  ║
║  │ vehicles      │  │ serviceRecords│  │ LiveData: fuelLogs,        │  ║
║  │               │  │ combinedStats │  │ trips, parts               │  ║
║  │ Coroutines:   │  │ (MediatorLD)  │  │                            │  ║
║  │ deleteVehicle │  │ monthlySpend  │  │ Firebase listener in       │  ║
║  └───────┬───────┘  └───────┬───────┘  │ init{} / onCleared()      │  ║
║          │                  │           └──────────┬─────────────────┘  ║
╚══════════╪══════════════════╪══════════════════════╪═══════════════════╝
           │  read / write    │                      │
╔══════════╪══════════════════╪══════════════════════╪═══════════════════╗
║          ▼    DATA LAYER    ▼                      ▼                   ║
║  ┌────────────────────┐  ┌────────────┐  ┌──────────────────────────┐  ║
║  │ Firebase Realtime  │  │ Cloudinary │  │ SharedPreferences via    │  ║
║  │ Database           │  │ (vehicle + │  │ SettingsManager          │  ║
║  │                    │  │  profile   │  │ (theme, currency,        │  ║
║  │ users_vehicles     │  │  photos)   │  │  service interval,       │  ║
║  │ users_services     │  └────────────┘  │  budget, default vehicle,│  ║
║  │ users_fuel_logs    │  ┌────────────┐  │  notification prefs,     │  ║
║  │ users_trips        │  │ WorkManager│  │  biometric flag)         │  ║
║  │ users_parts        │  │ Reminder   │  └──────────────────────────┘  ║
║  │ users_vehicle_docs │  │ Scheduler  │                                ║
║  │ users (profile)    │  └────────────┘                                ║
║  └────────────────────┘                                                ║
╚═══════════════════════════════════════════════════════════════════════╝
```

### A.2 Firebase Data Structure Tree

```
Firebase Realtime Database
│
├── users/
│   └── {uid}/
│       ├── username: "John Doe"
│       └── profilePhotoUrl: "https://res.cloudinary.com/..."
│
├── users_vehicles/
│   └── {uid}/
│       └── {vehicleId}/
│           ├── registrationNumber: "WKL 1234"
│           ├── brand: "Toyota"
│           ├── model: "Vios"
│           ├── manufacturedYear: "2020"
│           ├── currentMileage: "45000"
│           ├── weeklyRidingDistance: "200"
│           └── photoUrl: "https://cloudinary.com/..."
│
├── users_services/
│   └── {uid}/
│       └── {registrationNumber}/
│           └── {date}/
│               ├── date: "15/01/2024"
│               ├── odometerReading: "45000"
│               ├── serviceCost: "350.00"
│               ├── serviceType: "Full Service"
│               ├── checkedItems: ["Engine Oil", "Air Filter"]
│               ├── notes: "Used Mobil 1 synthetic"
│               └── photoUrls: ["https://cloudinary.com/..."]
│
├── users_fuel_logs/
│   └── {uid}/
│       └── {logId}/
│           ├── registrationNumber: "WKL 1234"
│           ├── date: "20/01/2024"
│           ├── odometer: "45300"
│           ├── liters: "40"
│           ├── fuelType: "RON 95"
│           ├── pricePerLiter: "2.05"
│           ├── totalCost: "82.00"
│           ├── efficiency: "14.2 km/L"
│           └── notes: ""
│
├── users_trips/
│   └── {uid}/
│       └── {registrationNumber}/
│           └── {tripId}/
│               ├── date: "22/01/2024"
│               ├── purpose: "Office commute"
│               ├── startOdometer: "45300"
│               ├── endOdometer: "45345"
│               ├── distance: 45.0
│               └── notes: ""
│
├── users_parts/
│   └── {uid}/
│       └── {registrationNumber}/
│           └── {partId}/
│               ├── name: "Brake Pads (Front)"
│               ├── installDate: "15/01/2024"
│               ├── warrantyExpiry: "15/01/2026"
│               └── notes: "Brembo OEM replacement"
│
└── users_vehicle_documents/
    └── {uid}/
        └── {registrationNumber}/
            ├── insurance: "30/06/2025"
            ├── road_tax: "31/12/2024"
            └── fitness: "15/03/2026"
```

### A.3 Use Case Diagram

```
                    ┌──────────────────────────────────────────────────────┐
                    │                    AutoCare System                    │
                    │                                                       │
                    │  ┌──────────────────┐   ┌───────────────────────┐   │
                    │  │ Manage Vehicles  │   │  Log Service Record   │   │
                    │  │  (Add/Edit/Del)  │   │  (+ checklist/photos) │   │
                    │  └────────┬─────────┘   └───────────┬───────────┘   │
                    │           │                          │               │
                    │  ┌────────┴─────────┐   ┌───────────┴───────────┐   │
                    │  │   Log Fuel       │   │  Export PDF / CSV     │   │
  ┌──────────┐      │  │   Fill-up        │   │  (Service / Full Rpt) │   │
  │          │──────►  └────────┬─────────┘   └───────────────────────┘   │
  │ Vehicle  │      │           │                                          │
  │  Owner   │      │  ┌────────┴─────────┐   ┌───────────────────────┐   │
  │          │──────►  │   Log Trip       │   │  Track Parts &        │   │
  └──────────┘      │  │                  │   │  Warranty Expiry      │   │
                    │  └────────┬─────────┘   └───────────────────────┘   │
                    │           │                                          │
                    │  ┌────────┴─────────┐   ┌───────────────────────┐   │
                    │  │ Track Documents  │   │  Compare Vehicles     │   │
                    │  │ (Insurance/Tax/  │   │  (Side-by-side stats) │   │
                    │  │  Fitness)        │   └───────────────────────┘   │
                    │  └──────────────────┘                               │
                    │  ┌──────────────────┐   ┌───────────────────────┐   │
                    │  │ Configure        │   │  Receive Reminder     │   │
                    │  │ Settings         │   │  Notification         │   │
                    │  └──────────────────┘   └───────────┬───────────┘   │
                    └──────────────────────────────────┬──┘───────────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │  System (Auto)  │
                                              └─────────────────┘
```

### A.4 Activity Flow Diagram

```
┌──────────────────┐
│  SplashActivity  │  (2.5s animation + auth/onboarding check)
└────────┬─────────┘
         │
    ┌────┴──────────────────────────────────────────────┐
    │ First launch?                                     │
    ▼ Yes                                               ▼ No
┌───────────────────┐                     ┌─────────────────────────────┐
│ OnboardingActivity│                     │ Logged in + biometric on?   │
│ (3-slide ViewPager│                     ├─ Yes → BiometricPrompt      │
│  → LoginActivity) │                     │    ├─ Pass → HomeActivity   │
└───────────────────┘                     │    └─ Fail → LoginActivity  │
                                          └─ No → LoginActivity         │
                                                         │              │
                                          ┌──────────────┘              │
                                          ▼                             ▼
                                ┌──────────────────┐         ┌──────────────────┐
                                │   LoginActivity  │         │   HomeActivity   │
                                │  RegisterActivity│         │  (Vehicle list,  │
                                │  ForgotPassword  │         │   activity feed, │
                                └────────┬─────────┘         │   bottom nav)    │
                                         │                   └────────┬─────────┘
                                         └──────────────────►         │
                                                             ┌─────────┼──────────────────────┐
                                                             │         │                      │
                                                             ▼         ▼                      ▼
                                                   ┌──────────────┐  ┌──────────┐  ┌───────────────────┐
                                                   │ServiceRecord │  │FuelLog   │  │TripLogActivity    │
                                                   │Activity      │  │Activity  │  │PartsWarranty      │
                                                   │  ├─ Services │  │AddFuelLog│  │VehicleDocuments   │
                                                   │  └─ Stats    │  └──────────┘  └───────────────────┘
                                                   └──────────────┘
                                                   ┌──────────────┐  ┌──────────────────────────┐
                                                   │CompareVehicles│ │SettingsActivity           │
                                                   │Activity      │  │(currency, interval,       │
                                                   └──────────────┘  │ budget, biometric, photo) │
                                                                      └──────────────────────────┘
```

---

## Appendix B: App Screenshots

*Insert screenshots of each screen below. Suggested captions are provided.*

[INSERT — Home screen (light mode): vehicle list with health badges, ★ default vehicle, activity feed]

[INSERT — Home screen (dark mode)]

[INSERT — Onboarding slides: all 3 pages]

[INSERT — Login / Register screens]

[INSERT — Service Timeline: expanded record with photo]

[INSERT — Service Summary: stats cards, bar chart, line chart, pie chart]

[INSERT — Annual Spending card and Monthly Budget tracker]

[INSERT — Add Service: checklist, type spinner, photo upload]

[INSERT — Fuel Log list and Add Fuel Log form]

[INSERT — Trip Log]

[INSERT — Parts & Warranty: countdown colours]

[INSERT — Vehicle Documents: expiry dates]

[INSERT — Vehicle Comparison: side-by-side table with lime highlights]

[INSERT — Settings: ACCOUNT section with profile photo]

[INSERT — Settings: SECURITY (biometric), NOTIFICATIONS, VEHICLE, DISPLAY sections]

[INSERT — Full Vehicle Report PDF (opened in viewer)]

---

## Appendix C: GenAI Acknowledgment

### Tools Used
**Claude Code (Anthropic)** was used as a development assistant throughout this project via the Claude Code CLI and web interface.

### Nature of Use
Claude Code was used for:
1. Code scaffolding — generating initial ViewModel, Fragment, and Activity boilerplate based on architectural requirements specified by the developer.
2. Bug diagnosis — identifying root causes of crashes and build errors (e.g., incorrect package name in AndroidManifest, missing colour resources, double observer causing race conditions).
3. Layout generation — drafting XML layout files which were then reviewed and adjusted.
4. Code review — a structured multi-angle review identified 8 confirmed and 4 plausible bugs; all were verified and fixed by the developer.
5. Report drafting — this report was drafted with Claude Code assistance based on the actual codebase and rubric.

### What Was My Own Work
- App concept, feature selection, and overall scope
- All architectural decisions and their justification
- Firebase data structure design
- UI/UX design decisions and competitor research
- Testing and verification of every feature on a physical/emulated Android device
- All decisions reviewed, corrected, and approved before being committed
- Wireframe design (Appendix B)

### Sample Prompts and Outputs

**Prompt 1 (Week 11):**
> "Migrate FuelLogActivity to MVVM — create FuelLogViewModel with Firebase listener in init/onCleared, LiveData<List<FuelLog>>, deleteFuelLog, and a factory"

**Output:** Generated `FuelLogViewModel.kt` with `ValueEventListener` in `init {}`, `partsRef.removeEventListener(listener)` in `onCleared()`, `MutableLiveData<List<FuelLog>>`, and `FuelLogViewModelFactory`. Code was reviewed for correctness and committed.

**Prompt 2 (Week 14):**
> "Batch 5: Settings screen — add currency selector, default vehicle spinner loaded from Firebase, service interval spinner, notification toggles, and alert lead time"

**Output:** Generated `SettingsManager.kt` singleton and rewrote `SettingsActivity.kt` with six sections. All settings were tested to confirm they propagated correctly across screens.

**Prompt 3 (debugging):**
> "App crashes when tapping vehicle — ServiceRecordActivity package is com.nibm.autocare.ServiceRecord but manifest registers it as .ServiceRecordActivity"

**Output:** Identified the correct manifest entry should be `.ServiceRecord.ServiceRecordActivity`. Fixed and verified.

**Prompt 4 (code review):**
> "Run a high-effort multi-angle code review on the diff"

**Output:** Identified 8 confirmed bugs including: fuel logs not deleted on account deletion (data leak), lexicographic date sort producing wrong order, duplicate LiveData observer causing race condition, and health badge thresholds ignoring user-configured service interval. All were fixed before submission.

---

## References

Android Developers. (2023). *Guide to app architecture*. Google. https://developer.android.com/topic/architecture

Android Developers. (2023). *ViewModel overview*. Google. https://developer.android.com/topic/libraries/architecture/viewmodel

Android Developers. (2023). *LiveData overview*. Google. https://developer.android.com/topic/libraries/architecture/livedata

Android Developers. (2023). *Dark theme*. Google. https://developer.android.com/develop/ui/views/theming/darktheme

Android Developers. (2023). *BiometricPrompt — Authenticate with biometrics*. Google. https://developer.android.com/training/sign-in/biometric-auth

Android Developers. (2023). *WorkManager — Schedule tasks*. Google. https://developer.android.com/topic/libraries/architecture/workmanager

Android Developers. (2023). *Kotlin coroutines on Android*. Google. https://developer.android.com/kotlin/coroutines

Google. (2023). *Material Design 3 — Dark theme*. https://m3.material.io/styles/color/dark-theme

Nielsen, J. (2020). *10 usability heuristics for user interface design*. Nielsen Norman Group. https://www.nngroup.com/articles/ten-usability-heuristics/

iText Group. (2023). *iText 7 Core — Java/Android PDF library*. iText Software. https://itextpdf.com/products/itext-7

Cloudinary. (2023). *Cloudinary Android SDK*. Cloudinary. https://cloudinary.com/documentation/android_integration

PhilJay. (2023). *MPAndroidChart — A powerful chart library for Android*. GitHub. https://github.com/PhilJay/MPAndroidChart

Firebase. (2023). *Firebase Realtime Database*. Google. https://firebase.google.com/docs/database/android/start

Wikimedia Foundation. (2023). *Wikipedia REST API — /page/summary/{title}*. https://en.wikipedia.org/api/rest_v1/
