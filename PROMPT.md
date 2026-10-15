# PROMPT.md: continue building Tessera

You are a senior Android engineer and product designer continuing work on **Tessera**, an open-source Android app in this repository (`Zirren-Git/WidgetApp`). This file tells you what to build, which rules you must never break, how the code is organized, and how to prove your work is correct. Read all of it before you change anything.

**Precedence:** the owner's newest instructions → this file → [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) → comments in code. The spec was written before decisions 15–18 (§2). Where they disagree, this file wins, and your first task is to update the spec (§14).

**When something isn't covered:** pick the option that best fits the rules in §3 and record it in the spec's decision log. Ask the owner only about choices that are hard to undo: signing keys, package names, new permissions, new network use, or anything that bends a rule in §3.

---

## 1. The product

Tessera is one app with two equal tabs:

1. **My Day**: a daily planner modelled on *Brite Daily Planner* (britetodo.com). The screen is made of cards the user picks from a gallery: tasks, habits, calendar, weather, mood, Pomodoro, notes, goals, budget, water and more. Each card opens a full screen, and each can also be placed on the home screen as a widget.
2. **Widgets**: a home-screen widget maker that is easier than KWGT. It offers templates, tap-to-edit and visual rules instead of formulas (spec §2–§8).

It runs on any Android phone from 8.0 up, needs no account, contains no AI, and keeps all data on the phone. It is free, MIT-licensed, and distributed only through GitHub Releases.

**Audience:** complete beginners. Every screen must look finished before the user touches anything.

---

## 2. Owner decisions

| # | Decision |
|---|---|
| 1 | Native Android: Kotlin, Jetpack Compose for the app, RemoteViews for home-screen widgets. |
| 2 | Complete beginners come first. |
| 3 | Widget designer: templates and tap-to-edit on a live preview. Layouts adapt to widget size, and a blank canvas is never the first screen. |
| 4 | No formulas, anywhere. Smart chips (`[Time · 24h]`) plus "When… then…" rules. |
| 5–7 | Widget content, tap actions and rendering tiers: spec §4 and §7. |
| 8 | Works on any Android phone (minSdk 26, no Google Play services) and has no AI. |
| 9 | One shared widget theme, optionally following the wallpaper. |
| 10 | Share widgets as files or QR codes, and import KWGT presets. No servers. |
| 11 | MIT license, no paid features or donation links, GitHub Releases only. |
| 12 | Original look: Paper (day) / Darkroom (night) plus a Material You switch. *Now the optional theme; see 18.* |
| 13 | Name **Tessera**, package `com.zirren.tessera`. Permanent. |
| 14 | Weather and calendar for widgets, plus quality-of-life features (spec §4.8). **Built.** |
| 15 | **Reference app: Brite Daily Planner.** In the owner's words: "It should have all the functions of Brite and look more similar to it." |
| 16 | **Two equal tabs:** *My Day* (the Brite-style planner) and *Widgets* (the designer). |
| 17 | **Rules unchanged:** no AI and no accounts. Everything stays on the phone. Quick-add such as "call mom tomorrow 5pm" uses an offline parser, and backup is a file the user exports. |
| 18 | **Look:** Brite-style by default (the **Lilac** theme, §6). Paper/Darkroom stays as an optional theme, and the Material You switch stays. |

**Brite is a reference, not a source.** Match its features, structure and friendly feel. Never use its name, logo, icons, illustrations, screenshots, marketing copy, habit names or quote texts. Write and draw everything yourself, or use assets whose licenses are compatible with MIT, and record those licenses in `app/src/main/assets/licenses/`.

---

## 3. Rules you must not break

1. **No AI.** No AI features, models or AI APIs. This includes on-device LLMs, OCR, in-app speech-to-text and "smart suggestions". Quick-add is a deterministic parser. Users who want to speak can use their keyboard's own voice typing.
2. **Nothing phones home.** No accounts, servers, analytics, ads, crash-reporting SDKs or remote config. The only network use allowed is:
   - MET Norway weather;
   - Open-Meteo place search, only while the user types;
   - an opt-in update check against the GitHub Releases API.

   Any other network use needs the owner's approval first.
