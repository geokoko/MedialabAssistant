package com.todo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todo.models.Task;
import com.todo.models.TaskCategory;
import com.todo.models.TaskPriority;
import com.todo.models.TaskReminder;
import com.todo.models.TaskStatus;
import com.fasterxml.jackson.databind.SerializationFeature;
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
	private List<TaskCategory> categories;
	private List<TaskPriority> priorities;
	private List<TaskReminder> reminders;

	public TaskManager() {
		tasks = new ArrayList<>();
		categories = new ArrayList<>();
		priorities = new ArrayList<>();
		reminders = new ArrayList<>();

		// On startup, load data from jsons in memory
		loadData("tasks.json", tasks, new TypeReference<List<Task>>() {});
		loadData("categories.json", categories, new TypeReference<List<TaskCategory>>() {});
		loadData("priorities.json", priorities, new TypeReference<List<TaskPriority>>() {});
		loadData("reminders.json", reminders, new TypeReference<List<TaskReminder>>() {});

		// MAKE SURE DEFAULT PRIORITY EXISTS !
		boolean defaultExists = false;
		for (TaskPriority p : priorities) {
			if (p.getName().equals("Default")) {
				defaultExists = true;
				break;
			}
		}

		if (!defaultExists) {
			priorities.add(new TaskPriority("Default"));
			saveData("priorities.json", priorities);
		}

		// If a task has a deadline in the past, mark it as Delayed automatically
		markDelayedTasks();
	}

	private <T> void loadData(String filename, List<T> list, TypeReference<List<T>> typeReference) {
		try {
			File file = new File("medialab/" + filename);
			if (file.exists()) {
				// Register JavaTimeModule for LocalDate support
				ObjectMapper objectMapper = new ObjectMapper()
						.registerModule(new JavaTimeModule())
						.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

				List<T> dataList = objectMapper.readValue(file, typeReference);
				list.addAll(dataList);

				// If loading reminders, re-establish links to actual Task objects
				if (list instanceof List<?> && !list.isEmpty() && list.get(0) instanceof TaskReminder) {
					List<TaskReminder> remindersList = (List<TaskReminder>) list;
					for (TaskReminder reminder : remindersList) {
						Task actualTask = getTaskByTitle(reminder.getTask().getTitle());
						if (actualTask != null) {
							reminder.setTask(actualTask); // Fix broken reference
						}
					}
				}

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
			ObjectMapper objectMapper = new ObjectMapper()
					.registerModule(new JavaTimeModule())
					.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

			objectMapper.writeValue(new File("medialab/" + filename), data);
			System.out.println("Saved to " + filename);
		} catch (IOException e) {
			System.out.println("Error saving " + filename + ": " + e.getMessage());
		}
	}

	// public helper to persist all data
	public void persistAll() {
		saveData("tasks.json", tasks);
		saveData("categories.json", categories);
		saveData("priorities.json", priorities);
		saveData("reminders.json", reminders);
	}

	// GETTERS FOR IN-MEMORY DATA
	public List<Task> getTasks() {
		return tasks;
	}

	public List<TaskCategory> getCategories() {
		return categories;
	}

	public List<TaskPriority> getPriorities() {
		return priorities;
	}

	public List<TaskReminder> getReminders() {
		return reminders;
	}

	public List<TaskReminder> getRemindersForTask(Task task) {
		return reminders.stream()
				.filter(r -> r.getTask().equals(task))
				.toList(); // Collect to a list
	}

	private Task getTaskByTitle(String title) {
		return tasks.stream().filter(t -> t.getTitle().equalsIgnoreCase(title)).findFirst().orElse(null);
	}

	// -----------------------------------------------------
	// CRUD OPERATIONS 
	// -----------------------------------------------------
	public void addTask(Task task) {
		// checks
		if (!categoryExists(task.getCategory())) {
			throw new IllegalArgumentException("Category does not exist: " + task.getCategory());
		}
		if (!priorityExists(task.getPriority())) {
			throw new IllegalArgumentException("Priority does not exist: " + task.getPriority());
		}

		tasks.add(task);
		// If the deadline is already overdue, set DELAYED (unless completed).
		if (task.getDeadline() != null &&
				task.getDeadline().isBefore(LocalDate.now()) &&
				task.getStatus() != TaskStatus.COMPLETED) {
				task.setStatus(TaskStatus.DELAYED);
		}
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
		if (!tasks.contains(task)) {
			throw new IllegalArgumentException("Task not found: " + task.getTitle());
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

		// Overdue => set DELAYED if not completed
		if (task.getDeadline() != null &&
				task.getDeadline().isBefore(LocalDate.now()) &&
				task.getStatus() != TaskStatus.COMPLETED) {
			task.setStatus(TaskStatus.DELAYED);
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
		categories.add(new TaskCategory(name));
	}

	public void removeCategory(TaskCategory category) {
		if (!categories.contains(category)) {
			throw new IllegalArgumentException("Category does not exist: " + category.getName());
		}
		categories.remove(category);
		tasks.removeIf(task -> task.getCategory().equals(category.getName()));
		reminders.removeIf(r -> !tasks.contains(r.getTask()));
	}

	public void renameCategory(TaskCategory category, String newName) {
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
		priorities.add(new TaskPriority(name));
	}

	public void removePriority(TaskPriority priority) {
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

	public void renamePriority(TaskPriority priority, String newName) {
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
	public void addReminder(String taskTitle, TaskReminder.ReminderType type, LocalDate customDate) {
		Task task = getTaskByTitle(taskTitle);
		if (task == null) {
			throw new IllegalArgumentException("Task does not exist: " + taskTitle);
		}
		if (task.getStatus() == TaskStatus.COMPLETED) {
			throw new IllegalStateException("Cannot add a reminder for a completed task!");
		}

		LocalDate reminderDate = null;
		switch (type) {
			case ONE_DAY_BEFORE:
				reminderDate = task.getDeadline().minusDays(1);
				break;
			case ONE_WEEK_BEFORE:
				reminderDate = task.getDeadline().minusWeeks(1);
				break;
			case ONE_MONTH_BEFORE:
				reminderDate = task.getDeadline().minusMonths(1);
				break;
			case CUSTOM_DATE:
				reminderDate = customDate;
				break;
		}

		// Validate that the reminder date makes sense
		if (reminderDate != null && reminderDate.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Reminder date must be in the future.");
		}

		reminders.add(new TaskReminder(task, type, customDate));
	}

	public void updateReminder(TaskReminder oldReminder, TaskReminder.ReminderType newType, LocalDate newCustomDate) {
		if (oldReminder == null) {
			throw new IllegalArgumentException("Reminder cannot be null.");
		}
		if (!reminders.contains(oldReminder)) {
			throw new IllegalArgumentException("Reminder does not exist.");
		}

		Task task = oldReminder.getTask();
		if (task.getStatus() == TaskStatus.COMPLETED) {
			throw new IllegalStateException("Cannot modify a reminder for a completed task.");
		}

		LocalDate newReminderDate = null;
		switch (newType) {
			case ONE_DAY_BEFORE:
				newReminderDate = task.getDeadline().minusDays(1);
				break;
			case ONE_WEEK_BEFORE:
				newReminderDate = task.getDeadline().minusWeeks(1);
				break;
			case ONE_MONTH_BEFORE:
				newReminderDate = task.getDeadline().minusMonths(1);
				break;
			case CUSTOM_DATE:
				newReminderDate = newCustomDate;
				break;
		}

		// Validate that the new reminder date makes sense
		if (newReminderDate != null && newReminderDate.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Reminder date must be in the future.");
		}

		oldReminder.setType(newType);
		oldReminder.setCustomReminderDate(newCustomDate);
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
