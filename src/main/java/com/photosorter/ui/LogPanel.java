package com.photosorter.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A colour-coded, auto-scrolling log panel.
 * Displays log entries with severity-based text colours:
 * - INFO: neutral (default)
 * - WARN: amber
 * - ERROR: red
 *
 * Auto-scrolls to the bottom when new entries arrive, unless the user has
 * manually scrolled up (which pauses auto-scroll until they scroll back down).
 */
public class LogPanel extends VBox {

    public enum Level { INFO, WARN, ERROR }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<LogEntry> entries = FXCollections.observableArrayList();
    private final ListView<LogEntry> listView;
    private final ScrollBar scrollBar;
    private boolean autoScroll = true;

    public record LogEntry(String timestamp, String message, Level level) {}

    public LogPanel() {
        setSpacing(0);
        setPadding(new Insets(4));

        listView = new ListView<>(entries);
        listView.setCellFactory(lv -> new LogCell());
        listView.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");

        // Find the scroll bar for auto-scroll detection
        ScrollPane scrollPane = findScrollPane(listView);
        scrollBar = findScrollBar(scrollPane);

        if (scrollBar != null) {
            scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                // If user scrolled up, pause auto-scroll
                if (newVal.doubleValue() < 0.99 && oldVal.doubleValue() >= 0.99) {
                    autoScroll = false;
                }
                // If user scrolled back to bottom, resume auto-scroll
                if (newVal.doubleValue() >= 0.99) {
                    autoScroll = true;
                }
            });
        }

        VBox.setVgrow(listView, Priority.ALWAYS);
        getChildren().add(listView);
    }

    /**
     * Appends a log entry. Thread-safe — dispatches to JavaFX Application Thread.
     */
    public void log(String message, Level level) {
        String timestamp = LocalTime.now().format(TIME_FMT);
        LogEntry entry = new LogEntry(timestamp, message, level);

        if (Platform.isFxApplicationThread()) {
            appendEntry(entry);
        } else {
            Platform.runLater(() -> appendEntry(entry));
        }
    }

    public void info(String message) { log(message, Level.INFO); }
    public void warn(String message) { log(message, Level.WARN); }
    public void error(String message) { log(message, Level.ERROR); }

    /**
     * Clears all log entries.
     */
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            entries.clear();
        } else {
            Platform.runLater(entries::clear);
        }
    }

    private void appendEntry(LogEntry entry) {
        entries.add(entry);
        if (autoScroll) {
            listView.scrollTo(entries.size() - 1);
        }
    }

    private ScrollPane findScrollPane(ListView<?> listView) {
        for (var node : listView.lookupAll(".scroll-pane")) {
            if (node instanceof ScrollPane sp) {
                return sp;
            }
        }
        // Fallback: look after layout
        listView.layout();
        for (var node : listView.lookupAll(".scroll-pane")) {
            if (node instanceof ScrollPane sp) {
                return sp;
            }
        }
        return null;
    }

    private ScrollBar findScrollBar(ScrollPane scrollPane) {
        if (scrollPane == null) return null;
        for (var node : scrollPane.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                return sb;
            }
        }
        return null;
    }

    /**
     * Custom cell that renders a log entry with colour-coded text.
     */
    private static class LogCell extends javafx.scene.control.ListCell<LogEntry> {
        @Override
        protected void updateItem(LogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Text timeText = new Text("[" + item.timestamp() + "] ");
            timeText.setStyle("-fx-fill: #666666;");

            Text msgText = new Text(item.message());
            switch (item.level()) {
                case WARN -> msgText.setStyle("-fx-fill: #d4a017;");
                case ERROR -> msgText.setStyle("-fx-fill: #cc0000; -fx-font-weight: bold;");
                default -> msgText.setStyle("-fx-fill: -fx-text-background-color;");
            }

            TextFlow flow = new TextFlow(timeText, msgText);
            setGraphic(flow);
        }
    }
}
