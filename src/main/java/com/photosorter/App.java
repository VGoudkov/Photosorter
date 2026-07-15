package com.photosorter;

import com.photosorter.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Photo Sorter — Desktop application entry point.
 * Organises photos into a yyyy/MM/dd folder hierarchy based on EXIF dates.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();

        Scene scene = new Scene(mainView, 900, 600);

        // Load CSS stylesheet
        String css = getClass().getResource("/css/app.css") != null
                ? getClass().getResource("/css/app.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        primaryStage.setTitle("Photo Sorter");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(450);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
