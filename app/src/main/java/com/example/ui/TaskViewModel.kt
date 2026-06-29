package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Converters
import com.example.data.Task
import com.example.data.TaskDatabase
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskStatusFilter {
    ALL, PENDING, COMPLETED
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    init {
        val database = TaskDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
    }

    // Raw tasks flow from Room
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedStatus = MutableStateFlow(TaskStatusFilter.ALL)
    
    // Day filter: Selected day offset from today (0 = Today, 1 = Tomorrow, 2 = Day after, etc.). -1 means All Days.
    val selectedDayOffset = MutableStateFlow(-1)

    // Starred filter
    val filterStarredOnly = MutableStateFlow(false)

    // Form / Sheet UI State
    val showAddTaskSheet = MutableStateFlow(false)
    val editingTask = MutableStateFlow<Task?>(null)

    // Derived filtered tasks flow
    val filteredTasks: StateFlow<List<Task>> = combine(
        combine(allTasks, searchQuery, selectedCategory) { tasks, query, category ->
            Triple(tasks, query, category)
        },
        combine(selectedStatus, selectedDayOffset, filterStarredOnly) { status, dayOffset, starredOnly ->
            Triple(status, dayOffset, starredOnly)
        }
    ) { left, right ->
        val (tasks, query, category) = left
        val (status, dayOffset, starredOnly) = right

        tasks.filter { task ->
            // Search query filter
            val matchesQuery = task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            
            // Category filter
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)

            // Status filter
            val matchesStatus = when (status) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.PENDING -> !task.isCompleted
                TaskStatusFilter.COMPLETED -> task.isCompleted
            }

            // Starred filter
            val matchesStarred = !starredOnly || task.isStarred

            // Date filter
            val matchesDate = if (dayOffset == -1) {
                true
            } else {
                isTaskOnOffsetDay(task.dueDateMillis, dayOffset)
            }

            matchesQuery && matchesCategory && matchesStatus && matchesStarred && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun isTaskOnOffsetDay(dueDateMillis: Long, offset: Int): Boolean {
        val targetCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }
        val taskCal = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
        }
        return targetCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)
    }

    // Actions
    fun addTask(
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDateMillis: Long
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDateMillis = dueDateMillis
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun toggleTaskStarred(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isStarred = !task.isStarred))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun deleteTaskById(id: Long) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    fun setEditingTask(task: Task?) {
        editingTask.value = task
        showAddTaskSheet.value = task != null
    }

    fun closeSheet() {
        showAddTaskSheet.value = false
        editingTask.value = null
    }

    fun openAddSheet() {
        editingTask.value = null
        showAddTaskSheet.value = true
    }

    // --- Theme State ---
    val isDarkTheme = MutableStateFlow(false)

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    // --- Focus Session States ---
    enum class FocusType {
        TIMER, STOPWATCH
    }
    val focusType = MutableStateFlow(FocusType.TIMER)
    val isFocusRunning = MutableStateFlow(false)
    val timerPresetSeconds = MutableStateFlow(25 * 60L) // 25 mins default Pomodoro
    val focusTimeRemaining = MutableStateFlow(25 * 60L) // timer countdown
    val focusTimeElapsed = MutableStateFlow(0L) // stopwatch or session tracking
    val selectedFocusTask = MutableStateFlow<Task?>(null)
    val showFullScreenFocus = MutableStateFlow(false)
    
    private var focusJob: kotlinx.coroutines.Job? = null

    fun selectFocusTask(task: Task?) {
        selectedFocusTask.value = task
    }

    fun setShowFullScreenFocus(show: Boolean) {
        showFullScreenFocus.value = show
    }

    fun setFocusType(type: FocusType) {
        if (isFocusRunning.value) pauseFocusSession()
        focusType.value = type
        if (type == FocusType.TIMER) {
            focusTimeRemaining.value = timerPresetSeconds.value
        } else {
            focusTimeElapsed.value = 0L
        }
    }

    fun setTimerPreset(seconds: Long) {
        timerPresetSeconds.value = seconds
        if (focusType.value == FocusType.TIMER && !isFocusRunning.value) {
            focusTimeRemaining.value = seconds
        }
    }

    fun startFocusSession() {
        if (isFocusRunning.value) return
        isFocusRunning.value = true
        focusJob = viewModelScope.launch {
            while (isFocusRunning.value) {
                kotlinx.coroutines.delay(1000)
                if (focusType.value == FocusType.TIMER) {
                    if (focusTimeRemaining.value > 0) {
                        focusTimeRemaining.value -= 1
                    } else {
                        isFocusRunning.value = false
                    }
                } else {
                    focusTimeElapsed.value += 1
                }
            }
        }
    }

    fun pauseFocusSession() {
        isFocusRunning.value = false
        focusJob?.cancel()
    }

    fun stopFocusSession() {
        isFocusRunning.value = false
        focusJob?.cancel()
        if (focusType.value == FocusType.TIMER) {
            focusTimeRemaining.value = timerPresetSeconds.value
        } else {
            focusTimeElapsed.value = 0L
        }
    }
}

class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
