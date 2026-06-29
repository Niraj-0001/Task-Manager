package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Task
import com.example.data.TaskPriority
import java.text.SimpleDateFormat
import java.util.*

// Pristine Dynamic Theme Colors (Frosted Glass Theme supporting Light & Dark modes)
data class ThemeColors(
    val bg: Color,
    val surface: Color,
    val border: Color,
    val primary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LightThemeColors = ThemeColors(
    bg = Color(0xFFFDF7FF), // Soft light lavender-grey backdrop background
    surface = Color(0xE6FFFFFF), // Translucent glass card background (90% opacity white)
    border = Color(0x3B6750A4), // Soft light translucent lavender border
    primary = Color(0xFF6750A4), // Primary deep royal purple
    accent = Color(0xFFD0BCFF), // Accent lilac
    textPrimary = Color(0xFF21005D), // Deep dark elegant purple
    textSecondary = Color(0xFF49454F), // Medium dark gray for captions
    isDark = false
)

val DarkThemeColors = ThemeColors(
    bg = Color(0xFF0C0E14), // Deep rich midnight obsidian backdrop background
    surface = Color(0xE6161923), // Translucent deep grey glass card (90% opacity)
    border = Color(0x24FFFFFF), // Subtle translucent white border
    primary = Color(0xFFD4AF37), // Luxury gold color (instead of cheap teal)
    accent = Color(0xFFE5C158), // Premium warm champagne/bronze accent
    textPrimary = Color(0xFFFFFFFF), // Pristine white for readable titles
    textSecondary = Color(0xFF90A4AE), // Cool slate silver-gray for descriptions/subtexts
    isDark = true
)

val LocalThemeColors = staticCompositionLocalOf { LightThemeColors }

// Category visual models
data class CategoryModel(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

val defaultCategories = listOf(
    CategoryModel("Study", Icons.Rounded.School, Color(0xFFD4AF37)), // Changed from Work to Study, color to luxury gold
    CategoryModel("Personal", Icons.Rounded.Person, Color(0xFF10B981)),
    CategoryModel("Wellness", Icons.Rounded.Favorite, Color(0xFF8B5CF6)),
    CategoryModel("Finance", Icons.Rounded.Payments, Color(0xFFF59E0B)),
    CategoryModel("Urgent", Icons.Rounded.Warning, Color(0xFFEF4444))
)

fun getCategoryColor(category: String): Color {
    return defaultCategories.find { it.name.equals(category, ignoreCase = true) }?.color ?: Color(0xFF94A3B8)
}

fun getCategoryIcon(category: String): ImageVector {
    return defaultCategories.find { it.name.equals(category, ignoreCase = true) }?.icon ?: Icons.Rounded.Label
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val allTasksList by viewModel.allTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val selectedStat by viewModel.selectedStatus.collectAsState()
    val dayOffset by viewModel.selectedDayOffset.collectAsState()
    val starredOnly by viewModel.filterStarredOnly.collectAsState()
    val showAddSheet by viewModel.showAddTaskSheet.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()

    // Focus session states
    val focusType by viewModel.focusType.collectAsState()
    val isFocusRunning by viewModel.isFocusRunning.collectAsState()
    val timerPresetSeconds by viewModel.timerPresetSeconds.collectAsState()
    val focusTimeRemaining by viewModel.focusTimeRemaining.collectAsState()
    val focusTimeElapsed by viewModel.focusTimeElapsed.collectAsState()
    val selectedFocusTask by viewModel.selectedFocusTask.collectAsState()
    val showFullScreenFocus by viewModel.showFullScreenFocus.collectAsState()

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val themeColors = if (isDarkTheme) DarkThemeColors else LightThemeColors

    // Determine orientation and responsive layout
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        val colors = LocalThemeColors.current
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openAddSheet() },
                    containerColor = colors.primary,
                    contentColor = if (colors.isDark) Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("add_task_fab")
                        .padding(8.dp)
                        .shadow(12.dp, shape = CircleShape, ambientColor = colors.primary, spotColor = colors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = colors.bg,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Soft theme-aware atmospheric top gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = if (colors.isDark) {
                                    listOf(Color(0xFF1B1437), Color.Transparent)
                                } else {
                                    listOf(Color(0xFFEADDFF), Color.Transparent)
                                },
                                startY = 0f,
                                endY = size.height * 0.45f
                            )
                        )
                        // Soft theme-aware glowing aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = if (colors.isDark) {
                                    listOf(Color(0xFF241441), Color.Transparent)
                                } else {
                                    listOf(Color(0xFFF3EDF7), Color.Transparent)
                                },
                                center = Offset(size.width * 0.85f, size.height * 0.2f),
                                radius = size.width * 0.7f
                            )
                        )
                    }
            ) {
                if (isTablet) {
                    // Wide Landscape / Tablet layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Stats, Focus Session & Quick Actions
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeaderSection(viewModel)
                            StatsPanel(allTasksList)
                            FocusSessionCard(
                                focusType = focusType,
                                isFocusRunning = isFocusRunning,
                                timerPresetSeconds = timerPresetSeconds,
                                focusTimeRemaining = focusTimeRemaining,
                                focusTimeElapsed = focusTimeElapsed,
                                selectedFocusTask = selectedFocusTask,
                                allTasks = allTasksList,
                                onSelectFocusTask = { viewModel.selectFocusTask(it) },
                                onOpenFullScreen = { viewModel.setShowFullScreenFocus(true) },
                                onSetType = { viewModel.setFocusType(it) },
                                onSetPreset = { viewModel.setTimerPreset(it) },
                                onStart = { viewModel.startFocusSession() },
                                onPause = { viewModel.pauseFocusSession() },
                                onStop = { viewModel.stopFocusSession() }
                            )
                            CategorySelector(selectedCat) { viewModel.selectedCategory.value = it }
                        }

                        // Right Column: Task List, Search & Filters
                        Column(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SearchAndFilters(
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.searchQuery.value = it },
                                selectedStat = selectedStat,
                                onStatChange = { viewModel.selectedStatus.value = it },
                                starredOnly = starredOnly,
                                onStarredToggle = { viewModel.filterStarredOnly.value = !starredOnly }
                            )

                            DateFilterRow(dayOffset) { viewModel.selectedDayOffset.value = it }

                            TaskList(
                                tasks = tasks,
                                modifier = Modifier.weight(1f),
                                onTaskChecked = { viewModel.toggleTaskCompletion(it) },
                                onTaskStarred = { viewModel.toggleTaskStarred(it) },
                                onTaskEdit = { viewModel.setEditingTask(it) },
                                onTaskDelete = { viewModel.deleteTask(it) }
                            )
                        }
                    }
                } else {
                    // Compact Phone Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeaderSection(viewModel)
                        StatsPanel(allTasksList)
                        FocusSessionCard(
                            focusType = focusType,
                            isFocusRunning = isFocusRunning,
                            timerPresetSeconds = timerPresetSeconds,
                            focusTimeRemaining = focusTimeRemaining,
                            focusTimeElapsed = focusTimeElapsed,
                            selectedFocusTask = selectedFocusTask,
                            allTasks = allTasksList,
                            onSelectFocusTask = { viewModel.selectFocusTask(it) },
                            onOpenFullScreen = { viewModel.setShowFullScreenFocus(true) },
                            onSetType = { viewModel.setFocusType(it) },
                            onSetPreset = { viewModel.setTimerPreset(it) },
                            onStart = { viewModel.startFocusSession() },
                            onPause = { viewModel.pauseFocusSession() },
                            onStop = { viewModel.stopFocusSession() }
                        )
                        SearchAndFilters(
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.searchQuery.value = it },
                            selectedStat = selectedStat,
                            onStatChange = { viewModel.selectedStatus.value = it },
                            starredOnly = starredOnly,
                            onStarredToggle = { viewModel.filterStarredOnly.value = !starredOnly }
                        )
                        DateFilterRow(dayOffset) { viewModel.selectedDayOffset.value = it }
                        CategorySelector(selectedCat) { viewModel.selectedCategory.value = it }

                        TaskList(
                            tasks = tasks,
                            modifier = Modifier.weight(1f),
                            onTaskChecked = { viewModel.toggleTaskCompletion(it) },
                            onTaskStarred = { viewModel.toggleTaskStarred(it) },
                            onTaskEdit = { viewModel.setEditingTask(it) },
                            onTaskDelete = { viewModel.deleteTask(it) }
                        )
                    }
                }

            // Elegant Custom Animated Form Sheet Overlay (replaces BottomSheet for single-view absolute safety)
            AnimatedVisibility(
                visible = showAddSheet,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                TaskFormOverlay(
                    editingTask = editingTask,
                    onDismiss = { viewModel.closeSheet() },
                    onSave = { title, desc, category, priority, date ->
                        if (editingTask != null) {
                            viewModel.updateTask(
                                editingTask!!.copy(
                                    title = title,
                                    description = desc,
                                    category = category,
                                    priority = priority,
                                    dueDateMillis = date
                                )
                            )
                        } else {
                            viewModel.addTask(title, desc, category, priority, date)
                        }
                        viewModel.closeSheet()
                    }
                )
            }

            if (showFullScreenFocus) {
                FullScreenFocusOverlay(
                    viewModel = viewModel,
                    onClose = { viewModel.setShowFullScreenFocus(false) }
                )
            }
        }
    }
}
}

