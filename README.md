# CalenPen

**CalenPen** is an Android diary-calendar app that lets you navigate your life like a
handwritten notebook — inspired by the Panly app and optimised for the **NXTPaper** e-ink tablet.

---

## Features

| Feature | Description |
|---|---|
| 📅 **Calendar view** | Monthly grid with day-level note indicators; tap any day to see or create notes |
| ✍️ **Infinite-page canvas** | Pressure-sensitive handwriting with smooth Bézier strokes; scrolls vertically as you write |
| 🖊️ **Tool palette** | Pen · Pencil · Highlighter · Eraser — each with configurable colour and stroke width |
| 📄 **Paper styles** | Blank · Lined · Dotted · Grid · Cornell — selectable per note |
| 📋 **Templates** | 10 built-in templates (Daily Planner, Weekly Overview, Habit Tracker, Mood Journal, Bullet Journal, Meeting Notes, Travel Log, Book Notes, Monthly Calendar, Blank) |
| 🗂️ **Assign notes to dates** | Every note can be linked to a calendar date (or left undated) |
| 🔍 **Full-text search** | Search across note titles, typed text, OCR-recognised handwriting, date keys, and tags |
| 🤖 **Handwriting OCR** | ML Kit Digital Ink Recognition converts your strokes to searchable text |
| 📥 **PDF import** | Open any PDF, preview all pages, and import selected or all pages as notes |
| 🖥️ **NXTPaper optimised** | High-contrast theme, GPU-accelerated canvas layer, minimal animations, e-ink-friendly colour palette |

---

## Architecture

```
app/
├── data/
│   ├── database/           Room database + type converters
│   │   ├── dao/            NoteDao · CalendarEntryDao · TemplateDao
│   │   └── entities/       Note · CalendarEntry · Template
│   └── repository/         NoteRepository (single source of truth)
├── ui/
│   ├── calendar/           CalendarFragment + CalendarViewModel + CalendarAdapter
│   ├── editor/             NoteEditorActivity · NoteEditorViewModel · DrawingCanvas
│   │                       PdfImportActivity · PdfPageAdapter
│   ├── notes/              NotesListFragment · NotesViewModel · NotesAdapter
│   ├── search/             SearchFragment · SearchViewModel
│   └── templates/          TemplatesFragment · TemplatesViewModel · TemplatesAdapter
└── utils/
    ├── DateUtils.kt         Date formatting helpers
    ├── HandwritingRecognizer.kt  ML Kit Digital Ink OCR wrapper
    └── PdfImporter.kt       Android PdfRenderer-based PDF → Bitmap converter
```

**Stack:** Kotlin · MVVM · Room · Coroutines/Flow · Navigation Component · ML Kit · ViewBinding

---

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Run JVM unit tests
./gradlew test

# Run instrumented tests (device/emulator required)
./gradlew connectedAndroidTest
```

**Requirements:** Android SDK 26+ (API 26) · Target SDK 34 · Kotlin 1.9

---

## NXTPaper Setup

1. Enable **Unknown sources** on your NXTPaper device.
2. Side-load the APK via ADB: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Open **CalenPen** from the launcher.
4. For handwriting OCR, connect to Wi-Fi so the ML Kit model (~20 MB) can download on first use.

---

## License

MIT – see [LICENSE](LICENSE) for details.
