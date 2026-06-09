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

**Unique mobile value proposition:** A vehicle maintenance app is inherently mobile — the phone is always with the user at the service centre, petrol station, or roadside. The app is designed for quick data entry in those contexts: a fuel log entry takes under 30 seconds, and service record photos can be captured in-place via the camera.

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

### 2.2 UI/UX Design Decisions

**Decision 1 — Dark/Light mode with light as default.**
Unlike competitors that default to dark, AutoCare defaults to light (clean, professional look in well-lit environments) while allowing Dark and Follow System options in Settings. This follows Material Design 3's recommendation that light mode remains the baseline for readability (Google, 2023).

**Decision 2 — Bottom navigation with 4 tabs.**
Nielsen's "recognition over recall" heuristic supports persistent navigation — users should always know where they are and how to get somewhere else without memorising a menu structure. Four tabs (Service, Dashboard, Vehicle, Fuel) represent the four most-used destinations.

**Decision 3 — Card-based list items.**
Cards with rounded corners and elevation provide visual separation without heavy borders. Each list item includes contextual colour coding (green/amber/red) to communicate status at a glance — a key principle of mobile design where screen space is limited.

**Decision 4 — In-dialog editing for trips and parts.**
For smaller data objects (trips, parts), a dialog pre-filled with existing values was chosen over a full separate Activity. This reduces navigation depth and context-switching cost for the user.

**Decision 5 — MYR currency.**
The app targets Malaysian vehicle owners, so Malaysian Ringgit (MYR) is used throughout for cost fields and exported PDFs/CSVs.

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
| US06 | As a vehicle owner, I want to see a fuel price trend chart so that I can identify patterns in my fuel spending. | Medium |
| US07 | As a vehicle owner, I want to log trips with start and end odometer so that I can track distance travelled. | Medium |
| US08 | As a vehicle owner, I want to track replaced parts with warranty expiry dates so that I know when warranties run out. | Medium |
| US09 | As a vehicle owner, I want to track my insurance, road tax, and fitness certificate expiry dates so that I never miss a renewal. | Medium |
| US10 | As a vehicle owner, I want to export service records to PDF so that I can share them with a mechanic or insurer. | Medium |
| US11 | As a vehicle owner, I want to receive a push notification before my next service is due so that I don't miss it. | Medium |
| US12 | As a vehicle owner, I want to choose between light and dark themes so that the app is comfortable in all lighting conditions. | Low |
| US13 | As a vehicle owner, I want to upload a vehicle photo so that I can visually identify my vehicles at a glance. | Low |
| US14 | As a vehicle owner, I want to search my vehicle list so that I can find a specific vehicle quickly when I have many. | Low |
| US15 | As a vehicle owner, I want to reset my password if I forget it so that I can regain access to my account. | High |

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

#### UC03 — Export Service Records as PDF

| Field | Detail |
|-------|--------|
| **Actor** | Authenticated vehicle owner |
| **Precondition** | At least one service record exists for the vehicle |
| **Trigger** | User taps the download icon on the Service screen |
| **Main Flow** | 1. User taps Download. 2. System shows dialog: "Export as PDF" or "Export as CSV". 3. User selects PDF. 4. System checks storage permissions (Android < 10) or proceeds directly (Android 10+). 5. System generates PDF using iText7 and saves to Downloads. 6. File picker opens offering save location. |
| **Postcondition** | PDF file saved to device; user can share or open in a PDF viewer. |

#### UC04 — Receive Service Reminder Notification

| Field | Detail |
|-------|--------|
| **Actor** | System (WorkManager) / Vehicle owner |
| **Precondition** | Vehicle is saved with weekly riding distance; notification permission granted |
| **Trigger** | WorkManager fires the scheduled OneTimeWorkRequest |
| **Main Flow** | 1. System calculates estimated days until next 5,000 km service. 2. WorkManager schedules notification for that date (capped at 90 days). 3. On trigger, ServiceReminderWorker builds and posts a notification. 4. User taps notification; app opens to Home screen. |
| **Postcondition** | User is reminded to book a service appointment. |

