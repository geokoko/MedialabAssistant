package com.todo.views;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TaskManagerUI is the JavaFX frontend for the Task Management System.
 * It provides a GUI for managing tasks, categories, priorities, reminders,
 * and for performing task searches. On startup, it notifies the user about
 * delayed tasks, and on exit, it persists data to JSON files in the "medialab" folder.
 */
public class TaskManagerUI extends Application {
    private TaskManager taskManager;
    private Label totalTasksLabel;
    private Label completedTasksLabel;
    private Label delayedTasksLabel;
    private Label upcomingTasksLabel;
    
    private TableView<Task> tasksTable;
    private ListView<Category> categoriesList;
    private ListView<Priority> prioritiesList;
    private TableView<Reminder> remindersTable;
    private TableView<Task> searchResultsTable;
    
    private final String dataDir = "medialab";
    private final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        launch(args);
    }
    
    /**
     * Starts the JavaFX application by initializing the TaskManager and building the GUI.
     * @param primaryStage the main stage.
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialize backend
        taskManager = new TaskManager();
        
        // Build the root layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top summary pane
        HBox summaryPane = createSummaryPane();
        root.setTop(summaryPane);
        
        // Center TabPane with multiple tabs for Tasks, Categories, Priorities, Reminders, and Search
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createTasksTab(),
            createCategoriesTab(),
            createPrioritiesTab(),
            createRemindersTab(),
            createSearchTab()
        );
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
    
    /**
     * Creates the summary pane displaying overall task statistics.
     * @return HBox containing summary labels.
     */
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
    
    /**
     * Creates the Tasks tab containing a table of tasks and action buttons.
     * @return Tab for task management.
     */
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
        
        tasksTable.getColumns().addAll(titleCol, categoryCol, priorityCol, deadlineCol, statusCol);
        
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
    
    /**
     * Creates the Categories tab for managing categories.
     * @return Tab for category management.
     */
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
    
    /**
     * Creates the Priorities tab for managing priority levels.
     * @return Tab for priority management.
     */
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
    
    /**
     * Creates the Reminders tab for managing reminders.
     * @return Tab for reminder management.
     */
    private Tab createRemindersTab() {
        Tab tab = new Tab("Reminders");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        
        remindersTable = new TableView<>();
        remindersTable.setPlaceholder(new Label("No reminders available"));
        TableColumn<Reminder, String> taskTitleCol = new TableColumn<>("Task Title");
        taskTitleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTask().getTitle()));
        TableColumn<Reminder, String> reminderDateCol = new TableColumn<>("Reminder Date");
        reminderDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReminderDate().toString()));
        remindersTable.getColumns().addAll(taskTitleCol, reminderDateCol);
        
        Button addButton = new Button("Add Reminder");
        addButton.setOnAction(e -> showAddReminderDialog());
        Button deleteButton = new Button("Delete Reminder");
        deleteButton.setOnAction(e -> deleteSelectedReminder());
        
        HBox buttons = new HBox(10, addButton, deleteButton);
        buttons.setAlignment(Pos.CENTER);
        
        vbox.getChildren().addAll(remindersTable, buttons);
        tab.setContent(vbox);
        return tab;
    }
    
    /**
     * Creates the Search tab that allows searching tasks by title, category, and priority.
     * @return Tab for task search.
     */
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
                taskManager.searchTasks(titleQuery, categoryQuery, priorityQuery)
            ));
        });
        
        // Populate ComboBoxes from current categories and priorities
        categoryBox.setItems(FXCollections.observableArrayList(
            taskManager.getCategories().stream().map(Category::getName).toList()
        ));
        priorityBox.setItems(FXCollections.observableArrayList(
            taskManager.getPriorities().stream().map(Priority::getName).toList()
        ));
        
        vbox.getChildren().addAll(searchFields, searchResultsTable);
        tab.setContent(vbox);
        return tab;
    }
    
    /**
     * Updates the summary labels based on current tasks.
     */
    private void updateSummary() {
        int total = taskManager.getTasks().size();
        int completed = (int) taskManager.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int delayed = (int) taskManager.getTasks().stream().filter(t -> t.getStatus() == TaskStatus.DELAYED).count();
        int upcoming = (int) taskManager.getTasks().stream().filter(t -> t.getDeadline() != null &&
            !t.getDeadline().isBefore(LocalDate.now()) && t.getDeadline().isBefore(LocalDate.now().plusDays(7))).count();
        
        totalTasksLabel.setText("Total Tasks: " + total);
        completedTasksLabel.setText("Completed: " + completed);
        delayedTasksLabel.setText("Delayed: " + delayed);
        upcomingTasksLabel.setText("Due in 7 Days: " + upcoming);
    }
    
    /**
     * Refreshes all UI views to reflect current data.
     */
    private void refreshAllViews() {
        tasksTable.setItems(FXCollections.observableArrayList(taskManager.getTasks()));
        categoriesList.setItems(FXCollections.observableArrayList(taskManager.getCategories()));
        prioritiesList.setItems(FXCollections.observableArrayList(taskManager.getPriorities()));
        remindersTable.setItems(FXCollections.observableArrayList(taskManager.getReminders()));
        updateSummary();
    }
    
    /**
     * Displays a dialog to add a new task.
     */
    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add Task");
        
        TextField titleField = new TextField();
        TextField descriptionField = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.setItems(FXCollections.observableArrayList(
            taskManager.getCategories().stream().map(Category::getName).toList()
        ));
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.setItems(FXCollections.observableArrayList(
            taskManager.getPriorities().stream().map(Priority::getName).toList()
        ));
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
                showError("Error Adding Task", ex.getMessage());
            }
        });
    }
    
    /**
     * Displays a dialog to edit the selected task.
     */
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
        categoryBox.setItems(FXCollections.observableArrayList(
            taskManager.getCategories().stream().map(Category::getName).toList()
        ));
        categoryBox.setValue(selected.getCategory());
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.setItems(FXCollections.observableArrayList(
            taskManager.getPriorities().stream().map(Priority::getName).toList()
        ));
        priorityBox.setValue(selected.getPriority());
        DatePicker deadlinePicker = new DatePicker(selected.getDeadline());
        
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
                taskManager.updateTask(selected, updatedTask.getTitle(), updatedTask.getDescription(),
                        updatedTask.getCategory(), updatedTask.getPriority(), updatedTask.getDeadline());
                refreshAllViews();
            } catch (IllegalArgumentException ex) {
                showError("Error Updating Task", ex.getMessage());
            }
        });
    }
    
    /**
     * Deletes the selected task.
     */
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
    
    /**
     * Displays a dialog to add a new category.
     */
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
    
    /**
     * Displays a dialog to edit the selected category.
     */
    private void showEditCategoryDialog() {
        Category selected = categoriesList.getSelectionModel().getSelectedItem();
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
    
    /**
     * Deletes the selected category.
     */
    private void deleteSelectedCategory() {
        Category selected = categoriesList.getSelectionModel().getSelectedItem();
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
    
    /**
     * Displays a dialog to add a new priority.
     */
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
    
    /**
     * Displays a dialog to edit the selected priority.
     */
    private void showEditPriorityDialog() {
        Priority selected = prioritiesList.getSelectionModel().getSelectedItem();
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
    
    /**
     * Deletes the selected priority.
     */
    private void deleteSelectedPriority() {
        Priority selected = prioritiesList.getSelectionModel().getSelectedItem();
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
    
    /**
     * Displays a dialog to add a new reminder.
     */
    private void showAddReminderDialog() {
        Dialog<Reminder> dialog = new Dialog<>();
        dialog.setTitle("Add Reminder");
        
        TextField taskTitleField = new TextField();
        taskTitleField.setPromptText("Task Title");
        DatePicker reminderDatePicker = new DatePicker();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Task Title:"), 0, 0);
        grid.add(taskTitleField, 1, 0);
        grid.add(new Label("Reminder Date:"), 0, 1);
        grid.add(reminderDatePicker, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Task task = taskManager.getTasks().stream()
                        .filter(t -> t.getTitle().equals(taskTitleField.getText()))
                        .findFirst().orElse(null);
                if (task != null) {
                    return new Reminder(task, reminderDatePicker.getValue());
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(reminder -> {
            try {
                taskManager.addReminder(reminder.getTask().getTitle(), reminder.getReminderDate());
                refreshAllViews();
            } catch (Exception ex) {
                showError("Error Adding Reminder", ex.getMessage());
            }
        });
    }
    
    /**
     * Deletes the selected reminder.
     */
    private void deleteSelectedReminder() {
        Reminder selected = remindersTable.getSelectionModel().getSelectedItem();
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
    
    /**
     * Displays an error alert with the given title and message.
     * @param title the error title.
     * @param message the error message.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Persists the current state of the TaskManager to JSON files in the "medialab" folder.
     */
    private void persistData() {
        try {
            Files.createDirectories(Paths.get(dataDir));
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/tasks.json"), taskManager.getTasks());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/categories.json"), taskManager.getCategories());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/priorities.json"), taskManager.getPriorities());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataDir + "/reminders.json"), taskManager.getReminders());
        } catch (IOException e) {
            showError("Persistence Error", "Failed to save data: " + e.getMessage());
        }
    }
}

