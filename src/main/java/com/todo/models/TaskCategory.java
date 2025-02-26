package com.todo.models;

public class TaskCategory {
    private String name;

	public TaskCategory() {

	}

    public TaskCategory(String name) {
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