### 3.3 Prototypes

*Note for appendix: Low-fidelity wireframes for the Home screen, Service Timeline, Fuel Log, and Trips screen should be inserted here. These were sketched prior to development and informed the final layout decisions described in Section 2.2.*

[INSERT WIREFRAME IMAGES — Home, Service Timeline, Add Service, Fuel Log, Trip Log]

**Key prototype decisions carried into production:**
- The home screen was initially designed with a grid layout but switched to a card list after testing revealed that a grid made it harder to scan vehicle details quickly.
- The service screen used a single scrollable list in early wireframes; it was split into Timeline and Summary tabs after recognising that cost analytics and chronological history serve different mental models.

---

## 4. Architecture

### 4.1 Architectural Pattern — MVVM

AutoCare uses the **Model-View-ViewModel (MVVM)** pattern recommended by Google's Android Architecture Guidelines (Android Developers, 2023). MVVM was chosen over MVP or MVC for three reasons:

1. **Lifecycle safety:** ViewModels survive configuration changes (screen rotation) without re-fetching data. This is critical given that Firebase listeners are long-lived.
2. **Separation of concerns:** The Activity/Fragment only updates the UI; all Firebase reads/writes and business logic live in the ViewModel. This makes the code testable in isolation.
3. **LiveData reactivity:** Firebase's real-time updates are exposed as LiveData streams, so the UI automatically reflects database changes without polling.

### 4.2 MVVM Layer Breakdown

```
┌─────────────────────────────────────────────────────┐
│                      View Layer                      │
│  Activities, Fragments, Adapters (UI only)           │
│  HomeActivity, ServiceRecordActivity, TripLogActivity│
│  PartsWarrantyActivity, FuelLogActivity, etc.        │
└────────────────────┬────────────────────────────────┘
                     │ observes LiveData
┌────────────────────▼────────────────────────────────┐
│                   ViewModel Layer                    │
│  VehicleViewModel, ServiceViewModel                  │
│  FuelLogViewModel, TripLogViewModel                  │
│  PartsWarrantyViewModel                              │
│  (Firebase listener owned here — init/onCleared)    │
└────────────────────┬────────────────────────────────┘
                     │ reads/writes
┌────────────────────▼────────────────────────────────┐
│                    Data Layer                        │
│  Firebase Realtime Database (cloud)                  │
│  Cloudinary (photo storage)                          │
│  SharedPreferences (theme preference)                │
│  WorkManager (notification scheduling)               │
└─────────────────────────────────────────────────────┘
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

Service record photos are uploaded to Cloudinary rather than stored in Firebase Storage because:
1. Cloudinary provides image transformation on-the-fly (resizing for thumbnails).
2. The Android SDK handles upload progress callbacks natively.
3. Firebase Storage would require separate billing setup for a demo app.

Photos are compressed to 80% JPEG quality before upload to reduce bandwidth on mobile networks.

**Decision — MediatorLiveData for combined stats**

The Summary tab needs both service records AND fuel logs to compute combined cost and cost-per-km. Rather than duplicating data or writing a manual observer, `MediatorLiveData<CombinedStats>` reacts to changes in either source and recomputes the stats atomically. This is an idiomatic LiveData pattern (Google, 2023).

### 4.4 Firebase Data Structure

```
users/
  {uid}/username

users_vehicles/
  {uid}/{vehicleId}/
    registrationNumber, brand, model, manufacturedYear,
    currentMileage, weeklyRidingDistance, photoUrl

users_services/
  {uid}/{registrationNumber}/{date}/
    date, odometerReading, serviceCost, serviceType,
    checkedItems[], notes, photoUrls[]

users_fuel/
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

