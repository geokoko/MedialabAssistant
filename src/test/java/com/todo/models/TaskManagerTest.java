package com.todo.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.todo.controller.TaskManager;

import java.time.LocalDate;

public class TaskManagerTest {

	private TaskManager taskManager;

	@BeforeEach
	public void setUp() {
		taskManager = new TaskManager();
		taskManager.getCategories().clear();
		taskManager.getPriorities().clear();
		taskManager.getTasks().clear();
		taskManager.getReminders().clear();

		taskManager.addCategory("Work");
		taskManager.addCategory("Personal");
		taskManager.addPriority("High");
		taskManager.addPriority("Default");
	}

	@Test
	public void testAddAndRemoveTask() {
		Task task = new Task("Test Task", "This is a test", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);
		assertEquals(1, taskManager.getTasks().size(), "Task should be added");
		System.out.println("Passed Task addition!");

		taskManager.removeTask(task);
		assertEquals(0, taskManager.getTasks().size(), "Task should be removed");
		System.out.println("Passed Task removal!");
	}

	@Test
	public void testUpdateTaskStatus() {
		Task task = new Task("Test Task", "Description", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);
		assertEquals(TaskStatus.OPEN, task.getStatus());

		taskManager.updateTaskStatus(task, TaskStatus.COMPLETED);
		assertEquals(TaskStatus.COMPLETED, task.getStatus());
		System.out.println("Passed Status update!");
	}

	@Test
	public void testAddTaskSuccess() {
		Task task = new Task("Task1", "Description1", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);
		assertEquals(1, taskManager.getTasks().size());
	}

