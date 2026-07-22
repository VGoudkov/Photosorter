# Photo Sorter — Agent Guide

JavaFX 21 desktop app that scans photos from a source directory, extracts EXIF dates, and moves them into `yyyy/MM/dd` folders at a destination.

## Build & Test

```bash
mvn clean compile              # compile
mvn test                       # run all tests
mvn pitest:mutationCoverage    # mutation tests (report: target/pit-reports/index.html)
mvn javafx:run                 # run via JavaFX plugin
mvn clean package              # uber-jar → target/photosorter-1.0.0.jar
java -jar target/photosorter-1.0.0.jar  # run packaged
```

No codegen, no formatter, no linter config. Just Maven.

## Entry Point Quirk

`com.photosorter.Launcher` is the main class (not `App.java`). JavaFX 11+ forbids `Application.launch()` from within a class that extends `Application` on the classpath. Launcher delegates to `App.main()`. Both `javafx-maven-plugin` and `maven-shade-plugin` reference `Launcher`.

## Architecture

```
com.photosorter
├── Launcher.java       # entry point (thin wrapper)
├── App.java            # JavaFX Application subclass
├── ui/                 # MainView, LogPanel, ProgressModel
├── scanner/            # FileCollector (walk + extension filter), PhotoFile (record)
├── exif/               # ExifDateReader (metadata-extractor), FileNameDateParser, DateResolver (chains them)
├── mover/              # SortEngine (orchestrator), DuplicateChecker, ConflictResolver, SortListener, Summary
└── util/               # AppLogger (stub, console-only currently)
```

Key flow: `SortEngine.execute()` runs on a **virtual thread** (`Thread.ofVirtual()`), calls `SortListener` callbacks dispatched to JavaFX thread via `Platform.runLater()`.

## Testing

- 8 test classes, JUnit 5 + AssertJ, no mocking framework
- TempDir for filesystem isolation in all tests
- `TestImageHelper.createMinimalJpeg()` / `createJpegWithExif()` for test fixtures
- Tests run headless — no JavaFX toolkit needed (SortEngine.dispatch() falls back to direct execution when `Platform.isFxApplicationThread()` is false)
- Run a single test: `mvn test -Dtest=DuplicateCheckerTest`

## Key Implementation Details

| Aspect | Behavior |
|--------|----------|
| Photo extensions | jpg, jpeg, png, tiff, tif, webp, heic, heif, raw, cr2, nef, arw, dng — in `FileCollector.PHOTO_EXTENSIONS` |
| Date resolution | EXIF DateTimeOriginal → file name pattern → file lastModified → epoch (1970-01-01) |
| File name patterns | Config at `src/main/resources/date-patterns.conf`, tried in order, first match wins |
| Duplicate check | (filename, size, date truncated to seconds) all must match |
| Conflict naming | `photo (1).jpg`, `photo (2).jpg` — before last extension dot |
| Move strategy | `ATOMIC_MOVE` first, fallback to copy+delete for cross-device |
| Logging | SLF4J + slf4j-simple, config at `src/main/resources/simplelogger.properties` |
| CSS | `src/main/resources/css/app.css` |
| Window | 900×600 default, 700×450 minimum |
| Virtual threads | `SortEngine` runs via `Thread.ofVirtual()` in `MainView` |

## Serena MCP Integration

Serena is configured at `.serena/project.yml` (language: Java, backend: LSP). Start with `./start-serena.sh`.

**Use Serena tools over raw operations for Java code.** Prefer `serena_find_symbol`, `serena_find_declaration`, `serena_find_implementations`, `serena_find_referencing_symbols`, `serena_replace_symbol_body`, `serena_replace_content`, `serena_rename_symbol`, and `serena_insert_before_symbol` / `serena_insert_after_symbol` over raw `grep`/`glob`/`edit`. The LSP backend understands Java semantics (methods, classes, fields, signatures) and gives correct results across the whole project. Raw tools are acceptable for non-Java files (configs, resources, scripts).

## Mutation Testing

Thresholds in pom.xml: 15% mutation score, 30% coverage. Run with `mvn pitest:mutationCoverage`. Reports at `target/pit-reports/index.html`.
