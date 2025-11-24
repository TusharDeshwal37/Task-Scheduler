class TaskSchedulerApp {
    constructor() {
        this.taskService = new TaskService();
        this.uiManager = new UIManager(this.taskService);
        console.log('✅ Task Scheduler App initialized');
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new TaskSchedulerApp();
});