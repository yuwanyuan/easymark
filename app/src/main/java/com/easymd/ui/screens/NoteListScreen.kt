package com.easymd.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.easymd.data.LayoutSettings
import com.easymd.data.Note
import com.easymd.data.storage.NoteLibrary
import com.easymd.data.storage.StorageConfig
import com.easymd.data.storage.StorageType

import com.easymd.ui.viewmodel.NoteListViewModel
import com.easymd.ui.viewmodel.NoteListViewModel.Companion.ALL_LIBRARIES_ID
import com.easymd.ui.viewmodel.ThemeMode
import com.easymd.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val viewModel: NoteListViewModel = viewModel()
    val notes by viewModel.notes.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterTag by viewModel.filterTag.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val activeLibrary by viewModel.activeLibrary.collectAsState()
    val totalNoteCount by viewModel.totalNoteCount.collectAsState()
    val totalAttachmentCount by viewModel.totalAttachmentCount.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTabletWidth = configuration.screenWidthDp >= 600
    val drawerWidth = if (isTabletWidth) 280.dp else (configuration.screenWidthDp * 0.75).dp
    val context = LocalContext.current

    var showSearchBar by remember { mutableStateOf(false) }
    var showNewLibraryDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val selectedNotes = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedNotes.isNotEmpty()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showNewLibraryDialog) {
        NewLibraryDialog(
            libraries = libraries,
            onDismiss = { showNewLibraryDialog = false },
            onConfirm = { storageConfig ->
                val newLib = NoteLibrary(storageConfig = storageConfig)
                viewModel.addLibrary(newLib)
                viewModel.switchLibrary(newLib.id)
                showNewLibraryDialog = false
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("删除笔记") },
            text = { Text("确定要删除「${note.title.ifBlank { "无标题" }}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(note.id)
                    noteToDelete = null
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("取消") }
            }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("批量删除") },
            text = { Text("确定要删除选中的 ${selectedNotes.size} 条笔记吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNotes(selectedNotes.toList())
                    selectedNotes.clear()
                    showBatchDeleteConfirm = false
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                tags = tags,
                libraries = libraries,
                activeLibrary = activeLibrary,
                totalNoteCount = totalNoteCount,
                totalAttachmentCount = totalAttachmentCount,
                onTagClick = { tag ->
                    viewModel.setFilterTag(tag)
                    scope.launch { drawerState.close() }
                },
                onSwitchLibrary = { viewModel.switchLibrary(it) },
                onNewLibraryClick = { showNewLibraryDialog = true },
                onLibrarySettingsClick = { libraryId ->
                    scope.launch { drawerState.close() }
                    navController.navigate("storageSettings/$libraryId")
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                },
                modifier = Modifier.width(drawerWidth)
            )
        }
    ) {
        val tabletColumns = if (isTabletWidth) LayoutSettings.getTabletColumns(context) else 1

        Scaffold(
            topBar = {
                Column {
                    if (isSelectionMode) {
                        TopAppBar(
                            title = {
                                Text("已选择 ${selectedNotes.size} 项", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            },
                            navigationIcon = {
                                IconButton(onClick = { selectedNotes.clear() }) {
                                    Icon(Icons.Outlined.Close, "取消选择", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    if (selectedNotes.size == notes.size) selectedNotes.clear()
                                    else { selectedNotes.clear(); selectedNotes.addAll(notes.map { it.id }) }
                                }) {
                                    Text(if (selectedNotes.size == notes.size) "取消全选" else "全选", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    } else {
                        val titleText = when {
                            activeLibrary?.id == ALL_LIBRARIES_ID -> "全部笔记库"
                            activeLibrary != null -> activeLibrary!!.name
                            else -> "全部笔记库"
                        }
                        TopAppBar(
                            title = { Text(titleText, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "菜单", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                    Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    AnimatedVisibility(visible = (showSearchBar && !isSelectionMode) || filterTag != null) {
                        SearchBar(query = if (filterTag != null) "" else searchQuery, onQueryChange = { viewModel.setSearchQuery(it) }, onClose = { showSearchBar = false; viewModel.setFilterTag(null); viewModel.setSearchQuery("") }, filterTag = filterTag, onClearFilter = { viewModel.setFilterTag(null) })
                    }
                }
            },
            bottomBar = {
                if (isSelectionMode) {
                    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedNotes.clear() }) {
                                Icon(Icons.Outlined.Close, null, Modifier.size(22.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("取消", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showBatchDeleteConfirm = true }) {
                                Icon(Icons.Outlined.Delete, null, Modifier.size(22.dp), MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("删除", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = { navController.navigate("newNote") },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Add, "新建笔记", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "点击新建",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val pullToRefreshState = rememberPullToRefreshState()
            var isRefreshing by remember { mutableStateOf(false) }

            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                    kotlinx.coroutines.delay(600)
                    isRefreshing = false
                }
            }

            val density = LocalDensity.current
            val thresholdPx = with(density) { 80.dp.toPx() }
            val indicatorSizePx = with(density) { 56.dp.toPx() }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refresh()
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize().padding(paddingValues).clipToBounds(),
                indicator = {
                    val distanceFraction = pullToRefreshState.distanceFraction
                    val pullOffset = distanceFraction * thresholdPx

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                translationY = pullOffset - indicatorSizePx
                            }
                            .padding(top = 8.dp)
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = if (isRefreshing) rotation else distanceFraction * 180f
                                },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            ) {
                val distanceFraction = pullToRefreshState.distanceFraction
                val contentOffsetY = (distanceFraction * thresholdPx).toInt()

                Box(modifier = Modifier.offset { IntOffset(0, contentOffsetY) }) {
                    if (tabletColumns > 1 && isTabletWidth) {
                        LazyVerticalGrid(columns = GridCells.Fixed(tabletColumns), modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(note = note, isSelected = note.id in selectedNotes, isSelectionMode = isSelectionMode, onClick = {
                                    if (isSelectionMode) { if (note.id in selectedNotes) selectedNotes.remove(note.id) else selectedNotes.add(note.id) }
                                    else navController.navigate("editNote/${note.id}")
                                }, onLongClick = { if (!isSelectionMode) selectedNotes.add(note.id) })
                            }
                            if (notes.isEmpty()) {
                                item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Text(if (searchQuery.isNotBlank()) "未找到匹配的笔记" else "暂无笔记，点击 + 创建", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) } }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(notes, key = { it.id }) { note ->
                                NoteCard(note = note, isSelected = note.id in selectedNotes, isSelectionMode = isSelectionMode, onClick = {
                                    if (isSelectionMode) { if (note.id in selectedNotes) selectedNotes.remove(note.id) else selectedNotes.add(note.id) }
                                    else navController.navigate("editNote/${note.id}")
                                }, onLongClick = { if (!isSelectionMode) selectedNotes.add(note.id) })
                            }
                            if (notes.isEmpty()) {
                                item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Text(if (searchQuery.isNotBlank()) "未找到匹配的笔记" else "暂无笔记，点击 + 创建", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) } }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit, filterTag: String? = null, onClearFilter: () -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(8.dp))
            if (filterTag != null) {
                SuggestionChip(
                    onClick = onClearFilter,
                    label = { Text("#$filterTag", fontSize = 14.sp) },
                    modifier = Modifier.height(28.dp)
                )
            } else {
                androidx.compose.foundation.text.BasicTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer), singleLine = true, decorationBox = { innerTextField ->
                    if (query.isEmpty()) Text("搜索笔记...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                    innerTextField()
                })
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimaryContainer) }
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Text("取消", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(note: Note, isSelected: Boolean, isSelectionMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (note.title.isNotBlank()) {
                        Text(text = note.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (note.getPreview().isNotBlank()) {
                        if (note.title.isNotBlank()) Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = note.getPreview(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }
                if (isSelected) {
                    Checkbox(checked = true, onCheckedChange = { onClick() }, modifier = Modifier.padding(end = 8.dp, top = 4.dp).size(24.dp))
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = note.getRelativeTime(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.tags.isNotEmpty()) {
                        note.tags.take(2).forEach { tag ->
                            Icon(Icons.Outlined.Label, null, Modifier.size(10.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(1.dp))
                            Text(tag, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    tags: List<String>,
    libraries: List<NoteLibrary>,
    activeLibrary: NoteLibrary?,
    totalNoteCount: Int,
    totalAttachmentCount: Int,
    onTagClick: (String) -> Unit,
    onSwitchLibrary: (String) -> Unit,
    onNewLibraryClick: () -> Unit,
    onLibrarySettingsClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var libraryExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(modifier = modifier, drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)) {
        Column(modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("易Mark", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                val headerName = if (activeLibrary?.id == ALL_LIBRARIES_ID) "全部笔记库" else (activeLibrary?.name ?: "未选择")
                val headerType = if (activeLibrary?.id == ALL_LIBRARIES_ID) "聚合" else (activeLibrary?.storageConfig?.type?.let { when (it) { StorageType.LOCAL -> "本地"; StorageType.WEBDAV -> "WebDAV"; StorageType.S3 -> "S3" } } ?: "")
                Row(modifier = Modifier.fillMaxWidth().clickable { libraryExpanded = !libraryExpanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Storage, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(headerName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(headerType, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = if (libraryExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                }
            }

            AnimatedVisibility(visible = libraryExpanded) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    libraries.forEach { library ->
                        val isActive = library.id == activeLibrary?.id && activeLibrary?.id != ALL_LIBRARIES_ID
                        Row(modifier = Modifier.fillMaxWidth().clickable { onSwitchLibrary(library.id); libraryExpanded = false }.background(if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storage, null, Modifier.size(16.dp), if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(library.name, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal, color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(when (library.storageConfig.type) { StorageType.LOCAL -> "本地"; StorageType.WEBDAV -> "WebDAV"; StorageType.S3 -> "S3" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isActive) {
                                IconButton(onClick = { onLibrarySettingsClick(library.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Settings, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val isAllActive = activeLibrary?.id == ALL_LIBRARIES_ID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchLibrary(ALL_LIBRARIES_ID) }
                        .background(if (isAllActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Note, null, Modifier.size(18.dp), if (isAllActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("全部笔记库", fontSize = 14.sp, fontWeight = if (isAllActive) FontWeight.Medium else FontWeight.Medium, color = if (isAllActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$totalNoteCount", fontSize = 12.sp, color = if (isAllActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (totalAttachmentCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.AttachFile, null, Modifier.size(12.dp), if (isAllActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalAttachmentCount", fontSize = 12.sp, color = if (isAllActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("标签", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        tags.forEach { tag ->
                            NavigationDrawerItem(icon = { Icon(Icons.Outlined.Label, null, Modifier.size(18.dp)) }, label = { Text(tag) }, selected = false, onClick = { onTagClick(tag) }, shape = RoundedCornerShape(2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("笔记库", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNewLibraryClick() }) {
                    Icon(Icons.Outlined.Storage, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("新建库", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSettingsClick() }) {
                    Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("设置", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NewLibraryDialog(
    libraries: List<NoteLibrary>,
    onDismiss: () -> Unit,
    onConfirm: (StorageConfig) -> Unit
) {
    var selectedType by remember { mutableStateOf(StorageType.LOCAL) }
    var localPath by remember { mutableStateOf("") }
    var webdavUrl by remember { mutableStateOf("") }
    var webdavUsername by remember { mutableStateOf("") }
    var webdavPassword by remember { mutableStateOf("") }
    var webdavPath by remember { mutableStateOf("/notes/") }
    var s3Endpoint by remember { mutableStateOf("") }
    var s3Region by remember { mutableStateOf("") }
    var s3Bucket by remember { mutableStateOf("") }
    var s3AccessKey by remember { mutableStateOf("") }
    var s3SecretKey by remember { mutableStateOf("") }
    var s3Prefix by remember { mutableStateOf("notes/") }
    var useExistingConfig by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val existingWebdavLibs = libraries.filter { it.storageConfig.type == StorageType.WEBDAV }
    val existingS3Libs = libraries.filter { it.storageConfig.type == StorageType.S3 }
    val hasExistingConfigs = when (selectedType) {
        StorageType.WEBDAV -> existingWebdavLibs.isNotEmpty()
        StorageType.S3 -> existingS3Libs.isNotEmpty()
        else -> false
    }

    val previewConfig = when (selectedType) {
        StorageType.LOCAL -> StorageConfig(type = StorageType.LOCAL, localPath = localPath)
        StorageType.WEBDAV -> StorageConfig(type = StorageType.WEBDAV, webdavUrl = webdavUrl, webdavUsername = webdavUsername, webdavPassword = webdavPassword, webdavPath = webdavPath)
        StorageType.S3 -> StorageConfig(type = StorageType.S3, s3Endpoint = s3Endpoint, s3Region = s3Region, s3Bucket = s3Bucket, s3AccessKey = s3AccessKey, s3SecretKey = s3SecretKey, s3Prefix = s3Prefix)
    }
    val previewName = NoteLibrary.deriveName(previewConfig)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(it)
            val parts = docId.split(":")
            if (parts.size == 2 && parts[0].equals("primary", ignoreCase = true)) {
                localPath = "/storage/emulated/0/${parts[1]}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建笔记库") },
        text = {
            Column {
                Text("存储类型", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageType.entries.forEach { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type; useExistingConfig = false }, label = { Text(when (type) { StorageType.LOCAL -> "本地"; StorageType.WEBDAV -> "WebDAV"; StorageType.S3 -> "S3" }, fontSize = 13.sp) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (hasExistingConfigs) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !useExistingConfig, onClick = { useExistingConfig = false }, label = { Text("新建配置", fontSize = 13.sp) })
                        FilterChip(selected = useExistingConfig, onClick = { useExistingConfig = true }, label = { Text("使用已有", fontSize = 13.sp) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (useExistingConfig && hasExistingConfigs) {
                    val existingLibs = when (selectedType) { StorageType.WEBDAV -> existingWebdavLibs; StorageType.S3 -> existingS3Libs; else -> emptyList() }
                    existingLibs.forEach { lib ->
                        val config = lib.storageConfig
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            when (selectedType) {
                                StorageType.WEBDAV -> { webdavUrl = config.webdavUrl; webdavUsername = config.webdavUsername; webdavPassword = config.webdavPassword; webdavPath = config.webdavPath }
                                StorageType.S3 -> { s3Endpoint = config.s3Endpoint; s3Region = config.s3Region; s3Bucket = config.s3Bucket; s3AccessKey = config.s3AccessKey; s3SecretKey = config.s3SecretKey }
                                else -> {}
                            }
                            useExistingConfig = false
                        }.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (selectedType) { StorageType.WEBDAV -> Icons.Outlined.Cloud; StorageType.S3 -> Icons.Outlined.Storage; else -> Icons.Outlined.Storage }, null, Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lib.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(when (selectedType) { StorageType.WEBDAV -> config.webdavUrl; StorageType.S3 -> "${config.s3Bucket}@${config.s3Endpoint}"; else -> "" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    when (selectedType) {
                        StorageType.LOCAL -> {
                            Text("存储路径", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = localPath, onValueChange = { localPath = it }, label = { Text("留空使用默认路径") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { folderPickerLauncher.launch(null) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("浏览", fontSize = 13.sp) }
                            }
                        }
                        StorageType.WEBDAV -> {
                            OutlinedTextField(value = webdavUrl, onValueChange = { webdavUrl = it }, label = { Text("WebDAV 地址") }, placeholder = { Text("https://dav.example.com") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = webdavPath, onValueChange = { webdavPath = it }, label = { Text("远程路径") }, placeholder = { Text("/notes/") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = webdavUsername, onValueChange = { webdavUsername = it }, label = { Text("用户名") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = webdavPassword, onValueChange = { webdavPassword = it }, label = { Text("密码") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                            }
                        }
                        StorageType.S3 -> {
                            OutlinedTextField(value = s3Endpoint, onValueChange = { s3Endpoint = it }, label = { Text("S3 端点") }, placeholder = { Text("s3.amazonaws.com") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = s3Bucket, onValueChange = { s3Bucket = it }, label = { Text("桶名") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = s3Region, onValueChange = { s3Region = it }, label = { Text("区域") }, placeholder = { Text("us-east-1") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = s3AccessKey, onValueChange = { s3AccessKey = it }, label = { Text("Access Key") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = s3SecretKey, onValueChange = { s3SecretKey = it }, label = { Text("Secret Key") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = s3Prefix, onValueChange = { s3Prefix = it }, label = { Text("对象前缀") }, placeholder = { Text("notes/") }, singleLine = true, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                if (previewName.isNotBlank() && previewName != "默认笔记库") {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("库名：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(previewName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val config = when (selectedType) {
                    StorageType.LOCAL -> StorageConfig(type = StorageType.LOCAL, localPath = localPath)
                    StorageType.WEBDAV -> StorageConfig(type = StorageType.WEBDAV, webdavUrl = webdavUrl, webdavUsername = webdavUsername, webdavPassword = webdavPassword, webdavPath = webdavPath)
                    StorageType.S3 -> StorageConfig(type = StorageType.S3, s3Endpoint = s3Endpoint, s3Region = s3Region, s3Bucket = s3Bucket, s3AccessKey = s3AccessKey, s3SecretKey = s3SecretKey, s3Prefix = s3Prefix)
                }
                onConfirm(config)
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
