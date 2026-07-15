# Photo Sorter — Agent Guide

This document provides essential context for AI agents working on the Photo Sorter project.

## Project Overview

**Photo Sorter** is a JavaFX desktop application that organizes photos into a date-based folder structure (`yyyy/MM/dd`) based on EXIF metadata. It scans a source directory, extracts shooting dates from photos, and moves them to a destination folder organized by date.

**Key Features:**
- Recursive directory scanning with photo extension filtering
- EXIF date extraction with file modification time fallback
- Duplicate detection (filename + size + date)
- Conflict resolution for name collisions
- Real-time progress logging with color-coded severity
- Cross-platform support (Windows, macOS, Linux)

## Architecture

### Package Structure

```
com.photosorter
├── App.java                    # JavaFX application entry point
├── Launcher.java               # Main class launcher (non-Application class for JavaFX)
├── ui/                         # User interface layer
│   ├── MainView.java          # Main application window and layout
│   ├── LogPanel.java          # Scrollable log display with color-coded messages
│   └── ProgressModel.java     # Progress tracking model
├── scanner/                    # File discovery
│   ├── FileCollector.java     # Recursive directory walker with extension filtering
│   └── PhotoFile.java         # Photo metadata record (path, size, date)
├── exif/                       # Metadata extraction
│   ├── ExifDateReader.java    # EXIF DateTimeOriginal extraction
│   └── DateResolver.java      # Date resolution with fallback logic
├── mover/                      # File organization
│   ├── SortEngine.java        # Main orchestration engine (scan → resolve → move)
│   ├── DuplicateChecker.java  # Duplicate detection logic
│   ├── ConflictResolver.java  # Name collision handling
│   ├── SortListener.java      # Event listener interface
│   └── Summary.java           # Processing statistics record
└── util/
    └── AppLogger.java         # Logging configuration
```

### Core Workflow

1. **FileCollection**: `FileCollector` recursively walks source directory, filtering by photo extensions
2. **DateResolution**: `DateResolver` extracts EXIF date via `ExifDateReader`, falls back to file modification time
3. **DuplicateCheck**: `DuplicateChecker` compares (filename, size, date) tuples
4. **ConflictResolution**: `ConflictResolver` generates unique names for collisions
5. **Sorting**: `SortEngine` orchestrates the pipeline, runs on background thread, posts events to UI

### Threading Model

- **Background Thread**: `SortEngine` runs on a separate thread to avoid blocking UI
- **UI Thread**: JavaFX Application Thread receives events via `Platform.runLater()`
- **Atomic Counters**: `AtomicInteger` for thread-safe progress tracking
- **Cancellation**: `AtomicBoolean` flag for graceful stop

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| GUI Framework | JavaFX | 21.0.2 |
| Build Tool | Maven | 3.x |
| EXIF Library | metadata-extractor | 2.19.0 |
| Logging | SLF4J + slf4j-simple | 2.0.13 |
| Testing | JUnit 5 + AssertJ | 5.10.2 / 3.25.3 |
| Mutation Testing | PIT | 1.15.3 |

## Build & Test Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR (creates uber-jar with all dependencies)
mvn clean package

# Run mutation tests
mvn pitest:mutationCoverage

# Run application (via JavaFX plugin)
mvn javafx:run

