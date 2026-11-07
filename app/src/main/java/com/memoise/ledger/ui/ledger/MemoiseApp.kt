package com.memoise.ledger.ui.ledger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.memoise.ledger.domain.model.DatePurpose
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.LedgerEntry
import com.memoise.ledger.domain.model.LedgerActivity
import com.memoise.ledger.domain.model.LedgerChecklistItem
import com.memoise.ledger.domain.model.LedgerDescriptionBlock
import com.memoise.ledger.domain.model.LedgerNote
import com.memoise.ledger.domain.model.LedgerRelationship
import com.memoise.ledger.domain.model.RelationshipType
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TIMELINE_ROUTE = "timeline"
private const val UNSCHEDULED_ROUTE = "unscheduled"
private const val OVERDUE_ROUTE = "overdue"
private const val ENTRY_ROUTE = "entry/{entryId}"
private const val CAPTURE_ROUTE = "capture"
private const val RELATED_CAPTURE_ROUTE =
    "related-capture/{parentId}/{relationshipType}?sourceContentId={sourceContentId}&sourceChecklistItemIds={sourceChecklistItemIds}"

private enum class PrimaryDestination(val route: String, val label: String, val glyph: String) {
    TIMELINE(TIMELINE_ROUTE, "Timeline", "◷"),
    UNSCHEDULED(UNSCHEDULED_ROUTE, "Unscheduled", "○"),
    OVERDUE(OVERDUE_ROUTE, "Overdue", "!"),
}