3. **Any Android phone.** minSdk 26, any brand, no Google Play services. That rules out Firebase, ML Kit, Play Services location and Google Sign-In. If a library needs Play services, don't use it.
4. **Private by default.** Data lives in app storage. It leaves the phone only when the user exports a backup file. Health data (mood, pills, period, body) never leaves the device in any other way.
5. **Free and open.** MIT license. No paywalls, Pro tier, billing code or donation links.
6. **Never show a formula.** If a beginner can't read it aloud, it doesn't belong in the UI.
7. **Clocks are never wrong.** Anything that ticks on a widget is a native view (`TextClock`, `Chronometer`), never a value the app redraws every minute.
8. **Just-in-time permissions.** Ask for a permission only at the moment a feature needs it, with one plain sentence saying why. Every feature must still work, in a reduced form, if the permission is refused.
9. **Accessible.** WCAG AA text contrast in every theme, TalkBack labels on every control, 48 dp touch targets, and layouts that survive 200% font scale.
10. **Finished by default.** Empty states use real copy and offer a real action. No lorem ipsum and no "Feature One".

---

## 4. What already exists

Branch history: everything is merged into `main` (PR #1). Release [`v0.1.0-preview`](https://github.com/Zirren-Git/WidgetApp/releases/tag/v0.1.0-preview) is published as a debug-signed pre-release.

### Toolchain (checked against Google Maven / Maven Central on 2026-10-15)

| Item | Version | Note |
|---|---|---|
| Gradle | 9.6.1 | AGP 9.4's tested version; upgrade deliberately |
| AGP | 9.4.1 | Built-in Kotlin: **never apply `org.jetbrains.kotlin.android`** |
| Kotlin | 2.4.21 | Compose compiler plugin at the same version |
| Compose BOM | 2026.09.00 | Compose 1.12 needs compileSdk 37 |
| SDK | minSdk 26, compileSdk 37, targetSdk 36 | targetSdk goes to 37 after testing on Android 17 |
| Other libraries | activity-compose 1.13.0, lifecycle 2.11.0, coroutines 1.11.0, kotlinx-serialization-json 1.11.0 (tree API only), JUnit 4.13.2 | |

All versions live in `gradle/libs.versions.toml`.

### Modules

| Path | What it is |
|---|---|
| `core/model` | Plain Kotlin. `GeoPoint`, `Place`, units with regional defaults (Sweden: °C, m/s), `DateTimeText`, `Age` / `AgePhrases`. |
| `core/weather` | Plain Kotlin. MET Norway client (User-Agent, 4-decimal coordinates, `Expires` / `If-Modified-Since`, 429 backoff), JSON parser, weather symbols, feels-like, on-device sunrise/sunset, rain outlook, `RefreshPolicy`, `WeatherReporter`, Open-Meteo `PlaceSearch`, `WeatherText`. |
| `core/calendar` | Plain Kotlin. `AgendaEngine` (UTC all-day handling, filters, duplicates, multi-day spans, privacy mode, next-change timing) and `AgendaText`. |
| `app` | Android. Packages: `data` (Settings via SharedPreferences, `WeatherStore`, `WeatherRepository`, `CalendarRepository` via CalendarContract, `DeviceLocation` without Play services, `HealthChecks`, `Texts`), `sync` (JobScheduler: hourly weather, calendar content trigger, boundary jobs; `SystemEventsReceiver`), `widget` (RemoteViews renderers: *Morning Paper* in 3 sizes, *Up Next* agenda), `ui` (Compose: Home, Weather, Place search, Calendar settings, Forecast, Health check; `Navigator`; theme). |

Tests: 87 JUnit tests in `core/*/src/test`.

### Other things in the repo

- **Resources:** bundled OFL fonts (Fraunces, Instrument Sans, IBM Plex Mono) and hand-drawn vector icons.
- **Strings:** all in `res/values/strings.xml`, with plurals where counts appear.
- **Lint:** clean, with deliberate exceptions documented in `app/lint.xml`.
- **CI** (`.github/workflows/build.yml`): runs on every push and runs core tests, `:app:lintDebug` and `:app:assembleDebug`, then uploads the debug APK.
- **Releases** (`.github/workflows/release.yml`): publishing a GitHub Release builds the APK from its tag and attaches `tessera-<version>.apk` and `SHA256SUMS.txt`. With the four repository secrets set (`TESSERA_KEYSTORE_BASE64`, `TESSERA_KEYSTORE_PASSWORD`, `TESSERA_KEY_ALIAS`, `TESSERA_KEY_PASSWORD`) it builds a signed release. Without them it builds a debug-signed preview (`com.zirren.tessera.debug`).
- **Not built yet:** the widget *designer* itself (`.tessera` format, editor, render planner; spec §8 and milestones M0–M7) and every planner feature below.

---

## 5. App structure to build

```
Bottom: floating rounded "pill" with two tabs [My Day] [Widgets], plus a round "+" button (quick add) at bottom right on My Day

My Day (start destination)
  Top bar:  avatar (initial on an accent circle, or a photo from the system photo picker) · "My Day" · settings
  Week strip: 7 days in the phone's week order; the selected day is a tall accent pill; dots mark days with items
  Summary card: "Your mood: Good" (tap to log) · "3 tasks to complete" + progress bar · Day ring (segments for tasks, habits, mood)
  Segments: Morning | Day | Evening (defaults to the current part of the day: 05–12, 12–18, 18–05)
  Cards, in the user's order: Tasks · Habits · Calendar · Weather · Pomodoro · Daily quote · Notes · later Mood, Goals, Water, Budget, Journal…
  Edit mode: card gallery grouped as Plan · Practices · Wellbeing · Health · Money
             → customize name, color and icon → "Add to My Day" · drag to reorder · remove (the card stays in the gallery)
  Tapping a card's title opens the feature screen (for example Tasks, with its lists)

Widgets
  Today's Home screen: Morning Paper, Up Next, data sources (Weather, Calendar), Health check, Appearance
  Later: the widget designer (spec M0–M7)

Calendar screen: Month · Week · Day · Schedule, with a red "now" line and colored event blocks
Settings: Appearance (theme: Lilac or Paper & Darkroom; Use wallpaper colors; text size) · Notifications · Backup & restore · Licenses · About
First run: "What should Tessera help you with?" (multi-select: tasks, habits, calendar, focus, notes, mood, money, health)
           → pre-fills My Day with matching cards, and asks for an optional first name for the greeting ("Good morning, Sara")
```

Keep the existing `Navigator` approach (a simple back stack) or replace it with a typed navigation library only if it is free of Play services and works on minSdk 26.

---

## 6. Design system

### 6.1 Lilac (default theme, Brite-style)

Soft lavender background, white rounded cards, one strong purple accent, colorful category icons, progress rings and gauges, a floating bottom pill and a round "+" button. It should feel friendly, airy and tidy.

**Core tokens.** Contrast is measured against WCAG 2.x; AA needs 4.5:1 for text.

| Token | Light | Dark | Use |
|---|---|---|---|
| `background` | `#EEF0FA` | `#121320` | Screen background |
| `card` | `#FFFFFF` | `#1C1E2E` | Cards, sheets |
| `ink` | `#1E2039` (15.9:1 on card, 14.0:1 on background) | `#ECEDF7` (14.1:1, 15.8:1) | Primary text |
| `muted` | `#5F6483` (5.8:1, 5.1:1) | `#A3A7C4` (7.0:1, 7.8:1) | Secondary text, input outlines |
| `accent` | `#5B3FD6` (6.7:1 on card, 5.9:1 on background) | `#A391FF` (6.3:1, 7.1:1) | Selection, primary buttons, checkboxes, Pomodoro card |
| `on-accent` | `#FFFFFF` (6.7:1) | `#170F33` (7.0:1) | Text on accent |
| `hairline` | `#E2E4F0` | `#2E3047` | Dividers (decorative only) |
| `ring-track` | `#E8EAF3` | `#2A2C40` | Empty part of rings and gauges |

**Card tints** (light). Ink and muted text both pass AA on all of them:

| Name | Color |
|---|---|
| blue | `#E8F0FF` |
| peach | `#FFF2E5` |
| lilac | `#EFEBFF` |
| mint | `#E6F7EE` |
| rose | `#FDEAF2` |
| sand | `#FFF7DD` |
| mist | `#F1F2F7` |

In dark mode, derive tints at about 12% of the category color over `card`, and re-check contrast.

**Category colors** are for icons, bars, rings and color dots only. They are at least 3:1 against white, as graphics need.

| Category | Light | Dark |
|---|---|---|
| Orange | `#D9661F` | `#F59A5B` |
| Blue | `#2F6FE4` | `#7AA7FF` |
| Green | `#1E8A4C` | `#5CCB8A` |
| Red | `#D63B3B` | `#F47C7C` |
| Amber | `#B7791F` | `#F2C14E` |
| Pink | `#C73A85` | `#F07EB8` |
| Teal | `#0F8A80` | `#4FD1C5` |
| Violet | `#5B3FD6` | `#A391FF` |

Orange, amber, green and teal fall below 4.5:1 on white, so **never use them for text**. Text uses `ink`, `muted` or `accent`.

**"Important" pill:** text `#A4470F` on `#FFE4CF` (5.0:1).

**Shapes and elevation**

| Element | Spec |
|---|---|
| Cards | 20 dp corners, no border; they stand off the lavender background by tone |
| Chips | 12 dp corners |
| Buttons | 14 dp corners |
| Week-strip pill | Fully rounded |
| "+" button | 56 dp circle |
| Floating nav pill and "+" button | The **only** elements with a shadow: soft, low and short |

**Type.** Add **Plus Jakarta Sans** (SIL OFL 1.1, from `tokotype/PlusJakartaSans`; bundle Medium, Bold and ExtraBold, plus the license text) and use it in place of Instrument Sans in Lilac.

| Role | Style |
|---|---|
| Screen title ("My Day") | 26–28 sp, Bold |
| Card titles ("TASKS", "MORNING ROUTINES") | 13 sp, Bold, uppercase, +0.04 em tracking, with a colored icon to the left |
| Body | 16 sp, line height 1.5 |
| Numbers (Pomodoro "24:18", streak "42") | ExtraBold with tabular figures |

**Components**

- **Week strip:** day letters in `muted`. Today is underlined. The selected day is a vertical accent pill holding a white circle with the date.
- **Day ring:** a donut with 2–3 colored segments and a centered number. Tapping it explains how the number is made: "4 of 6 tasks, 2 of 3 habits, mood logged". Never show an unexplained score.
- **Habit gauge:** a 240° arc in a category color over `ring-track`, an icon in the middle, the name below, and a streak "🔥 27" shown after 3 days.
- **Task row:** a circular checkbox in `accent`, then the title, an optional time (clock icon plus time in `accent`) and an optional "Important" pill. A done task gets a strikethrough, turns `muted`, and moves to "Completed".
- **Pomodoro card:** `accent` background, a big `on-accent` time, a circular progress ring and a pause/play button.
- **Quote card:** typographic, on a tint or `accent` background. Use photo backgrounds only with CC0 images whose sources are recorded.
- **Card header:** icon, uppercase title, and an "+ Add" chip at the right that adds an item without leaving My Day.

**Icons:** draw your own. For UI icons, use 24 dp line icons in the existing hand-drawn style: 1.6 stroke, round caps (see `app/src/main/res/drawable/ic_*`). For category and habit icons, use simple filled shapes in category colors. Mood faces are your own 5 vector faces, not emoji, so they look the same on every phone. Emoji are allowed only in copy for streaks (🔥).

**Motion:**
- Rings fill over about 600 ms with an ease-out.
- A completed check strikes through, then fades.
- Dragged cards lift gently.
- Respect the system "remove animations" setting.

### 6.2 Paper and Darkroom (optional theme)

Keep the existing palette and fonts exactly as they are (spec §9; `ui/theme/Theme.kt`, `res/values*/colors.xml`). Implement theming so that either theme drives the whole app and all widgets.

The Material You switch (Android 12+) applies dynamic colors to whichever theme is chosen, while that theme's shapes and fonts stay.

### 6.3 Implementation

- **Setting:** add `Settings.themeStyle` (`LILAC` or `PAPER`; default `LILAC`).
- **Theme provider:** provide `TesseraColors` (tints, category colors, ring track, pill colors) through a `CompositionLocal`, next to Material 3's `ColorScheme`.
- **No hardcoded colors** in composables.
- **Home-screen widgets follow the theme.**
  - Android 12+: set backgrounds with `setInt(id, "setBackgroundResource", res)` and text colors with `RemoteViews.setColorStateList(id, "setTextColor", colorRes)`, so the launcher resolves day/night colors itself.
  - Android 8–11: resolve colors for the current UI mode at render time.

---

## 7. Brite feature parity

"All the functions of Brite" means everything below. The phases are defined in §9.

| Brite feature | Tessera | Phase |
|---|---|---|
| My Day built from cards, a gallery, customizable name/color/icon, reorder and remove | Same | P1 |
| Week strip, summary (mood, tasks to complete), "Energy" ring | Same; the ring is the explained **Day ring** | P1 |
| Morning / Day / Evening routines | Segments; tasks and habits carry a part of day | P1 |
| First-run interest quiz that pre-fills the main screen | Same | P1 |
| Personal and work tasks, priorities and tags ("Important"), due times, repeats, check-off | Lists ("Spaces": Personal, Work…), tags, repeat rules | P1 |
| Quick add in natural language | Offline parser, English first (§8.4) | P1 |
| Habit tracker with 80+ presets and streaks | ≥ 80 presets written by us, schedules, streaks, stats | P1 |
| Calendar: day / week / month / schedule; Google, Apple and Outlook sync | Phone calendars via CalendarContract (Android syncs the accounts), plus local events | P1 (agenda and month), P2 (week, day, create events) |
| Notes and docs, markdown editor, templates, daily note | Markdown-lite editor, templates, pin, tags | P1 |
| Pomodoro / focus mode | 25/5 with a long break; configurable; stats | P1 |
| Daily quotes / motivation, custom quotes | ≥ 365 public-domain quotes plus the user's own | P1 |
| Weather | Existing MET Norway weather as a card | P1 |
| Home-screen widgets: interactive tasks, calendar list/week/month, notes, Pomodoro | §10 | P1–P2 |
| Mood diary with reasons and statistics | Same | P2 |
| Goals tracker (daily, weekly, monthly, yearly; %) | Same | P2 |
| Water tracker | Same, with a "+250 ml" widget | P2 |
| Budget / expenses, categories, monthly budget | Same; amounts stored in minor units | P2 |
| Journal, idea journal, photo journal, diary templates | Same; photos via the system photo picker | P2 |
| Meditation / mental-health tools | Guided **breathing** timer (no audio content) | P2 |
| Reminders | Notifications; exact time when the user allows "Alarms & reminders" | P2 |
| Statistics (habits, mood, focus) | Same | P2 |
| Fitness: programs, exercises, workout timer | Programs built from **free-exercise-db** (`yuhonas/free-exercise-db`, Unlicense) plus a timer and history | P3 |
| Pills, period, fasting, body measurements | Same, local only | P3 |
| Food diary | Manual entries; an offline food database only if the owner approves its size | P3 |
| Projects: list, Kanban, Eisenhower matrix | Same | P4 |
| Archive, search by tags | Same | P4 |
| Export calendar to PDF / print | `PrintManager` / `PdfDocument` | P4 |
| Shopping lists, sketches | Same | P4 |
| Themes, accent colors, backgrounds, text size, app icon choice | Lilac / Paper, accent picker, My Day background photo, text size, icon variants via `activity-alias` | P1 (themes), P4 (the rest) |
| Lock-screen widget | Keyguard widget category where the launcher supports it | P4 |

**Deliberately excluded by the owner's rules:**
- the AI assistant and AI suggestions;
- accounts, sign-in and cloud sync;
- collaboration, shared spaces, shared budgets and messaging;
- Photos OCR;
- in-app voice input;
- Zoom and Meet integrations;
- apps for other platforms;
- subscriptions.

**What replaces sync** is backup and restore to a file.

---

## 8. Architecture

### 8.1 Modules

Keep domain logic Android-free and tested.

- **New plain-Kotlin modules:**
  - `core/planner`: tasks, repeat rules, habits and streaks, the Day ring formula, the Pomodoro state machine, budget math, goal progress, mood statistics.
  - `core/quickadd`: the quick-add parser.
- **Gradle setup:** use the same Gradle setup as `core/weather` (`java-library` plus `kotlin.jvm`, JVM target 17) and add each module to the CI test list.
- **Persistence:** Room in `app`, or in a new Android library module if `app` gets crowded.
  - Use the **KSP** processor, never kapt.
  - With AGP 9's built-in Kotlin you need KSP 2.3.1 or newer; verify the current stable Room and KSP versions first.
  - Repositories expose `Flow`.
  - Use interfaces in core, implementations in Android.
- **Settings** stay in the existing SharedPreferences `Settings`.

### 8.2 Data conventions

| Kind of data | Store as |
|---|---|
| IDs | `kotlin.uuid.Uuid` strings (stable in Kotlin 2.4), so backups merge cleanly |
| Dates | `LocalDate` as epoch day (`Long`) |
| Instants | Epoch milliseconds |
| Times of day | Minute of day |
| Money | `Long` minor units plus an ISO currency code (never `Double`) |

**Repeat rules:** a small subset of RRULE that we own, kept in `core/planner` and tested around DST and month ends:
- daily;
- weekdays;
- weekly on chosen days;
- every N days;
- monthly on day N or the last day;
- yearly.

**My Day layout:** a `card` table with type, title, color, icon, order, part of day and a JSON config.

### 8.3 Backup

- **Export:** `tessera-backup-YYYY-MM-DD.json` through the Storage Access Framework (`ACTION_CREATE_DOCUMENT`).
- **Schema:** `{"format": "tessera/backup", "version": 1, …}`.
- **Import:** validates, migrates and shows a summary before replacing anything. It never uploads anywhere.

### 8.4 Quick-add parser (`core/quickadd`)

**Behavior**
- Deterministic and offline, with "now", time zone, locale and first day of week passed in.
- Returns structured fields plus the remaining title.
- The UI shows what it understood as removable chips while the user types, for example "Tomorrow · 17:00 · #work". Removing a chip puts those words back into the title.
- English first. Add Swedish only when the owner asks for translations.

**What it understands**

| Kind | Examples |
|---|---|
| Dates | `today`, `tonight`, `tomorrow`, weekday names, `next monday`, `in 3 days`, `on 17 oct`, `oct 17`, `17/10` (day/month order from the locale), `this weekend` |
| Times | `5pm`, `17:00`, `at 9`, `noon`, `morning` (09:00), `evening` (18:00) |
| Repeats | `every day`, `every weekday`, `every monday`, `every 2 weeks`, `monthly` |
| Duration | `for 30 min` |
| Tags | `#work` |
| Priority | `!` or `important` |
| List | `@personal` |

At least 40 unit tests, including ambiguous inputs ("may" as a word versus the month) and past times, which roll to the next day.

### 8.5 Background work, reminders, notifications

- **Keep the existing JobScheduler patterns:** no permanent services.
- **Reminders** use `AlarmManager`:
  - `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` is true;
  - otherwise `setWindow`, with a Health check line that offers the "Alarms & reminders" setting.
  - Declare `SCHEDULE_EXACT_ALARM`, never `USE_EXACT_ALARM`.
  - Re-arm reminders after boot, time changes and app updates.
- **Notifications:** channels "Reminders" and "Focus timer". Ask for `POST_NOTIFICATIONS` (Android 13+) when the user creates their first reminder or starts their first focus session.
- **Pomodoro needs no foreground service:**
  - an ongoing notification with a countdown chronometer (`setUsesChronometer(true)`, `setChronometerCountDown(true)`, `setWhen(end)`);
  - an alarm at the end time;
  - state persisted so it survives process death;
  - widgets show a native `Chronometer` countdown.
- **Calendar writes:** ask for `WRITE_CALENDAR` only when the user creates an event in a phone calendar. Without it, the event is saved as a timed local item.
- **Photos:** use `ActivityResultContracts.PickVisualMedia`, which falls back to the document picker on phones without the system photo picker. Copy a downscaled copy into app storage.

---

## 9. Plan of work

Work in **vertical slices**. Each slice ends with:
- green CI;
- an installable APK;
- updated docs.

Alternate between planner phases and designer milestones so both tabs keep moving.

### P1: Lilac, tabs and the planner core (do this first)

1. Record decisions 15–18 in spec §1 and update §4, §9, §10 and §12 to match this file (a docs-only commit).
2. Build the theme system (§6.3): Lilac by default, Paper/Darkroom selectable, the Material You switch still working, Plus Jakarta Sans bundled.
3. Build the navigation shell: the bottom pill with My Day and Widgets, and the "+" button. Today's Home screen becomes the Widgets tab, unchanged in behavior. Weather, Calendar settings, Forecast and Health stay reachable.
4. Add Room, `core/planner` and backup v1, with export and import screens under Settings.
5. Build the My Day screen: top bar, week strip, summary card, Morning/Day/Evening, edit mode with the gallery, and first-run interests.
6. Build these cards and their screens:
   - **Tasks:** Spaces, add/edit (title, date, time, repeat, important, tags, part of day, notes), swipe to complete or delete, a Completed section.
   - **Habits:** ≥ 80 presets with our own names and icons, custom habits, schedules, check-ins, streaks, weekly/monthly view.
   - **Calendar:** today's agenda from the existing `AgendaEngine`, plus a month view.
   - **Weather:** a card over the existing repository.
   - **Pomodoro.**
   - **Daily quote.**
   - **Notes.**
7. Build quick add with the parser from §8.4.
8. Build home-screen widgets: **Tasks** (interactive), **Habits**, **Pomodoro** (§10).

**Acceptance:**
- A new user installs the app, picks interests, and sees a finished My Day.
- They add "call mom tomorrow 5pm" and see it under tomorrow at 17:00.
- They tick a task on the home-screen widget and My Day updates.
- They tick a habit and the streak increments.
- They run a focus session with the screen off and get notified at the end.
- They export a backup, clear app data, import it, and get everything back.

### W1: designer foundations (spec M0 remainder plus M1)

- `.tessera` v1 format and migrations.
- The template gallery inside the Widgets tab.
- Text, shape, image and stack blocks.
- The render planner (native and image tiers).
- The pin flow.
- 3 templates.

**Done when:** picking a template puts it on the home screen.

### P2: wellbeing, money, deeper calendar

Mood diary with statistics, goals, water (and its widget), budget and expenses (and their widget), journal and photo journal, a breathing timer, reminders, statistics screens, calendar week/day/schedule views with event creation, calendar list/week/month widgets, and notes, mood and quote widgets.

### W2: designer live content and rules (spec M2–M3)

Smart chips, live data (clock, date, battery, weather, photo, countdown, quotes), the rules editor with *Try it*, and quick controls.

### P3: health

Fitness programs from `yuhonas/free-exercise-db` (Unlicense; bundle text first; add images only if the APK size stays reasonable), a workout timer, pills with reminders, a period tracker (local only, with an optional app lock), fasting and body measurements with charts. Food diary scope needs the owner's approval.

### P4: organize and polish

Projects (list, Kanban, Eisenhower matrix), archive, tag search, PDF export, shopping lists, sketches, accent picker, a My Day background photo, app icon variants and lock-screen widgets.

### W3 and later: spec M4–M7

Themes, sharing, KWGT import, the animated tier and hardening.

### Releases

At the end of each phase, publish a GitHub Release. The workflow attaches the APK. Mark it as a pre-release until the owner adds the signing secrets.

---

## 10. Home-screen widgets for planner features

Follow the patterns in `app/src/main/kotlin/com/zirren/tessera/widget/`:

- **RemoteViews only.** Use allowed view classes, put `@android:id/background` on the root, and use size-mapped `RemoteViews` on Android 12+, with one layout chosen from the reported size on older versions.
- **Redraw through `WidgetUpdater`, scheduled through `Scheduler`:** boundary jobs, never a minute timer.
- **Tasks (interactive):**
  - Android 12+: `RemoteCollectionItems` with `CheckBox`, `setCompoundButtonChecked` and `setOnCheckedChangeResponse`.
  - Android 8–11: tapping a row toggles it through a broadcast `PendingIntent` (or `RemoteViewsService` with fill-in intents).
  - Redraw immediately after a toggle.
- **Habits:** tap to check today; show the streak.
- **Pomodoro:** a native `Chronometer` countdown, with start, pause and stop as broadcasts.
- **Calendar:**
  - List reuses `AgendaEngine`.
  - Week and month use a `GridLayout` of text views, redrawn at midnight and when the calendar changes.
- **Water, mood and quote:** a single tap does the obvious thing (+250 ml, log a mood, open the quote).
- **Previews and colors:** every widget gets a picker preview layout and theme-aware colors (§6.3).

---

## 11. How to work

- **Build and test:**

  ```
  ./gradlew :core:model:test :core:weather:test :core:calendar:test
  ./gradlew :app:lintDebug :app:assembleDebug
  ```

  Add new core modules to these commands and to `.github/workflows/build.yml`.

- **No local Gradle?** If your environment can't download Gradle or Android dependencies, push to your branch and use the **Build** GitHub Actions run as your compiler. Read its log; the lint report is printed in the "Show lint report" step.

- **Lint stays clean:** 0 errors and 0 warnings. A new exception goes in `app/lint.xml` with a one-line reason.

- **Tests:**
  - New rules and calculations get JUnit 4 tests in plain-Kotlin modules.
  - Fix "now", the time zone and the locale in tests.
  - Never depend on the machine's default locale; pass `Locale` explicitly.

- **Android compatibility of core code:** core modules run on API 26. Don't use Java APIs newer than what Android 8 has, for example `LocalDate.ofInstant`, `List.of`, `InputStream.readAllBytes`, `String.isBlank` from Java or `Duration.toSeconds`. Kotlin stdlib equivalents are fine.

- **Strings:**
  - Every user-visible word goes in `res/values/strings.xml`.
  - Use positional arguments (`%1$s`) when there are several, and `<plurals>` for counts.
  - In Compose, read text with `stringResource` or `LocalResources.current`, **never** `LocalContext.current.getString`. Lint fails the build on that.

- **Dependencies:** check every new library for:
  1. a license compatible with MIT;
  2. no Play services;
  3. minSdk ≤ 26;
  4. no network or analytics.

  Pin its version in the catalog after checking Google Maven or Maven Central.

- **Commits:** small, imperative, scoped (`feat(planner): …`, `fix(widget): …`, `ui: …`, `core: …`, `docs: …`, `ci: …`). Keep the build green at every commit.

- **Docs:** when behavior changes, update `docs/PRODUCT_SPEC.md` (decisions, milestones, verification table) and `README.md` (features, install). Keep `docs/images/widgets-mockup.png` truthful, or label it as a mock-up.

- **Copy and tone:**
  - Short, warm, concrete: "3 tasks to complete", "Nothing planned. Enjoy the quiet, or tap + to add something."
  - Beginners never see jargon such as "sync", "RRULE" or "permissions dialog".

---

## 12. Pitfalls this project already hit

- **AGP 9:** applying `org.jetbrains.kotlin.android` breaks the build. Use `kotlin { compilerOptions { … } }` if you need options.
- **AGP 9 lint:** `textOutput = file("stdout")` writes a *file* named `stdout`. Use the default report location.
- **List removal:** `MutableList.removeLast()` / `removeFirst()` compile to Java 21 methods and crash on Android below 15. Use `removeAt(lastIndex)`.
- **All-day events** in CalendarContract are stored at UTC midnight. Read them as dates in UTC, never in the local zone (see `EventInstance.firstDay`).
- **`LocationListener`:** implement all four callbacks. Before Android 10 they have no default bodies.
- **SharedPreferences listeners** are held weakly. Keep a strong reference (see `rememberSettingsVersion`).
- **RemoteViews:**
  - Plain `View`, custom views and Compose aren't allowed.
  - Use a 1 dp `FrameLayout` with a background as a divider.
  - Many setters are API 31+; guard them with `Build.VERSION.SDK_INT`.
- **Interactive widgets:** give each `PendingIntent` a unique request code or data URI. `PendingIntent` equality ignores extras.
- **MET Norway terms:** send an identifying User-Agent, at most 4 coordinate decimals, honour `Expires`, and revalidate with `If-Modified-Since`.
- **Fonts:** check glyph coverage before using a character. Instrument Sans lacks superscript digits and Fraunces lacks "→".
- **Release workflow** triggers on `release: published` and builds from the release's tag commit.

---

## 13. Definition of done (every change)

- [ ] Builds with the pinned toolchain; CI green (core tests, lint with 0 warnings, debug APK).
- [ ] New logic lives in plain-Kotlin modules with unit tests; core code is API 26 compatible.
- [ ] Works and looks right in Lilac light and dark, and in Paper/Darkroom; text contrast meets AA.
- [ ] No hardcoded strings or colors. TalkBack labels are present, touch targets are 48 dp, and it works at 200% font scale.
- [ ] No new network use, AI, account or Play-services dependency. Permissions are asked just in time, with a reduced-mode fallback.
- [ ] Widgets: allowed views only, size-mapped on Android 12+, redraws scheduled through `Scheduler`, a picker preview.
- [ ] Spec and README updated. Assets and licenses recorded.

---

## 14. Start here

1. Read [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md), [`README.md`](README.md) and the code in `core/` and `app/`.
2. Run the build and test commands in §11, or push and read CI. Confirm 87 tests and a clean lint before you change anything.
3. Commit decisions 15–18 into the spec (P1 step 1).
4. Continue with P1 in order. After each step, push and confirm CI is green before starting the next.