# Run application (from packaged JAR)
java -jar target/photosorter-1.0.0.jar
```

## Important Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration, dependencies, plugins |
| `SPEC.md` | Detailed product specification and requirements |
| `AGENTS.md` | This file — agent context guide |
| `start-serena.sh` | Script to start Serena MCP server |
| `src/main/java/com/photosorter/Launcher.java` | Application entry point (required for JavaFX classpath mode) |
| `src/main/java/com/photosorter/mover/SortEngine.java` | Core sorting orchestration logic |
| `src/main/java/com/photosorter/ui/MainView.java` | Main UI layout and controls |

## Known Issues & Current State

### JavaFX Configuration

**Issue**: JavaFX runtime components missing error when running from classpath.

**Solution**: Application uses `Launcher.java` as entry point instead of `App.java`. This is required because JavaFX 11+ forbids the class that calls `Application.launch()` from extending `Application` when running on the classpath (non-modular mode).

**Configuration**: Both `javafx-maven-plugin` and `maven-shade-plugin` reference `com.photosorter.Launcher` as the main class.

### Windows Native Libraries

**Issue**: JavaFX requires platform-specific native libraries (DLLs on Windows).

**Solution**: `pom.xml` includes Windows-specific JavaFX dependencies with `<classifier>win</classifier>`:
- `javafx-base:win`
- `javafx-graphics:win`
- `javafx-controls:win`
- `javafx-fxml:win`

These are bundled into the uber-jar by maven-shade-plugin.

### Mutation Testing Status

**Current Metrics** (as of last run):
- Line Coverage: 36% (157/442 lines)
- Mutation Score: 17% (43/254 killed)
- Test Strength: 62%

**Thresholds**: Set to 15% mutation score, 30% coverage (realistic baseline).

**Reports**: Generated in `target/pit-reports/index.html`

**Areas Needing Improvement**:
- `VoidMethodCallMutator`: Only 6% killed — tests don't verify side effects
- `ConditionalsBoundaryMutator`: 0% killed — boundary conditions untested
- Many mutations have no coverage at all

## Development Workflow

### Adding Features

1. Follow existing package structure (ui/scanner/exif/mover/util)
2. Use dependency injection where possible (see `SortEngine` constructor)
3. Keep UI operations on JavaFX Application Thread
4. Use SLF4J logging with appropriate levels (INFO/WARN/ERROR)
5. Add unit tests for new logic (JUnit 5 + AssertJ)

### Testing Guidelines

- Unit tests in `src/test/java/` mirror main source structure
- Use `TestImageHelper` for creating test images with EXIF data
- Test edge cases: empty directories, unreadable files, duplicates, conflicts
- Verify both happy paths and error conditions

### Code Style

- Java 21 features encouraged (records, pattern matching, etc.)
- Immutable data structures preferred
- Use `Logger` from SLF4J, not `System.out`
- Keep methods focused and under ~30 lines where possible

## Common Tasks

### Running the Application

```bash
# Development mode (via Maven)
mvn javafx:run

# Production mode (from JAR)
mvn clean package
java -jar target/photosorter-1.0.0.jar
```

### Debugging

- Enable verbose logging: Modify `src/main/resources/simplelogger.properties`
- Check mutation test reports: `target/pit-reports/index.html`
- Review test coverage: `target/site/jacoco/index.html` (if JaCoCo enabled)

### Adding New Photo Extensions

Edit `FileCollector.java` and update the `SUPPORTED_EXTENSIONS` set.

### Modifying Date Format

Edit `SortEngine.java` and update the `DATE_FORMATTER` pattern (currently `yyyy/MM/dd`).

## Serena MCP Integration

The project uses Serena for code analysis. Configuration in `.serena/project.yml`:
- Language: `java`
- Backend: LSP
- Modes: `interactive`, `editing`

**Available Tools** (22 total):
- Symbol navigation: `find_symbol`, `find_declaration`, `find_implementations`, `find_referencing_symbols`
- Code analysis: `get_symbols_overview`, `get_diagnostics_for_file`, `search_for_pattern`
- Refactoring: `rename_symbol`, `replace_symbol_body`, `insert_before_symbol`, `insert_after_symbol`
- Memory: `read_memory`, `write_memory`, `list_memories`, `edit_memory`

**Start Serena**: `./start-serena.sh`

## References

- **SPEC.md**: Full product specification and requirements
- **JavaFX Documentation**: https://openjfx.io/
- **metadata-extractor**: https://github.com/drewnoakes/metadata-extractor
- **PIT Mutation Testing**: https://pitest.org/