@Composable
fun MemoiseApp(viewModel: EntryViewModel) {
    val entries by viewModel.entries.collectAsState()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showPrimaryNavigation = currentRoute in setOf(
        TIMELINE_ROUTE,
        UNSCHEDULED_ROUTE,
        OVERDUE_ROUTE,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showPrimaryNavigation) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    PrimaryDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(TIMELINE_ROUTE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Text(
                                    text = destination.glyph,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showPrimaryNavigation) {
                FloatingActionButton(
                    onClick = { navController.navigate(CAPTURE_ROUTE) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text("+", fontSize = 30.sp, lineHeight = 32.sp)
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = TIMELINE_ROUTE,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(TIMELINE_ROUTE) {
                TimelineScreen(
                    entries = entries.filter { it.scheduledDate != null },
                    onOpenEntry = { navController.navigate("entry/${it.id}") },
                )
            }
            composable(UNSCHEDULED_ROUTE) {
                UnscheduledScreen(
                    entries = entries.filter { it.scheduledDate == null },
                    onOpenEntry = { navController.navigate("entry/${it.id}") },
                )
            }
            composable(OVERDUE_ROUTE) {
                val today = LocalDate.now()
                OverdueScreen(
                    entries = entries.filter { entry ->
                        entry.scheduledDate?.isBefore(today) == true &&
                            entry.datePurpose != DatePurpose.EVENT &&
                            entry.status != EntryStatus.COMPLETED &&
                            entry.status != EntryStatus.CANCELLED
                    }.sortedByDescending { it.scheduledDate },
                    onOpenEntry = { navController.navigate("entry/${it.id}") },
                )
            }
            composable(
                route = ENTRY_ROUTE,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) { routeEntry ->
                val entryId = routeEntry.arguments?.getString("entryId")
                val entry = entries.firstOrNull { it.id == entryId }
                if (entry == null) {
                    MissingEntryScreen(onBack = { navController.popBackStack() })
                } else {
                    val relationships by remember(entry.id) {
                        viewModel.relationshipsFor(entry.id)
                    }.collectAsState(initial = emptyList())
                    val checklist by remember(entry.id) {
                        viewModel.checklistFor(entry.id)
                    }.collectAsState(initial = emptyList())
                    val activities by remember(entry.id) {
                        viewModel.activitiesFor(entry.id)
                    }.collectAsState(initial = emptyList())
                    val descriptionBlocks by remember(entry.id) {
                        viewModel.descriptionBlocksFor(entry.id)
                    }.collectAsState(initial = emptyList())
                    val notes by remember(entry.id) {
                        viewModel.notesFor(entry.id)
                    }.collectAsState(initial = emptyList())
                    EntryDetailScreen(
                        entry = entry,
                        entries = entries,
                        relationships = relationships,
                        checklist = checklist,
                        activities = activities,
                        descriptionBlocks = descriptionBlocks,
                        notes = notes,
                        onUpdate = viewModel::update,
                        onSetStatus = viewModel::setStatus,
                        onSetDatePurpose = viewModel::setDatePurpose,
                        onAddChecklistItem = viewModel::addChecklistItem,
                        onUpdateChecklistItem = viewModel::updateChecklistItem,
                        onDeleteChecklistItem = viewModel::deleteChecklistItem,
                        onAddDescriptionBlock = viewModel::addDescriptionBlock,
                        onUpdateDescriptionBlock = viewModel::updateDescriptionBlock,
                        onDeleteDescriptionBlock = viewModel::deleteDescriptionBlock,
                        onConvertDescriptionBlock = { type, block ->
                            navController.navigate(
                                "related-capture/${entry.id}/${type.name}?sourceContentId=${block.id}",
                            )
                        },
                        onAddNote = viewModel::addNote,
                        onUpdateNote = viewModel::updateNote,
                        onDeleteNote = viewModel::deleteNote,
                        onConvertChecklistItems = { type, items ->
                            navController.navigate(
                                "related-capture/${entry.id}/${type.name}?sourceChecklistItemIds=${items.joinToString(",") { it.id }}",
                            )
                        },
                        onDelete = {
                            viewModel.delete(entry)
                            navController.popBackStack()
                        },
                        onAddRelationship = { type ->
                            navController.navigate("related-capture/${entry.id}/${type.name}")
                        },
                        onOpenEntry = { related -> navController.navigate("entry/${related.id}") },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(CAPTURE_ROUTE) {
                CaptureScreen(
                    heading = "New entry",
                    onCancel = { navController.popBackStack() },
                    onSave = { title, date ->
                        viewModel.create(title, date)
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = RELATED_CAPTURE_ROUTE,
                arguments = listOf(
                    navArgument("parentId") { type = NavType.StringType },
                    navArgument("relationshipType") { type = NavType.StringType },
                    navArgument("sourceContentId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("sourceChecklistItemIds") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { routeEntry ->
                val parentId = routeEntry.arguments?.getString("parentId")
                val type = routeEntry.arguments?.getString("relationshipType")
                    ?.let { value -> runCatching { RelationshipType.valueOf(value) }.getOrNull() }
                val sourceContentId = routeEntry.arguments?.getString("sourceContentId")
                val sourceChecklistItemIds = routeEntry.arguments?.getString("sourceChecklistItemIds")
                    ?.split(',')
                    ?.filter(String::isNotBlank)
                    .orEmpty()
                val parent = entries.firstOrNull { it.id == parentId }
                if (parent == null || type == null) {
                    MissingEntryScreen(onBack = { navController.popBackStack() })
                } else {
                    val descriptionBlocks by remember(parent.id) {
                        viewModel.descriptionBlocksFor(parent.id)
                    }.collectAsState(initial = emptyList())
                    val sourceBlock = descriptionBlocks.firstOrNull { it.id == sourceContentId }
                    val checklist by remember(parent.id) {
                        viewModel.checklistFor(parent.id)
                    }.collectAsState(initial = emptyList())
                    val sourceChecklistItems = checklist.filter { it.id in sourceChecklistItemIds }
                    CaptureScreen(
                        heading = type.creationLabel(),
                        contextLabel = "For ${parent.title}",
                        initialTitle = sourceBlock?.content ?: sourceChecklistItems.joinToString(", ") { it.title },
                        onCancel = { navController.popBackStack() },
                        onSave = { title, date ->
                            viewModel.createRelated(
                                parent.id,
                                type,
                                title,
                                date,
                                sourceContentId,
                                sourceChecklistItemIds,
                            )
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

private sealed interface TimelineItem {
    val key: String

    data class DayHeader(val date: LocalDate) : TimelineItem {
        override val key = "day-${date.toEpochDay()}"
    }

    data class EntryRowItem(val entry: LedgerEntry) : TimelineItem {
        override val key = "entry-${entry.id}"
    }
}

@Composable
private fun TimelineScreen(
    entries: List<LedgerEntry>,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    val today = LocalDate.now()
    val grouped = remember(entries) {
        entries.filter { it.scheduledDate != null }
            .groupBy { it.scheduledDate!! }
            .toSortedMap()
    }
    val items = remember(grouped) {
        buildList {
            grouped.forEach { (date, dayEntries) ->
                add(TimelineItem.DayHeader(date))
                dayEntries.forEach { add(TimelineItem.EntryRowItem(it)) }
            }
        }
    }
    val todayIndex = items.indexOfFirst { it is TimelineItem.DayHeader && !it.date.isBefore(today) }
        .let { if (it >= 0) it else (items.indexOfLast { item -> item is TimelineItem.DayHeader }) }
    val listState = rememberLazyListState()
    var positionedNearToday by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(items.size) {
        if (!positionedNearToday && items.isNotEmpty() && todayIndex >= 0) {
            listState.scrollToItem(todayIndex)
            positionedNearToday = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        LedgerHeader(
            title = "Memoise",
            subtitle = "PERSONAL LEDGER",
            trailing = {
                TextButton(
                    onClick = {
                        if (todayIndex >= 0) listState.requestScrollToItem(todayIndex)
                    },
                ) { Text("Today") }
            },
        )
        if (items.isEmpty()) {
            EmptyLedgerState(
                title = "Your ledger begins here",
                message = "A quiet place for the things you want to remember.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(items, key = TimelineItem::key) { item ->
                    when (item) {
                        is TimelineItem.DayHeader -> DateSeparator(item.date)
                        is TimelineItem.EntryRowItem -> LedgerEntryRow(
                            entry = item.entry,
                            overdue = isOverdue(item.entry, today),
                            onClick = { onOpenEntry(item.entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnscheduledScreen(
    entries: List<LedgerEntry>,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LedgerHeader(
            title = "Unscheduled",
            subtitle = "Things to place in time, when you're ready.",
        )
        if (entries.isEmpty()) {
            EmptyLedgerState(
                title = "Nothing waiting for a date",
                message = "Entries you capture without a date will stay here.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(entries.sortedByDescending(LedgerEntry::createdAt), key = LedgerEntry::id) {
                    LedgerEntryRow(entry = it, onClick = { onOpenEntry(it) })
                }
            }
        }
    }
}

@Composable
private fun OverdueScreen(
    entries: List<LedgerEntry>,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LedgerHeader(
            title = "Overdue",
            subtitle = "Unfinished obligations, kept in their original place.",
        )
        if (entries.isEmpty()) {
            EmptyLedgerState(
                title = "Nothing overdue",
                message = "Unfinished dated obligations will appear here.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(entries, key = LedgerEntry::id) { entry ->
                    val date = entry.scheduledDate ?: return@items
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
                            .uppercase(Locale.getDefault()),
                        modifier = Modifier.padding(start = 26.dp, top = 18.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.1.sp,
                    )
                    LedgerEntryRow(
                        entry = entry,
                        overdue = true,
                        onClick = { onOpenEntry(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerHeader(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.padding(start = 26.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = if (title == "Memoise") 2.2.sp else 0.25.sp,
                )
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (title == "Memoise") {
                    "TODAY  ·  ${todayLabel()}"
                } else {
                    subtitle.uppercase(Locale.getDefault())
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.75.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun DateSeparator(date: LocalDate) {
    val label = when (date) {
        LocalDate.now() -> "TODAY · ${monthDayLabel(date)}"
        LocalDate.now().plusDays(1) -> "TOMORROW · ${monthDayLabel(date)}"
        else -> date.format(DateTimeFormatter.ofPattern("MMMM d · EEEE", Locale.getDefault()))
            .uppercase(Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, end = 22.dp, top = 18.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (date == LocalDate.now()) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.05.sp,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun LedgerEntryRow(
    entry: LedgerEntry,
    overdue: Boolean = false,
    onClick: () -> Unit,
) {
    val completed = entry.status == EntryStatus.COMPLETED
    val indicatorColor = when {
        overdue -> MaterialTheme.colorScheme.secondary
        completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 27.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(21.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(9.dp)
                    .background(
                        color = if (completed) indicatorColor else Color.Transparent,
                        shape = CircleShape,
                    ),
            )
            if (completed) {
                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 7.sp, lineHeight = 9.sp)
            } else {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .width(1.dp)
                        .height(29.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            val supportingText = when {
                overdue -> entry.scheduledDate?.let { overdueLabel(it) }
                entry.status == EntryStatus.IN_PROGRESS -> "In progress"
                entry.status == EntryStatus.COMPLETED -> "Completed"
                entry.status == EntryStatus.CANCELLED -> "Cancelled"
                else -> null
            }
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun EmptyLedgerState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(1.dp)
                .height(46.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(9.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CaptureScreen(
    heading: String,
    contextLabel: String? = null,
    initialTitle: String = "",
    onCancel: () -> Unit,
    onSave: (String, LocalDate?) -> Unit,
) {
    var title by rememberSaveable(heading, initialTitle) { mutableStateOf(initialTitle) }
    var date by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val saveEntry = {
        if (title.isNotBlank()) {
            focusManager.clearFocus()
            onSave(title, date)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Close") }
            Text(
                text = heading,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = saveEntry, enabled = title.isNotBlank()) { Text("Save") }
        }
        Spacer(Modifier.height(36.dp))
        if (contextLabel != null) {
            Text(
                text = contextLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(15.dp))
        }
        Text(
            text = "What do you need to remember?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                lineHeight = 29.sp,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveEntry() }),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    if (title.isEmpty()) {
                        Text(
                            "Add a title",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.height(23.dp))
        DatePickerControl(
            date = date,
            onDateSelected = { date = it },
            label = "Add date",
            canRemoveDate = true,
        )
    }
}

@Composable
private fun EntryDetailScreen(
    entry: LedgerEntry,
    entries: List<LedgerEntry>,
    relationships: List<LedgerRelationship>,
    checklist: List<LedgerChecklistItem>,
    activities: List<LedgerActivity>,
    descriptionBlocks: List<LedgerDescriptionBlock>,
    notes: List<LedgerNote>,
    onUpdate: (LedgerEntry) -> Unit,
    onSetStatus: (LedgerEntry, EntryStatus) -> Unit,
    onSetDatePurpose: (LedgerEntry, DatePurpose) -> Unit,
    onAddChecklistItem: (String, String) -> Unit,
    onUpdateChecklistItem: (LedgerChecklistItem) -> Unit,
    onDeleteChecklistItem: (LedgerChecklistItem) -> Unit,
    onAddDescriptionBlock: (String, String) -> Unit,
    onUpdateDescriptionBlock: (LedgerDescriptionBlock) -> Unit,
    onDeleteDescriptionBlock: (LedgerDescriptionBlock) -> Unit,
    onConvertDescriptionBlock: (RelationshipType, LedgerDescriptionBlock) -> Unit,
    onConvertChecklistItems: (RelationshipType, List<LedgerChecklistItem>) -> Unit,
    onAddNote: (String, String) -> Unit,
    onUpdateNote: (LedgerNote) -> Unit,
    onDeleteNote: (LedgerNote) -> Unit,
    onAddRelationship: (RelationshipType) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable(entry.id) { mutableStateOf(entry.title) }
    var showDeleteConfirmation by rememberSaveable(entry.id) { mutableStateOf(false) }
    LaunchedEffect(entry.title) {
        if (title.isNotBlank()) title = entry.title
    }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹  Back") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showDeleteConfirmation = true }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 25.dp, end = 25.dp, bottom = 28.dp),
        ) {
            item {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) onUpdate(entry.copy(title = it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Serif,
                        fontSize = 30.sp,
                        lineHeight = 37.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) Text("Entry title", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(16.dp))
                DatePickerControl(
                    date = entry.scheduledDate,
                    onDateSelected = { onUpdate(entry.copy(scheduledDate = it)) },
                    label = "Add date",
                    canRemoveDate = true,
                )
                Spacer(Modifier.height(12.dp))
                SectionLabel("STATUS")
                EntryDropdown(
                    selected = entry.status.displayName(),
                    options = EntryStatus.entries.associateBy { it.displayName() },
                    onSelected = { label ->
                        EntryStatus.entries.firstOrNull { it.displayName() == label }
                            ?.let { onSetStatus(entry, it) }
                    },
                )
                if (entry.scheduledDate != null) {
                    Spacer(Modifier.height(18.dp))
                    SectionLabel("DATE PURPOSE")
                    EntryDropdown(
                        selected = entry.datePurpose.displayName(),
                        options = DatePurpose.entries.associateBy { it.displayName() },
                        onSelected = { label ->
                            DatePurpose.entries.firstOrNull { it.displayName() == label }
                                ?.let { onSetDatePurpose(entry, it) }
                        },
                    )
                }
                Spacer(Modifier.height(22.dp))
                DescriptionSection(
                    entryId = entry.id,
                    entries = entries,
                    blocks = descriptionBlocks,
                    onAdd = onAddDescriptionBlock,
                    onUpdate = onUpdateDescriptionBlock,
                    onDelete = onDeleteDescriptionBlock,
                    onConvert = { type, block -> onConvertDescriptionBlock(type, block) },
                    onOpenEntry = onOpenEntry,
                )
                Spacer(Modifier.height(22.dp))
                SectionLabel("ACTIONS")
                ChecklistSection(
                    entryId = entry.id,
                    items = checklist,
                    entries = entries,
                    onAdd = onAddChecklistItem,
                    onUpdate = onUpdateChecklistItem,
                    onDelete = onDeleteChecklistItem,
                    onConvert = { type, items -> onConvertChecklistItems(type, items) },
                    onOpenEntry = onOpenEntry,
                )
                Spacer(Modifier.height(22.dp))
                NotesSection(
                    entryId = entry.id,
                    notes = notes,
                    onAdd = onAddNote,
                    onUpdate = onUpdateNote,
                    onDelete = onDeleteNote,
                )
                Spacer(Modifier.height(32.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Created ${formatDateTime(entry.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.completedAt != null) {
                    Text(
                        text = "Completed ${formatDateTime(entry.completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Spacer(Modifier.height(30.dp))
                RelationshipSection(
                    title = "PREPARATIONS",
                    relationType = RelationshipType.PREPARATION,
                    entry = entry,
                    entries = entries,
                    relationships = relationships,
                    onAdd = { onAddRelationship(RelationshipType.PREPARATION) },
                    onOpen = onOpenEntry,
                )
                Spacer(Modifier.height(20.dp))
                RelationshipSection(
                    title = "FOLLOW-UPS",
                    relationType = RelationshipType.FOLLOW_UP,
                    entry = entry,
                    entries = entries,
                    relationships = relationships,
                    onAdd = { onAddRelationship(RelationshipType.FOLLOW_UP) },
                    onOpen = onOpenEntry,
                )
                Spacer(Modifier.height(20.dp))
                RelationshipSection(
                    title = "RELATED ENTRIES",
                    relationType = RelationshipType.RELATED,
                    entry = entry,
                    entries = entries,
                    relationships = relationships,
                    onAdd = { onAddRelationship(RelationshipType.RELATED) },
                    onOpen = onOpenEntry,
                )
                val incoming = relationships.filter {
                    it.targetEntryId == entry.id && it.type != RelationshipType.RELATED
                }
                if (incoming.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    RelationshipListSection(
                        title = "CONNECTED FROM",
                        links = incoming,
                        entry = entry,
                        entries = entries,
                        onOpen = onOpenEntry,
                    )
                }
                Spacer(Modifier.height(24.dp))
                ActivityHistory(activities)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this entry?") },
            text = { Text("${entry.title} will be removed from your ledger.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Keep entry") }
            },
        )
    }
}

@Composable
private fun DescriptionSection(
    entryId: String,
    entries: List<LedgerEntry>,
    blocks: List<LedgerDescriptionBlock>,
    onAdd: (String, String) -> Unit,
    onUpdate: (LedgerDescriptionBlock) -> Unit,
    onDelete: (LedgerDescriptionBlock) -> Unit,
    onConvert: (RelationshipType, LedgerDescriptionBlock) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    var draft by rememberSaveable(entryId) { mutableStateOf("") }
    Column {
        SectionLabel("DESCRIPTION")
        if (blocks.isEmpty()) {
            Text(
                text = "Add a little context whenever you’re ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
        } else {
            blocks.forEach { block ->
                DescriptionBlockRow(
                    block = block,
                    linkedEntry = entries.firstOrNull {
                        it.sourceEntryId == entryId && it.sourceContentId == block.id
                    },
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onConvert = onConvert,
                    onOpenEntry = onOpenEntry,
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Add a paragraph") },
            minLines = 2,
        )
        TextButton(
            onClick = {
                onAdd(entryId, draft)
                draft = ""
            },
            enabled = draft.isNotBlank(),
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Add paragraph")
        }
    }
}

@Composable
private fun DescriptionBlockRow(
    block: LedgerDescriptionBlock,
    linkedEntry: LedgerEntry?,
    onUpdate: (LedgerDescriptionBlock) -> Unit,
    onDelete: (LedgerDescriptionBlock) -> Unit,
    onConvert: (RelationshipType, LedgerDescriptionBlock) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    var editing by rememberSaveable(block.id) { mutableStateOf(false) }
    var draft by rememberSaveable(block.id) { mutableStateOf(block.content) }
    var confirmDelete by rememberSaveable(block.id) { mutableStateOf(false) }
    var linkMenuExpanded by remember(block.id) { mutableStateOf(false) }
    LaunchedEffect(block.content, editing) {
        if (!editing) draft = block.content
    }

    if (editing) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Paragraph") },
            minLines = 2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    draft = block.content
                    editing = false
                },
            ) { Text("Cancel") }
            TextButton(
                onClick = {
                    onUpdate(block.copy(content = draft))
                    editing = false
                },
                enabled = draft.isNotBlank(),
            ) { Text("Save") }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = block.content,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { linkMenuExpanded = true },
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DropdownMenu(
                expanded = linkMenuExpanded,
                onDismissRequest = { linkMenuExpanded = false },
            ) {
                if (linkedEntry == null) {
                    DropdownMenuItem(
                        text = { Text("Create preparation") },
                        onClick = {
                            linkMenuExpanded = false
                            onConvert(RelationshipType.PREPARATION, block)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Create follow-up") },
                        onClick = {
                            linkMenuExpanded = false
                            onConvert(RelationshipType.FOLLOW_UP, block)
                        },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Open linked entry") },
                        onClick = {
                            linkMenuExpanded = false
                            onOpenEntry(linkedEntry)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Edit paragraph") },
                    onClick = {
                        linkMenuExpanded = false
                        editing = true
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove paragraph", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        linkMenuExpanded = false
                        confirmDelete = true
                    },
                )
            }
        }
        if (linkedEntry != null && !editing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onOpenEntry(linkedEntry) }) {
                    Text("Linked to ${linkedEntry.title}")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove this paragraph?") },
            text = { Text("This paragraph will be removed from the entry description.") },
            confirmButton = {
                TextButton(onClick = { onDelete(block) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep paragraph") }
            },
        )
    }
}

@Composable
private fun ChecklistSection(
    entryId: String,
    items: List<LedgerChecklistItem>,
    entries: List<LedgerEntry>,
    onAdd: (String, String) -> Unit,
    onUpdate: (LedgerChecklistItem) -> Unit,
    onDelete: (LedgerChecklistItem) -> Unit,
    onConvert: (RelationshipType, List<LedgerChecklistItem>) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    var draft by rememberSaveable(entryId) { mutableStateOf("") }
    var selectionMode by rememberSaveable(entryId) { mutableStateOf(false) }
    var selectedItemIds by remember(entryId) { mutableStateOf(emptySet<String>()) }
    val selectedItems = items.filter { it.id in selectedItemIds && it.linkedEntryId == null }
    Column {
        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${selectedItems.size} selected",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    selectedItemIds = emptySet()
                    selectionMode = false
                }) { Text("Cancel") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = { onConvert(RelationshipType.PREPARATION, selectedItems) },
                    enabled = selectedItems.isNotEmpty(),
                ) { Text("Create preparation") }
                TextButton(
                    onClick = { onConvert(RelationshipType.FOLLOW_UP, selectedItems) },
                    enabled = selectedItems.isNotEmpty(),
                ) { Text("Create follow-up") }
            }
        }
        if (items.isEmpty()) {
            Text(
                text = "No checklist items yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        } else {
            items.forEach { item ->
                ChecklistRow(
                    item = item,
                    linkedEntry = entries.firstOrNull { it.id == item.linkedEntryId },
                    selectionMode = selectionMode,
                    selected = item.id in selectedItemIds,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onStartSelection = {
                        if (item.linkedEntryId == null) {
                            selectionMode = true
                            selectedItemIds = setOf(item.id)
                        }
                    },
                    onToggleSelection = {
                        if (item.linkedEntryId == null) {
                            selectedItemIds = if (item.id in selectedItemIds) {
                                selectedItemIds - item.id
                            } else {
                                selectedItemIds + item.id
                            }
                        }
                    },
                    onConvert = { type, selected -> onConvert(type, listOf(selected)) },
                    onOpenEntry = onOpenEntry,
                )
            }
        }
        if (!selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Add an action") },
                )
                TextButton(
                    onClick = {
                        onAdd(entryId, draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.padding(start = 5.dp),
                ) { Text("Add") }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: LedgerChecklistItem,
    linkedEntry: LedgerEntry?,
    selectionMode: Boolean,
    selected: Boolean,
    onUpdate: (LedgerChecklistItem) -> Unit,
    onDelete: (LedgerChecklistItem) -> Unit,
    onStartSelection: () -> Unit,
    onToggleSelection: () -> Unit,
    onConvert: (RelationshipType, LedgerChecklistItem) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit,
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    var editDialogVisible by rememberSaveable(item.id) { mutableStateOf(false) }
    var editTitle by rememberSaveable(item.id) { mutableStateOf(item.title) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = if (selectionMode) selected else item.completed,
                enabled = !selectionMode || item.linkedEntryId == null,
                onCheckedChange = {
                    if (selectionMode) onToggleSelection() else onUpdate(item.copy(completed = it))
                },
            )
            Text(
                text = item.title,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { if (selectionMode) onToggleSelection() },
                        onLongClick = {
                            if (selectionMode) onToggleSelection() else menuExpanded = true
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            if (!selectionMode) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) { Text("•••") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (linkedEntry == null) {
                            DropdownMenuItem(
                                text = { Text("Create preparation") },
                                onClick = {
                                    menuExpanded = false
                                    onConvert(RelationshipType.PREPARATION, item)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Create follow-up") },
                                onClick = {
                                    menuExpanded = false
                                    onConvert(RelationshipType.FOLLOW_UP, item)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Select multiple") },
                                onClick = {
                                    menuExpanded = false
                                    onStartSelection()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Open linked entry") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenEntry(linkedEntry)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                editTitle = item.title
                                editDialogVisible = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDelete(item)
                            },
                        )
                    }
                }
            }
        }
        if (linkedEntry != null) {
            TextButton(
                onClick = { onOpenEntry(linkedEntry) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Linked to ${linkedEntry.title}")
            }
        }
    }
    if (editDialogVisible) {
        AlertDialog(
            onDismissRequest = { editDialogVisible = false },
            title = { Text("Edit checklist item") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) onUpdate(item.copy(title = editTitle))
                        editDialogVisible = false
                    },
                    enabled = editTitle.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editDialogVisible = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun NotesSection(
    entryId: String,
    notes: List<LedgerNote>,
    onAdd: (String, String) -> Unit,
    onUpdate: (LedgerNote) -> Unit,
    onDelete: (LedgerNote) -> Unit,
) {
    var draft by rememberSaveable(entryId) { mutableStateOf("") }
    Column {
        SectionLabel("NOTES")
        if (notes.isEmpty()) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        } else {
            notes.forEach { note ->
                NoteRow(note = note, onUpdate = onUpdate, onDelete = onDelete)
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Add a note") },
            minLines = 2,
        )
        TextButton(
            onClick = {
                onAdd(entryId, draft)
                draft = ""
            },
            enabled = draft.isNotBlank(),
            modifier = Modifier.align(Alignment.End),
        ) { Text("Add note") }
    }
}

@Composable
private fun NoteRow(
    note: LedgerNote,
    onUpdate: (LedgerNote) -> Unit,
    onDelete: (LedgerNote) -> Unit,
) {
    var menuExpanded by remember(note.id) { mutableStateOf(false) }
    var editDialogVisible by rememberSaveable(note.id) { mutableStateOf(false) }
    var deleteConfirmationVisible by rememberSaveable(note.id) { mutableStateOf(false) }
    var draft by rememberSaveable(note.id) { mutableStateOf(note.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatDateTime(note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box {
            TextButton(onClick = { menuExpanded = true }) { Text("•••") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit note") },
                    onClick = {
                        menuExpanded = false
                        draft = note.content
                        editDialogVisible = true
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove note", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        deleteConfirmationVisible = true
                    },
                )
            }
        }
    }

    if (editDialogVisible) {
        AlertDialog(
            onDismissRequest = { editDialogVisible = false },
            title = { Text("Edit note") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdate(note.copy(content = draft))
                        editDialogVisible = false
                    },
                    enabled = draft.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editDialogVisible = false }) { Text("Cancel") }
            },
        )
    }

    if (deleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationVisible = false },
            title = { Text("Remove this note?") },
            text = { Text("This note will be removed from the entry.") },
            confirmButton = {
                TextButton(onClick = { onDelete(note) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationVisible = false }) { Text("Keep note") }
            },
        )
    }
}

@Composable
private fun ActivityHistory(activities: List<LedgerActivity>) {
    Column {
        SectionLabel("ACTIVITY HISTORY")
        if (activities.isEmpty()) {
            Text(
                text = "No activity recorded yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 9.dp),
            )
        } else {
            activities.take(8).forEach { activity ->
                Column(Modifier.padding(top = 11.dp)) {
                    Text(
                        text = activity.action,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!activity.details.isNullOrBlank()) {
                        Text(
                            text = activity.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatDateTime(activity.occurredAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationshipSection(
    title: String,
    relationType: RelationshipType,
    entry: LedgerEntry,
    entries: List<LedgerEntry>,
    relationships: List<LedgerRelationship>,
    onAdd: () -> Unit,
    onOpen: (LedgerEntry) -> Unit,
) {
    val children = relationships.asSequence()
        .filter {
            it.type == relationType &&
                (it.sourceEntryId == entry.id ||
                    (relationType == RelationshipType.RELATED && it.targetEntryId == entry.id))
        }
        .mapNotNull { link ->
            val otherId = if (link.sourceEntryId == entry.id) link.targetEntryId else link.sourceEntryId
            entries.firstOrNull { it.id == otherId }
        }
        .toList()

    Column {
        SectionLabel(title)
        if (children.isEmpty()) {
            Text(
                text = "Nothing added yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 9.dp),
            )
        } else {
            children.forEach { child ->
                RelationshipRow(entry = child, onClick = { onOpen(child) })
            }
        }
        TextButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text("+ Add ${relationType.creationLabel().lowercase()}")
        }
    }
}

@Composable
private fun RelationshipListSection(
    title: String,
    links: List<LedgerRelationship>,
    entry: LedgerEntry,
    entries: List<LedgerEntry>,
    onOpen: (LedgerEntry) -> Unit,
) {
    Column {
        SectionLabel(title)
        links.forEach { link ->
            val otherId = if (link.sourceEntryId == entry.id) link.targetEntryId else link.sourceEntryId
            val other = entries.firstOrNull { it.id == otherId } ?: return@forEach
            RelationshipRow(entry = other, onClick = { onOpen(other) })
        }
    }
}

@Composable
private fun RelationshipRow(
    entry: LedgerEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                color = MaterialTheme.colorScheme.onSurface,
            )
            val details = buildList {
                entry.scheduledDate?.let { add(formatDate(it)) }
                if (entry.status == EntryStatus.COMPLETED) add("Completed")
                if (entry.status == EntryStatus.IN_PROGRESS) add("In progress")
                if (entry.status == EntryStatus.CANCELLED) add("Cancelled")
            }.joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 23.sp)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DatePickerControl(
    date: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    label: String,
    canRemoveDate: Boolean = false,
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { showPicker = true }) {
            Text(date?.let(::formatDate) ?: "◷  $label")
        }
        if (canRemoveDate && date != null) {
            TextButton(onClick = { onDateSelected(null) }) { Text("Remove date") }
        }
    }
    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                            ?.let { millis -> java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate() }
                        if (selected != null) onDateSelected(selected)
                        showPicker = false
                    },
                ) { Text("Set date") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EntryDropdown(
    selected: String,
    options: Map<String, *>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.keys.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun MissingEntryScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("This entry is no longer available.", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onBack) { Text("Go back") }
    }
}

private fun isOverdue(entry: LedgerEntry, today: LocalDate): Boolean =
    entry.scheduledDate?.isBefore(today) == true &&
        entry.datePurpose != DatePurpose.EVENT &&
        entry.status != EntryStatus.COMPLETED &&
        entry.status != EntryStatus.CANCELLED

private fun todayLabel(): String = LocalDate.now()
    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    .uppercase(Locale.getDefault())

private fun monthDayLabel(date: LocalDate): String = date
    .format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
    .uppercase(Locale.getDefault())

private fun formatDate(date: LocalDate): String = date
    .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))

private fun formatDateTime(timestamp: Long): String = java.time.Instant.ofEpochMilli(timestamp)
    .atZone(java.time.ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault()))

private fun overdueLabel(date: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now())
    val period = if (days == 1L) "day" else "days"
    return "$days $period overdue"
}

private fun EntryStatus.displayName(): String = when (this) {
    EntryStatus.PENDING -> "Pending"
    EntryStatus.IN_PROGRESS -> "In progress"
    EntryStatus.COMPLETED -> "Completed"
    EntryStatus.CANCELLED -> "Cancelled"
}

private fun DatePurpose.displayName(): String = when (this) {
    DatePurpose.EVENT -> "Event"
    DatePurpose.DEADLINE -> "Deadline"
    DatePurpose.FLEXIBLE_TASK -> "Flexible task date"
}

private fun RelationshipType.creationLabel(): String = when (this) {
    RelationshipType.PREPARATION -> "Preparation"
    RelationshipType.FOLLOW_UP -> "Follow-up"
    RelationshipType.RELATED -> "Related entry"
}
