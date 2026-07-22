package com.photosorter.ui;

import com.photosorter.mover.SortEngine;
import com.photosorter.mover.SortListener;
import com.photosorter.mover.Summary;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.photosorter.util.Config;

/**
 * Main application view containing source/destination pickers, log panel, and progress controls.
 */
public class MainView extends BorderPane {

    private final TextField sourceField = new TextField();
    private final TextField destField = new TextField();
    private final Button startButton = new Button("Start");
    private final Button stopButton = new Button("Stop");
    private final LogPanel logPanel = new LogPanel();
    private final ProgressModel progressModel = new ProgressModel();
    private final Label scannedLabel = new Label("Scanned: 0");
    private final Label movedLabel = new Label("Moved: 0");
    private final Label skippedLabel = new Label("Skipped: 0");
    private final Label errorsLabel = new Label("Errors: 0");
    private final Label statusLabel = new Label("Ready");

    private SortEngine sortEngine;
    private Thread sortThread;

    public MainView() {
        setPadding(new Insets(10));

        buildTopPanel();
        setCenter(logPanel);
        buildBottomPanel();

        loadConfig();
        bindListeners();
        updateStartButtonState();
    }

    private void buildTopPanel() {
        sourceField.setPromptText("Select source folder...");
        sourceField.setEditable(false);
        Button sourceBrowse = new Button("Browse…");
        sourceBrowse.setOnAction(e -> browseSource());

        HBox sourceBox = new HBox(5, new Label("Source:"), sourceField, sourceBrowse);
        HBox.setHgrow(sourceField, Priority.ALWAYS);
        sourceBox.setAlignment(Pos.CENTER_LEFT);

        destField.setPromptText("Select destination folder...");
        destField.setEditable(false);
        Button destBrowse = new Button("Browse…");
        destBrowse.setOnAction(e -> browseDestination());

        HBox destBox = new HBox(5, new Label("Destination:"), destField, destBrowse);
        HBox.setHgrow(destField, Priority.ALWAYS);
        destBox.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(8, sourceBox, destBox);
        setTop(topBox);
    }

    private void buildBottomPanel() {
        // Progress counters
        HBox counterBox = new HBox(15, scannedLabel, movedLabel, skippedLabel, errorsLabel);
        counterBox.setAlignment(Pos.CENTER_LEFT);

        // Status label
        statusLabel.setStyle("-fx-font-weight: bold;");

        // Buttons
        stopButton.setDisable(true);
        startButton.setOnAction(e -> startSort());
        stopButton.setOnAction(e -> stopSort());

        HBox buttonBox = new HBox(10, startButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomBox = new HBox(15, counterBox, statusLabel, buttonBox);
        HBox.setHgrow(counterBox, Priority.ALWAYS);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setPadding(new Insets(8, 0, 0, 0));

        setBottom(bottomBox);
    }

    private void bindListeners() {
        progressModel.scannedProperty().addListener((obs, old, val) -> {
            scannedLabel.setText("Scanned: " + val.intValue());
            updateStartButtonState();
        });
        progressModel.movedProperty().addListener((obs, old, val) ->
                movedLabel.setText("Moved: " + val.intValue()));
        progressModel.skippedProperty().addListener((obs, old, val) ->
                skippedLabel.setText("Skipped: " + val.intValue()));
        progressModel.errorsProperty().addListener((obs, old, val) ->
                errorsLabel.setText("Errors: " + val.intValue()));
    }

    private void loadConfig() {
        String src = Config.get("source");
        if (src != null && !src.isEmpty()) {
            sourceField.setText(src);
        }
        String dst = Config.get("destination");
        if (dst != null && !dst.isEmpty()) {
            destField.setText(dst);
        }
    }

    private void browseSource() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Source Folder");
        File dir = chooser.showDialog(getScene().getWindow());
        if (dir != null) {
            sourceField.setText(dir.getAbsolutePath());
            Config.set("source", dir.getAbsolutePath());
            updateStartButtonState();
        }
    }