| Feature | Activities/Fragments | CRUD | Firebase Path |
|---------|---------------------|------|---------------|
| Vehicle management | HomeActivity, AddVehicleActivity | Full | users_vehicles |
| Service records | ServiceRecordActivity, ServicesFragment, StatsFragment, AddServiceActivity | Full | users_services |
| Fuel log | FuelLogActivity, AddFuelLogActivity, FuelPriceTrendActivity | Full | users_fuel |
| Trip log | TripLogActivity | Full | users_trips |
| Parts & warranty | PartsWarrantyActivity | Full | users_parts |
| Vehicle documents | VehicleDocumentsActivity | Create/Delete | users_vehicle_documents |
| Settings | SettingsActivity | — | SharedPreferences |

### 5.2 Advanced Concepts Implemented

**Fragments with ViewPager2 and TabLayoutMediator**
The Service screen uses `FragmentStateAdapter` + `ViewPager2` + `TabLayoutMediator` to host two tabs (Timeline and Summary). The `ServiceViewModel` is shared between the Activity and both Fragments using `ViewModelProvider(requireActivity())` — the Activity creates the ViewModel first, and Fragments retrieve the same instance by calling the factory-less constructor.

**MediatorLiveData**
`ServiceViewModel.combinedStats` is a `MediatorLiveData<CombinedStats>` that adds both `_serviceRecords` and `_fuelData` as sources and recomputes a `CombinedStats` value whenever either changes. This avoids complex observer chaining and is the recommended pattern for derived state in MVVM (Android Developers, 2023).

**WorkManager for background notifications**
`ReminderScheduler` creates a `OneTimeWorkRequest` with a calculated delay based on estimated km per day. `ServiceReminderWorker` runs in the background, builds a `NotificationCompat`, creates a notification channel (required since Android 8.0), and posts the notification. Permission is checked at runtime for Android 13+ (`POST_NOTIFICATIONS`).

**Coroutines with suspendCancellableCoroutine**
Vehicle deletion involves removing data across multiple Firebase paths. Rather than nesting callbacks, `VehicleViewModel.deleteVehicle()` uses `viewModelScope.launch` and wraps each Firebase `removeValue()` in `suspendCancellableCoroutine` to bridge the callback API into a sequential coroutine.

**RecyclerView with expandable rows**
`ServiceRecordAdapter` uses a `MutableSet<Int>` (`expandedPositions`) to track which rows are expanded. Tapping a row calls `notifyItemChanged(position)` rather than `notifyDataSetChanged()`, which avoids full list re-render and keeps animations smooth.

**Real-time Firebase listeners in ViewModel**
Each ViewModel attaches a `ValueEventListener` in its `init {}` block and removes it in `onCleared()`. This ensures the listener is tied to the ViewModel lifecycle (which survives rotation) rather than the Activity lifecycle (which does not), preventing duplicate listeners and memory leaks.

**Wikipedia image fetching**
`WikipediaImageFetcher` queries the Wikipedia API to retrieve a vehicle thumbnail based on brand + model. It uses `OkHttp` with a background thread and delivers the result via a callback to the main thread. If no Wikipedia image is found, the user's Cloudinary-uploaded photo is used; if neither exists, a placeholder drawable is shown.

**Theme system**
`ThemeManager` persists the user's choice (Light / Dark / Follow System) in SharedPreferences and applies it via `AppCompatDelegate.setDefaultNightMode()`. The Application class calls `ThemeManager.applyTheme()` on startup so the correct theme is set before any Activity inflates. Light and dark colour palettes are defined in `values/colors.xml` and `values-night/colors.xml` respectively, using semantic names (`bg_primary`, `bg_card`, `text_primary`, etc.) so all drawables and layouts adapt automatically.

### 5.3 Code Quality Measures

- **KDoc comments** on all ViewModel classes document the Firebase listener lifecycle, LiveData pattern rationale, and non-obvious logic.
- **Private/public LiveData pair** (`_field: MutableLiveData` private, `field: LiveData` public) enforced across all ViewModels to prevent Activities from writing ViewModel state.
- **Package structure** follows Android conventions: `adapter/`, `model/`, `Home/`, `ServiceRecord/`, `FuelLog/`, `TripLog/`, `Parts/`, `Authentication/`, `Reminder/`, `PDFGenerator/`.
- **No Firebase operations in Activities** — all reads and writes are delegated to ViewModels.
- **Null-safety** — Firebase snapshot fields use `?: continue` or `?: ""` to skip or default missing fields rather than crashing.

