# Photo Sorter — Specification

A desktop Java application that scans a source directory tree for photos, reads their shooting dates (EXIF first, file metadata fallback), and moves them into a clean `yyyy / MM / dd` folder hierarchy at a user-chosen destination. Duplicates are detected and left in place.

---

## 1. Overview

| Attribute | Detail |
|-----------|--------|
| **Name** | Photo Sorter |
| **Purpose** | Organise a disorganised haystack of photos into a predictable date-based folder layout |
| **Platform** | Cross-platform (Windows, macOS, Linux) |
| **Language** | Java 25 |
| **Distribution** | Bundled platform-specific JRE shipped alongside the application |

---

## 2. Functional Requirements

### 2.1 Source Selection
- User selects a **source folder** via native file chooser.
- The application recursively traverses the selected folder and all sub-folders.
- Recognised photo extensions: `.jpg`, `.jpeg`, `.png`, `.tiff`, `.tif`, `.webp`, `.heic`, `.heif`, `.raw`, `.cr2`, `.nef`, `.arw`, `.dng`.

### 2.2 Destination Selection
- User selects a **destination folder** via native file chooser.
- Destination must not be a sub-folder of the source (validation warning).
- Application may optionally preview the number of photos found and estimated outcome.

### 2.3 EXIF & Metadata Reading
- For each recognised file the application extracts the "original date/time taken":
  1. **Primary** — EXIF `DateTimeOriginal` tag (or format-equivalent for HEIC/raw) or `DateTaken`
  2. **Secondary** - file name pattern. The program should have a config file with date patterns which can be presented in file name
  3. **Fallback** — file system "last modified" timestamp when EXIF data is absent or unreadable.
- Files that are unreadable or corrupt are logged and skipped.

### 2.4 Target Folder Structure
- Destination organisation pattern: `destination/yyyy/MM/dd/`
- Example: `destination/2020/07/15/`
- Missing intermediate directories are created automatically.

### 2.5 Move Logic
- Photos are **moved** from source to the calculated destination path.
- The original file name is preserved by default.
- A name collision at the destination triggers the duplicate check (§ 2.6).
- If no collision, the file is renamed only if the new name is already taken (suffix appended).

### 2.6 Duplicate Detection
A photo is considered a **duplicate** when **all three** fields match an existing file at the destination:
- file name (base name + extension)
- file size (bytes)
- shooting date (resolved date used for the move, with second-level precision)

Duplicates:
- **Remain in place** (source file is skipped).
- Event is recorded in the processing log.

### 2.7 Processing & Reporting
- A live **log panel** embedded in the main window displays processing events in real time:
  - file scanned, moved, skipped, or errored
  - auto-scroll with a pinned-to-bottom default; scroll-up temporarily pauses auto-scroll
  - text selection and copy-to-clipboard support
  - severity levels colour-coded: info = neutral, warning = amber, error = red
- A progress indicator (count of scanned / moved / skipped) is shown alongside the log panel.
- Upon completion a summary is appended to the log panel:
  - total scanned
  - successfully moved
  - skipped (duplicates)
  - errors / unreadable files
- An optional detailed log file (`photossorter-YYYY-MM-DD.log`) is written to the destination.

### 2.8 Date patterns (if no EXIF or other metadata)
- sometimes there is no EXIF data, but file pattern looks like `20160818_124425.jpg` or `2015-08-22 21.59.57.jpg`
- the app must have a config file, where such patterns are presented in following lines
  - YYYYMMDD_hhmmss.*
  - YYYY-MM-DD hh.mm.ss
  - may be MM DD YYYY hh mm
