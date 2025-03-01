package com.todo.models;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.controller.TaskManager;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class TaskManagerPersistenceTest {

    private final String baseDir = "medialab";
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        // Create the "medialab" directory.
        Files.createDirectories(Paths.get(baseDir));
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Recursively delete the "medialab" directory after each test.
        Path path = Paths.get(baseDir);
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    // Test: Loading tasks from JSON, ensuring that tasks with past deadlines are marked DELAYED.
    @Test
    public void testLoadTasksFromJson() throws IOException {
        // Create a JSON file for tasks with one task having a past deadline.
        String tasksJson = "[{\"title\":\"Persisted Task\",\"description\":\"From JSON\",\"category\":\"Work\",\"priority\":\"High\",\"deadline\":\"" 
                + LocalDate.now().minusDays(1) + "\",\"status\":\"OPEN\"}]";
        Files.writeString(Paths.get(baseDir, "tasks.json"), tasksJson);

        // Create minimal JSON files for categories and priorities.
        String categoriesJson = "[{\"name\":\"Work\"}]";
        Files.writeString(Paths.get(baseDir, "categories.json"), categoriesJson);

        String prioritiesJson = "[{\"name\":\"High\"}, {\"name\":\"Default\"}]";
        Files.writeString(Paths.get(baseDir, "priorities.json"), prioritiesJson);

        // Create an empty reminders file.
        Files.writeString(Paths.get(baseDir, "reminders.json"), "[]");

        // Instantiate TaskManager; it should load data from the JSON files.
        TaskManager tm = new TaskManager();
        List<Task> tasks = tm.getTasks();
        assertEquals(1, tasks.size());
        
        // The task should be marked as DELAYED because its deadline is in the past.
        assertEquals(TaskStatus.DELAYED, tasks.get(0).getStatus());
    }

    // Test: Verify that priorities are loaded and that the default is present.
    @Test
    public void testLoadPrioritiesFromJson() throws IOException {
        // Create JSON for priorities without the "Default" priority.
        String prioritiesJson = "[{\"name\":\"High\"}]";
        Files.writeString(Paths.get(baseDir, "priorities.json"), prioritiesJson);

        // Minimal files for tasks, categories, and reminders.
        Files.writeString(Paths.get(baseDir, "tasks.json"), "[]");
        String categoriesJson = "[{\"name\":\"Work\"}]";
        Files.writeString(Paths.get(baseDir, "categories.json"), categoriesJson);
        Files.writeString(Paths.get(baseDir, "reminders.json"), "[]");

        // Instantiate TaskManager. Its constructor should add "Default" if missing.
        TaskManager tm = new TaskManager();
        assertTrue(tm.getPriorities().stream().anyMatch(p -> p.getName().equals("Default")));
        assertTrue(tm.getPriorities().stream().anyMatch(p -> p.getName().equals("High")));
    }
}