---

## 6. Testing & Debugging

### 6.1 Manual Testing

Each screen was tested against the following scenarios:

| Screen | Scenario Tested |
|--------|----------------|
| Home | Add, edit (name/photo/mileage), delete vehicle; search filters correctly |
| Add Service | All service types; checklist persist on edit; photo upload via gallery and camera; edit pre-fills all fields |
| Service Timeline | Records appear in reverse-date order; expand/collapse shows correct data; edit navigates back and updates list |
| Service Summary | Stats update immediately after adding/editing/deleting a record |
| Fuel Log | Add, edit, delete; filter by vehicle; efficiency calculated between consecutive entries |
| Fuel Price Trend | Chart plots correctly per vehicle; spinner filter shows correct dataset |
| Trip Log | Add, edit, delete; summary bar shows correct total, longest, count |
| Parts & Warranty | Add, edit, delete; expiry countdown shows correct colour (green >30d, amber ≤30d, red expired) |
| Documents | Set and clear each of the three document dates |
| Settings | All three theme options apply immediately on selection |
| PDF Export | PDF and CSV generated correctly; MYR currency displayed |
| Reminders | WorkManager schedules notification within expected delay |

### 6.2 Bugs Found and Fixed

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| App crashed on vehicle tap | `ServiceRecordActivity` declared as `.ServiceRecordActivity` in manifest, but its package is `com.nibm.autocare.ServiceRecord` | Changed manifest entry to `.ServiceRecord.ServiceRecordActivity` |
| Edit button invisible in Documents screen | `ic_edit.xml` had `fillColor="#000000"` (black); invisible on dark background with no tint override | Changed `fillColor` to `@color/text_secondary` which resolves to gray in both themes |
| Build error after theme refactor | Removed `purple_700`, `teal_200`, `light_gray` from `colors.xml`; auth layouts still referenced them | Restored legacy colours to `values/colors.xml` |
| Trip summary numbers invisible in light mode | Summary bar used `@color/white` text on `@color/dark_gray` background, which became near-white in light mode | Changed background to `@color/bg_surface` and text to `@color/text_primary` |
| Fuel log auto-calc overwrote saved total in edit mode | `TextWatcher` on liters/price fields fired during prefill, recalculating `totalCost` | Wrapped prefill in `autoCalcEnabled = false` / `true` guard |

---

## 7. Development Process & Time Log

### 7.1 Time Log

| Week | Tasks | Estimated Hours |
|------|-------|----------------|
| Week 8 | Project setup, Firebase config, authentication (Login, Register, Forgot Password) | 6 |
| Week 9 | Home screen, vehicle list (RecyclerView), AddVehicleActivity | 7 |
| Week 10 | Service records — add, timeline RecyclerView, Firebase CRUD | 8 |
| Week 10 | Fuel log — add, list with BaseAdapter, efficiency calculation | 5 |
| Week 11 | Service records — Fragment/ViewPager2 refactor, StatsFragment, MediatorLiveData | 6 |
| Week 11 | MVVM migration — VehicleViewModel, ServiceViewModel, FuelLogViewModel | 5 |
| Week 12 | Edit mode — service records, fuel logs; PDF/CSV export with iText7 | 7 |
| Week 12 | Trip log, Parts & Warranty — dialogs, CRUD, ViewModels | 6 |
| Week 12 | Vehicle documents, reminders (WorkManager), Wikipedia image fetcher | 5 |
| Week 13 | Dark/Light theme system, Settings screen, light-mode visual fixes | 4 |
| Week 13 | Bug fixes, manifest corrections, currency change (Rs → MYR) | 3 |
| Week 13 | Report writing | 6 |
| **Total** | | **~68 hours** |

### 7.2 Commit History Summary

The Git repository (`jacoblearncode/autologger`) documents the development progression:

