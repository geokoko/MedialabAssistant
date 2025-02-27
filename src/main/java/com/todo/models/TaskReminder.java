package com.todo.models;

import java.time.LocalDate;

public class TaskReminder {
    private Task task;
    private LocalDate reminderDate;

    // default constructor
    public TaskReminder() {
    
    }

    public TaskReminder(Task task, LocalDate reminderDate) {
        this.task = task;
        this.reminderDate = reminderDate;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    @Override
    public String toString() {
        return "Reminder for task: " + task.getTitle() + " on " + reminderDate;
    }
}