- the app should try to parse file name as one of dates, starting from 1-st line. If one format is suitable (the valid date can be constructed - use this and don't try others
- if no pattern can be used - then fallback to file attributes

---

## 3. Non-Functional Requirements

| Requirement | Detail |
|-------------|--------|
| **Java version** | Java 25 (LTS when available) |
| **GUI framework** | JavaFX (modern layout engine, CSS styling, reactive controls, native OS file choosers, rich text / colour-coded log view) |
| **Threading** | EXIF reading and file moves run on a background thread pool (e.g. virtual threads); GUI thread never blocks |
| **Memory** | Streaming / on-demand metadata extraction; no full in-memory index of all files beyond the scan list |
| **Disk I/O** | Uses Java NIO.2 for move operations; moves within the same filesystem are atomic where supported |
| **JRE bundling** | Each release ships a platform-specific JRE build (via `jlink`) so the end user does not need Java installed |
| **Localisation** | Dates formatted locale-agnostic (ISO pattern for folders). UI strings should be externalised for future i18n. |

---

## 4. User Flow

```
┌─────────────────────┐
│  Launch application │
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ Select source folder│──┐
└─────────┬───────────┘  │
          ▼              │
┌─────────────────────┐  │
│Select destination   │  │
│     folder          │  │  (both can be selected in either order)
└─────────┬───────────┘  │
          ▼              │
┌─────────────────────┐  │
│ Validate selection  │──┘ (source ≠ destination, no nesting)
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│  Start sorting      │──▶ background scan begins
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ Processing view     │  (progress bar, live log panel, stop button)
└─────────┬───────────┘
          ▼
Summary appended to the log panel with final counts. (moved / skipped / errors)
```

---

## 5. Technical Architecture

### 5.1 Module / Package Outline (logical, not code)

```
photo-sorter/
├── main
│   ├── App                 (bootstraps JavaFX Application)
│   └── cli                 (optional CLI entry point for scripting)
├── ui
│   ├── MainStage           (application window, layout)
│   ├── SourcePanel         (source folder picker)
│   ├── DestinationPanel    (destination folder picker)
│   ├── ProgressBarPane     (progress bar + live counters)
│   ├── LogsPane            (scrollable log output with colour-coded levels)
│   └── SummaryBar          (final counts shown inline)
├── scanner
│   ├── FileCollector       (recursive directory walk, extension filter)
│   └── PhotoFile           (light-weight record: path, size, resolved date)
├── exif
│   ├── ExifReader          (abstract extraction; pluggable back-end)
│   └── FallbackDateSource  (file attribute reader)
├── mover
│   ├── SortEngine          (orchestrates scan → resolve → move loop)
│   ├── DuplicateChecker    (hash / metadata comparison)
│   └── ConflictResolver    (name-collision handling)
└── util
    └── LogConfig            (structured logging setup)
```

### 5.2 Key Design Decisions

- **JavaFX** for GUI: modern controls, CSS styling, and rich components (e.g. colour-coded log view); available as separate modules from Java 11 onwards.
- **Virtual threads** (Java 21+): preferred over executor pools for IO-bound EXIF reading tasks — simpler code with excellent throughput.
- **Atomic where possible**: `Files.move` with `ATOMIC_MOVE` on same filesystem; fall back to standard copy+delete on cross-device moves.
- **No database**: duplicate detection relies on in-memory metadata during a single run; persisted state is unnecessary.
- **jpackage for installers**: produces platform-appropriate packages (.dmg, .msi, .deb) that include a trimmed JRE for zero-dependency installation.

---

## 6. Dependencies (planned)

| Category | Library | Note |
|----------|---------|------|
| EXIF | `com.drewnoakes:metadata-extractor` | Widely used, actively maintained |
| Logging | `org.slf4j:slf4j-simple` | Zero-config file/console logging |
| Testing | JUnit 5, AssertJ | Standard test stack |
| Build | Maven or Gradle | With jpackage for native installer creation |
| GUI | `org.openjfx:javafx-*` | Modern JDK module set |

---

## 7. Build & Distribution

### 7.1 Bundled JRE Distribution
Each release includes a platform-specific JRE so the end user never needs to install Java separately.

| OS | Build Host | Packaging | JRE Source |
|----|-----------|-----------|------------|
| Linux x86_64 | Linux | .deb / .rpm / tarball | `jlink` |
| macOS aarch64 | macOS | .dmg / .pkg | `jlink` |
| Windows x86_64 | Windows | .msi / .exe installer | `jlink` |

### 7.2 JRE Trimming & Module Path
- `jlink` assembles a minimal JRE containing only the modules the application requires.
- JavaFX modules are bundled as part of the JRE image.
- `jpackage` wraps the image into a native installer per platform.
- GraalVM and native-image tools are **not** used.

### 7.3 CI
- GitHub Actions matrix builds and packages per OS on push to `main`.

---

## 8. Error Handling

| Scenario | Behaviour |
|----------|-----------|
| Source folder empty or no photos found | Info message in log panel; allow re-selection |
| Destination not writable | Error in log panel; abort processing |
| Unreadable / corrupt file | Logged; counted in "errors"; skip and continue |
| Insufficient disk space | Error in log panel; abort remaining moves |
| Interrupted by user (stop button) | Graceful cancellation; partial progress kept; summary shown |

---

## 9. Edge Cases

- **File with identical name but different content**: not a duplicate — the new file gets a unique suffix (e.g. `photo (1).jpg`).
- **Photos taken at midnight (00:00:00)**: grouped under the correct calendar day per EXIF.
- **Timezone ambiguity**: EXIF does not store timezone; application treats dates as wall-clock values without offset conversion.
- **Symlinks in source tree**: followed and de-duplicated by resolved canonical path.
- **Read-only source files**: file permissions preserved after move where supported.

---

## 10. Future Considerations (out of scope)

- Photo preview / thumbnail grid before sorting.
- Undo / restore moved files.
- Configuration profiles (custom date patterns, extension lists).
- CLI-only headless mode for scripting / cron.
- Multi-language UI localisation.