| Commit | Description |
|--------|-------------|
| Initial commit | Base project structure |
| Add MVVM architecture with ViewModel and LiveData | VehicleViewModel, base MVVM setup |
| Add Fragment-based tab UI to ServiceRecordActivity | ViewPager2, TabLayout, ServicesFragment, StatsFragment |
| Add KDoc and inline comments to MVVM architecture files | Documentation pass |
| Add Update/Edit for service records (full CRUD) | Edit mode in AddServiceActivity |
| Fuel log edit/ViewModel migration + document delete | FuelLogViewModel, edit flow, document clear |
| Add TripLog + PartsWarranty edit support and MVVM migration | TripLogViewModel, PartsWarrantyViewModel |
| Fix crash: correct ServiceRecordActivity package in AndroidManifest | Bug fix |
| Add dark/light/system theme toggle with Settings screen | ThemeManager, SettingsActivity, colour system |
| Fix build errors: restore legacy colours | Bug fix |
| Fix light mode visual bugs and remove TestActivity | Trip summary bar, timeline lines |
| Change currency from Rs to MYR and brighten light-mode accent | UX polish |

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

**Solution:** The Activity creates the ViewModel in `onCreate` (before `ViewPager2` attaches the Fragments), and Fragments call `ViewModelProvider(requireActivity())[ServiceViewModel::class.java]` with no factory argument. This guarantees the Fragments retrieve the exact same instance that the Activity created.

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

**Root cause:** The app was originally developed entirely in dark mode with some hardcoded colours (`#252525` in drawables, `@color/white` for text that assumed a dark background). When the semantic colour system was introduced (`bg_primary`, `bg_card`, etc.), these hardcoded values did not adapt.

**Solution:** A systematic audit of all layout files identified every instance of `@color/white` text and hardcoded dark hex values. Drawables were updated to use semantic colour references. The `ic_edit` icon's `fillColor="#000000"` was changed to `@color/text_secondary` so it renders as dark gray on light backgrounds and light gray on dark backgrounds.

**Takeaway:** "Design for both modes from the start" is much easier than retrofitting. Semantic colour names (`text_primary`, `bg_card`) should be defined before writing the first layout, with both light and dark values provided. Hardcoding hex colours in layouts is a technical debt that compounds with every screen added.

### 9.4 Exploration — Wikipedia Image Integration

One exploration that went beyond the core requirements was integrating the Wikipedia API to automatically fetch vehicle thumbnail images. The feature works as follows:

1. After saving a vehicle, `WikipediaImageFetcher` constructs a query: `https://en.wikipedia.org/api/rest_v1/page/summary/{brand}_{model}`.
2. If a thumbnail exists in the response, its URL is stored alongside the vehicle record in Firebase.
3. On the Home screen, `VehicleAdapter` prioritises: Cloudinary photo (user-uploaded) → Wikipedia thumbnail → placeholder drawable.

This was exploratory because the Wikipedia API returns inconsistent results (e.g., "Lamborghini Huracán" returns a car photo, but "Perodua Myvi" may return a disambiguation page). Handling these edge cases required testing with Malaysian vehicle brands specifically.

**Outcome:** The feature adds visual context to the vehicle list without requiring the user to manually find and upload a photo — a meaningful UX improvement that reduces friction for new users.

---

## 10. Conclusion

AutoCare demonstrates a complete multi-activity Android application using MVVM architecture, Firebase Realtime Database, LiveData, Fragments, WorkManager, and coroutines — going substantially beyond the baseline requirements by implementing full CRUD across seven data modules, a light/dark theme system, PDF/CSV export, push notifications, and a cloud-based photo system.

The development process reinforced three principles that will carry forward into future Android projects:
1. Invest in architecture upfront — refactoring an Activity-based app to MVVM halfway through is significantly more costly than starting with MVVM.
2. Use semantic colour resources from day one to avoid theme regression.
3. Coroutines make asynchronous code readable; Firebase's callback API is one of many places where `suspendCancellableCoroutine` pays dividends.

