package com.todo.models;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskReminder {
    public enum ReminderType {
        ONE_DAY_BEFORE,
        ONE_WEEK_BEFORE,
        ONE_MONTH_BEFORE,
        CUSTOM_DATE
    }

    private Task task;
    private ReminderType type;
    private LocalDate customReminderDate; // Only used if type is CUSTOM_DATE


    public TaskReminder() { // default constructor
    }

    public TaskReminder(Task task, ReminderType type, LocalDate customReminderDate) {
        this.task = task;
        this.type = type;
        this.customReminderDate = customReminderDate;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public ReminderType getType() {
        return type;
    }

    public void setType(ReminderType type) {
        this.type = type;
    }

    public LocalDate getCustomReminderDate() {
        return customReminderDate;
    }

    public void setCustomReminderDate(LocalDate customReminderDate) {
        this.customReminderDate = customReminderDate;
    }

    public LocalDate computeReminderDate() {
        if (task.getDeadline() == null)
            return null;
        switch (type) {
            case ONE_DAY_BEFORE:
                return task.getDeadline().minusDays(1);
            case ONE_WEEK_BEFORE:
                return task.getDeadline().minusWeeks(1);
            case ONE_MONTH_BEFORE:
                return task.getDeadline().minusMonths(1);
            case CUSTOM_DATE:
                return customReminderDate;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "Reminder for task: " + task.getTitle() +
                " on " + computeReminderDate() + " (" + type + ")";
    }
}
