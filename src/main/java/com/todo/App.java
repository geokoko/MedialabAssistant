package com.todo;

import com.todo.views.TaskManagerUI;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public String getGreeting() {
        return "Hello, Task Manager!";
    }

    @Override
    public void start(Stage primaryStage) {
        new TaskManagerUI().start(primaryStage); // Delegate to TaskManagerUI
    }
}