---

## Appendix A: Architecture & Data Diagrams

### A.1 MVVM Component Diagram

```
╔══════════════════════════════════════════════════════════════════════╗
║                          VIEW LAYER                                  ║
║                                                                      ║
║  ┌─────────────┐  ┌──────────────────┐  ┌────────────────────────┐  ║
║  │ HomeActivity│  │ServiceRecord     │  │ FuelLogActivity        │  ║
║  │             │  │Activity          │  │ AddFuelLogActivity     │  ║
║  │ AddVehicle  │  │  ├─ Services     │  │ FuelPriceTrendActivity │  ║
║  │ Activity    │  │  │  Fragment     │  └────────────────────────┘  ║
║  └──────┬──────┘  │  └─ Stats       │  ┌────────────────────────┐  ║
║         │         │     Fragment    │  │ TripLogActivity        │  ║
║         │         └────────┬────────┘  │ PartsWarrantyActivity  │  ║
║         │                  │           │ VehicleDocumentsActivity│  ║
║         │                  │           │ SettingsActivity       │  ║
╚═════════╪══════════════════╪═══════════╪════════════════════════╪═══╝
          │  observe LiveData│           │                        │
╔═════════╪══════════════════╪═══════════╪════════════════════════╪═══╗
║         ▼    VIEWMODEL LAYER           ▼                        ▼   ║
║  ┌───────────────┐  ┌───────────────┐  ┌──────────────────────────┐ ║
║  │ VehicleView   │  │ ServiceView   │  │ FuelLogViewModel         │ ║
║  │ Model         │  │ Model         │  │ TripLogViewModel         │ ║
║  │               │  │               │  │ PartsWarrantyViewModel   │ ║
║  │ LiveData:     │  │ LiveData:     │  │                          │ ║
║  │ vehicles      │  │ serviceRecords│  │ LiveData: fuelLogs,      │ ║
║  │               │  │ combinedStats │  │ trips, parts             │ ║
║  │ Coroutines:   │  │ (MediatorLD)  │  │                          │ ║
║  │ deleteVehicle │  │               │  │ Firebase listener in     │ ║
║  └───────┬───────┘  └───────┬───────┘  │ init{} / onCleared()    │ ║
║          │                  │           └──────────┬───────────────┘ ║
╚══════════╪══════════════════╪══════════════════════╪════════════════╝
           │  read / write    │                      │
╔══════════╪══════════════════╪══════════════════════╪════════════════╗
║          ▼    DATA LAYER    ▼                      ▼                ║
║  ┌────────────────────┐  ┌────────────┐  ┌─────────────────────┐   ║
║  │ Firebase Realtime  │  │ Cloudinary │  │ SharedPreferences   │   ║
║  │ Database           │  │ (photos)   │  │ (theme setting)     │   ║
║  │                    │  └────────────┘  └─────────────────────┘   ║
║  │ users_vehicles     │  ┌────────────┐                            ║
║  │ users_services     │  │ WorkManager│                            ║
║  │ users_fuel         │  │ (reminder  │                            ║
║  │ users_trips        │  │ scheduler) │                            ║
║  │ users_parts        │  └────────────┘                            ║
║  │ users_vehicle_docs │                                            ║
║  └────────────────────┘                                            ║
╚═══════════════════════════════════════════════════════════════════╝
```

### A.2 Firebase Data Structure Tree