    private void browseDestination() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Destination Folder");
        File dir = chooser.showDialog(getScene().getWindow());
        if (dir != null) {
            destField.setText(dir.getAbsolutePath());
            Config.set("destination", dir.getAbsolutePath());
            updateStartButtonState();
        }
    }

    private void updateStartButtonState() {
        boolean sourceValid = !sourceField.getText().isEmpty() && Files.isDirectory(Path.of(sourceField.getText()));
        boolean destValid = !destField.getText().isEmpty() && Files.isDirectory(Path.of(destField.getText()));
        boolean running = sortThread != null && sortThread.isAlive();

        boolean canStart = sourceValid && destValid && !running;
        startButton.setDisable(!canStart);

        // Validation warning: destination inside source
        if (sourceValid && destValid) {
            Path sourcePath = Path.of(sourceField.getText());
            Path destPath = Path.of(destField.getText());
            if (destPath.startsWith(sourcePath)) {
                statusLabel.setText("⚠ Destination cannot be inside source");
                statusLabel.setStyle("-fx-text-fill: #d4a017; -fx-font-weight: bold;");
                startButton.setDisable(true);
                return;
            }
        }

        if (!running) {
            statusLabel.setText("Ready");
            statusLabel.setStyle("-fx-font-weight: bold;");
        }
    }

    private void startSort() {
        Path source = Path.of(sourceField.getText());
        Path dest = Path.of(destField.getText());

        // Reset UI
        progressModel.reset();
        logPanel.clear();
        logPanel.info("Starting sort: " + source + " → " + dest);

        startButton.setDisable(true);
        stopButton.setDisable(false);
        statusLabel.setText("Running...");
        statusLabel.setStyle("-fx-text-fill: #0066cc; -fx-font-weight: bold;");

        sortEngine = new SortEngine();
        SortListener listener = createListener();

        sortThread = Thread.ofVirtual().name("sort-engine").start(() -> {
            sortEngine.execute(source, dest, listener);
        });
    }

    private void stopSort() {
        if (sortEngine != null) {
            logPanel.warn("Cancellation requested...");
            sortEngine.cancel();
            stopButton.setDisable(true);
            statusLabel.setText("Stopping...");
        }
    }

    private SortListener createListener() {
        return new SortListener() {
            @Override
            public void onScanned(Path file) {
            }

            @Override
            public void onMoved(Path from, Path to) {
                logPanel.info("Moved: " + from.getFileName() + " → " + to);
            }

            @Override
            public void onSkipped(Path file, String reason) {
                logPanel.warn("Skipped: " + file.getFileName() + " (" + reason + ")");
            }

            @Override
            public void onError(Path file, Exception ex) {
                logPanel.error("Error: " + file.getFileName() + " — " + ex.getMessage());
            }

            @Override
            public void onProgress(int scanned, int moved, int skipped, int errors) {
                progressModel.update(scanned, moved, skipped, errors);
            }

            @Override
            public void onComplete(Summary summary) {
                Platform.runLater(() -> {
                    logPanel.info("─────────────────────────────────");
                    logPanel.info("Summary:");
                    logPanel.info("  Scanned: " + summary.scanned());
                    logPanel.info("  Moved:   " + summary.moved());
                    logPanel.info("  Skipped: " + summary.skipped());
                    logPanel.info("  Errors:  " + summary.errors());

                    if (sortEngine != null && sortEngine.isCancelled()) {
                        statusLabel.setText("Cancelled");
                        statusLabel.setStyle("-fx-text-fill: #d4a017; -fx-font-weight: bold;");
                    } else {
                        statusLabel.setText("Complete");
                        statusLabel.setStyle("-fx-text-fill: #006600; -fx-font-weight: bold;");
                    }

                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                });
            }
        };
    }
}
