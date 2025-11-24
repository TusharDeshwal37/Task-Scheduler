class TaskService {
    constructor() {
        this.baseUrl = 'http://localhost:8080/api';
    }

    async getAllTasks() {
        try {
            const response = await fetch(`${this.baseUrl}/tasks`);
            if (!response.ok) throw new Error('Failed to fetch tasks');
            return await response.json();
        } catch (error) {
            console.error('Error fetching tasks:', error);
            throw error;
        }
    }

    async createTask(taskData) {
        try {
            const response = await fetch(`${this.baseUrl}/tasks`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(taskData)
            });
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to create task: ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error creating task:', error);
            throw error;
        }
    }

    async updateTask(id, taskData) {
        try {
            const response = await fetch(`${this.baseUrl}/tasks/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(taskData)
            });
            
            if (!response.ok) throw new Error('Failed to update task');
            return await response.json();
        } catch (error) {
            console.error('Error updating task:', error);
            throw error;
        }
    }

    async deleteTask(id) {
        try {
            const response = await fetch(`${this.baseUrl}/tasks/${id}`, {
                method: 'DELETE'
            });
            
            if (!response.ok) throw new Error('Failed to delete task');
            return true;
        } catch (error) {
            console.error('Error deleting task:', error);
            throw error;
        }
    }

    async toggleTaskCompletion(id, completed) {
        try {
            const response = await fetch(`${this.baseUrl}/tasks/${id}/completion`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ completed })
            });
            
            if (!response.ok) throw new Error('Failed to update task completion');
            return await response.json();
        } catch (error) {
            console.error('Error updating task completion:', error);
            throw error;
        }
    }
}