```
Firebase Realtime Database
│
├── users/
│   └── {uid}/
│       └── username: "John Doe"
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
│               ├── date: "2024-01-15"
│               ├── odometerReading: "45000"
│               ├── serviceCost: "350.00"
│               ├── serviceType: "Full Service"
│               ├── checkedItems: ["Engine Oil", "Air Filter"]
│               ├── notes: "Used Mobil 1 synthetic"
│               └── photoUrls: ["https://cloudinary.com/..."]
│
├── users_fuel/
│   └── {uid}/
│       └── {logId}/
│           ├── registrationNumber: "WKL 1234"
│           ├── date: "2024-01-20"
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
│               ├── date: "2024-01-22"
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
                    ┌─────────────────────────────────────────────────┐
                    │                  AutoCare System                 │
                    │                                                  │
                    │  ┌─────────────────┐   ┌──────────────────────┐│
                    │  │ Manage Vehicles │   │  Log Service Record  ││
                    │  │  (Add/Edit/Del) │   │  (+ photo upload)    ││
                    │  └────────┬────────┘   └──────────┬───────────┘│
                    │           │                        │             │
                    │  ┌────────┴────────┐   ┌──────────┴───────────┐│
                    │  │   Log Fuel      │   │  Export PDF / CSV    ││
  ┌──────────┐      │  │   Fill-up       │   │  (iText7)            ││
  │          │──────►  └────────┬────────┘   └──────────────────────┘│
  │ Vehicle  │      │           │                                      │
  │  Owner   │      │  ┌────────┴────────┐   ┌──────────────────────┐│
  │          │──────►  │   Log Trip      │   │  Track Parts &       ││
  └──────────┘      │  │  (odometer in/  │   │  Warranty Expiry     ││
                    │  │   out)          │   └──────────────────────┘│
                    │  └────────┬────────┘                            │
                    │           │            ┌──────────────────────┐ │
                    │  ┌────────┴────────┐   │  Receive Service     │ │
                    │  │ Track Documents │   │  Reminder            │ │
                    │  │ (Insurance,Road │   │  (WorkManager)       │ │
                    │  │  Tax, Fitness)  │   └──────────┬───────────┘ │
                    │  └─────────────────┘              │             │
                    └──────────────────────────────┬────┘─────────────┘
                                                   │
                                          ┌────────▼────────┐
                                          │  System (Auto)  │
                                          └─────────────────┘
```

### A.4 Activity Flow Diagram

```
┌──────────────────┐
│  SplashActivity  │  (2s delay, checks auth state)
└────────┬─────────┘
         │
    ┌────┴────────────────────────────────┐
    │ Logged in?                          │
    ▼ No                                  ▼ Yes
┌──────────────┐                 ┌────────────────────┐
│ LoginActivity│                 │    HomeActivity     │
│              │                 │  (Vehicle list,     │
│ ┌──────────┐ │                 │   bottom nav)       │
│ │Register  │ │                 └──────────┬──────────┘
│ │Activity  │ │                            │
│ └──────────┘ │         ┌──────────────────┼──────────────────────────┐
│ ┌──────────┐ │         │                  │                          │
│ │Forgot    │ │         ▼                  ▼                          ▼
│ │Password  │ │  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐
│ │Activity  │ │  │ ServiceRecord│  │FuelLogActivity│  │  TripLogActivity  │
│ └──────────┘ │  │ Activity     │  │               │  │                   │
└──────┬───────┘  │  ┌─────────┐│  │AddFuelLog     │  │  (+ dialog edit)  │
       │          │  │Services ││  │Activity       │  └───────────────────┘
  ┌────┘          │  │Fragment ││  │               │
  │               │  └─────────┘│  │FuelPriceTrend │  ┌───────────────────┐
  └──────────────►│  ┌─────────┐│  │Activity       │  │PartsWarranty      │
   (on register   │  │Stats    ││  └───────────────┘  │Activity           │
    / login)      │  │Fragment ││                     │  (+ dialog edit)  │
                  │  └─────────┘│  ┌───────────────┐  └───────────────────┘
                  │             │  │Vehicle        │
                  │AddService   │  │Documents      │  ┌───────────────────┐
                  │Activity     │  │Activity       │  │  SettingsActivity │
                  └─────────────┘  └───────────────┘  │  (theme toggle)   │
                                                       └───────────────────┘
```

---

## Appendix B: App Screenshots

*[INSERT screenshots of each screen: Home (light + dark), Service Timeline, Service Summary, Add Service, Fuel Log, Fuel Price Trend, Trip Log, Parts & Warranty, Vehicle Documents, Settings]*

---

