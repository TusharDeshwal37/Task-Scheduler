package com.scheduler;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scheduler.controller.ReminderScheduler;
import com.scheduler.controller.TaskService;
import com.scheduler.model.Task;

import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

public class Main {
    private static final TaskService taskService = new TaskService();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        // Configure JSON mapper
        objectMapper.registerModule(new JavaTimeModule());
        
        // Configure web server
        port(8080);
        staticFiles.location("/web");
        staticFiles.externalLocation("web");
        enableCORS();
        
        System.out.println("🚀 Starting Task Scheduler API...");
        
        // API Routes
        get("/api/tasks", (req, res) -> {
            res.type("application/json");
            try {
                return taskService.getAllTasks();
            } catch (Exception e) {
                res.status(500);
                return createErrorResponse("Error fetching tasks: " + e.getMessage());
            }
        }, objectMapper::writeValueAsString);
        
        get("/api/tasks/:id", (req, res) -> {
            res.type("application/json");
            try {
                int taskId = Integer.parseInt(req.params(":id"));
                Task task = taskService.getTaskById(taskId);
                if (task != null) {
                    return task;
                } else {
                    res.status(404);
                    return createErrorResponse("Task not found");
                }
            } catch (NumberFormatException e) {
                res.status(400);
                return createErrorResponse("Invalid task ID");
            } catch (Exception e) {
                res.status(500);
                return createErrorResponse("Error fetching task: " + e.getMessage());
            }
        }, objectMapper::writeValueAsString);
        
        post("/api/tasks", (req, res) -> {
            res.type("application/json");
            try {
                Task task = objectMapper.readValue(req.body(), Task.class);
                
                if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                    res.status(400);
                    return createErrorResponse("Task title is required");
                }
                if (task.getDueDate() == null) {
                    res.status(400);
                    return createErrorResponse("Due date is required");
                }
                
                System.out.println("📝 Creating new task:");
                System.out.println("   - Title: " + task.getTitle());
                System.out.println("   - Due Date: " + task.getDueDate());
                System.out.println("   - Priority: " + task.getPriority());
                
                taskService.addTask(task);
                res.status(201);
                return task;
            } catch (Exception e) {
                res.status(400);
                return createErrorResponse("Invalid task data: " + e.getMessage());
            }
        }, objectMapper::writeValueAsString);
        
        put("/api/tasks/:id", (req, res) -> {
            res.type("application/json");
            try {
                int taskId = Integer.parseInt(req.params(":id"));
                Task updatedTask = objectMapper.readValue(req.body(), Task.class);
                
                if (updatedTask.getTitle() == null || updatedTask.getTitle().trim().isEmpty()) {
                    res.status(400);
                    return createErrorResponse("Task title is required");
                }
                if (updatedTask.getDueDate() == null) {
                    res.status(400);
                    return createErrorResponse("Due date is required");
                }
                
                taskService.updateTask(taskId, updatedTask);
                return updatedTask;
            } catch (Exception e) {
                res.status(400);
                return createErrorResponse("Error updating task: " + e.getMessage());
            }
        }, objectMapper::writeValueAsString);
        
        delete("/api/tasks/:id", (req, res) -> {
            try {
                int taskId = Integer.parseInt(req.params(":id"));
                taskService.deleteTask(taskId);
                res.status(204);
                return "";
            } catch (Exception e) {
                res.status(400);
                return createErrorResponse("Error deleting task: " + e.getMessage());
            }
        });
        
        patch("/api/tasks/:id/completion", (req, res) -> {
            res.type("application/json");
            try {
                int taskId = Integer.parseInt(req.params(":id"));
                @SuppressWarnings("unchecked")
                Map<String, Object> completionData = objectMapper.readValue(req.body(), Map.class);
                Boolean completed = (Boolean) completionData.get("completed");
                
                if (completed == null) {
                    res.status(400);
                    return createErrorResponse("Completion status is required");
                }
                
                taskService.toggleTaskCompletion(taskId, completed);
                Task updatedTask = taskService.getTaskById(taskId);
                return updatedTask;
            } catch (Exception e) {
                res.status(400);
                return createErrorResponse("Error updating task completion: " + e.getMessage());
            }
        }, objectMapper::writeValueAsString);
        
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return createSuccessResponse("Task Scheduler API is running");
        }, objectMapper::writeValueAsString);
        
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });
        
        // Start services
        ReminderScheduler reminderScheduler = new ReminderScheduler(taskService);
        reminderScheduler.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down Task Scheduler...");
            reminderScheduler.stop();
        }));
        
        System.out.println("✅ Task Scheduler API running on http://localhost:8080");
        System.out.println("📱 Frontend available at http://localhost:8080/index.html");
        System.out.println("🔗 API endpoints available at http://localhost:8080/api/tasks");
        System.out.println("❤️  Health check: http://localhost:8080/api/health");
        
        // Keep application running
        System.out.println("=========================================");
        System.out.println("✅ Application is RUNNING - Press Ctrl+C to stop");
        System.out.println("=========================================");
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Application interrupted");
        }
    }
    
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });
        
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "*");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS,PATCH");
        });
    }
    
    private static Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
    
    private static Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}
