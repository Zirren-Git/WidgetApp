# Tessera — product spec

> A home-screen widget maker for Android that a complete beginner can use in two minutes.
> Open source · any Android 8+ phone · no AI · no accounts · no ads.

**Status:** discovery done (2026-10-15) · **Next:** Milestone 0
The 13 answers in §1 are the owner's decisions. Everything else is the engineering recommendation that follows from them, and can be changed.

**Contents**
1. [Decisions so far](#1-decisions-so-far)
2. [Who it's for](#2-who-its-for)
3. [Rules we build by](#3-rules-we-build-by)
4. [What's in v1](#4-whats-in-v1)
5. [What's not in v1](#5-whats-not-in-v1)
6. [Key flows](#6-key-flows)
7. [How widgets reach the home screen](#7-how-widgets-reach-the-home-screen)
8. [The `.tessera` file format](#8-the-tessera-file-format)
9. [How the editor looks](#9-how-the-editor-looks)
10. [Architecture](#10-architecture)
11. [Distribution](#11-distribution)
12. [Milestones](#12-milestones)
13. [Risks and open questions](#13-risks-and-open-questions)

---

## 1. Decisions so far

Three rounds of questions, 15 Oct 2026.

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
| 12 | Editor look | **"Paper by day, darkroom by night and an option to switch to Material You"** | Two hand-tuned palettes that follow system dark mode, plus a "Use wallpaper colors" switch (Android 12+). |
| 13 | Name | **Tessera** | App label "Tessera", package `com.zirren.tessera`. Effectively permanent once people install it, because updates need the same package name and signing key. |

---

## 2. Who it's for

Complete beginners: someone who saw a beautiful home screen online, installed KWGT, met `$df(hh:mm)$` and a layer tree, and gave up.

**They want:** a widget that looks good in under two minutes, matches their wallpaper, and shows their photos, the weather and how many days until something.

**They never want to see:** formulas, layer trees, `scale: 137`, anchors and padding math, weather API keys, or a blank canvas as the first screen.

---

## 3. Rules we build by

1. **Never show a formula.** If a beginner can't read it aloud, it doesn't belong in the UI.
2. **It fits, every time.** Layout uses stacks and dp units. There is no scale field, and the editor previews every common widget size before you place it. (Sizing is KWGT's most common complaint.)
3. **Clocks are never wrong.** Anything that ticks is drawn by the launcher itself, so it doesn't depend on our app staying alive.
4. **Any Android phone.** Android 8.0+, any brand, no Google Play services. If a library needs them, we don't use it.
5. **Made by people, not generated.** No AI features or AI APIs. Templates are designed by people.
6. **Nothing phones home.** No accounts, analytics, ads or crash-reporting SDKs. The network is used only for weather and an opt-in update check. *(This is an inference from answers 8 and 11. Confirm if you want it stricter or looser.)*
7. **Beautiful untouched.** Every template and default looks finished before the user changes anything.

---

## 4. What's in v1

### 4.1 Content blocks

| Block | Shows | Permission | How it stays current |
|---|---|---|---|
| Clock (digital) | Time in any curated font | none | Native `TextClock`: the launcher ticks it, with no battery cost from Tessera |
| Clock (analog) | Dial + hands | none | Android 12+: native `AnalogClock` with custom dial and hands. Android 8–11: native with bundled dial styles; fully custom dials use the minute-refresh fallback (§7.4) |
| Date | Weekday, day, month, year | none | Native `TextClock` with a date pattern |
| Battery | Level, charging state, ring or bar | none | System battery broadcasts, on change |
| Weather | Now, high/low, condition, icon, place | Location optional; city search works without it | Every 60 min via WorkManager. MET Norway data, no API key |
| Photo | One photo or a slideshow | none (system photo picker) | The slideshow flips natively (`ViewFlipper`), so the app never wakes |
| Countdown | "62 days until Midsummer" | none | Daily at midnight. Under 24 h, a native `Chronometer` counts down |
| Quote | Quote of the day + author: a bundled public-domain set plus your own | none | Daily |
| To-do | Short checklist you can tick on the home screen | none | On tap |
| Habit | Streak + last-4-weeks grid; tap marks today done | none | On tap and at midnight |

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
| Quote | text · author |
| To-do | items left · next item |
| Habit | streak · done today |

### 4.3 Visual rules

A rule is **When** (one or more conditions, combined with *all of* or *any of*, one level deep) and **Then** (one or more effects).

- **When:** battery below/above X% · charging / not charging · time between A and B · weekday / weekend · weather is rain / snow / clear / cloudy · temperature below/above X · countdown day arrives · all to-dos done · habit not done by HH:MM
- **Then:** change color · change text · show/hide a block · swap photo or icon · switch to the theme's light/dark variant
- **"Try it" simulator** in the editor: slide "Pretend battery is 15%" or "Pretend it's 22:00" and watch the rule fire. KWGT has nothing like this, and it's how beginners learn what a rule does.

### 4.4 Tap actions

**Smart defaults**

| Block | Tap does |
|---|---|
| Clock | Opens the system alarms |
| Date | Opens the calendar app at today |
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

---

## 5. What's not in v1

| Not in v1 | Why |
|---|---|
| AI features of any kind | Decision 8. Never. |
| Paid features, donation page | Decision 11. Never. |
| Google Play, F-Droid | Decision 11: GitHub only. |
| Accounts, cloud sync, community gallery | Needs servers and moderation; file/QR sharing covers sharing. |
| Calendar, now-playing music, step count | Not picked for v1. Candidates for v1.1 (each needs a sensitive permission). |
| Web/RSS feeds, Tasker, system stats, notification counts | Power-user features that would confuse beginners. Maybe later. |
| Lock-screen widgets | v1.1 candidate (recent Pixel phones support them). |
| iOS | Decision 1. |

---

## 6. Key flows

**First run: under two minutes, no permission prompts**
1. Open Tessera. The gallery shows templates rendered live with your real time and battery.
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
| Time, date, short countdowns | Native views | 0 |
| Slideshow | Native `ViewFlipper` | 0 |
| Battery | System broadcast, on change | only on change |
| Weather | WorkManager, every 60 min | 24/day |
| Countdown days, quote, habit day rollover | Scheduled after midnight | 1/day |
| Rules with time windows ("22:00–06:00") | Inexact alarms with a bounded window (no exact-alarm permission) | per boundary |

The default path has **no permanent foreground service**.

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

## 9. How the editor looks

This translates the attached *human-ui-craft* rules (written for web) into Android/Compose terms: a restrained palette, one accent, hairlines instead of shadows, and real copy.

### 9.1 Palette

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

**Material You switch:** Settings → Appearance → *Use wallpaper colors* (Android 12+) swaps in the dynamic color scheme. Typography, hairlines and layout stay Tessera's.

### 9.2 Type

All three typefaces are SIL OFL and bundled; their licenses appear under Settings → Licenses.

| Role | Typeface | Details |
|---|---|---|
| Display | Fraunces | Heavy, tight, line height 1.1–1.2 |
| UI text | Instrument Sans | Line height 1.5 |
| Labels and numbers | IBM Plex Mono | Uppercase micro-labels, +0.08 em tracking, tabular figures |

### 9.3 Surfaces and layout

- No drop shadows. Depth comes from the `raised` tone plus a 1 px hairline.
- One exception: the widget preview casts a single directional shadow (desk lamp at top-left), so it reads as an object on a table.
- Paper mode has a faint paper grain (2–3% noise). Darkroom has none.
- **Editor:** the preview sits on a "desk" in the top ~45% of the screen, with tools in a bottom sheet.
- **Gallery:** asymmetric. One large featured template, then a denser list, not a grid of identical cards.

### 9.4 Details

- Pressed states are crisp, with no bounce. A haptic tick plays when a block snaps into a stack.
- Widgets currently on the home screen show `● LIVE · updated 2 min ago` in mono, with a subtle pulse on the dot.
- Empty states use real copy and a real action: *"Nothing on your home screen yet. Start with Morning Paper →"*

---

## 10. Architecture

### 10.1 Modules

```
:app                  editor UI, navigation, settings
:core:format          .tessera document model + JSON + migrations      (plain Kotlin)
:core:logic           chip formatting + rule evaluation                (plain Kotlin)
:core:data            time, battery, weather, photos, countdowns, quotes, to-dos, habits
:core:theme           shared theme, wallpaper colors, font registry
:render:planner       chooses native / image / animated per element     (plain Kotlin)
:render:native        RemoteViews builder + generated XML "cells"
:render:image         bitmap renderer shared with the editor preview
:render:animated      Remote Compose tier (experimental, Android 16+)
:widget               AppWidgetProviders, receivers, update scheduling
:feature:share        .tessera export/import, QR
:feature:kwgt-import  .kwgt parser + translator
```

The three modules marked *plain Kotlin* have no Android dependencies on purpose, so they can be compiled and unit-tested anywhere.

### 10.2 Stack

| Concern | Choice | Why |
|---|---|---|
| Language / UI | Kotlin 2.x, Jetpack Compose, Material 3 with custom color schemes | Modern, fast to iterate |
| Native widget tier | Hand-built RemoteViews | Full control over `TextClock`, `AnalogClock`, `Chronometer`, `ViewFlipper` and size-mapped layouts, some of which Glance doesn't expose |
| Animated tier | Glance 1.3 alpha + `androidx.compose.remote` alpha | The only route to animated widgets; kept isolated |
| Documents | kotlinx.serialization; one folder per widget in app storage | Same file as export |
| To-dos, habits, countdowns | Room | Streak queries |
| Settings | DataStore | Standard |
| Background | WorkManager, AlarmManager (bounded inexact), broadcast receivers | No exact-alarm permission, no permanent service |
| Weather | MET Norway Locationforecast 2.0 (free, no key, attribution shown in-app); city search via Open-Meteo Geocoding (free for non-commercial use) | "Just works" without the KWGT provider/API-key setup |
| Location | Platform `LocationManager`, coarse, optional | No Google Play services |
| Photos | System photo picker, falling back to the document picker | No storage permission; works without Google services |
| QR | ZXing + CameraX | ML Kit would need Google Play services |
| Build | Gradle Kotlin DSL + version catalog; minSdk 26; compile/target the latest stable SDK; Java 17 toolchain | |
| Tests | JVM unit tests (core, planner), Roborazzi screenshot tests of every template, instrumented widget tests on API 26 / 31 / 34 / 36 | |

**Implementation note.** RemoteViews can only inflate layouts that already exist in the APK, and a TextView's font can't be changed remotely. So `:render:native` assembles each widget from small generic XML "cells" (frame, row, column, image, clock…). A Gradle task generates one text cell and one clock cell per bundled font.

---

## 11. Distribution

- **GitHub Releases only.** A tagged commit triggers GitHub Actions to build, test and sign the APK (keystore in Actions secrets), then attach it to the release with a SHA-256 checksum. File names follow `tessera-v0.1.0.apk` so tools like Obtainium can track releases.
- **Beginner install guide** in the README, illustrated: download → allow your browser to install apps → open.
- **Update check:** opt-in and off by default. It asks GitHub's releases API whether a newer version exists.
- **The signing key must never be lost or changed.** Updates and developer registration both depend on it.

**Google's developer verification.** This applies to APKs from GitHub too. Enforcement on certified Android devices began 30 Sep 2026 in select regions and expands globally in 2027 ([Google](https://developer.android.com/developer-verification)). There are two paths for an open-source app ([guide](https://developer.android.com/developer-verification/guides/open-source-app-registration)):

1. **Register** `com.zirren.tessera` and the release key's SHA-256 fingerprint in the Android Developer Console under a verified identity. Users then install normally. Google Play isn't involved, and GitHub-only distribution is unchanged.
2. **Don't register.** Users must turn on Google's one-time "advanced flow" to allow apps from unverified developers.

*Recommendation: option 1, because the audience is complete beginners.* This must be decided before the first public release; it doesn't block development.

---

## 12. Milestones

Each milestone ends with an APK you can install.

| # | Milestone | Done when |
|---|---|---|
| M0 | **Foundations:** Gradle project, modules, CI, `.tessera` v1 + migrations, theme tokens, Paper/Darkroom/Material You app shell | The app installs and shows the (static) gallery |
| M1 | **First widget on the home screen:** text/shape/image/stack blocks, render planner (native + image tiers), pin flow, 3 templates | Pick a template → it's on the home screen |
| M2 | **Live content:** native clock and date, analog clock, battery, weather, photo + slideshow, countdown, quotes; chip picker | All 9 starter templates are live |
| M3 | **Taps and rules:** smart defaults, quick controls, to-dos and habits on the widget, rules editor + *Try it* | Battery Ring turns red at 19% |
| M4 | **Themes:** shared theme, follow wallpaper, per-widget opt-out, curated fonts | One theme change restyles every widget |
| M5 | **Sharing and KWGT import:** export/import, QR, `.kwgt` translator + report | A real KWGT preset imports with an honest report |
| M6 | **Animated tier** (Android 16+, experimental switch) | Smooth resize and tap feedback on an Android 16 phone |
| M7 | **Hardening and v1.0:** device matrix (Pixel, Samsung, Xiaomi, OnePlus, Motorola; Nova and Lawnchair launchers), TalkBack and font-scale pass, battery measurement, install guide | v1.0 published on GitHub Releases |

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

### 13.2 Open questions (none block M0)

1. **Developer verification:** register (recommended) or rely on the advanced flow? Needed before the first public release.
2. **Languages at launch:** English only, or English + Swedish? Strings are translation-ready either way.
3. **v1.1:** add calendar, now-playing and steps, or keep permissions minimal?
4. **Lock-screen widgets** in v1.1?

### 13.3 What can be verified where

This sandbox has no Android SDK, and Google's Maven and Gradle servers are unreachable from it. So Android code can't be compiled here, and every milestone has to be built in Android Studio. The plain-Kotlin modules (`:core:format`, `:core:logic`, `:render:planner`) are designed to be compile-checked and unit-tested outside Android; setting that up here is a first task for M0.
