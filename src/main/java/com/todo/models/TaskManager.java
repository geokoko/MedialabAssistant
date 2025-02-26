package com.todo.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Acts as a database layer.
 * TaskManager is responsible for:
 * - Loading and saving tasks, categories, priorities, and reminders from/to JSON files
 * - Holding the in-memory lists used by the application
 * - Managing CRUD operations on these entities
 */

public class TaskManager {
	private List<Task> tasks;
	private List<Category> categories;
	private List<Priority> priorities;
	private List<Reminder> reminders;

	public TaskManager() {
		tasks = new ArrayList<>();
		categories = new ArrayList<>();
		priorities = new ArrayList<>();
		reminders = new ArrayList<>();

		// On startup, load data from jsons in memory
		loadData("tasks.json", tasks, new TypeReference<List<Task>>() {});
		loadData("categories.json", categories, new TypeReference<List<Category>>() {});
		loadData("priorities.json", priorities, new TypeReference<List<Priority>>() {});
		loadData("reminders.json", reminders, new TypeReference<List<Reminder>>() {});

		// MAKE SURE DEFAULT PRIORITY EXISTS !
		boolean defaultExists = false;
		for (Priority p : priorities) {
			if (p.getName().equals("Default")) {
				defaultExists = true;
				break;
			}
		}

		if (!defaultExists) {
			priorities.add(new Priority("Default"));
			saveData("priorities.json", priorities);
		}

		// If a task has a deadline in the past, mark it as Delayed automatically
		markDelayedTasks();
	}

	private <T> void loadData(String filename, List<T> list, TypeReference<List<T>> typeReference) {
		try {
			File file = new File("medialab/" + filename);
			if (file.exists()) {
				ObjectMapper objectMapper = new ObjectMapper();
				List<T> dataList = objectMapper.readValue(file, typeReference);
				list.addAll(dataList);
				System.out.println("Loaded " + filename + " (" + dataList.size() + " records)");
			} else {
				System.out.println(filename + " not found, starting fresh.");
			}
		} catch (IOException e) {
			System.out.println("Error loading " + filename + ": " + e.getMessage());
		}
	}

	private void saveData(String filename, Object data) {
		try {
			Files.createDirectories(Paths.get("medialab"));
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.writeValue(new File("medialab/" + filename), data);
			System.out.println("Saved to " + filename);
		} catch (IOException e) {
			System.out.println("Error saving " + filename + ": " + e.getMessage());
		}
	}

	// GETTERS FOR IN-MEMORY DATA
	public List<Task> getTasks() {
		return tasks;
	}

	public List<Category> getCategories() {
		return categories;
	}

	public List<Priority> getPriorities() {
		return priorities;
	}

	public List<Reminder> getReminders() {
		return reminders;
	}

	private Task getTaskByTitle(String title) {
		return tasks.stream().filter(t -> t.getTitle().equalsIgnoreCase(title)).findFirst().orElse(null);
	}


	// -----------------------------------------------------
	// CRUD OPERATIONS 
	// -----------------------------------------------------
	public void addTask(Task task) {
		// Validate references
		if (!categoryExists(task.getCategory())) {
			throw new IllegalArgumentException("Category does not exist: " + task.getCategory());
		}
		if (!priorityExists(task.getPriority())) {
			throw new IllegalArgumentException("Priority does not exist: " + task.getPriority());
		}

		tasks.add(task);
	}


	public void removeTask(Task task) {
		if (task == null) {
			throw new IllegalArgumentException("Task cannot be null.");
		}
		if (!tasks.contains(task)) {
			throw new IllegalArgumentException("Task does not exist: " + task.getTitle());
		}

		tasks.remove(task);
		// Also remove reminders for this task
		reminders.removeIf(r -> r.getTask().getTitle().equals(task.getTitle()));
	}

	public void updateTask(Task task, String newTitle, String newDescription, String newCategory, String newPriority, LocalDate newDeadline) {
		if (task == null) {
			throw new IllegalArgumentException("Task cannot be null.");
		}
		if (!tasks.contains(task)) {
			throw new IllegalArgumentException("Task does not exist: " + task.getTitle());
		}

		// Update only non-null fields
		if (newTitle != null && !newTitle.trim().isEmpty()) {
			task.setTitle(newTitle);
		}
		if (newDescription != null && !newDescription.trim().isEmpty()) {
			task.setDescription(newDescription);
		}
		if (newCategory != null && !newCategory.trim().isEmpty()) {
			if (!categoryExists(newCategory)) {
				throw new IllegalArgumentException("Category does not exist: " + newCategory);
			}
			task.setCategory(newCategory);
		}
		if (newPriority != null && !newPriority.trim().isEmpty()) {
			if (!priorityExists(newPriority)) {
				throw new IllegalArgumentException("Priority does not exist: " + newPriority);
			}
			task.setPriority(newPriority);
		}
		if (newDeadline != null) {
			if (newDeadline.isBefore(LocalDate.now())) {
				throw new IllegalArgumentException("Deadline cannot be in the past.");
			}
			task.setDeadline(newDeadline);
		}
	}

