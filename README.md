# Medialab Assistant
## Introduction
Medialab Asssistant is a Task Management System implemented in Java with a JavaFX GUI.

The system allows users to:

* Create, edit, and delete tasks with attributes like title, description, category, priority, deadline, and status.
* Manage categories (add, rename, delete).
* Manage priorities (add, rename, delete; a default priority always exists).
* Set reminders for tasks and remove them automatically upon task completion.
* Automatically update overdue tasks (status changes to DELAYED if the deadline has passed).
* Search tasks by title, category, and priority.
* Persist data using JSON files in a medialab/ folder.
* Provide a JavaFX GUI with summary statistics and multiple tabs for managing tasks efficiently.

## Installation & Running the Application

### Prerequisites
1. Java 21 (configured via Gradle toolchain)
2. Gradle (or use the Gradle Wrapper gradlew)
3. JavaFX (handled automatically by the Gradle plugin)
4. Jackson JSON library for data serialization

### Running the application with gradle

1. Clone the Repository
    ``` bash
    git clone https://github.com/geokoko/MedialabAssistant.git
    cd MedialabAssistant
    ```

2. Run the Application
    * Use the Gradle wrapper (recommended):
        ``` bash
        ./gradlew run   # (Linux/macOS)
        gradlew run     # (Windows)
        ```

    * Use Gradle if you have it installed:
        If you have Gradle>=8.12.1, then run:
        ``` bash
        gradle run
        ```

    Gradle will:
    * Automatically download dependencies (JavaFX, Jackson, JUnit, etc.).
    * Set up the module path correctly for JavaFX.
    * Launch the application (com.todo.App).

## User Guide

### Managing Tasks

1. Select a task to edit or delete it.
2. Changing a task’s status to Completed removes its associated reminders.

### Managing Categories & Priorities

1. Create new categories or rename/delete existing ones.
2. Create, rename, or delete priorities (except for Default).
3. If a category is deleted, all tasks under that category are also removed.

### Searching for Tasks

1. Go to the Search tab.
2. Enter a title (or part of a title), select a category and/or priority.
3. Click Search to view matching tasks.

### Troubleshooting
1. ClassNotFoundException (JavaFX-related)
    JavaFX is managed by Gradle, but if issues occur, ensure org.openjfx.javafxplugin is applied in build.gradle.

2. JSON Parsing Errors
    If corrupted JSON files cause startup failures, delete the problematic files in medialab/ to reset data.

3. Date Validation Issues
    The system prevents setting deadlines in the past.

## References
* [JavaFX](https://docs.oracle.com/javase/8/javafx/get-started-tutorial/jfx-overview.htm)

* [Jackson](https://github.com/FasterXML/jackson)

* [Gradle](https://gradle.org/)