@Composable
fun HeaderSection(viewModel: TaskViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val colors = LocalThemeColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "My Day",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                letterSpacing = (-1.5).sp
            )
            val today = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()) }
            Text(
                text = today,
                fontSize = 14.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(
            onClick = { viewModel.toggleTheme() },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StatsPanel(allTasks: List<Task>) {
    val completedCount = allTasks.count { it.isCompleted }
    val totalCount = allTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animateProgress by animateFloatAsState(targetValue = progress, animationSpec = spring())
    val colors = LocalThemeColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aesthetic Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalCount > 0) {
                        "$completedCount of $totalCount tasks completed"
                    } else {
                        "No tasks added yet"
                    },
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (progress == 1f) colors.primary.copy(alpha = 0.2f) else colors.accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = if (progress == 1f) "All Clear! 🎉" else "Keep pushing! ⚡",
                            color = if (progress == 1f) colors.primary else colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
            ) {
                // Smooth Custom Progress Circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track circle
                    drawCircle(
                        color = colors.border.copy(alpha = 0.5f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Animated Progress path
                    drawArc(
                        color = colors.primary,
                        startAngle = -90f,
                        sweepAngle = animateProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(animateProgress * 100).toInt()}%",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStat: TaskStatusFilter,
    onStatChange: (TaskStatusFilter) -> Unit,
    starredOnly: Boolean,
    onStarredToggle: () -> Unit
) {
    val colors = LocalThemeColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search tasks...", color = colors.textSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = colors.textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("task_search_input")
            )

            // Starred toggle
            IconButton(
                onClick = onStarredToggle,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (starredOnly) colors.accent.copy(alpha = 0.2f) else colors.surface)
                    .border(1.dp, if (starredOnly) colors.primary else colors.border, RoundedCornerShape(16.dp))
                    .testTag("star_filter_button")
            ) {
                Icon(
                    imageVector = if (starredOnly) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Starred Filter",
                    tint = if (starredOnly) colors.primary else colors.textSecondary
                )
            }
        }

        // Custom M3 Status Selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TaskStatusFilter.values().forEach { status ->
                val isSelected = selectedStat == status
                val bg by animateColorAsState(targetValue = if (isSelected) colors.bg else Color.Transparent)
                val tc by animateColorAsState(targetValue = if (isSelected) colors.primary else colors.textSecondary)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onStatChange(status) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tc
                    )
                }
            }
        }
    }
}

