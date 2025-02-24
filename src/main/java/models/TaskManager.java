package models;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
	private List<Task> tasks;
	private List<Category> categories;
	private List<Priority> priorities;

	public TaskManager() {
		this.tasks = new ArrayList<>();
		this.categories = new ArrayList<>();
		this.priorities = new ArrayList<>();
		// MAKE SURE DEFAULT PRIORITY EXISTS !
		boolean defaultExists = false;
    	for (Priority p : priorities) {
        	if (p.getName().equals("Default")) {
            	defaultExists = true;
            	break;
        	}
    	}
    
    	if (!defaultExists) {
        	this.priorities.add(new Priority("Default"));
   		}
	}

	// TASK MANAGEMENT
	public void addTask(Task task) {
		tasks.add(task);
	}

	public void removeTask(Task task) {
		tasks.remove(task);
	}

	public void updateTask(Task task, String newTitle, Category newCategory, Priority newPriority) {
		task.setTitle(newTitle);
		task.setCategory(newCategory);
		task.setPriority(newPriority);
	}

	public void updateTaskStatus(Task task, TaskStatus newStatus) {
		task.setStatus(newStatus);
	}

	// CATEGORY MANAGEMENT
	public void addCategory(String name) {
		for (Category category : categories) {
			if (category.getName().equalsIgnoreCase(name)) {
				return; // Prevent duplicate categories
			}
		}
		categories.add(new Category(name));
	} 

	public void removeCategory(Category category) {
		categories.remove(category);
		tasks.removeIf(task -> task.getCategory().equals(category)); // Delete associated tasks
	}

	public void renameCategory(Category category, String newName) {
		category.setName(newName);
	}

	// PRIORITY MANAGEMENT
	public void addPriority(String name) {
		for (Priority priority : priorities) {
			if (priority.getName().equalsIgnoreCase(name)) {
				return; // Prevent duplicate priorities
			}
		}
		priorities.add(new Priority(name));
	}

	public void removePriority(Priority priority) {
		if (priority.getName().equals("Default")) {
			throw new IllegalArgumentException("Cannot delete default priority");
		}
		priorities.remove(priority);
		for (Task task : tasks) {
			if (task.getPriority().equals(priority)) {
				task.setPriority(new Priority("Default")); // Reassign to default priority
			}
		}
	}

	public void renamePriority(Priority priority, String newName) {
		if (!priority.getName().equals("Default")) {
			priority.setName(newName);
		}
	}

	// RETURN ALL SAVED TASKS
	public List<Task> getTasks() {
		return tasks;
	}

	// METHOD TO SEARCH FOR TASKS, BASED ON FILTERS (TITLE, CATEGORY, PRIORITY). MUST HAVE ALL INCLUDED FILTERS IN THE SEARCH QUERY	
	public List<Task> searchTasks(String title, Category category, Priority priority) {
		List<Task> filteredTasks = new ArrayList<>();

		for (Task task : tasks) {
			boolean matches = true;

			// If title is provided, check if it matches (the search should be case insensitive)
			if (title != null && !title.isEmpty()) {
				if (!task.getTitle().toLowerCase().contains(title.toLowerCase())) {
					matches = false;
				}
			}

			// If a category is provided, check if it matches
			if (category != null && !task.getCategory().equals(category)) {
				matches = false;
			}

			// If a priority is provided, check if it matches
			if (priority != null && !task.getPriority().equals(priority)) {
				matches = false;
			}

			if (matches) {
				filteredTasks.add(task);
			}
		}

		return filteredTasks;
	}

}

