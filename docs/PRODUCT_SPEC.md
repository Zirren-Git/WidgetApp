# Tessera — product spec

> A friendly daily planner and a home-screen widget maker for Android, both easy enough for a complete beginner.
> Open source · any Android 8+ phone · no AI · no accounts · no ads.

**Status:** building (2026-10-15). Project skeleton, weather and calendar data, and the first two widgets are in the repo; see §12. The planner (decisions 15–18) is specified in §4.9 and scheduled in §12, but not built yet.
The 18 decisions in §1 are the owner's. Everything else is the engineering recommendation that follows from them, and can be changed.

**Contents**
1. [Decisions so far](#1-decisions-so-far)
2. [Who it's for](#2-who-its-for)
3. [Rules we build by](#3-rules-we-build-by)
4. [What's in v1](#4-whats-in-v1)
5. [What's not in v1](#5-whats-not-in-v1)
6. [Key flows](#6-key-flows)
7. [How widgets reach the home screen](#7-how-widgets-reach-the-home-screen)
8. [The `.tessera` file format](#8-the-tessera-file-format)
9. [How the app looks](#9-how-the-app-looks)
10. [Architecture](#10-architecture)
11. [Distribution](#11-distribution)
12. [Milestones](#12-milestones)
13. [Risks and open questions](#13-risks-and-open-questions)

---

## 1. Decisions so far

Three rounds of questions and a follow-up request, then a fourth round when the owner asked for Brite Daily Planner's features and look. All on 15 Oct 2026.

| # | Question | Answer | What it means for the build |
|---|---|---|---|
| 1 | Platform, and what to build first | **Android, straight to native** | Kotlin + Jetpack Compose editor. Widgets built from RemoteViews, plus Glance for the animated tier. Built and tested in Android Studio, because this sandbox can't compile Android code. |
| 2 | Who we design for first | **Complete beginners** | No formulas, no layer tree, no "scale" number. Every default must already look finished. |
| 3 | How building should feel | **Templates + tap-to-edit** | The template gallery is the front door. Editing means tapping things on a live preview. Layout adapts to widget size on its own. A blank canvas exists but is never the first screen. |
| 4 | What replaces KWGT formulas | **Visual rules + smart chips** | Text holds structured chips (`[Time · 24h]`), and logic is "When… then…" rules. The file format contains **no expression strings**. |
| 5 | What widgets show in v1 | **Essentials + personal & aesthetic** | Clock, date, battery, weather, photos (single + slideshow), countdowns, quotes, to-dos, habits. Calendar, now-playing music and steps are v1.1 candidates. |
| 6 | What tapping does | **Smart defaults + quick controls, customizable** | Every block has a sensible tap. Quick controls: media keys, flashlight, checkboxes. Any tap can be changed to another action. |
| 7 | What matters on the home screen | **"All of the above. Time is not an issue."** | Tiered renderer (§7): native views for anything that ticks, image layers for decoration, Remote Compose animation on Android 16+. Chosen automatically for each element and each phone. |
| 8 | Our edge over Gemini "Create My Widget" | **Works on any Android phone + no AI** | minSdk 26 (Android 8.0). No Google Play services dependency anywhere, so it also runs on Huawei and de-Googled phones. No AI features or AI APIs. |
| 9 | Colors and fonts across widgets | **Shared theme + wallpaper option** | One theme for every widget, optionally following the wallpaper. Each widget can opt out. |
| 10 | Sharing | **File/QR sharing + KWGT import** | `.tessera` files via the share sheet, QR codes for small widgets, and a `.kwgt` importer that reports what it couldn't translate. No servers. |
| 11 | Money | **"No paid features or donation page. Fully open source and only on GitHub."** | MIT license (already in the repo). No billing code and no donation links. APKs on GitHub Releases only: no Play Store, no F-Droid. See §11 on Google's developer verification. |
| 12 | Editor look | **"Paper by day, darkroom by night and an option to switch to Material You"** | Two hand-tuned palettes that follow system dark mode, plus a "Use wallpaper colors" switch (Android 12+). *Now the optional theme; see 18.* |
| 13 | Name | **Tessera** | App label "Tessera", package `com.zirren.tessera`. Effectively permanent once people install it, because updates need the same package name and signing key. |
| 14 | Follow-up request | **"Add the possibility for the widgets to fetch weather data and calendar (and other QoL features)"** | Calendar moves into v1 (it was a v1.1 candidate). Weather is specified in detail (§4.1, §7.3). New §4.8 lists the quality-of-life features. Both data sources are built first, ahead of the editor (§12). |
| 15 | Follow-up request | **"It should have all the functions of Brite and look more similar to it"** (Brite = *Brite Daily Planner*, britetodo.com) | Tessera becomes a daily planner as well as a widget maker. §4.9 lists Brite's features, the phase that builds each one, and what our rules exclude. Brite is a reference, not a source: no names, logos, icons, illustrations, copy, habit names or quotes are copied. |
| 16 | What Tessera becomes | **Both: planner and widget maker** | Two equal tabs in a floating bottom bar: *My Day* (the planner and the start screen) and *Widgets* (today's Home screen, later the designer). Every planner card can also go on the home screen as a widget. |
| 17 | Brite's AI, accounts and sync vs. our rules | **Keep the rules** | No AI assistant, accounts, cloud sync or collaboration. Quick add ("call mom tomorrow 5pm") uses an offline parser, and backup and restore is a file the user exports. Confirms rule 6 (§3). |
| 18 | Look | **Brite-style by default, Paper/Darkroom as an option** | New default theme **Lilac** (§9.1): lavender background, white rounded cards, one purple accent and colorful category icons. Paper/Darkroom (decision 12) stays as an optional theme, and the Material You switch works with both. Lilac knowingly departs from the human-ui-craft "one accent, no purple" rule; Paper/Darkroom keeps it. |

---

## 2. Who it's for

Complete beginners: someone who saw a beautiful home screen online, installed KWGT, met `$df(hh:mm)$` and a layer tree, and gave up.

**They want:** a widget that looks good in under two minutes, matches their wallpaper, and shows their photos, the weather and how many days until something.

**They never want to see:** formulas, layer trees, `scale: 137`, anchors and padding math, weather API keys, or a blank canvas as the first screen.

**The planner (decisions 15–16) is for the same people.** They want one calm place for today's tasks, habits and plans that looks finished without any setup, and they never meet a sign-up wall, a subscription screen or an AI assistant.

---

## 3. Rules we build by

1. **Never show a formula.** If a beginner can't read it aloud, it doesn't belong in the UI.
2. **It fits, every time.** Layout uses stacks and dp units. There is no scale field, and the editor previews every common widget size before you place it. (Sizing is KWGT's most common complaint.)
3. **Clocks are never wrong.** Anything that ticks is drawn by the launcher itself, so it doesn't depend on our app staying alive.
4. **Any Android phone.** Android 8.0+, any brand, no Google Play services. If a library needs them, we don't use it.
5. **Made by people, not generated.** No AI features, models or AI APIs. That includes on-device language models, OCR, in-app speech-to-text and "smart suggestions". Quick add is a deterministic parser (decision 17). Templates, habit presets and quotes are chosen by people.
6. **Nothing phones home.** No accounts, analytics, ads or crash-reporting SDKs. The network is used only for weather, city search (when you type), and an opt-in update check. Any other network use needs the owner's approval first. *(Confirmed by decision 17.)*
7. **Beautiful untouched.** Every template and default looks finished before the user changes anything.
8. **Private by default.** Data lives in app storage and leaves the phone only in a backup file the user exports. Health data (mood, pills, period, body) never leaves it any other way.
9. **Just-in-time permissions.** A permission is asked for only when a feature needs it, with one plain sentence saying why. Every feature still works, in a reduced form, if the answer is no.
10. **Accessible.** WCAG AA text contrast in every theme, TalkBack labels on every control, 48 dp touch targets, and layouts that survive 200% font scale.

---

## 4. What's in v1

§4.1–§4.7 describe the widget designer, §4.8 the quality-of-life features, and §4.9 the planner (*My Day*).

### 4.1 Content blocks

| Block | Shows | Permission | How it stays current |
|---|---|---|---|
| Clock (digital) | Time in any curated font | none | Native `TextClock`: the launcher ticks it, with no battery cost from Tessera |
| Clock (analog) | Dial + hands | none | Android 12+: native `AnalogClock` with custom dial and hands. Android 8–11: native with bundled dial styles; fully custom dials use the minute-refresh fallback (§7.4) |
| Date | Weekday, day, month, year | none | Native `TextClock` with a date pattern |
| Battery | Level, charging state, ring or bar | none | System battery broadcasts, on change |
| Weather | Now, feels like, high/low, condition, icon, wind, humidity, UV, "Rain from 14:00", next hours, coming days, sunrise/sunset, place | Location optional; city search works without it | Hourly, network-only background job; never before MET's `Expires`. MET Norway data, no API key |
| Calendar | Next event ("10:30 Standup", "Now · until 11:00"), agenda for today and the coming days | Calendar (read-only), asked when you add it | A few seconds after any calendar change (content-change trigger), plus at each event's start and end |
| Photo | One photo or a slideshow | none (system photo picker) | The slideshow flips natively (`ViewFlipper`), so the app never wakes |
| Countdown | "62 days until Midsummer" | none | Daily at midnight. Under 24 h, a native `Chronometer` counts down |
| Quote | Quote of the day + author: a bundled public-domain set plus your own | none | Daily |
| To-do | Short checklist you can tick on the home screen, from the planner's tasks (§4.9) | none | On tap |
| Habit | Streak + last-4-weeks grid; tap marks today done. The same habits as the planner (§4.9) | none | On tap and at midnight |

Design pieces: **Text, Shape** (rectangle, circle, pill, squircle), **Icon, Image, Progress** (bar or ring), **Spacer, Stack** (row, column, overlay).

### 4.2 Smart chips

Chips are tappable tokens inside text. Formats are chosen by tapping a preview of the real value ("14:05" / "2:05 PM"), never by typing pattern codes.

| Source | Chips |
|---|---|
| Time | 24h · 12h · hour · minute · seconds · AM/PM |
| Date | weekday · short weekday · day · month · short month · year · week number |
| Battery | level % · charging state · time to full (Android 9+) |
| Weather | temperature · feels like · high · low · condition · chance of rain · wind · place |
| Countdown | days left · event name · event date · "in 3 days / today / 2 days ago" |
| Calendar | next event title · next event time · "in 25 min" · events today · day of a multi-day event ("Day 2 of 3") |
| Quote | text · author |
| To-do | items left · next item |
| Habit | streak · done today |

### 4.3 Visual rules

A rule is **When** (one or more conditions, combined with *all of* or *any of*, one level deep) and **Then** (one or more effects).

- **When:** battery below/above X% · charging / not charging · time between A and B · weekday / weekend · weather is rain / snow / clear / cloudy · temperature below/above X · rain expected within N hours · an event is on now · next event within N minutes · no more events today · countdown day arrives · all to-dos done · habit not done by HH:MM
- **Then:** change color · change text · show/hide a block · swap photo or icon · switch to the theme's light/dark variant
- **"Try it" simulator** in the editor: slide "Pretend battery is 15%" or "Pretend it's 22:00" and watch the rule fire. KWGT has nothing like this, and it's how beginners learn what a rule does.

### 4.4 Tap actions

**Smart defaults**

| Block | Tap does |
|---|---|
| Clock | Opens the system alarms |
| Date | Opens the calendar app at today |
| Calendar event | Opens that event in the calendar app; "+" on the agenda creates a new event |
| Battery | Opens battery settings |
| Weather | Opens Tessera's forecast sheet (works on phones with no weather app) |
| Photo | Shows the next photo (slideshow) or opens it in the gallery |
| Countdown | Opens the countdown in Tessera |
| Quote | Shows another quote |
| To-do item | Ticks it right on the home screen |
| Habit | Marks today done |

**Quick controls:** play/pause, next, previous (media keys go to the active player, so no notification access is needed) · flashlight on/off · tick a to-do · mark a habit · next photo · refresh weather.

**Customizable:** every block has a *When tapped* row with these choices: Default · Open an app · Open a website · Open an app shortcut · Quick control · Nothing.

### 4.5 Starter templates

Starter set at launch, each rendered live in the gallery with the user's real time, battery and weather:

| Template | What it is |
|---|---|
| Morning Paper | Big serif date, time, and one line of today's weather |
| Polaroid | Your photo, with a handwritten-style caption and the date |
| Days Until | "62 days until Midsummer": edit the event and it counts down |
| Battery Ring | Ring + percentage that turns red below 20% (shows off a rule) |
| Three Things | Three-item to-do list you can tick on the home screen |
| Streak | Habit grid for the last four weeks; tap to mark today |
| Analog Moss | Analog clock with a soft custom dial |
| Quiet Quote | Quote of the day in serif italic with a small label for the author |
| Darkroom Clock | Huge tabular digits with a tiny uppercase date label |

### 4.6 Theming

- **A theme is** a palette (background, surface, text, muted text, accent), a font pair (display + text) and a corner style (sharp / soft / round).
- **Shared by default.** Changing the shared theme re-renders every placed widget at once.
- **Follow wallpaper (optional).** Android 12+ uses the system's dynamic colors. Android 8.1–11 derives a palette from `WallpaperManager.getWallpaperColors()`. Android 8.0 keeps the manual palette.
- **Per-widget opt-out.** Each widget has a "Use shared theme" switch, on by default.
- **Fonts:** a curated set of open-license (OFL) fonts bundled in the APK, so they work offline and in native widget text. Imported font files (from KWGT presets or your own) work too, with the caveat in §7.4.

### 4.7 Sharing and KWGT import

- **Export** a `.tessera` file (a zip with `widget.json` and `assets/`) through the Android share sheet: any messenger, email, Bluetooth.
- **QR code** for widgets without embedded photos or fonts: compressed JSON in one QR (around 2.9 KB of capacity, enough for most templates). If it doesn't fit, the app says so plainly and offers the file instead.
- **Import** by opening a `.tessera` file, scanning a QR code (ZXing + CameraX, no Google services), or pasting.
- **KWGT import:** open a `.kwgt` preset (a zip with `preset.json`, fonts and bitmaps).
  - Groups become stacks (overlap → overlay, stack → row/column). Text, shape, image, progress and font-icon items map to Tessera blocks. Komponents are flattened into groups.
  - Common formulas are translated: date formats (`$df(…)$`) → time/date chips, battery (`$bi(…)$`) → battery chips, weather (`$wi(…)$`) → weather chips, simple `$if(…)$` comparisons → rules, globals → constants or theme tokens.
  - Anything else goes on a **"Needs your attention"** list that shows the original formula. The element keeps its last value as static text. Example result: "Imported 14 of 16 elements. 2 need your attention."
  - Many KWGT presets come from paid packs. Import is for personal use, and exporting an imported widget shows a reminder about the original creator's rights.

### 4.8 Quality-of-life features

Beginners judge an app by what happens when something goes slightly wrong, so these are part of v1. **Built** means it's in the code now; the rest is planned.

**Weather**

| Feature | Why it matters | Status |
|---|---|---|
| Works with no API key or account (MET Norway) | KWGT users have to choose a weather provider and paste in keys | Built |
| City search *or* "Use my location", and location is optional | Weather without granting location access | Built |
| Location rounded to ~1 km before it's stored or sent | Privacy by default | Built |
| Units follow the phone's region (Sweden: °C and m/s; US: °F, mph, inches; UK: °C, mph), with manual override | Correct without touching settings | Built |
| Offline-safe: last forecast cached; the hour shown keeps advancing from the cache | Widgets never go blank on a train | Built |
| Freshness note only when it matters ("Updated 5 h ago", "Offline · updated 20 h ago"); tap it to refresh | Honest without clutter | Built |
| "Rain from 14:00" / "Snow until 16:00" / "Dry for the next 12 hours" | The one thing people check the weather for | Built |
| Feels-like temperature (wind chill / heat index) | Nordic winters | Built |
| Sunrise and sunset computed on the phone, including midnight sun and polar night | No extra API | Built |
| Polite refreshing: never before `Expires`, revalidates with `If-Modified-Since`, backs off after errors | Stays within MET's terms; saves battery and data | Built |
| Weather alerts | Needs per-country sources | Later |

**Calendar**

| Feature | Why it matters | Status |
|---|---|---|
| Asks for access only when you open the calendar screen, with a plain explanation | No permission wall on first run | Built |
| Choose which calendars to show (holidays, a shared family calendar…) | Less noise | Built |
| Hide declined events; merge the same meeting found in two calendars | Agendas stay readable | Built |
| All-day events stay on the right date in every time zone | A classic Android calendar-widget bug (stored at UTC midnight) | Built |
| Multi-day events: "Day 2 of 3" | Trips and conferences | Built |
| Privacy mode: "Busy" instead of titles | When others can see your phone | Built |
| Tap an event to open it; "+" to add one | Widgets as shortcuts | Built |
| "Nothing else today" plus a peek at what's next | Calm empty states | Built |
| Week numbers that follow the region (ISO weeks in Sweden) | Swedes plan in weeks ("v. 42") | Built |

**General**

| Feature | Why it matters | Status |
|---|---|---|
| Health check: "Why isn't my widget updating?" with one-tap fixes | KWGT's most common complaint, answered in plain language | Built |
| Clocks drawn by the launcher (TextClock), so they never freeze | No keep-alive service, no battery cost | Built |
| Add to home screen from the app (no hunting in the widget list) | The hardest step for beginners | Built |
| Widgets switch instantly between small/medium/large while resizing (Android 12+) | No scale settings, ever | Built |
| Day/night widget colours follow the phone automatically | Matches the system without settings | Built |
| Undo and version history in the editor | Fearless editing | Planned (M1) |
| Contrast warning when text is hard to read on your wallpaper | Beginners don't know WCAG | Planned (M4) |
| Backup and restore to one file: planner data and settings first, widgets with M5 | New phone, same planner and home screen. Replaces cloud sync (decision 17) | Planned (P1, M5) |

### 4.9 My Day: the planner

Decisions 15–18. Modelled on Brite Daily Planner's features and structure; everything is written and drawn by us.

**Screens**

- **Bottom bar:** a floating rounded pill with two tabs, *My Day* and *Widgets*, plus a round **+** button for quick add on My Day.
- **My Day** (the start screen): avatar, "My Day" and settings at the top. Below them:
  - a week strip in the phone's week order, with the selected day as a tall accent pill and dots on days that have items;
  - a summary card: "Your mood: Good", "3 tasks to complete" with a progress bar, and the **Day ring**;
  - Morning / Day / Evening segments (05–12, 12–18, 18–05), opening on the current one;
  - the user's cards, in their order.
- **Cards:** Tasks · Habits · Calendar · Weather · Pomodoro · Daily quote · Notes, later Mood, Goals, Water, Budget, Journal and more. Edit mode opens a gallery grouped as *Plan · Practices · Wellbeing · Health · Money*: customize a card's name, color and icon, then *Add to My Day*. Cards can be dragged to reorder and removed (they stay in the gallery). A card's title opens its full screen.
- **Calendar screen:** Month · Week · Day · Schedule, with a red "now" line and colored event blocks.
- **Settings:** Appearance (Lilac or Paper & Darkroom, wallpaper colors, text size) · Notifications · Backup & restore · Licenses · About.
- **First run:** "What should Tessera help you with?" (tasks, habits, calendar, focus, notes, mood, money, health). The answers pre-fill My Day with matching cards, and an optional first name personalizes the greeting ("Good morning, Sara").

**Day ring.** A donut with 2–3 colored segments and a number in the middle. Tapping it explains the number: "4 of 6 tasks, 2 of 3 habits, mood logged". It is never an unexplained score.

**Quick add.** Typing "call mom tomorrow 5pm #family" shows what was understood as removable chips (*Tomorrow · 17:00 · #family*) before saving. The parser is deterministic and offline, English first. It understands dates, times, repeats ("every weekday"), durations, `#tags`, `!` for important and `@list`. Removing a chip puts its words back into the title.

**One source of truth.** The designer's To-do and Habit blocks (§4.1) read the planner's data, so there is one list of tasks and habits, not two.

**Brite feature parity.** "All the functions of Brite" means every row below. Phases are defined in §12.

| Brite feature | Tessera | Phase |
|---|---|---|
| My Day built from cards, a gallery, customizable name/color/icon, reorder and remove | Same | P1 |
| Week strip, summary (mood, tasks to complete), "Energy" ring | Same; the ring is the explained **Day ring** | P1 |
| Morning / Day / Evening routines | Segments; tasks and habits carry a part of day | P1 |
| First-run interest quiz that pre-fills the main screen | Same | P1 |
| Personal and work tasks, priorities and tags ("Important"), due times, repeats, check-off | Lists ("Spaces": Personal, Work…), tags, repeat rules | P1 |
| Quick add in natural language | Offline parser, English first (*Quick add* above) | P1 |
| Habit tracker with 80+ presets and streaks | ≥ 80 presets written by us, schedules, streaks, stats | P1 |
| Calendar: day / week / month / schedule; Google, Apple and Outlook sync | Phone calendars via CalendarContract (Android syncs the accounts), plus local events | P1 (agenda and month), P2 (week, day, create events) |
| Notes and docs, markdown editor, templates, daily note | Markdown-lite editor, templates, pin, tags | P1 |
| Pomodoro / focus mode | 25/5 with a long break; configurable; stats | P1 |
| Daily quotes / motivation, custom quotes | ≥ 365 public-domain quotes plus the user's own | P1 |
| Weather | Existing MET Norway weather as a card | P1 |
| Home-screen widgets: interactive tasks, calendar list/week/month, notes, Pomodoro | *Planner widgets* below | P1–P2 |
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

**Excluded by our rules** (decision 17):
- the AI assistant and AI suggestions;
- accounts, sign-in and cloud sync;
- collaboration, shared spaces, shared budgets and messaging;
- photo OCR and in-app voice input (the keyboard's own voice typing still works);
- Zoom and Meet integrations, apps for other platforms, and subscriptions.

Backup and restore to a file replaces sync.

**Planner widgets**

| Widget | What it does | Phase |
|---|---|---|
| Tasks | Tick tasks on the home screen: native checkboxes on Android 12+, tap-to-toggle rows on older phones | P1 |
| Habits | Tap to check today; shows the streak | P1 |
| Pomodoro | Native `Chronometer` countdown with start, pause and stop | P1 |
| Calendar: list, week, month | The list reuses the agenda engine; week and month grids redraw at midnight and when the calendar changes | P2 |
| Water, mood, quote | One tap does the obvious thing: +250 ml, log a mood, open the quote | P2 |
| Notes, budget | Glanceable summaries that open the full screen | P2 |
| Lock screen | Keyguard widgets where the launcher supports them | P4 |

Planner widgets use the native tier (§7.1) and the same theme as the app (§9.4).

### 4.10 Offline tools

Small, focused utilities that run completely offline without accounts, analytics, ads or network permissions. Each tool is available from the in-app *Tools* screen and can be pinned to the home screen as a shortcut (`tessera://open/<tool>`) or launched from the app icon's long-press menu.

| Tool | What it does | Status | Out-of-scope choices |
|---|---|---|---|
| Calculator | Decimal arithmetic (`+ − × ÷`), operator precedence, repeated equals, percentages, sign change, backspace, and division-by-zero protection. Tactile keypad with TalkBack labels. | **Built** | Live RemoteViews calculator widget is excluded because RemoteViews cannot receive keyboard text input; pinned shortcut opens the in-app tool directly. |
| Unit converter | Length, mass, volume, temperature, speed and area conversions using exact factors and region-aware default pairs (e.g. US gets inches/cm, UK gets stone/kg, SE gets m/ft). | **Built** | Obscure or specialized engineering units outside daily consumer use. |
| Timer & Stopwatch | Countdown timer with presets (1m, 3m, 5m, 10m, 15m, 25m) and +1m bump. Stopwatch with millisecond precision and lap splits table. | **Built** | No system alarms or background notifications; runs strictly within the app to avoid background battery drain and alarm permissions. |
| Days between dates | Counts total calendar days, working business days (Mon–Fri), weekend days, and calendar breakdown (years, months, days) between any two dates. Quick offsets (+30d, +100d, month/year ends). | **Built** | Complex multi-calendar religious or astronomical holiday rules. |
| Tip & Split | Calculates tip amounts, splits bills across N people, optional round-up to nearest currency unit. Includes percentage calculators (X% of Y, ratio, % change). | **Built** | Itemized receipt OCR or camera scanning (violates no-AI rule 5). |
| Dice & Coin | Polyhedral dice roller (d4, d6, d8, d10, d12, d20, d100) with multi-dice rolls and modifiers. Coin flipper with running flip statistics. | **Built** | 3D physics simulation engine (unnecessary complexity and battery drain). |
| Tally counter | Big-tap counter with custom step sizes (+1, +5, +10, +50, +100), goal/target alert, haptics and persistent state. | **Built** | Cloud sync or multi-user counter sharing. |

---

## 5. What's not in v1

| Not in v1 | Why |
|---|---|
| AI features of any kind, including Brite's AI assistant, photo OCR and in-app voice input | Decisions 8 and 17. Never. |
| Paid features, donation page | Decision 11. Never. |
| Google Play, F-Droid | Decision 11: GitHub only. |
| Accounts, cloud sync, collaboration, community gallery | Needs servers and moderation (decision 17). Backup files and file/QR sharing cover it. |
| Zoom and Meet integrations, subscriptions | Brite has them; decisions 11 and 17 rule them out. |
| Now-playing music, step count | Not picked for v1. Candidates for v1.1 (each needs a sensitive permission). Calendar moved into v1 with decision 14. |
| Web/RSS feeds, Tasker, system stats, notification counts | Power-user features that would confuse beginners. Maybe later. |
| iOS | Decision 1. |

---

## 6. Key flows

**First run: under two minutes, no permission prompts**
1. Open Tessera. It asks "What should Tessera help you with?" and offers a few interests (tasks, habits, calendar, focus…), plus an optional first name.
2. My Day opens already filled with matching cards, the week strip and the Day ring.
3. Tap **+**, type "call mom tomorrow 5pm", and it lands under tomorrow at 17:00.

**First widget**
1. Open the **Widgets** tab. The gallery shows templates rendered live with your real time and battery.
2. Tap *Morning Paper*. The editor opens with the widget preview at the top.
3. Tap the clock. A bottom sheet shows *Font · Size · Color · When tapped*. Choosing a font updates the preview instantly.
4. Tap **Add to home screen**. The system pin dialog appears (`requestPinAppWidget`) and the widget lands on the home screen.
5. If the launcher can't pin widgets, a short illustrated guide shows: long-press home screen → Widgets → Tessera.

**Editing**
- Tap to select. A thin outline and a label (`CLOCK`) show what's selected.
- Drag to reorder inside a stack. Overlay stacks use a 9-point alignment grid instead of pixel coordinates.
- **+** opens a visual block palette. Undo and redo are always on screen.
- A size switcher (*Small 2×2 · Wide 4×2 · Large 4×4*) shows how the design adapts.
- *Try it* simulates battery, time of day and weather to test rules.

**Customizing My Day:** *Edit* → pick a card from the gallery → name, color and icon → *Add to My Day*. Drag cards to reorder them.

**New phone:** Settings → Backup & restore → *Export* on the old phone, *Import* on the new one. A summary shows what will be restored before anything is replaced.

**Sharing:** widget menu → *Share* → *As a file* or *As a QR code*.

**KWGT import:** *Import* → pick a `.kwgt` file → translation report → open in the editor.

---

## 7. How widgets reach the home screen

Answer 7 was "all of the above", so the renderer has three tiers. The **render planner** picks one for each element, in this order of preference: animated (if enabled and supported) → native → image.

### 7.1 Tiers

| Tier | Used for | Android | Notes |
|---|---|---|---|
| **Native** | Time and date text, analog clocks, countdown timers, text in bundled fonts, checkboxes (12+), progress, slideshows | 8.0+ | Built directly as RemoteViews: `TextClock`, `AnalogClock`, `Chronometer`, `ViewFlipper`, `CheckBox`. Sharp at any size, works with screen readers, no battery cost. |
| **Image layer** | Photos with masks, blur, grain, rotation, custom shapes, imported fonts | 8.0+ | Drawn by Tessera's own renderer, **the same drawing code as the editor preview**, so the result is pixel-identical. Saved as a PNG and served to the launcher through a content URI, which avoids the ~1 MB size limit for data passed to the launcher. |
| **Animated** | Morphing shapes, tap feedback, smooth resizing | 16+ | Remote Compose through Glance 1.3. **Alpha** (`androidx.compose.remote` 1.0.0-alpha20, Sept 2026), so it lives in its own module behind an *Experimental* switch. The app never depends on it. |

Example: in **Polaroid**, the photo with rounded mask and grain is an image layer, the handwritten date caption is a native `TextClock` in a bundled font, and a tap shows the next photo.

**Mixed text stays native.** Text like "It's 14:05 · 12°" is one `TextClock`: the non-time values go into the clock's pattern as quoted literals and are swapped when they change. The time keeps ticking without Tessera.

### 7.2 Sizing

- Android 12+: the app sends ready-made layouts for each size bucket, so the launcher switches instantly while you resize.
- Android 8–11: re-render when the size changes (`onAppWidgetOptionsChanged`).
- The editor previews the common sizes. The file format has no scale field.

### 7.3 Update strategy

| Content | Mechanism | Wake-ups from Tessera |
|---|---|---|
| Time, date, short countdowns | Native views (`TextClock`, `Chronometer`) | 0 |
| Slideshow | Native `ViewFlipper` | 0 |
| Battery | System broadcast, on change | only on change |
| Weather | Periodic JobScheduler job, hourly, network only; never before MET's `Expires` | ≤ 24/day |
| Calendar | Content-change trigger on the calendar provider (fires seconds after an edit) | per change |
| Event starts/ends, top of the hour, midnight | One-off "boundary" job at the next moment the widget would look different | per boundary |
| Rules with time windows ("22:00–06:00") | The same boundary job | per boundary |
| Planner widgets (tasks, habits, water…) | Redrawn right after a change in the app or a tap on the widget, and at midnight | per change |
| Focus timer (Pomodoro) | Native `Chronometer` countdown | 0 while it runs |

Widget updates run through **JobScheduler**: no permanent foreground service. Jobs re-arm after reboot and app updates. The widget's own `updatePeriodMillis` (hourly) is only a safety net. Planner reminders (P2) and the end of a focus session use `AlarmManager` instead (§10.2): exact when the user allows *Alarms & reminders*, otherwise within a short window.

### 7.4 The fallback: minute refresh

This is used only when something ticks but can't be native: a clock in an imported font, or a fully custom analog dial on Android 8–11. Tessera then redraws it every minute while the screen is on. The editor says so honestly: *"This clock uses an imported font, so Tessera redraws it every minute. Some phones pause this to save battery."* An opt-in **"Keep clocks exact"** mode runs a foreground service with a quiet notification, and the app links to battery-saver instructions for each phone brand.

### 7.5 Accessibility

Every widget gets a spoken description. Image layers carry the text they show ("Wednesday 15 October, 12°, light rain"). Tap targets are at least 48 dp. The editor respects the system font scale.

---

## 8. The `.tessera` file format

One JSON document is shared by the editor, the renderer, sharing and import. Sizes are in dp. Colors and fonts can refer to theme tokens (`@accent`). Logic is structured data and never an expression string.

```json
{
  "format": "tessera/widget",
  "version": 1,
  "meta": { "name": "Battery Ring", "template": "battery-ring", "createdWith": "Tessera 0.1.0" },
  "theme": { "useShared": true },
  "root": {
    "type": "overlay", "id": "root", "padding": 12,
    "children": [
      { "type": "progress", "id": "ring", "style": "ring",
        "value": { "chip": "battery.level" },
        "thickness": 6, "color": "@accent", "track": "@surface" },
      { "type": "text", "id": "label", "align": "center",
        "content": [ { "chip": "battery.level", "format": "percent" } ],
        "font": "@display", "size": 28, "color": "@text",
        "onTap": { "action": "default" } }
    ]
  },
  "rules": [
    { "id": "low-battery",
      "when": { "all": [
        { "source": "battery.level", "below": 20 },
        { "source": "battery.charging", "equals": false } ] },
      "then": [
        { "set": "ring.color",  "to": "#D64521" },
        { "set": "label.color", "to": "#D64521" } ] }
  ]
}
```

- `version` lets future Tessera releases migrate older files. Every change to the schema needs a migration and a test.
- A `.tessera` file is a zip containing `widget.json` and `assets/` (photos, fonts, icons). QR codes carry the JSON alone, compressed.

---

## 9. How the app looks

Tessera has two themes; the user picks one under Settings → Appearance.

- **Lilac** (default, decision 18): Brite-style. Soft lavender background, white rounded cards, one strong purple accent, colorful category icons, progress rings and gauges, a floating bottom pill and a round "+" button. Friendly, airy and tidy.
- **Paper & Darkroom** (optional, decision 12): the original look. It translates the attached *human-ui-craft* rules (written for web) into Android/Compose terms: a restrained palette, one accent, hairlines instead of shadows, and real copy.

Both follow the phone's dark mode and meet WCAG AA. The **Material You switch** (*Use wallpaper colors*, Android 12+) applies wallpaper colors to whichever theme is chosen, while that theme's shapes, typography and hairlines stay. Home-screen widgets follow the chosen theme too (§9.4).

### 9.1 Lilac (default)

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

**Type.** **Plus Jakarta Sans** (SIL OFL 1.1, from `tokotype/PlusJakartaSans`; Medium, Bold and ExtraBold, bundled with its license) replaces Instrument Sans in Lilac.

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

**Icons:** our own. For UI icons, use 24 dp line icons in the existing hand-drawn style: 1.6 stroke, round caps (see `app/src/main/res/drawable/ic_*`). For category and habit icons, use simple filled shapes in category colors. Mood faces are your own 5 vector faces, not emoji, so they look the same on every phone. Emoji are allowed only in copy for streaks (🔥).

**Motion:**
- Rings fill over about 600 ms with an ease-out.
- A completed check strikes through, then fades.
- Dragged cards lift gently.
- Respect the system "remove animations" setting.

### 9.2 Paper and Darkroom (optional)

Contrast was measured against WCAG 2.x (AA needs 4.5:1 for normal text).

| Token | Paper (day) | Darkroom (night) | Use |
|---|---|---|---|
| `canvas` | `#F3EEE4` | `#11100E` | App background |
| `raised` | `#FAF7F1` | `#23201C` | Sheets, menus |
| `ink` | `#1C1A17` · 15.0:1 | `#EDE6D8` · 15.3:1 | Primary text |
| `ink-muted` | `#615B51` · 5.8:1 | `#A39C8F` · 7.0:1 | Secondary text, input outlines |
| `hairline` | `#D9D0C0` | `#302C27` | 1 px dividers only (decorative) |
| `accent` | `#B03A12` vermilion · 5.3:1 | `#F07445` safelight · 6.6:1 | The one accent: primary action, selection |
| `on-accent` | `#FFF8F0` · 5.8:1 | `#1A0D07` · 6.6:1 | Text on accent |

The Darkroom accent is named for the red-orange safelight of a photo darkroom. Input outlines use `ink-muted`, not `hairline`, so form fields stay visible (WCAG 1.4.11).

**Type.** All three typefaces are SIL OFL and bundled; their licenses appear under Settings → Licenses.

| Role | Typeface | Details |
|---|---|---|
| Display | Fraunces | Heavy, tight, line height 1.1–1.2 |
| UI text | Instrument Sans | Line height 1.5 |
| Labels and numbers | IBM Plex Mono | Uppercase micro-labels, +0.08 em tracking, tabular figures |

**Surfaces**

- No drop shadows. Depth comes from the `raised` tone plus a 1 px hairline.
- One exception: the widget preview casts a single directional shadow (desk lamp at top-left), so it reads as an object on a table.
- Paper mode has a faint paper grain (2–3% noise). Darkroom has none.

### 9.3 Layout and details (both themes)

- **Editor:** the preview sits on a "desk" in the top ~45% of the screen, with tools in a bottom sheet.
- **Gallery:** asymmetric. One large featured template, then a denser list, not a grid of identical cards.
- Pressed states are crisp, with no bounce. A haptic tick plays when a block snaps into a stack.
- Widgets currently on the home screen show `● LIVE · updated 2 min ago` in mono, with a subtle pulse on the dot.
- Empty states use real copy and a real action: *"Nothing on your home screen yet. Start with Morning Paper →"*

### 9.4 How theming works

- `Settings.themeStyle` is `LILAC` or `PAPER`, defaulting to `LILAC`.
- `TesseraColors` (tints, category colors, ring track, pill colors) comes through a `CompositionLocal`, next to Material 3's `ColorScheme`. Composables never hardcode colors.
- Home-screen widgets follow the theme:
  - Android 12+: backgrounds via `setInt(id, "setBackgroundResource", res)` and text colors via `RemoteViews.setColorStateList(id, "setTextColor", colorRes)`, so the launcher resolves day and night itself.
  - Android 8–11: colors are resolved for the current UI mode when the widget is drawn.

---

## 10. Architecture

### 10.1 Modules

**Today** (built):

```
:core:model           places, units and regional defaults, date/time text, ages           (plain Kotlin)
:core:weather         MET Norway client + parser, symbols, feels-like, sun, outlook,
                      refresh policy, report builder, city search                          (plain Kotlin)
:core:calendar        agenda engine (filters, all-day handling, grouping), labels,
                      next-change timing                                                   (plain Kotlin)
:app                  Android: data (settings, cache, calendar, location), sync (jobs),
                      widget (providers + RemoteViews), ui (Compose screens)
```

The packages inside `:app` (`data`, `sync`, `widget`, `ui`) will become the modules below as they grow.

**Target:**

```
:app                  editor UI, navigation, settings
:core:format          .tessera document model + JSON + migrations      (plain Kotlin)
:core:logic           chip formatting + rule evaluation                (plain Kotlin)
:core:data            time, battery, weather, photos, countdowns, quotes (to-dos and habits come from the planner)
:core:theme           shared theme, wallpaper colors, font registry
:core:planner         tasks, repeat rules, habits + streaks, Day ring, Pomodoro state machine,
                      budget math, goal progress, mood statistics      (plain Kotlin)
:core:quickadd        offline quick-add parser                         (plain Kotlin)
:render:planner       chooses native / image / animated per element     (plain Kotlin)
:render:native        RemoteViews builder + generated XML "cells"
:render:image         bitmap renderer shared with the editor preview
:render:animated      Remote Compose tier (experimental, Android 16+)
:widget               AppWidgetProviders, receivers, update scheduling
:feature:share        .tessera export/import, QR
:feature:kwgt-import  .kwgt parser + translator
```

The modules marked *plain Kotlin* have no Android dependencies on purpose, so they can be compiled and unit-tested anywhere. Room (the planner's database) lives in `:app` at first, or in its own Android library module once `:app` gets crowded. Note the two planners: `:core:planner` is the day planner's logic, `:render:planner` the widget render planner (§7). They are unrelated.

### 10.2 Stack

| Concern | Choice | Why |
|---|---|---|
| Language / UI | Kotlin 2.x, Jetpack Compose, Material 3 with custom color schemes (Lilac, Paper/Darkroom) | Modern, fast to iterate |
| Native widget tier | Hand-built RemoteViews (built) | Full control over `TextClock`, `AnalogClock`, `Chronometer`, `ViewFlipper` and size-mapped layouts, some of which Glance doesn't expose |
| Animated tier | Glance 1.3 alpha + `androidx.compose.remote` alpha | The only route to animated widgets; kept isolated |
| Documents | kotlinx.serialization; one folder per widget in app storage | Same file as export |
| Planner data (tasks, habits, notes, mood, budget…) and countdowns | Room with the KSP processor (KSP 2.3.1 or newer for AGP 9's built-in Kotlin); repositories expose `Flow` | Streak and statistics queries; one source for the planner, its widgets and the designer's blocks |
| Data conventions | `kotlin.uuid.Uuid` IDs, dates as epoch days, instants as epoch milliseconds, times of day as minutes, money as `Long` minor units plus an ISO currency code | Backups merge cleanly; no rounding errors in money |
| Backup | One JSON file (`tessera/backup`, version 1) through the Storage Access Framework. Import validates, migrates and shows a summary first | Replaces cloud sync (decision 17) |
| Settings | SharedPreferences (built) | Synchronous reads inside widget callbacks; a dozen keys don't need DataStore |
| Background | JobScheduler for widget refreshes (built). `AlarmManager` for reminders and the end of a focus session: `setExactAndAllowWhileIdle` when the user allows *Alarms & reminders* (`SCHEDULE_EXACT_ALARM`, never `USE_EXACT_ALARM`), otherwise `setWindow` | No dependency and no permanent service. Content-change triggers watch the calendar directly; WorkManager would only wrap the same API. Reminders re-arm after reboot, time changes and app updates |
| Notifications | Channels "Reminders" and "Focus timer". `POST_NOTIFICATIONS` (Android 13+) is asked at the first reminder or focus session | Just-in-time (rule 9). The focus timer is an ongoing notification with a countdown chronometer, not a foreground service |
| Weather | MET Norway Locationforecast 2.0 `complete` (built): free, no key, attribution shown in-app. City search via Open-Meteo Geocoding (free for non-commercial use) | "Just works" without the KWGT provider/API-key setup |
| JSON | kotlinx.serialization JSON tree API | No compiler plugin needed; tolerant of fields MET adds later |
| HTTP | `HttpURLConnection` | Works on Android and the JVM, so the weather client is testable without a device |
| Location | Platform `LocationManager`, coarse, optional | No Google Play services |
| Photos | System photo picker (`PickVisualMedia`), falling back to the document picker; a downscaled copy is kept in app storage | No storage permission; works without Google services |
| Calendar writes | `WRITE_CALENDAR`, asked only when the user creates an event in a phone calendar. Without it, the event is saved as a local timed item | Rule 9 |
| Fonts | Plus Jakarta Sans (Lilac); Fraunces, Instrument Sans and IBM Plex Mono (Paper/Darkroom). All SIL OFL | Bundled, so they work offline and in widgets; licenses under Settings → Licenses |
| Exercise data (P3) | `yuhonas/free-exercise-db` (Unlicense), text first | Public domain and offline |
| QR | ZXing + CameraX | ML Kit would need Google Play services |
| Build | Gradle 9.6.1, AGP 9.4.1 (built-in Kotlin), Kotlin 2.4.21, Compose BOM 2026.09.00; minSdk 26, compileSdk 37, targetSdk 36; Java 17 bytecode | Versions checked against Google Maven / Maven Central on 2026-10-15 |
| Tests | JVM unit tests (core modules, including the day planner and at least 40 quick-add cases), Roborazzi screenshot tests of every template, instrumented widget tests on API 26 / 31 / 34 / 36 | |

**Implementation note.** RemoteViews can only inflate layouts that already exist in the APK, and a TextView's font can't be changed remotely. So `:render:native` assembles each widget from small generic XML "cells" (frame, row, column, image, clock…). A Gradle task generates one text cell and one clock cell per bundled font.

---

## 11. Distribution

- **GitHub Releases only (built).** Publishing a release runs `.github/workflows/release.yml`, which tests, builds the APK from the release's tag, and attaches it with `SHA256SUMS.txt`. File names follow `tessera-0.1.0.apk` so tools like Obtainium can track releases.
- **Previews until there's a release key.** The signing key lives only in Actions secrets (`TESSERA_KEYSTORE_BASE64`, `TESSERA_KEYSTORE_PASSWORD`, `TESSERA_KEY_ALIAS`, `TESSERA_KEY_PASSWORD`), added by the owner. Without it, the workflow publishes a debug-signed preview: package `com.zirren.tessera.debug`, a fresh key per build, so each preview is uninstalled before the next. Previews never collide with signed releases.
- **Beginner install guide** in the README, illustrated: download → allow your browser to install apps → open.
- **Update check:** opt-in and off by default. It asks GitHub's releases API whether a newer version exists.
- **The signing key must never be lost or changed.** Updates and developer registration both depend on it.

**Google's developer verification.** This applies to APKs from GitHub too. Enforcement on certified Android devices began 30 Sep 2026 in select regions and expands globally in 2027 ([Google](https://developer.android.com/developer-verification)). There are two paths for an open-source app ([guide](https://developer.android.com/developer-verification/guides/open-source-app-registration)):

1. **Register** `com.zirren.tessera` and the release key's SHA-256 fingerprint in the Android Developer Console under a verified identity. Users then install normally. Google Play isn't involved, and GitHub-only distribution is unchanged.
2. **Don't register.** Users must turn on Google's one-time "advanced flow" to allow apps from unverified developers.

*Recommendation: option 1, because the audience is complete beginners.* This must be decided before the first public release; it doesn't block development.

---

## 12. Milestones

Each milestone ends with an APK you can install. Decision 14 pulled weather and calendar forward, so the first build is a data-first slice: **M0 + the weather and calendar parts of M2**.

**Done in the first slice:** Gradle project and CI · plain-Kotlin core with 87 unit tests · MET Norway weather with caching and polite refresh · city search and "Use my location" · calendar agenda engine · JobScheduler sync · two native widgets ("Morning Paper" in 3 sizes, "Up Next") · app screens: Home, Weather, Calendar, Forecast, Health check · Paper/Darkroom/Material You themes · bundled fonts and hand-drawn icons.

**Still open in M0:** the `.tessera` file format and migrations.

**Order of work.** Decisions 15–18 add the planner as phases **P1–P4**. They alternate with the designer milestones so both tabs keep moving:

**P1 → W1 (M0 rest + M1) → P2 → W2 (M2–M3) → P3 → P4 → W3 (M4–M7, ending with v1.0)**

Each phase and milestone ends with green CI, an installable APK and updated docs. Each planner phase is also published as a GitHub Release (a pre-release until the signing secrets exist).

| # | Planner phase | Done when |
|---|---|---|
| P1 | **Lilac, tabs and the planner core:** the theme system (Lilac by default, Paper/Darkroom, Material You), the bottom pill with My Day and Widgets, Room, `core/planner`, backup v1, My Day with edit mode and first-run interests, the Tasks, Habits, Calendar (agenda and month), Weather, Pomodoro, Daily quote and Notes cards, quick add, and the Tasks, Habits and Pomodoro widgets | A new user picks interests and sees a finished My Day. "call mom tomorrow 5pm" lands under tomorrow at 17:00. Ticking a task on the widget updates My Day, and ticking a habit increments its streak. A focus session ends with a notification while the screen is off. A backup restores everything after the app's data is cleared |
| P2 | **Wellbeing, money, deeper calendar:** mood diary and statistics, goals, water, budget and expenses, journal and photo journal, a breathing timer, reminders, statistics, calendar week/day/schedule views with event creation, and the calendar, notes, mood, quote, water and budget widgets | Every P2 row in §4.9 works |
| P3 | **Health:** fitness programs and a workout timer, pills with reminders, a period tracker (local only, optional app lock), fasting and body measurements. Food diary scope needs the owner's approval | Every P3 row in §4.9 works |
| P4 | **Organize and polish:** projects (list, Kanban, Eisenhower matrix), archive, tag search, PDF export, shopping lists, sketches, an accent picker, a My Day background photo, app icon variants and lock-screen widgets | Every P4 row in §4.9 works |

The designer milestones:

| # | Milestone | Done when |
|---|---|---|
| M0 | **Foundations:** Gradle project, modules, CI, `.tessera` v1 + migrations, theme tokens, Paper/Darkroom/Material You app shell | The app installs and shows the (static) gallery |
| M1 | **First widget on the home screen:** text/shape/image/stack blocks, render planner (native + image tiers), pin flow, 3 templates | Pick a template → it's on the home screen |
| M2 | **Live content:** native clock and date, analog clock, battery, weather, photo + slideshow, countdown, quotes; chip picker | All 9 starter templates are live |
| M3 | **Taps and rules:** smart defaults, quick controls, to-dos and habits on the widget, rules editor + *Try it* | Battery Ring turns red at 19% |
| M4 | **Themes:** shared theme, follow wallpaper, per-widget opt-out, curated fonts | One theme change restyles every widget |
| M5 | **Sharing and KWGT import:** export/import, QR, `.kwgt` translator + report | A real KWGT preset imports with an honest report |
| M6 | **Animated tier** (Android 16+, experimental switch) | Smooth resize and tap feedback on an Android 16 phone |
| M7 | **Hardening and v1.0:** device matrix (Pixel, Samsung, Xiaomi, OnePlus, Motorola; Nova and Lawnchair launchers), TalkBack and font-scale pass across both tabs, battery measurement, install guide | v1.0 published on GitHub Releases |

---

## 13. Risks and open questions

### 13.1 Risks

| Risk | Mitigation |
|---|---|
| Remote Compose is alpha, so its APIs will change | Isolated module and experimental switch. Nothing else depends on it. |
| Bundled fonts in native widget text must render in third-party launchers | Test in M1 across the device matrix. If a launcher fails, fall back to an image layer. |
| Launchers report sizes and padding differently (Samsung, Xiaomi) | Size-mapped layouts on 12+, a test matrix for each launcher, and never a scale knob. |
| Battery savers can pause the minute-refresh fallback | Keep the fallback rare, explain it in the editor, offer the opt-in "Keep clocks exact" mode. |
| Beginners vs. GitHub-only sideloading | Register for developer verification (§11) and ship an illustrated install guide. |
| The KWGT format is undocumented | Build the importer against a set of real presets, and report what didn't translate instead of guessing. |
| Weather API terms | MET Norway: attribution shown in the forecast sheet and an identifying User-Agent. Open-Meteo is used only for city search, under its non-commercial terms (fits a no-revenue open-source app). |
| Brite's feature set is very large | Vertical slices with acceptance criteria for each phase (§12). The parity table (§4.9) is the checklist, and every phase ships an installable pre-release. |
| Looking too much like Brite (names, icons, copy) | Brite is a reference only (decision 15): our own names, icons, habit presets and copy, public-domain quotes, and third-party assets only with MIT-compatible licenses, recorded in `app/src/main/assets/licenses/`. |
| Android 14+ no longer pre-grants exact alarms (`SCHEDULE_EXACT_ALARM`) to new installs | Reminders fall back to a short window, and the health check offers the *Alarms & reminders* setting. |
| Health data is sensitive | Local only (rule 8), an optional app lock for the period tracker, and it leaves the phone only inside a backup file the user exports. |

### 13.2 Open questions (none block P1)

1. **Developer verification:** register (recommended) or rely on the advanced flow? Needed before the first public release.
2. **Languages at launch:** English only, or English + Swedish? Strings are translation-ready either way, and the quick-add parser starts in English.
3. **v1.1:** add now-playing music and steps, or keep permissions minimal? (Calendar is decided: it's in v1.)
4. **Food diary (P3):** manual entries only, or also an offline food database? A database adds APK size, so it needs the owner's approval. (Lock-screen widgets, an earlier question, are decided: Brite has them, so they're in P4.)

### 13.3 What can be verified where

This sandbox can't run Gradle (Google's Maven and Gradle servers are unreachable). The real build therefore runs on GitHub Actions on every push, and everything else was checked here:

| Check | Result |
|---|---|
| **Full Gradle build on GitHub Actions** (AGP 9.4.1, Kotlin 2.4.21, Gradle 9.6.1): core tests, `lintDebug`, `assembleDebug` | **Passes.** All code, including the Compose UI, compiles; lint reports 0 errors and 0 warnings (2 hints, both documented in `app/lint.xml`); a debug APK is built and attached to each run |
| `:core:*` unit tests (JUnit 4 sources, run with a stand-in harness) | 87/87 pass; deliberately broken code makes the right tests fail |
| `:core:*` compiled against **Android 8.0's** `android.jar` with no JDK | Passes: no Java API newer than minSdk 26 |
| Android framework code (data, sync, widgets) compiled against `android-37.jar` with an `R` class generated from the real resources | Passes, no warnings |
| Same code against Android 8.0 | Only four newer APIs show up, each behind an `SDK_INT` check |
| Resource references, RemoteViews-allowed views, string formats, style parents | Pass |
| **Compose UI** (`ui/`) | Can't be compiled in this sandbox (needs AndroidX); compiled and linted by CI instead |
| Real devices and launchers | Not yet: M7's device matrix |

Known gaps: weather condition labels ("Light rain") are English-only for now (the symbol's `labelKey` is the hook for translations). Widget fonts in third-party launchers still need on-device testing (§13.1).
