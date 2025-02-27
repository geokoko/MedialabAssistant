package com.todo.models;

public class TaskPriority {
    private String name;

    // default constructor
	public TaskPriority() {

	}

    public TaskPriority(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
