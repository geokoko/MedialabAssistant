package models;
import java.time.LocalDate;

public class Task {
    private String title;
    private String description;
    private Category category;
    private Priority priority;
    private LocalDate deadline;
    private TaskStatus status;

    public Task(String title, String description, Category category, Priority priority, LocalDate deadline) {
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

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Task{" +
            "title='" + title + '\'' +
            ", category=" + category.getName() +
            ", priority=" + priority.getName() +
            ", dueDate=" + deadline +
            ", status=" + status +
            '}';
    }
}
