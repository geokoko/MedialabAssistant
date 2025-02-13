package models;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private List<Task> tasks;

    public TaskManager() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Task> getTasksByCategory(Category category) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getCategory().equals(category)) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    public List<Task> getTasksByPriority(Priority priority) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getPriority().equals(priority)) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    public void updateTaskStatus(Task task, TaskStatus newStatus) {
        task.setStatus(newStatus);
    }
}
