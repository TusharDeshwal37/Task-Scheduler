package com.scheduler.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.scheduler.model.Task;

public class ReminderScheduler {
    private final TaskService taskService;
    private final ScheduledExecutorService scheduler;
    
    public ReminderScheduler(TaskService taskService) {
        this.taskService = taskService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 30, TimeUnit.SECONDS);
        System.out.println("🔔 Reminder scheduler started");
    }
    
    public void stop() {
        scheduler.shutdown();
        System.out.println("🔔 Reminder scheduler stopped");
    }
    
    private void checkReminders() {
        List<Task> pendingTasks = taskService.getPendingTasks();
        LocalDateTime now = LocalDateTime.now();
        
        for (Task task : pendingTasks) {
            if (isDueSoon(task, now)) {
                System.out.printf("⏰ REMINDER: Task '%s' is due soon!%n", task.getTitle());
            }
            
            if (isOverdue(task, now)) {
                System.out.printf("🚨 OVERDUE: Task '%s' is overdue!%n", task.getTitle());
            }
        }
    }
    
    private boolean isDueSoon(Task task, LocalDateTime now) {
        return !task.isCompleted() && 
               now.isAfter(task.getDueDate().minusHours(24)) && 
               now.isBefore(task.getDueDate());
    }
    
    private boolean isOverdue(Task task, LocalDateTime now) {
        return !task.isCompleted() && now.isAfter(task.getDueDate());
    }
}