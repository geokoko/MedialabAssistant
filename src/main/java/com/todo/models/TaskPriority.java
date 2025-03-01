package com.todo.models;

/**
 * Represents a named priority level for a {@code Task}.
 * <p>
 * A {@code TaskPriority} consists of a single field, {@code name},
 * which represents a textual label for the priority, e.g. "High," "Medium,"
 * "Low," or "Default." The name can be accessed or modified
 * via {@link #getName()} and {@link #setName(String)}.
 * </p>
 *
 * @author
 * @version 1.0
 */
public class TaskPriority {

    private String name;

    /**
     * Constructs a {@code TaskPriority} without assigning a name.
     * The name may be set later via {@link #setName(String)}.
     */
    public TaskPriority() {
        // Default constructor
    }

    /**
     * Constructs a {@code TaskPriority} with the specified name.
     *
     * @param name the textual label to represent this priority
     */
    public TaskPriority(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this priority.
     *
     * @return the priority's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a new name (label) for this priority.
     *
     * @param name the new name to be assigned
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a string representation of this priority.
     * <p>
     * In this implementation, the returned string is simply
     * the {@code name} of the priority.
     * </p>
     *
     * @return the textual name of this priority
     */
    @Override
    public String toString() {
        return name;
    }
}