	@Test
	public void testAddTaskInvalidPriority() {
		Task task = new Task("Task3", "Description3", "Work", "Low", LocalDate.now().plusDays(1));
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			taskManager.addTask(task);
		});
		assertTrue(exception.getMessage().contains("Priority does not exist"));
	}

	// Test: Removing a task also clears its reminders.
	@Test
	public void testRemoveTaskAndReminders() {
		Task task = new Task("Task4", "Description4", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);

		// Use CUSTOM_DATE, or any type you like
		LocalDate customReminderDate = LocalDate.now().plusDays(1);
		taskManager.addReminder(task.getTitle(), TaskReminder.ReminderType.CUSTOM_DATE, customReminderDate);

		// We still assert that removing the task also removes the reminder
		assertEquals(1, taskManager.getReminders().size());

		taskManager.removeTask(task);
		assertEquals(0, taskManager.getTasks().size());
		assertEquals(0, taskManager.getReminders().size(),
				"Removing the task should also remove its reminders");
	}

	// Test: Updating a task's fields.
	@Test
	public void testUpdateTask() {
		Task task = new Task("Task5", "Description5", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);

		taskManager.updateTask(task, "New Task5", "New Description", "Personal", "High", LocalDate.now().plusDays(2));
		assertEquals("New Task5", task.getTitle());
		assertEquals("New Description", task.getDescription());
		assertEquals("Personal", task.getCategory());
		assertEquals(LocalDate.now().plusDays(2), task.getDeadline());
	}

	// Test: Marking a task as COMPLETED removes its reminders.
	@Test
	public void testUpdateTaskStatusRemovesReminders() {
		Task task = new Task("Task6", "Description6", "Work", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);

		taskManager.addReminder(
				task.getTitle(),
				TaskReminder.ReminderType.ONE_DAY_BEFORE,
				null // since we only need a custom date if type == CUSTOM_DATE
		);

		assertEquals(1, taskManager.getReminders().size(),
				"One reminder should have been added");

		// Mark as completed => reminders removed
		taskManager.updateTaskStatus(task, TaskStatus.COMPLETED);
		assertEquals(TaskStatus.COMPLETED, task.getStatus());
		assertEquals(0, taskManager.getReminders().size(),
				"All reminders for this task should be removed");
	}

	@Test
	public void testAddReminderForTask() {
		Task task = new Task("Reminder Task", "Test reminder", "Work", "High", LocalDate.now().plusDays(2));
		taskManager.addTask(task);

		// We'll use a CUSTOM_DATE type here
		LocalDate customDate = LocalDate.now().plusDays(1);
		taskManager.addReminder(task.getTitle(), TaskReminder.ReminderType.CUSTOM_DATE, customDate);

		assertEquals(1, taskManager.getReminders().size(), "Reminder should be added");

		TaskReminder reminder = taskManager.getReminders().get(0);
		// Verify the correct task
		assertEquals(task.getTitle(), reminder.getTask().getTitle(),
				"Reminder should be associated with the correct task");
		// Verify the correct type
		assertEquals(TaskReminder.ReminderType.CUSTOM_DATE, reminder.getType(),
				"Reminder type should be CUSTOM_DATE");
		// Verify the custom date
		assertEquals(customDate, reminder.getCustomReminderDate(),
				"Custom reminder date should match what we set");
		// Also check the computed date:
		assertEquals(customDate, reminder.computeReminderDate(),
				"computeReminderDate() should return the custom date");
	}

	// Test: Category management (add, rename, remove).
	@Test
	public void testCategoryManagement() {
		// Add a new category.
		taskManager.addCategory("Hobby");
		assertTrue(taskManager.getCategories().stream().anyMatch(c -> c.getName().equals("Hobby")));

		// Rename an existing category.
		TaskCategory category = taskManager.getCategories().stream()
			.filter(c -> c.getName().equals("Work")).findFirst().orElse(null);
		assertNotNull(category);
		taskManager.renameCategory(category, "Office");
		assertFalse(taskManager.getCategories().stream().anyMatch(c -> c.getName().equals("Work")));
		assertTrue(taskManager.getCategories().stream().anyMatch(c -> c.getName().equals("Office")));

		// Removing a category should also remove tasks associated with it.
		Task task = new Task("Task7", "Description7", "Personal", "High", LocalDate.now().plusDays(1));
		taskManager.addTask(task);
		TaskCategory personalCategory = taskManager.getCategories().stream()
			.filter(c -> c.getName().equals("Personal")).findFirst().orElse(null);
		assertNotNull(personalCategory);
		taskManager.removeCategory(personalCategory);
		assertFalse(taskManager.getCategories().stream().anyMatch(c -> c.getName().equals("Personal")));
		assertFalse(taskManager.getTasks().stream().anyMatch(t -> t.getCategory().equals("Personal")));
	}

	// Test: Priority management (add, rename, remove).
	@Test
	public void testPriorityManagement() {
		// Add a new priority.
		taskManager.addPriority("Urgent");
		assertTrue(taskManager.getPriorities().stream().anyMatch(p -> p.getName().equals("Urgent")));

		// Rename the new priority.
		TaskPriority priority = taskManager.getPriorities().stream()
			.filter(p -> p.getName().equals("Urgent")).findFirst().orElse(null);
		assertNotNull(priority);
		taskManager.renamePriority(priority, "Critical");
		assertFalse(taskManager.getPriorities().stream().anyMatch(p -> p.getName().equals("Urgent")));
		assertTrue(taskManager.getPriorities().stream().anyMatch(p -> p.getName().equals("Critical")));

		// Removing a priority should update tasks using it to the "Default" priority.
		Task task = new Task("Task8", "Description8", "Work", "Critical", LocalDate.now().plusDays(1));
		taskManager.addTask(task);
		TaskPriority criticalPriority = taskManager.getPriorities().stream()
			.filter(p -> p.getName().equals("Critical")).findFirst().orElse(null);
		assertNotNull(criticalPriority);
		taskManager.removePriority(criticalPriority);
		assertEquals("Default", task.getPriority());
	}

	// Test: Reminder management.
	@Test
	public void testReminderManagement() {
		Task task = new Task("Task9", "Description9", "Work", "High", LocalDate.now().plusDays(5));
		taskManager.addTask(task);
		// Add a valid reminder.
		taskManager.addReminder(
				task.getTitle(),
				TaskReminder.ReminderType.CUSTOM_DATE, // or ONE_DAY_BEFORE, ONE_WEEK_BEFORE, etc.
				LocalDate.now().plusDays(4));
		assertEquals(1, taskManager.getReminders().size());

		// After marking the task COMPLETED, adding a reminder should fail.
		taskManager.updateTaskStatus(task, TaskStatus.COMPLETED);
		Exception exception = assertThrows(IllegalStateException.class, () -> {
			taskManager.addReminder(
					task.getTitle(),
					TaskReminder.ReminderType.CUSTOM_DATE, // or ONE_DAY_BEFORE, ONE_WEEK_BEFORE, etc.
					LocalDate.now().plusDays(4));
		});
		assertTrue(exception.getMessage().contains("Cannot add a reminder for a completed task"));
	}

	// Test: Task search functionality.
	@Test
	public void testSearchTasks() {
		Task task1 = new Task("Email", "Send email", "Work", "High", LocalDate.now().plusDays(1));
		Task task2 = new Task("Shopping", "Buy groceries", "Personal", "High", LocalDate.now().plusDays(2));
		Task task3 = new Task("Meeting", "Project meeting", "Work", "High", LocalDate.now().plusDays(3));
		taskManager.addTask(task1);
		taskManager.addTask(task2);
		taskManager.addTask(task3);

		// Search by title substring.
		var results = taskManager.searchTasks("e", null, null);
		assertEquals(2, results.size());

		// Search by category.
		results = taskManager.searchTasks(null, "Personal", null);
		assertEquals(1, results.size());
		assertEquals("Shopping", results.get(0).getTitle());

		// Search by priority.
		results = taskManager.searchTasks(null, null, "High");
		assertEquals(3, results.size());

		// Combined search: category "Work" and title containing "Meeting".
		results = taskManager.searchTasks("Meeting", "Work", null);
		assertEquals(1, results.size());
		assertEquals("Meeting", results.get(0).getTitle());
	}
}

