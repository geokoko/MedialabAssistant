/*
 * This source file was generated by the Gradle 'init' task
 */
package com.todo;

import com.todo.views.TaskManagerUI;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        new TaskManagerUI().start(primaryStage);
    }

    // The missing method for AppTest
    public String getGreeting() {
        return "Hello from MediaLab Assistant!";
    }
}