	public void updateTaskStatus(Task task, TaskStatus newStatus) {
		if (task == null) {
			throw new IllegalArgumentException("Task cannot be null.");
		}
		if (!tasks.contains(task)) {
			throw new IllegalArgumentException("Task does not exist: " + task.getTitle());
		}
		if (newStatus == null) {
			throw new IllegalArgumentException("Task status cannot be null.");
		}

		task.setStatus(newStatus);

		// If marking completed -> remove any reminders
		if (newStatus == TaskStatus.COMPLETED) {
			reminders.removeIf(r -> r.getTask().getTitle().equals(task.getTitle()));
		}
	}

	// -----------------------------------------------------
	// CATEGORY Management
	// -----------------------------------------------------
	public void addCategory(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Category name cannot be empty.");
		}
		if (categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name))) {
			throw new IllegalArgumentException("Category already exists: " + name);
		}
		categories.add(new Category(name));
	}

	public void removeCategory(Category category) {
		if (!categories.contains(category)) {
			throw new IllegalArgumentException("Category does not exist: " + category.getName());
		}
		categories.remove(category);
		tasks.removeIf(task -> task.getCategory().equals(category.getName()));
	}

	public void renameCategory(Category category, String newName) {
		if (newName == null || newName.trim().isEmpty()) {
			throw new IllegalArgumentException("New category name cannot be empty.");
		}
		if (categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(newName))) {
			throw new IllegalArgumentException("Category with name " + newName + " already exists.");
		}
		category.setName(newName);
	}

	// -----------------------------------------------------
	// PRIORITY Management
	// -----------------------------------------------------
	public void addPriority(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Priority name cannot be empty.");
		}
		if (priorities.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))) {
			throw new IllegalArgumentException("Priority already exists: " + name);
		}
		priorities.add(new Priority(name));
	}

	public void removePriority(Priority priority) {
		if (!priorities.contains(priority)) {
			throw new IllegalArgumentException("Priority does not exist: " + priority.getName());
		}
		if (priority.getName().equalsIgnoreCase("Default")) {
			throw new IllegalArgumentException("Cannot delete default priority.");
		}
		priorities.remove(priority);
		tasks.forEach(task -> {
			if (task.getPriority().equalsIgnoreCase(priority.getName())) {
				task.setPriority("Default"); // Assign "Default" priority to affected tasks
			}
		});
	}

	public void renamePriority(Priority priority, String newName) {
		if (newName == null || newName.trim().isEmpty()) {
			throw new IllegalArgumentException("New priority name cannot be empty.");
		}
		if (priority.getName().equalsIgnoreCase("Default")) {
			throw new IllegalArgumentException("Cannot rename 'Default' priority.");
		}
		if (priorities.stream().anyMatch(p -> p.getName().equalsIgnoreCase(newName))) {
			throw new IllegalArgumentException("Priority with name " + newName + " already exists.");
		}
		priority.setName(newName);
	}

	// -----------------------------------------------------
	// REMINDER Management
	// -----------------------------------------------------
	public void addReminder(String taskTitle, LocalDate reminderDate) {
		Task task = getTaskByTitle(taskTitle);
		if (task == null) {
			throw new IllegalArgumentException("Task does not exist: " + taskTitle);
		}
		if (task.getStatus() == TaskStatus.COMPLETED) {
			throw new IllegalStateException("Cannot add a reminder for a completed task!");
		}
		reminders.add(new Reminder(task, reminderDate));
	}

	// -----------------------------------------------------
	// SEARCH Utilities
	// -----------------------------------------------------
	public List<Task> searchTasks(String title, String category, String priority) {
		List<Task> results = new ArrayList<>();
		for (Task t : tasks) {
			boolean match = true;
			if (title != null && !title.isEmpty() && !t.getTitle().toLowerCase().contains(title.toLowerCase())) {
				match = false;
			}
			if (category != null && !category.isEmpty() && !t.getCategory().equalsIgnoreCase(category)) {
				match = false;
			}
			if (priority != null && !priority.isEmpty() && !t.getPriority().equalsIgnoreCase(priority)) {
				match = false;
			}
			if (match) {
				results.add(t);
			}
		}
		return results;
	}

	// -----------------------------------------------------
	// HELPER BOOLEAN METHODS
	// -----------------------------------------------------
	private boolean categoryExists(String categoryName) {
		return categories.stream().anyMatch(c -> c.getName().equalsIgnoreCase(categoryName));
	}

	private boolean priorityExists(String priorityName) {
		return priorities.stream().anyMatch(p -> p.getName().equalsIgnoreCase(priorityName));
	}

	// -----------------------------------------------------
	// AUTOMATICALLY HANDLE DELAYED TASKS BASED ON DATE
	// -----------------------------------------------------
	private void markDelayedTasks() {
		LocalDate today = LocalDate.now();
		for (Task t : tasks) {
			if (t.getStatus() != TaskStatus.COMPLETED && t.getDeadline() != null && t.getDeadline().isBefore(today)) {
				t.setStatus(TaskStatus.DELAYED);
			}
		}
	}

}
