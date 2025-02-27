package com.todo.models;

import java.time.LocalDate;

public class Task {
    private String title;
    private String description;
    private String category;
    private String priority; // don't need to store the whole object, only its title as a key
    private LocalDate deadline;
    private TaskStatus status;

    // default constructor
	public Task() {

	}

    public Task(String title, String description, String category, String priority, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.deadline = deadline;
        this.status = TaskStatus.OPEN; // Default status
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Task{" +
            "title='" + title + '\'' +
            ", category=" + category +
            ", priority=" + priority +
            ", dueDate=" + deadline +
            ", status=" + status +
            '}';
    }
}
