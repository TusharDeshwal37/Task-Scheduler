package com.scheduler.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Task implements Serializable, Comparable<Task> {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME; 
    
    private int id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Priority priority;
    private boolean completed;
    private LocalDateTime createdAt;
    
    public enum Priority {
        HIGH, MEDIUM, LOW
    }
    
    public Task() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Task(String title, String description, LocalDateTime dueDate, Priority priority) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.description = description != null ? description : "";
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @JsonIgnore 
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    // Custom getter for JSON serialization
    @JsonProperty("dueDate")
    public String getFormattedDueDate() {
        return dueDate != null ? dueDate.format(formatter) : null;
    }
    
    // Custom setter for JSON deserialization
    @JsonProperty("dueDate")
    public void setFormattedDueDate(String dateString) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                this.dueDate = LocalDateTime.parse(dateString, formatter);
                System.out.println("✅ Successfully parsed date from JSON: " + this.dueDate);
            } catch (Exception e) {
                System.err.println("❌ Error parsing date from JSON: " + dateString);
                e.printStackTrace();
            }
        }
    }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    // Add @JsonIgnore to prevent createdAt array serialization
    @JsonIgnore
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Custom getter for createdAt serialization
    @JsonProperty("createdAt")
    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(formatter) : null;
    }
    
    // Custom setter for createdAt deserialization  
    @JsonProperty("createdAt")
    public void setFormattedCreatedAt(String dateString) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                this.createdAt = LocalDateTime.parse(dateString, formatter);
                System.out.println("✅ Successfully parsed createdAt from JSON: " + this.createdAt);
            } catch (Exception e) {
                System.err.println("❌ Error parsing createdAt from JSON: " + dateString);
                e.printStackTrace();
            }
        }
    }
    
    // CRITICAL: Add @JsonIgnore to prevent these from being serialized
    @JsonIgnore
    public boolean isDueSoon() {
        return !completed && 
               LocalDateTime.now().isAfter(dueDate.minusHours(24)) && 
               LocalDateTime.now().isBefore(dueDate);
    }
    
    @JsonIgnore
    public boolean isOverdue() {
        return !completed && LocalDateTime.now().isAfter(dueDate);
    }
    
    @Override
    public int compareTo(Task other) {
        // Sort primarily by due date
        int dateComparison = this.dueDate.compareTo(other.dueDate);
        if (dateComparison != 0) return dateComparison;
        
        // Secondary sort by reversed priority (HIGH -> MEDIUM -> LOW)
        return other.priority.compareTo(this.priority);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id && Objects.equals(title, task.title);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
    
    @Override
    public String toString() {
        return String.format("%s (Due: %s, Priority: %s, Completed: %s)", 
            title, dueDate, priority, completed);
    }
}