package com.todo.views;

import com.todo.controller.TaskManager;
import com.todo.models.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Import Jackson datatypes for time
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TaskManagerUI extends Application {
    private TaskManager taskManager;
    private Label totalTasksLabel;
    private Label completedTasksLabel;
    private Label delayedTasksLabel;
    private Label upcomingTasksLabel;

    private TableView<Task> tasksTable;
    private ListView<TaskCategory> categoriesList;
    private ListView<TaskPriority> prioritiesList;
    private TableView<TaskReminder> remindersTable;
    private TableView<Task> searchResultsTable;

    private final String dataDir = "medialab";

    // Register the JavaTimeModule so it can handle LocalDate properly
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize backend
        taskManager = new TaskManager();
        startReminderChecker();

        // Build the root layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top summary pane
        HBox summaryPane = createSummaryPane();
        root.setTop(summaryPane);

        // Center TabPane with multiple tabs for Tasks, Categories...
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createTasksTab(),
                createCategoriesTab(),
                createPrioritiesTab(),
                createRemindersTab(),
                createSearchTab());
        root.setCenter(tabPane);

        // If there are any delayed tasks, show a popup alert
        int delayedCount = (int) taskManager.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.DELAYED)
                .count();
        if (delayedCount > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Overdue Tasks");
            alert.setHeaderText("Delayed Tasks Detected");
            alert.setContentText("There are " + delayedCount + " delayed tasks.");
            alert.showAndWait();
        }

        // Set scene and show stage
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("MediaLab Assistant");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateSummary();
        refreshAllViews();

        // On close, persist data to JSON files
        primaryStage.setOnCloseRequest(e -> {
            persistData();
            Platform.exit();
        });
    }

    private HBox createSummaryPane() {
        totalTasksLabel = new Label("Total Tasks: 0");
        completedTasksLabel = new Label("Completed: 0");
        delayedTasksLabel = new Label("Delayed: 0");
        upcomingTasksLabel = new Label("Due in 7 Days: 0");

        HBox hbox = new HBox(20, totalTasksLabel, completedTasksLabel, delayedTasksLabel, upcomingTasksLabel);
        hbox.setPadding(new Insets(10));
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }

    private Tab createTasksTab() {
        Tab tab = new Tab("Tasks");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        tasksTable = new TableView<>();
        tasksTable.setPlaceholder(new Label("No tasks available"));
        TableColumn<Task, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Task, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        TableColumn<Task, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority()));
        TableColumn<Task, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDeadline() != null ? data.getValue().getDeadline().toString() : ""));
        TableColumn<Task, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        // [CHANGED]
        TableColumn<Task, String> remindersSetCol = new TableColumn<>("Reminders");
        remindersSetCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(taskManager.getRemindersForTask(data.getValue()).size())));
        
        // Custom cell factory for reminders column [CHANGED]
        remindersSetCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item);
                }
            }
        });

        tasksTable.getColumns().addAll(titleCol, categoryCol, priorityCol, deadlineCol, statusCol, remindersSetCol);

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> showAddTaskDialog());
        Button editButton = new Button("Edit Task");
        editButton.setOnAction(e -> showEditTaskDialog());
        Button deleteButton = new Button("Delete Task");
        deleteButton.setOnAction(e -> deleteSelectedTask());

        HBox buttons = new HBox(10, addButton, editButton, deleteButton);
        buttons.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(tasksTable, buttons);
        tab.setContent(vbox);
        return tab;
    }

    private Tab createCategoriesTab() {
        Tab tab = new Tab("Categories");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        categoriesList = new ListView<>();

        Button addButton = new Button("Add Category");
        addButton.setOnAction(e -> showAddCategoryDialog());
        Button editButton = new Button("Edit Category");
        editButton.setOnAction(e -> showEditCategoryDialog());
        Button deleteButton = new Button("Delete Category");
        deleteButton.setOnAction(e -> deleteSelectedCategory());

        HBox buttons = new HBox(10, addButton, editButton, deleteButton);
        buttons.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(categoriesList, buttons);
        tab.setContent(vbox);
        return tab;
    }

    private Tab createPrioritiesTab() {
        Tab tab = new Tab("Priorities");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        prioritiesList = new ListView<>();

        Button addButton = new Button("Add Priority");
        addButton.setOnAction(e -> showAddPriorityDialog());
        Button editButton = new Button("Edit Priority");
        editButton.setOnAction(e -> showEditPriorityDialog());
        Button deleteButton = new Button("Delete Priority");
        deleteButton.setOnAction(e -> deleteSelectedPriority());

        HBox buttons = new HBox(10, addButton, editButton, deleteButton);
        buttons.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(prioritiesList, buttons);
        tab.setContent(vbox);
        return tab;
    }

    private Tab createRemindersTab() {
        Tab tab = new Tab("Reminders");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        remindersTable = new TableView<>();
        remindersTable.setPlaceholder(new Label("No reminders available"));

        TableColumn<TaskReminder, String> taskTitleCol = new TableColumn<>("Task Title");
        taskTitleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTask().getTitle()));

        TableColumn<TaskReminder, String> reminderDateCol = new TableColumn<>("Reminder Date");
        reminderDateCol.setCellValueFactory(data -> {
            LocalDate date = data.getValue().computeReminderDate();
            return new SimpleStringProperty(date != null ? date.toString() : "N/A");
        });

        remindersTable.getColumns().addAll(taskTitleCol, reminderDateCol);

        Button addButton = new Button("Add Reminder");
        addButton.setOnAction(e -> showAddReminderDialog());
        Button editButton = new Button("Edit Reminder");
        editButton.setOnAction(e -> showEditReminderDialog());
        Button deleteButton = new Button("Delete Reminder");
        deleteButton.setOnAction(e -> deleteSelectedReminder());

        HBox buttons = new HBox(10, addButton, editButton, deleteButton);
        buttons.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(remindersTable, buttons);
        tab.setContent(vbox);
        return tab;
    }

    private Tab createSearchTab() {
        Tab tab = new Tab("Search");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setPromptText("Category");
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.setPromptText("Priority");
        Button searchButton = new Button("Search");

        HBox searchFields = new HBox(10, new Label("Title:"), titleField,
                new Label("Category:"), categoryBox,
                new Label("Priority:"), priorityBox, searchButton);
        searchFields.setAlignment(Pos.CENTER);

        searchResultsTable = new TableView<>();
        TableColumn<Task, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        TableColumn<Task, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        TableColumn<Task, String> priCol = new TableColumn<>("Priority");
        priCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority()));
        TableColumn<Task, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDeadline() != null ? data.getValue().getDeadline().toString() : ""));
        searchResultsTable.getColumns().addAll(titleCol, catCol, priCol, deadlineCol);

        searchButton.setOnAction(e -> {
            String titleQuery = titleField.getText();
            String categoryQuery = categoryBox.getValue();
            String priorityQuery = priorityBox.getValue();
            searchResultsTable.setItems(FXCollections.observableArrayList(
                    taskManager.searchTasks(titleQuery, categoryQuery, priorityQuery)));
        });

        // Populate ComboBoxes from current categories and priorities
        categoryBox.setItems(FXCollections.observableArrayList(
                taskManager.getCategories().stream().map(TaskCategory::getName).toList()));
        priorityBox.setItems(FXCollections.observableArrayList(
                taskManager.getPriorities().stream().map(TaskPriority::getName).toList()));

        vbox.getChildren().addAll(searchFields, searchResultsTable);
        tab.setContent(vbox);
        return tab;
    }

    private void updateSummary() {
        int total = taskManager.getTasks().size();
        int completed = (int) taskManager.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();
        int delayed = (int) taskManager.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.DELAYED).count();
        int upcoming = (int) taskManager.getTasks().stream().filter(t -> t.getDeadline() != null &&
                !t.getDeadline().isBefore(LocalDate.now()) && t.getDeadline().isBefore(LocalDate.now().plusDays(7)))
                .count();

        totalTasksLabel.setText("Total Tasks: " + total);
        completedTasksLabel.setText("Completed: " + completed);
        delayedTasksLabel.setText("Delayed: " + delayed);
        upcomingTasksLabel.setText("Due in 7 Days: " + upcoming);
    }

    private void refreshAllViews() {
        tasksTable.setItems(FXCollections.observableArrayList(taskManager.getTasks()));
        tasksTable.refresh();
        categoriesList.setItems(FXCollections.observableArrayList(taskManager.getCategories()));
        prioritiesList.setItems(FXCollections.observableArrayList(taskManager.getPriorities()));
        remindersTable.setItems(FXCollections.observableArrayList(taskManager.getReminders()));
        updateSummary();
    }

    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add Task");

        TextField titleField = new TextField();
        TextField descriptionField = new TextField();
        // [CHANGED] Pick Category, after checking that there exists at least one category
        ComboBox<String> categoryBox = new ComboBox<>();
        List<TaskCategory> category_list = taskManager.getCategories();
        if (category_list.size() == 0) {
            showError("Error Adding Task!", "Please add a category first.");
            return;
        }
        categoryBox.setItems(FXCollections.observableArrayList(
                category_list.stream().map(TaskCategory::getName).toList()));
        
        // [CHANGED] Pick Priority, after checking that there exists at least one priority
        ComboBox<String> priorityBox = new ComboBox<>();
        List<TaskPriority> priority_list = taskManager.getPriorities();
        if (priority_list.size() == 0) {
            showError("Error Adding Task!", "Please add a priority first.");
            return;
        }
        priorityBox.setItems(FXCollections.observableArrayList(
                priority_list.stream().map(TaskPriority::getName).toList()));
        
        DatePicker deadlinePicker = new DatePicker();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryBox, 1, 2);
        grid.add(new Label("Priority:"), 0, 3);
        grid.add(priorityBox, 1, 3);
        grid.add(new Label("Deadline:"), 0, 4);
        grid.add(deadlinePicker, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new Task(titleField.getText(), descriptionField.getText(),
                        categoryBox.getValue(), priorityBox.getValue(), deadlinePicker.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
            try {
                taskManager.addTask(task);
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Adding Task!", ex.getMessage());
            }
        });
    }

    private void showEditTaskDialog() {
        Task selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a task to edit.");
            return;
        }

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");

        TextField titleField = new TextField(selected.getTitle());
        TextField descriptionField = new TextField(selected.getDescription());
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setItems(FXCollections.observableArrayList(taskManager.getCategories().stream().map(TaskCategory::getName).toList()));
        categoryBox.setValue(selected.getCategory());
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.setItems(FXCollections.observableArrayList(
                taskManager.getPriorities().stream().map(TaskPriority::getName).toList()));
        priorityBox.setValue(selected.getPriority());
        DatePicker deadlinePicker = new DatePicker(selected.getDeadline());
        // [CHANGED] Pick Status
        ComboBox<TaskStatus> statusBox = new ComboBox<>();
        statusBox.setItems(FXCollections.observableArrayList(TaskStatus.IN_PROGRESS, TaskStatus.POSTPONED, TaskStatus.COMPLETED));
        statusBox.setValue(selected.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryBox, 1, 2);
        grid.add(new Label("Priority:"), 0, 3);
        grid.add(priorityBox, 1, 3);
        grid.add(new Label("Deadline:"), 0, 4);
        grid.add(deadlinePicker, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return new Task(titleField.getText(), descriptionField.getText(),
                        categoryBox.getValue(), priorityBox.getValue(), deadlinePicker.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            try {
                taskManager.updateTask(selected, updatedTask.getTitle(), updatedTask.getDescription(), updatedTask.getCategory(), updatedTask.getPriority(), updatedTask.getDeadline());
                taskManager.updateTaskStatus(selected, statusBox.getValue());
            
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Updating Task", ex.getMessage());
            }
        });
    }

    private void deleteSelectedTask() {
        Task selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a task to delete.");
            return;
        }
        try {
            taskManager.removeTask(selected);
            refreshAllViews();
        } catch (IllegalArgumentException ex) {
            showError("Error Deleting Task", ex.getMessage());
        }
    }

    private void showAddCategoryDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Category");
        dialog.setHeaderText("Enter new category name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                taskManager.addCategory(name);
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Adding Category", ex.getMessage());
            }
        });
    }

    private void showEditCategoryDialog() {
        TaskCategory selected = categoriesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a category to edit.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Edit Category");
        dialog.setHeaderText("Enter new name for the category:");
        dialog.showAndWait().ifPresent(newName -> {
            try {
                taskManager.renameCategory(selected, newName);
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Renaming Category", ex.getMessage());
            }
        });
    }

    private void deleteSelectedCategory() {
        TaskCategory selected = categoriesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a category to delete.");
            return;
        }
        try {
            taskManager.removeCategory(selected);
            refreshAllViews();
        } catch (IllegalArgumentException ex) {
            showError("Error Deleting Category", ex.getMessage());
        }
    }

    private void showAddPriorityDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Priority");
        dialog.setHeaderText("Enter new priority name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                taskManager.addPriority(name);
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Adding Priority", ex.getMessage());
            }
        });
    }

    private void showEditPriorityDialog() {
        TaskPriority selected = prioritiesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a priority to edit.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Edit Priority");
        dialog.setHeaderText("Enter new name for the priority:");
        dialog.showAndWait().ifPresent(newName -> {
            try {
                taskManager.renamePriority(selected, newName);
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Renaming Priority", ex.getMessage());
            }
        });
    }

    private void deleteSelectedPriority() {
        TaskPriority selected = prioritiesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a priority to delete.");
            return;
        }
        try {
            taskManager.removePriority(selected);
            refreshAllViews();
        } catch (IllegalArgumentException ex) {
            showError("Error Deleting Priority", ex.getMessage());
        }
    }

    private void showAddReminderDialog() {
        Dialog<TaskReminder> dialog = new Dialog<>();
        dialog.setTitle("Add Reminder");

        ComboBox<String> taskDropdown = new ComboBox<>();
        taskDropdown.setItems(FXCollections.observableArrayList(
                taskManager.getTasks().stream().map(Task::getTitle).toList()));

        ComboBox<TaskReminder.ReminderType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(TaskReminder.ReminderType.values());
        typeBox.setValue(TaskReminder.ReminderType.ONE_DAY_BEFORE); // Default

        DatePicker customDatePicker = new DatePicker();
        customDatePicker.setDisable(true);

        typeBox.setOnAction(e -> {
            customDatePicker.setDisable(typeBox.getValue() != TaskReminder.ReminderType.CUSTOM_DATE);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Task Title:"), 0, 0);
        grid.add(taskDropdown, 1, 0);
        grid.add(new Label("Reminder Type:"), 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Custom Date (if applicable):"), 0, 2);
        grid.add(customDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Task task = taskManager.getTasks().stream()
                        .filter(t -> t.getTitle().equals(taskDropdown.getValue()))
                        .findFirst().orElse(null);
                if (task != null) {
                    return new TaskReminder(task, typeBox.getValue(), customDatePicker.getValue());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reminder -> {
            try {
                taskManager.addReminder(reminder.getTask().getTitle(), reminder.getType(),
                        reminder.getCustomReminderDate());
                refreshAllViews(); // Ensure UI updates
                remindersTable.refresh(); // Force refresh of the table
            } catch (Exception ex) {
                showError("Error Adding Reminder", ex.getMessage());
            }
        });

    }

    private void showEditReminderDialog() {
        TaskReminder selected = remindersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a reminder to edit.");
            return;
        }

        Dialog<TaskReminder> dialog = new Dialog<>();
        dialog.setTitle("Edit Reminder");

        TextField taskTitleField = new TextField(selected.getTask().getTitle());
        taskTitleField.setDisable(true); // Prevent changing the task

        ComboBox<TaskReminder.ReminderType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(TaskReminder.ReminderType.values());
        typeBox.setValue(selected.getType());

        DatePicker customDatePicker = new DatePicker(selected.getCustomReminderDate());
        customDatePicker.setDisable(selected.getType() != TaskReminder.ReminderType.CUSTOM_DATE);

        typeBox.setOnAction(e -> {
            customDatePicker.setDisable(typeBox.getValue() != TaskReminder.ReminderType.CUSTOM_DATE);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Task Title:"), 0, 0);
        grid.add(taskTitleField, 1, 0);
        grid.add(new Label("Reminder Type:"), 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Custom Date (if applicable):"), 0, 2);
        grid.add(customDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return new TaskReminder(selected.getTask(), typeBox.getValue(), customDatePicker.getValue());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedReminder -> {
            try {
                taskManager.updateReminder(selected, updatedReminder.getType(),
                        updatedReminder.getCustomReminderDate());
                refreshAllViews(); // Ensure UI updates
                remindersTable.refresh(); // Force refresh of the table
            } catch (Exception ex) {
                showError("Error Updating Reminder", ex.getMessage());
            }
        });

    }

    private void deleteSelectedReminder() {
        TaskReminder selected = remindersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select a reminder to delete.");
            return;
        }
        try {
            taskManager.getReminders().removeIf(r -> r.getTask().getTitle().equals(selected.getTask().getTitle()));
            refreshAllViews();
        } catch (Exception ex) {
            showError("Error Deleting Reminder", ex.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // [CHANGED] Snooze reminder functionality
    /**
     * Reminder snooze functionality
     */
    private void snoozeReminder(TaskReminder reminder) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> Platform.runLater(() -> showReminderAlert(reminder)), 5, TimeUnit.MINUTES);
    }

    /**
     * Method to show reminder alert
     * @param reminder The reminder to snooze
     */
    private void showReminderAlert(TaskReminder reminder) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reminder Alert");
        alert.setHeaderText("Reminder for Task: " + reminder.getTask().getTitle());
        alert.setContentText("Due today: " + reminder.computeReminderDate());

        ButtonType snoozeButton = new ButtonType("Snooze (5 min)");
        ButtonType dismissButton = new ButtonType("Dismiss", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(snoozeButton, dismissButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == snoozeButton) {
                snoozeReminder(reminder);
            } else if (response == dismissButton) {
                taskManager.getReminders().remove(reminder); // Remove reminder permanently
                refreshAllViews();
            }
        });
    }

    /**
     * Reminder checkers
     */

    private void checkReminders() {
        LocalDate today = LocalDate.now();

        List<TaskReminder> dueReminders = taskManager.getReminders().stream()
                .filter(r -> r.computeReminderDate().equals(today)).toList();

        for (TaskReminder reminder : dueReminders) {
            showReminderAlert(reminder);
        }
    }

    private void startReminderChecker() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> checkReminders());
        }, 0, 1, TimeUnit.MINUTES); // Runs every minute
    }

    /**
     * Persists the current state of the TaskManager to JSON files in the "medialab"
     * folder.
     */
    private void persistData() {
        try {
            Files.createDirectories(Paths.get(dataDir));
            // We'll be using our 'mapper' that has the JavaTimeModule registered
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/tasks.json"),
                    taskManager.getTasks());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/categories.json"),
                    taskManager.getCategories());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/priorities.json"),
                    taskManager.getPriorities());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/reminders.json"),
                    taskManager.getReminders());
        } catch (IOException e) {
            showError("Persistence Error", "Failed to save data: " + e.getMessage());
        }
    }
}

