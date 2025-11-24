package com.scheduler.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scheduler.model.Task;

public class TaskService {
    private final Map<Integer, Task> tasks;
    private final AtomicInteger idCounter;
    private final ObjectMapper objectMapper;
    private static final String DATA_FILE = "data/tasks.json";
    
    public TaskService() {
        this.tasks = new ConcurrentHashMap<>();
        this.idCounter = new AtomicInteger(1);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // CRITICAL: Configure Jackson to ignore unknown properties
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        loadTasks();
    }
    
    public List<Task> getAllTasks() {
        List<Task> taskList = new ArrayList<>(tasks.values());
        Collections.sort(taskList);
        return taskList;
    }
    
    public Task getTaskById(int id) {
        return tasks.get(id);
    }
    
    public void addTask(Task task) {
        int newId = idCounter.getAndIncrement();
        task.setId(newId);
        tasks.put(newId, task);
        saveTasks();
        System.out.println("✅ Task added - ID: " + newId + ", Title: " + task.getTitle());
    }
    
    public void updateTask(int id, Task updatedTask) {
        Task existingTask = tasks.get(id);
        if (existingTask != null) {
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setDueDate(updatedTask.getDueDate());
            existingTask.setPriority(updatedTask.getPriority());
            existingTask.setCompleted(updatedTask.isCompleted());
            saveTasks();
        }
    }
    
    public void deleteTask(int id) {
        tasks.remove(id);
        saveTasks();
    }
    
    public void toggleTaskCompletion(int id, boolean completed) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setCompleted(completed);
            saveTasks();
        }
    }
    
    public List<Task> getPendingTasks() {
        return tasks.values().stream()
                .filter(task -> !task.isCompleted())
                .toList();
    }
    
    @SuppressWarnings("unchecked")
    private void loadTasks() {
        try {
            if (!Files.exists(Paths.get(DATA_FILE))) {
                System.out.println("📁 No existing tasks file found. Starting with empty task list.");
                return;
            }
            
            System.out.println("🔄 Loading tasks from " + DATA_FILE);
            String content = Files.readString(Paths.get(DATA_FILE));
            System.out.println("📄 File content: " + content);
            
            Map<String, Object> data = objectMapper.readValue(content, Map.class);
            
            List<Map<String, Object>> tasksData = (List<Map<String, Object>>) data.get("tasks");
            int maxId = (Integer) data.get("nextId");
            
            System.out.println("📊 Found " + (tasksData != null ? tasksData.size() : 0) + " tasks in file");
            
            tasks.clear();
            if (tasksData != null) {
                for (Map<String, Object> taskData : tasksData) {
                    System.out.println("🔍 Processing task data: " + taskData);
                    try {
                        Task task = objectMapper.convertValue(taskData, Task.class);
                        tasks.put(task.getId(), task);
                        System.out.println("✅ Successfully loaded task - ID: " + task.getId() + ", Title: " + task.getTitle());
                        System.out.println("   Due Date: " + task.getDueDate());
                        System.out.println("   Created At: " + task.getCreatedAt());
                    } catch (Exception e) {
                        System.err.println("❌ Error converting task data: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            idCounter.set(maxId);
            System.out.println("✅ Loaded " + tasks.size() + " tasks from storage. Next ID: " + maxId);
            
        } catch (Exception e) {
            System.err.println("❌ Error loading tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveTasks() {
        try {
            Files.createDirectories(Paths.get("data"));
            
            Map<String, Object> data = new HashMap<>();
            data.put("tasks", new ArrayList<>(tasks.values()));
            data.put("nextId", idCounter.get());
            
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.write(Paths.get(DATA_FILE), json.getBytes());
            
            System.out.println("💾 Saved " + tasks.size() + " tasks to storage");
            System.out.println("📁 Saved data: " + json);
            
        } catch (Exception e) {
            System.err.println("❌ Error saving tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }
}