## Appendix C: GenAI Acknowledgment

### Tools Used
**Claude Code (Anthropic)** was used as a development assistant throughout this project via the Claude Code CLI and web interface.

### Nature of Use
Claude Code was used for:
1. Code scaffolding — generating initial ViewModel, Fragment, and Activity boilerplate based on architectural requirements I specified.
2. Bug diagnosis — identifying root causes of crashes and build errors (e.g., incorrect package name in AndroidManifest, missing colour resources).
3. Layout generation — drafting XML layout files which I then reviewed and adjusted.
4. Report drafting — this report was drafted with Claude Code assistance based on the actual codebase and rubric.

### What Was My Own Work
- App concept, feature selection, and overall scope
- All architectural decisions and their justification
- Firebase data structure design
- UI/UX design decisions and competitor research
- Testing and verification of every feature on a physical/emulated Android device
- All decisions reviewed, corrected, and approved before being committed

### Sample Prompts and Outputs

**Prompt 1 (Week 11):**
> "Migrate FuelLogActivity to MVVM — create FuelLogViewModel with Firebase listener in init/onCleared, LiveData<List<FuelLog>>, deleteFuelLog, and a factory"

**Output:** Generated `FuelLogViewModel.kt` with `ValueEventListener` in `init {}`, `partsRef.removeEventListener(listener)` in `onCleared()`, `MutableLiveData<List<FuelLog>>`, and `FuelLogViewModelFactory`. Code was reviewed for correctness and committed.

**Prompt 2 (Week 13):**
> "Add dark/light/system theme toggle — ThemeManager with SharedPreferences, SettingsActivity with RadioGroup, apply on startup in AutoCareCloudinary, add Settings to HomeActivity menu"

**Output:** Generated `ThemeManager.kt`, `SettingsActivity.kt`, `activity_settings.xml`, updated `AutoCareCloudinary.kt` and `menu_home.xml`. Light/dark colour files were also updated. Build errors from missing legacy colours were diagnosed and fixed in a follow-up prompt.

**Prompt 3 (debugging):**
> "App crashes when tapping vehicle — ServiceRecordActivity package is com.nibm.autocare.ServiceRecord but manifest registers it as .ServiceRecordActivity"

**Output:** Identified the correct manifest entry should be `.ServiceRecord.ServiceRecordActivity`. Fixed and verified.

---

## References

Android Developers. (2023). *Guide to app architecture*. Google. https://developer.android.com/topic/architecture

Android Developers. (2023). *ViewModel overview*. Google. https://developer.android.com/topic/libraries/architecture/viewmodel

Android Developers. (2023). *LiveData overview*. Google. https://developer.android.com/topic/libraries/architecture/livedata

Android Developers. (2023). *Dark theme*. Google. https://developer.android.com/develop/ui/views/theming/darktheme

Google. (2023). *Material Design 3 — Dark theme*. https://m3.material.io/styles/color/dark-theme

Nielsen, J. (2020). *10 usability heuristics for user interface design*. Nielsen Norman Group. https://www.nngroup.com/articles/ten-usability-heuristics/

iText Group. (2023). *iText 7 Core — Java/Android PDF library*. iText Software. https://itextpdf.com/products/itext-7

Cloudinary. (2023). *Cloudinary Android SDK*. Cloudinary. https://cloudinary.com/documentation/android_integration

PhilJay. (2023). *MPAndroidChart — A powerful chart library for Android*. GitHub. https://github.com/PhilJay/MPAndroidChart

Firebase. (2023). *Firebase Realtime Database*. Google. https://firebase.google.com/docs/database/android/start

Android Developers. (2023). *WorkManager — Schedule tasks with WorkManager*. Google. https://developer.android.com/topic/libraries/architecture/workmanager

Android Developers. (2023). *Kotlin coroutines on Android*. Google. https://developer.android.com/kotlin/coroutines

Wikimedia Foundation. (2023). *Wikipedia REST API — /page/summary/{title}*. https://en.wikipedia.org/api/rest_v1/