@Composable
fun DateFilterRow(
    selectedOffset: Int,
    onOffsetChange: (Int) -> Unit
) {
    val colors = LocalThemeColors.current
    val items = listOf(
        -1 to "All Days",
        0 to "Today",
        1 to "Tomorrow",
        2 to "Upcoming"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items) { (offset, label) ->
            val isSelected = selectedOffset == offset
            val bg by animateColorAsState(targetValue = if (isSelected) colors.primary else colors.surface)
            val tc by animateColorAsState(targetValue = if (isSelected) Color.White else colors.textPrimary)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(12.dp))
                    .clickable { onOffsetChange(offset) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = tc,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    val colors = LocalThemeColors.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            val isSelected = selectedCategory == "All"
            CategoryChip(
                name = "All",
                icon = Icons.Rounded.Category,
                color = colors.primary,
                isSelected = isSelected,
                onClick = { onCategoryChange("All") }
            )
        }
        items(defaultCategories) { cat ->
            val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
            CategoryChip(
                name = cat.name,
                icon = cat.icon,
                color = cat.color,
                isSelected = isSelected,
                onClick = { onCategoryChange(cat.name) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalThemeColors.current
    val bg by animateColorAsState(targetValue = if (isSelected) color.copy(alpha = 0.25f) else colors.surface)
    val borderCol by animateColorAsState(targetValue = if (isSelected) color else colors.border)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = name,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onTaskChecked: (Task) -> Unit,
    onTaskStarred: (Task) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit
) {
    val colors = LocalThemeColors.current

    if (tasks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.15f),
                        radius = size.width * 0.45f,
                        center = center
                    )
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.1f),
                        radius = size.width * 0.35f,
                        center = center
                    )
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.4f),
                        radius = size.width * 0.45f,
                        center = center,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(10f, 10f), 0f
                            )
                        )
                    )
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.35f, size.height * 0.5f)
                        lineTo(size.width * 0.47f, size.height * 0.62f)
                        lineTo(size.width * 0.68f, size.height * 0.38f)
                    }
                    drawPath(
                        path = path,
                        color = colors.primary,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "Aesthetic Peace",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All tasks completed or matched out",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onChecked = { onTaskChecked(task) },
                    onStarred = { onTaskStarred(task) },
                    onEdit = { onTaskEdit(task) },
                    onDelete = { onTaskDelete(task) }
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onChecked: () -> Unit,
    onStarred: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalThemeColors.current
    val categoryColor = getCategoryColor(task.category)
    val animateStrike by animateFloatAsState(targetValue = if (task.isCompleted) 1f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onChecked,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .border(
                        2.dp,
                        if (task.isCompleted) colors.primary else colors.textSecondary.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(1.dp, categoryColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(task.category),
                                contentDescription = task.category,
                                tint = categoryColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = task.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        }
                    }

                    val priorityColor = when (task.priority) {
                        TaskPriority.HIGH -> Color(0xFFEF4444)
                        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                        TaskPriority.LOW -> Color(0xFF3B82F6)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) colors.textSecondary else colors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = "Due Date",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    val dateFormatted = remember(task.dueDateMillis) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        sdf.format(Date(task.dueDateMillis))
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onStarred,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Star Task",
                        tint = if (task.isStarred) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormOverlay(
    editingTask: Task?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, TaskPriority, Long) -> Unit
) {
    val colors = LocalThemeColors.current
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var description by remember { mutableStateOf(editingTask?.description ?: "") }
    var category by remember { mutableStateOf(editingTask?.category ?: "Study") }
    var priority by remember { mutableStateOf(editingTask?.priority ?: TaskPriority.MEDIUM) }
    
    // Set default due date to today
    var selectedDate by remember {
        mutableStateOf(editingTask?.dueDateMillis ?: System.currentTimeMillis())
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (editingTask != null) "Edit Task" else "Create Task",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-1).sp
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task title...", color = colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Task description...", color = colors.textSecondary) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )

                // Category Selection
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaultCategories) { cat ->
                        val isSel = category.equals(cat.name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) cat.color.copy(alpha = 0.25f) else colors.bg)
                                .border(1.dp, if (isSel) cat.color else colors.border, RoundedCornerShape(10.dp))
                                .clickable { category = cat.name }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = cat.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = cat.name,
                                    color = colors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Priority Selector
                Text(
                    text = "Priority",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { pri ->
                        val isSel = priority == pri
                        val col = when (pri) {
                            TaskPriority.HIGH -> Color(0xFFEF4444)
                            TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                            TaskPriority.LOW -> Color(0xFF3B82F6)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) col.copy(alpha = 0.25f) else colors.bg)
                                .border(1.dp, if (isSel) col else colors.border, RoundedCornerShape(10.dp))
                                .clickable { priority = pri }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = pri.name,
                                color = if (isSel) col else colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Due Date Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bg)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Date Selector",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Due Date",
                            color = colors.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                    val formatted = remember(selectedDate) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        sdf.format(Date(selectedDate))
                    }
                    Text(
                        text = formatted,
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty()) {
                                onSave(title, description, category, priority, selectedDate)
                            }
                        },
                        enabled = title.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = if (colors.isDark) Color.Black else Color.White,
                            disabledContainerColor = colors.border,
                            disabledContentColor = colors.textSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_save_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Material 3 Custom Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.primary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = colors.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary,
                    headlineContentColor = colors.textPrimary,
                    selectedDayContainerColor = colors.primary,
                    selectedDayContentColor = if (colors.isDark) Color.Black else Color.White,
                    todayContentColor = colors.primary,
                    todayDateBorderColor = colors.primary,
                    dayContentColor = colors.textPrimary
                )
            )
        }
    }
}

