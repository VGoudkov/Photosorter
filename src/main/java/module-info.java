module com.photosorter {
    requires javafx.controls;
    requires javafx.fxml;
    requires metadata.extractor;
    requires org.slf4j;
    requires java.desktop;

    exports com.photosorter;
    exports com.photosorter.ui;
    exports com.photosorter.scanner;
    exports com.photosorter.exif;
    exports com.photosorter.mover;
    exports com.photosorter.util;
}
