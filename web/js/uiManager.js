class UIManager {
    constructor(taskService) {
        this.taskService = taskService;
        this.currentFilter = 'all';
        this.currentSort = 'dueDate';
        this.tasks = [];
        
        this.initializeEventListeners();
        this.loadTasks();
    }

    initializeEventListeners() {
        // Task form
        document.getElementById('taskForm').addEventListener('submit', (e) => this.handleAddTask(e));
        document.getElementById('clearForm').addEventListener('click', () => this.clearForm());
        
        // Filters
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.addEventListener('click', (e) => this.handleFilter(e));
        });
        
        // Search
        document.getElementById('searchInput').addEventListener('input', (e) => this.handleSearch(e));
        
        // Sort
        document.getElementById('sortTasks').addEventListener('click', () => this.handleSort());
    }

    async loadTasks() {
        try {
            console.log('🔄 Loading tasks from backend...');
            this.tasks = await this.taskService.getAllTasks();
            console.log('✅ Raw tasks from backend:', this.tasks);
            
            // Enhanced Debug: Check what date fields we have with detailed parsing info
            console.log('🔍 DEBUG: Detailed date analysis for all tasks...');
            this.tasks.forEach((task, index) => {
                console.log(`Task ${index + 1}:`, {
                    id: task.id,
                    title: task.title,
                    dueDate: task.dueDate,
                    formattedDueDate: task.formattedDueDate,
                    completed: task.completed,
                    typeOfDueDate: typeof task.dueDate,
                    typeOfFormattedDueDate: typeof task.formattedDueDate
                });
                
                // Test parsing and show results
                console.log('   Testing date parsing...');
                const parsedDate = this.parseDate(task);  // Pass the whole task object
                console.log('   Parsed result:', parsedDate);
                console.log('   Is valid date?', parsedDate && !isNaN(parsedDate.getTime()));
                if (parsedDate && !isNaN(parsedDate.getTime())) {
                    console.log('   Formatted:', parsedDate.toLocaleString());
                }
                console.log('   ---');
            });
            
            this.renderTasks();
            this.updateStatistics();
            this.updateTaskCount();
        } catch (error) {
            console.error('❌ Error loading tasks:', error);
            this.showNotification('Error loading tasks. Make sure backend is running on port 8080.', 'error');
        }
    }

    renderTasks() {
        const container = document.getElementById('tasksContainer');
        const filteredTasks = this.filterTasks(this.tasks);
        const sortedTasks = this.sortTasks(filteredTasks);

        if (sortedTasks.length === 0) {
            container.innerHTML = this.getEmptyStateHTML();
            return;
        }

        container.innerHTML = sortedTasks.map(task => this.createTaskCard(task)).join('');
        this.attachTaskEventListeners();
    }

    createTaskCard(task) {
        console.log(`🎯 Creating card for: ${task.title}`, task);
        
        const dueDate = this.parseDate(task);
        const now = new Date();
        
        let statusClass = '';
        let statusText = '';

        if (dueDate && !isNaN(dueDate.getTime())) {
            const timeDiff = dueDate - now;
            const hoursDiff = timeDiff / (1000 * 60 * 60);

            console.log(`📅 ${task.title}: Hours until due = ${hoursDiff.toFixed(2)}`);

            if (task.completed) {
                statusClass = 'completed';
                statusText = '✅ Completed';
            } else if (hoursDiff < 0) {
                statusClass = 'overdue';
                statusText = '⚠️ Overdue';
            } else if (hoursDiff <= 24) {
                statusClass = 'due-soon';
                statusText = '🔔 Due Soon';
            }
        } else {
            console.error('❌ Invalid date for task:', task.title, 'Date string:', task.formattedDueDate);
        }

        // Format date for display
        let dueDateFormatted = 'Date Error';
        if (dueDate && !isNaN(dueDate.getTime())) {
            dueDateFormatted = dueDate.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        const description = task.description || 'No description';

        return `
            <div class="task-card ${statusClass} ${task.priority.toLowerCase()}-priority" data-task-id="${task.id}">
                <div class="task-header">
                    <div>
                        <div class="task-title ${task.completed ? 'completed' : ''}">
                            ${task.title}
                            ${statusText ? `<span class="status-badge">${statusText}</span>` : ''}
                        </div>
                        <div class="task-priority priority-${task.priority}">${task.priority}</div>
                    </div>
                    <div class="task-actions">
                        <button class="btn btn-success complete-btn" data-task-id="${task.id}">
                            <i class="fas ${task.completed ? 'fa-undo' : 'fa-check'}"></i>
                            ${task.completed ? 'Undo' : 'Complete'}
                        </button>
                        <button class="btn btn-danger delete-btn" data-task-id="${task.id}">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
                <div class="task-description">${description}</div>
                <div class="task-meta">
                    <div class="task-due-date ${statusClass}">
                        <i class="fas fa-clock"></i>
                        Due: ${dueDateFormatted}
                        ${dueDate && !isNaN(dueDate.getTime()) ? 
                            `<br><small>${this.getTimeDifferenceText(dueDate)}</small>` : ''}
                    </div>
                    <div class="task-id">ID: ${task.id}</div>
                </div>
            </div>
        `;
    }

    parseDate(task) {
        console.log('🔍 Looking for date in task:', task);
        
        // Try multiple possible date fields
        const possibleDateFields = [
            task.formattedDueDate,  // Primary field from backend
            task.dueDate,           // Fallback field
            task.dueDateTime,       // Another possible field name
            task.due                // Another possible field name
        ];
        
        for (const dateField of possibleDateFields) {
            if (!dateField) continue;
            
            console.log(`📅 Trying to parse: "${dateField}"`);
            
            try {
                let date;
                
                if (typeof dateField === 'string') {
                    // Handle ISO string
                    if (dateField.includes('T')) {
                        date = new Date(dateField);
                    } 
                    // Handle timestamp
                    else if (!isNaN(dateField)) {
                        date = new Date(parseInt(dateField));
                    }
                    // Try direct parsing
                    else {
                        date = new Date(dateField);
                    }
                } 
                // Handle date array from Jackson serialization [year, month, day, hour, minute, second]
                else if (Array.isArray(dateField) && dateField.length >= 3) {
                    const [year, month, day, hour = 0, minute = 0, second = 0] = dateField;
                    date = new Date(year, month - 1, day, hour, minute, second);
                }
                // Handle date object
                else if (typeof dateField === 'object' && dateField !== null) {
                    if (dateField.year && dateField.month && dateField.day) {
                        date = new Date(dateField.year, dateField.month - 1, dateField.day, 
                                      dateField.hour || 0, dateField.minute || 0, dateField.second || 0);
                    }
                }
                
                if (date && !isNaN(date.getTime())) {
                    console.log('✅ Successfully parsed date:', date);
                    return date;
                }
            } catch (error) {
                console.log('❌ Failed to parse:', dateField, error);
                continue;
            }
        }
        
        console.error('❌ No valid date found in task:', task);
        return null;
    }

    // FIX: Add missing getTimeDifferenceText method
    getTimeDifferenceText(dueDate) {
        const now = new Date();
        const timeDiff = dueDate - now;
        const daysDiff = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
        const hoursDiff = Math.floor((timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

        if (timeDiff < 0) {
            const absDays = Math.abs(daysDiff);
            const absHours = Math.abs(hoursDiff);
            if (absDays > 0) {
                return `${absDays} day${absDays !== 1 ? 's' : ''} overdue`;
            } else {
                return `${absHours} hour${absHours !== 1 ? 's' : ''} overdue`;
            }
        } else {
            if (daysDiff > 0) {
                return `in ${daysDiff} day${daysDiff !== 1 ? 's' : ''}`;
            } else {
                return `in ${hoursDiff} hour${hoursDiff !== 1 ? 's' : ''}`;
            }
        }
    }

    updateStatistics() {
        console.log('📊 CALCULATING STATISTICS ==============');
        const now = new Date();
        let dueSoonCount = 0;
        let overdueCount = 0;
        let completedCount = 0;

        this.tasks.forEach(task => {
            console.log(`Processing: "${task.title}"`, {
                completed: task.completed,
                formattedDueDate: task.formattedDueDate
            });
            
            if (task.completed) {
                completedCount++;
                console.log(`  ✅ Counted as COMPLETED`);
                return;
            }

            const dueDate = this.parseDate(task);
            if (!dueDate || isNaN(dueDate.getTime())) {
                console.log(`  ❌ Skipped - invalid date`);
                return;
            }

            const timeDiff = dueDate - now;
            const hoursDiff = timeDiff / (1000 * 60 * 60);

            console.log(`  📅 Date: ${dueDate.toLocaleString()}, Hours diff: ${hoursDiff.toFixed(2)}`);

            if (hoursDiff < 0) {
                overdueCount++;
                console.log(`  🚨 Counted as OVERDUE`);
            } else if (hoursDiff <= 24) {
                dueSoonCount++;
                console.log(`  ⏰ Counted as DUE SOON`);
            } else {
                console.log(`  📅 Counted as FUTURE`);
            }
        });

        console.log(`📊 FINAL COUNTS: Due Soon=${dueSoonCount}, Overdue=${overdueCount}, Completed=${completedCount}`);
        console.log('=====================================');

        // Update UI
        document.getElementById('dueSoonCount').textContent = dueSoonCount;
        document.getElementById('overdueCount').textContent = overdueCount;
        document.getElementById('completedCount').textContent = completedCount;
    }

    updateTaskCount() {
        const totalTasks = this.tasks.length;
        const pendingTasks = this.tasks.filter(task => !task.completed).length;
        
        document.getElementById('totalTasks').textContent = `${totalTasks} task${totalTasks !== 1 ? 's' : ''}`;
        document.getElementById('pendingTasks').textContent = `${pendingTasks} pending`;
    }

    attachTaskEventListeners() {
        document.querySelectorAll('.complete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const taskId = e.currentTarget.dataset.taskId;
                this.handleCompleteTask(taskId);
            });
        });

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const taskId = e.currentTarget.dataset.taskId;
                this.handleDeleteTask(taskId);
            });
        });
    }

    async handleAddTask(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const dueDateInput = formData.get('dueDate');
        
        // FIX: Better date formatting for backend
        let formattedDueDate = dueDateInput;
        if (dueDateInput) {
            // Convert "YYYY-MM-DDTHH:mm" to "YYYY-MM-DDTHH:mm:00" for backend
            formattedDueDate = dueDateInput + ':00';
        }
        
        const taskData = {
            title: formData.get('title'),
            description: formData.get('description'),
            dueDate: formattedDueDate,
            priority: formData.get('priority')
        };

        console.log('📝 Adding task with data:', taskData);

        if (!taskData.title || taskData.title.trim() === '') {
            this.showNotification('Task title is required!', 'error');
            return;
        }

        if (!taskData.dueDate) {
            this.showNotification('Due date is required!', 'error');
            return;
        }

        try {
            await this.taskService.createTask(taskData);
            this.showNotification('Task created successfully!', 'success');
            this.clearForm();
            await this.loadTasks();
        } catch (error) {
            console.error('Backend error:', error);
            this.showNotification('Error creating task: ' + error.message, 'error');
        }
    }

    async handleCompleteTask(taskId) {
        const task = this.tasks.find(t => t.id == taskId);
        if (!task) return;

        try {
            await this.taskService.toggleTaskCompletion(taskId, !task.completed);
            this.showNotification(`Task ${!task.completed ? 'completed' : 'marked as pending'}!`, 'success');
            await this.loadTasks();
        } catch (error) {
            this.showNotification('Error updating task', 'error');
        }
    }

    async handleDeleteTask(taskId) {
        if (!confirm('Are you sure you want to delete this task?')) return;

        try {
            await this.taskService.deleteTask(taskId);
            this.showNotification('Task deleted successfully!', 'success');
            await this.loadTasks();
        } catch (error) {
            this.showNotification('Error deleting task', 'error');
        }
    }

    handleFilter(e) {
        document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
        e.target.classList.add('active');
        
        this.currentFilter = e.target.dataset.filter;
        this.renderTasks();
    }

    handleSearch(e) {
        const searchTerm = e.target.value.toLowerCase();
        const taskCards = document.querySelectorAll('.task-card');
        
        taskCards.forEach(card => {
            const title = card.querySelector('.task-title').textContent.toLowerCase();
            const description = card.querySelector('.task-description').textContent.toLowerCase();
            
            if (title.includes(searchTerm) || description.includes(searchTerm)) {
                card.style.display = 'block';
            } else {
                card.style.display = 'none';
            }
        });
    }

    handleSort() {
        const sortOptions = ['dueDate', 'priority', 'title'];
        const currentIndex = sortOptions.indexOf(this.currentSort);
        this.currentSort = sortOptions[(currentIndex + 1) % sortOptions.length];
        
        this.renderTasks();
        this.showNotification(`Sorted by ${this.currentSort}`, 'success');
    }

    filterTasks(tasks) {
        const now = new Date();
        
        switch (this.currentFilter) {
            case 'pending':
                return tasks.filter(task => !task.completed);
            case 'completed':
                return tasks.filter(task => task.completed);
            case 'overdue':
                return tasks.filter(task => {
                    if (task.completed) return false;
                    const dueDate = this.parseDate(task);
                    return dueDate && dueDate < now;
                });
            case 'dueSoon':
                const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
                return tasks.filter(task => {
                    if (task.completed) return false;
                    const dueDate = this.parseDate(task);
                    return dueDate && dueDate > now && dueDate <= tomorrow;
                });
            default:
                return tasks;
        }
    }

    sortTasks(tasks) {
        return tasks.sort((a, b) => {
            switch (this.currentSort) {
                case 'dueDate':
                    // FIX: Use parseDate(task) instead of parseDate(task.formattedDueDate)
                    const dateA = this.parseDate(a);
                    const dateB = this.parseDate(b);
                    return (dateA || new Date(0)) - (dateB || new Date(0));
                case 'priority':
                    const priorityOrder = { HIGH: 0, MEDIUM: 1, LOW: 2 };
                    return priorityOrder[a.priority] - priorityOrder[b.priority];
                case 'title':
                    return a.title.localeCompare(b.title);
                default:
                    return 0;
            }
        });
    }

    getEmptyStateHTML() {
        return `
            <div class="empty-state">
                <i class="fas fa-clipboard-list"></i>
                <h3>No tasks yet</h3>
                <p>Add your first task to get started!</p>
            </div>
        `;
    }

    clearForm() {
        document.getElementById('taskForm').reset();
    }

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 5px;
            color: white;
            font-weight: bold;
            z-index: 1000;
            background: ${type === 'error' ? '#e63946' : type === 'success' ? '#4cc9f0' : '#4361ee'};
        `;
        notification.textContent = message;
        document.body.appendChild(notification);
        
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 3000);
    }
}