// Helper extension to keep syntax clean
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDatePickerState(
    initialSelectedDateMillis: Long? = null
): DatePickerState {
    return androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )
}

@Composable
fun FocusSessionCard(
    focusType: TaskViewModel.FocusType,
    isFocusRunning: Boolean,
    timerPresetSeconds: Long,
    focusTimeRemaining: Long,
    focusTimeElapsed: Long,
    selectedFocusTask: Task?,
    allTasks: List<Task>,
    onSelectFocusTask: (Task?) -> Unit,
    onOpenFullScreen: () -> Unit,
    onSetType: (TaskViewModel.FocusType) -> Unit,
    onSetPreset: (Long) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showTaskSelectorDialog by remember { mutableStateOf(false) }
    val colors = LocalThemeColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Focus Session Icon",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Focus Session",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        val statusText = if (isFocusRunning) {
                            if (focusType == TaskViewModel.FocusType.TIMER) "Timer running..." else "Stopwatch running..."
                        } else "Ready to Focus"
                        Text(
                            text = if (selectedFocusTask != null) "🎯 ${selectedFocusTask.title}" else statusText,
                            fontSize = 12.sp,
                            color = if (selectedFocusTask != null) colors.primary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isExpanded) {
                        val displaySeconds = if (focusType == TaskViewModel.FocusType.TIMER) focusTimeRemaining else focusTimeElapsed
                        val mins = displaySeconds / 60
                        val secs = displaySeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenFullScreen,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Open Full Screen",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Toggle Expand",
                        tint = colors.textSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.3f)))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.border.copy(alpha = 0.1f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            TaskViewModel.FocusType.TIMER to "Timer",
                            TaskViewModel.FocusType.STOPWATCH to "Stopwatch"
                        ).forEach { (type, label) ->
                            val isSelected = focusType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primary else Color.Transparent)
                                    .clickable { onSetType(type) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) (if (colors.isDark) Color.Black else Color.White) else colors.textPrimary
                                )
                            }
                        }
                    }

                    // Task Linker Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.border.copy(alpha = 0.08f))
                            .border(1.dp, colors.border.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable { showTaskSelectorDialog = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Assignment,
                                    contentDescription = "Task Focus",
                                    tint = if (selectedFocusTask != null) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (selectedFocusTask != null) selectedFocusTask.title else "Select Study Goal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedFocusTask != null) colors.textPrimary else colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (selectedFocusTask != null) "Linked to this focus session" else "Tap to link a task",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            if (selectedFocusTask != null) {
                                IconButton(
                                    onClick = { onSelectFocusTask(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Unlink Task",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Select Task Dropdown",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        val displaySeconds = if (focusType == TaskViewModel.FocusType.TIMER) focusTimeRemaining else focusTimeElapsed
                        val mins = displaySeconds / 60
                        val secs = displaySeconds % 60
                        
                        val progressFraction = if (focusType == TaskViewModel.FocusType.TIMER) {
                            if (timerPresetSeconds > 0) focusTimeRemaining.toFloat() / timerPresetSeconds else 0f
                        } else {
                            (focusTimeElapsed % 60) / 60f
                        }
                        
                        val sweepAngle = progressFraction * 360f

                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawCircle(
                                color = colors.border.copy(alpha = 0.2f),
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = colors.primary,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (focusType == TaskViewModel.FocusType.TIMER) "remaining" else "elapsed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (focusType == TaskViewModel.FocusType.TIMER && !isFocusRunning) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                5 * 60L to "5 Min",
                                15 * 60L to "15 Min",
                                25 * 60L to "25 Min",
                                50 * 60L to "50 Min"
                            ).forEach { (seconds, label) ->
                                val isPresetSelected = timerPresetSeconds == seconds
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPresetSelected) colors.primary.copy(alpha = 0.15f) else colors.border.copy(alpha = 0.05f))
                                        .border(1.dp, if (isPresetSelected) colors.primary else colors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable { onSetPreset(seconds) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPresetSelected) colors.primary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isFocusRunning) {
                            Button(
                                onClick = onStart,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = if (colors.isDark) Color.Black else Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start", fontWeight = FontWeight.Bold, color = if (colors.isDark) Color.Black else Color.White)
                            }
                        } else {
                            Button(
                                onClick = onPause,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.border.copy(alpha = 0.1f))
                                .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset Timer",
                                tint = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Task Selector Dialog
    if (showTaskSelectorDialog) {
        Dialog(onDismissRequest = { showTaskSelectorDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Study Goal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary
                    )
                    
                    val pendingTasks = allTasks.filter { !it.isCompleted }
                    
                    if (pendingTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active study goals available",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingTasks) { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.border.copy(alpha = 0.05f))
                                        .clickable {
                                            onSelectFocusTask(task)
                                            showTaskSelectorDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Assignment,
                                        contentDescription = "Task",
                                        tint = if (task.category.equals("Study", ignoreCase = true)) colors.primary else getCategoryColor(task.category),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = task.category,
                                            fontSize = 10.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = { showTaskSelectorDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenFocusOverlay(
    viewModel: TaskViewModel,
    onClose: () -> Unit
) {
    val colors = LocalThemeColors.current
    val focusType by viewModel.focusType.collectAsState()
    val isFocusRunning by viewModel.isFocusRunning.collectAsState()
    val timerPresetSeconds by viewModel.timerPresetSeconds.collectAsState()
    val focusTimeRemaining by viewModel.focusTimeRemaining.collectAsState()
    val focusTimeElapsed by viewModel.focusTimeElapsed.collectAsState()
    val selectedFocusTask by viewModel.selectedFocusTask.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    
    var showTaskSelectorInFullScreen by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0E14)) // Immersive premium dark luxury background
                .drawBehind {
                    // Deep luxury radial gold glow aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1FDFB237), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.45f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Exit Full Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Study Mode",
                            tint = Color(0xFFD4AF37), // Luxury Gold
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "IMMERSIVE STUDY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFFD4AF37)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Immersive Mode",
                            tint = Color.White
                        )
                    }
                }
                
                // Mode Toggle: Timer vs Stopwatch
                Row(
                    modifier = Modifier
                        .width(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x11FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        TaskViewModel.FocusType.TIMER to "Timer",
                        TaskViewModel.FocusType.STOPWATCH to "Stopwatch"
                    ).forEach { (type, label) ->
                        val isSelected = focusType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFD4AF37) else Color.Transparent)
                                .clickable { viewModel.setFocusType(type) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
                
                // Big Circular Timer Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    val displaySeconds = if (focusType == TaskViewModel.FocusType.TIMER) focusTimeRemaining else focusTimeElapsed
                    val mins = displaySeconds / 60
                    val secs = displaySeconds % 60
                    
                    val progressFraction = if (focusType == TaskViewModel.FocusType.TIMER) {
                        if (timerPresetSeconds > 0) focusTimeRemaining.toFloat() / timerPresetSeconds else 0f
                    } else {
                        (focusTimeElapsed % 60) / 60f
                    }
                    val sweepAngle = progressFraction * 360f

                    Canvas(modifier = Modifier.size(260.dp)) {
                        drawCircle(
                            color = Color(0x12FFFFFF),
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFFD4AF37), // Luxury gold
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (focusType == TaskViewModel.FocusType.TIMER) "MINUTES REMAINING" else "SECONDS ELAPSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xAAFFFFFF)
                        )
                    }
                }
                
                // Focus Task details (with optional completion checkbox!)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedFocusTask != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x15FFFFFF))
                                .border(1.dp, Color(0x1FDFB237), RoundedCornerShape(16.dp))
                                .clickable { showTaskSelectorInFullScreen = true }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.toggleTaskCompletion(selectedFocusTask!!)
                                    viewModel.selectFocusTask(null) // clear focus session task
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Complete Task",
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = selectedFocusTask!!.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                                Text(
                                    text = "Tap to change study goal",
                                    fontSize = 10.sp,
                                    color = Color(0x88FFFFFF)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x0AFFFFFF))
                                .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(16.dp))
                                .clickable { showTaskSelectorInFullScreen = true }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Assignment,
                                    contentDescription = "No Goal Linked",
                                    tint = Color(0x66FFFFFF)
                                )
                                Text(
                                    text = "Link a Study Goal... 🎯",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xCCFFFFFF)
                                )
                            }
                        }
                    }
                }
                
                // Quick Presets (Timer mode only)
                if (focusType == TaskViewModel.FocusType.TIMER && !isFocusRunning) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(320.dp)
                    ) {
                        listOf(
                            5 * 60L to "5m",
                            15 * 60L to "15m",
                            25 * 60L to "25m",
                            50 * 60L to "50m"
                        ).forEach { (seconds, label) ->
                            val isPresetSelected = timerPresetSeconds == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPresetSelected) Color(0x33DFB237) else Color(0x0AFFFFFF))
                                    .border(1.dp, if (isPresetSelected) Color(0xFFD4AF37) else Color(0x15FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setTimerPreset(seconds) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPresetSelected) Color(0xFFD4AF37) else Color(0xCCFFFFFF)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(30.dp))
                }

                // Media Control Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    // Reset Button
                    IconButton(
                        onClick = { viewModel.stopFocusSession() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x0DFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset Session",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Start/Pause Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4AF37))
                            .clickable {
                                if (isFocusRunning) viewModel.pauseFocusSession() else viewModel.startFocusSession()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFocusRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isFocusRunning) "Pause" else "Start",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Dynamic spacing
                    Spacer(modifier = Modifier.width(56.dp))
                }
            }
        }
    }

    // Dynamic Task Selector Dialogue inside Full Screen overlay
    if (showTaskSelectorInFullScreen) {
        Dialog(onDismissRequest = { showTaskSelectorInFullScreen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161923)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Link Study Goal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    val pendingTasks = allTasks.filter { !it.isCompleted }
                    
                    if (pendingTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active study goals available",
                                fontSize = 13.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingTasks) { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0DFFFFFF))
                                        .clickable {
                                            viewModel.selectFocusTask(task)
                                            showTaskSelectorInFullScreen = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Assignment,
                                        contentDescription = "Task",
                                        tint = if (task.category.equals("Study", ignoreCase = true)) Color(0xFFD4AF37) else getCategoryColor(task.category),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = task.category,
                                            fontSize = 10.sp,
                                            color = Color(0x88FFFFFF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedFocusTask != null) {
                            TextButton(
                                onClick = {
                                    viewModel.selectFocusTask(null)
                                    showTaskSelectorInFullScreen = false
                                }
                            ) {
                                Text("Unlink Goal", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(
                            onClick = { showTaskSelectorInFullScreen = false }
                        ) {
                            Text("Close", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
