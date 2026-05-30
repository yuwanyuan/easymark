package com.easymd.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.easymd.data.ToolbarSettingsRepository
import com.easymd.ui.components.MarkdownSyntax
import com.easymd.ui.components.MarkdownToolbar
import com.easymd.ui.components.ToolbarSettingsDialog
import com.easymd.ui.components.WysiwygEditor
import com.easymd.ui.viewmodel.EditNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    navController: NavController,
    noteId: String?,
    viewModel: EditNoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(noteId) {
        viewModel.loadNote(context, noteId)
    }

    var hasSaved by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (!hasSaved) {
                viewModel.saveNote(context)
            }
        }
    }

    val title by viewModel.title.collectAsState()
    val contentTextField by viewModel.contentTextField.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val attachments by viewModel.attachments.collectAsState()

    var isContentFocused by remember { mutableStateOf(false) }
    var contentFocusTrigger by remember { mutableStateOf(0) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showToolbarSettings by remember { mutableStateOf(false) }
    var showAttachmentManager by remember { mutableStateOf(false) }

    val isTitleFilled = title.isNotEmpty()
    var isTitleExpanded by remember { mutableStateOf(isTitleFilled) }

    LaunchedEffect(isTitleFilled) {
        if (isTitleFilled) {
            isTitleExpanded = true
        }
    }

    var enabledToolbarItems by remember { mutableStateOf(ToolbarSettingsRepository.getEnabledSyntaxList(context)) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.insertImage(context, it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.insertFileReference(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(viewModel.getExportMarkdown().toByteArray())
                }
                Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        hasSaved = true
        viewModel.saveNote(context)
        navController.popBackStack()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除笔记") },
            text = { Text("确定要删除此笔记吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(context)
                    showDeleteConfirm = false
                    navController.popBackStack()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showTagDialog) {
        TagEditorDialog(
            currentTags = tags,
            onDismiss = { showTagDialog = false },
            onAddTag = { viewModel.addTag(it) },
            onRemoveTag = { viewModel.removeTag(it) }
        )
    }

    if (showAttachmentManager) {
        AttachmentManagerDialog(
            attachments = attachments,
            onDismiss = { showAttachmentManager = false },
            onDelete = { entry ->
                viewModel.deleteAttachment(context, entry)
            }
        )
    }

    if (showToolbarSettings) {
        ToolbarSettingsDialog(
            enabledItems = enabledToolbarItems.map { it.name }.toSet(),
            onConfirm = { newEnabled ->
                ToolbarSettingsRepository.setEnabledItems(context, newEnabled)
                enabledToolbarItems = MarkdownSyntax.entries.filter { it.name in newEnabled }
                showToolbarSettings = false
            },
            onDismiss = { showToolbarSettings = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        hasSaved = true
                        viewModel.saveNote(context)
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    if (isTitleFilled && !isTitleExpanded) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hasSaved = true
                        viewModel.saveNote(context)
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = {
                        filePickerLauncher.launch("*/*")
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "附件",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        imagePickerLauncher.launch("image/*")
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "图片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = viewModel.getShareIntent(context)
                        context.startActivity(Intent.createChooser(shareIntent, "分享笔记"))
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "分享",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑标签") },
                                onClick = {
                                    showMoreMenu = false
                                    showTagDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出 Markdown") },
                                onClick = {
                                    showMoreMenu = false
                                    val fileName = "${title.ifBlank { "未命名" }}.md"
                                    exportLauncher.launch(fileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("附件管理") },
                                onClick = {
                                    showMoreMenu = false
                                    showAttachmentManager = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("检查引用完整性") },
                                onClick = {
                                    showMoreMenu = false
                                    val result = viewModel.checkAttachmentIntegrity(context)
                                    if (result != null) {
                                        val msg = if (result.brokenReferences.isEmpty() && result.orphanAttachments.isEmpty()) {
                                            "所有引用正常"
                                        } else {
                                            "${result.brokenReferences.size}个失效引用，${result.orphanAttachments.size}个孤立附件"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清理孤立附件") },
                                onClick = {
                                    showMoreMenu = false
                                    val count = viewModel.cleanOrphanAttachments(context)
                                    Toast.makeText(context, "已清理${count}个孤立附件", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("删除笔记", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isContentFocused) {
                MarkdownToolbar(
                    onSyntaxClick = { syntax ->
                        viewModel.insertMarkdownSyntax(syntax)
                    },
                    enabledItems = enabledToolbarItems,
                    onSettingsClick = { showToolbarSettings = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val density = LocalDensity.current
        val thresholdPx = with(density) { 40.dp.toPx() }

        val pullToRefreshState = rememberPullToRefreshState()
        var isPullingTitle by remember { mutableStateOf(false) }

        LaunchedEffect(isPullingTitle) {
            if (isPullingTitle) {
                kotlinx.coroutines.delay(200)
                isPullingTitle = false
            }
        }

        PullToRefreshBox(
            isRefreshing = isPullingTitle,
            onRefresh = {
                isTitleExpanded = true
                isPullingTitle = true
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                val distanceFraction = pullToRefreshState.distanceFraction
                val titleAlpha = if (isTitleExpanded) 1f else distanceFraction.coerceIn(0f, 1f)

                Column(
                    modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { viewModel.onTitleChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { contentFocusTrigger++ }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "标题",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            innerTextField()
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { showTagDialog = true },
                                label = { Text(tag, fontSize = 11.sp) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                WysiwygEditor(
                    content = contentTextField,
                    onContentChange = { viewModel.onContentChange(it) },
                    onBlockContentChange = { index, text, cursorOffset -> viewModel.onBlockContentChange(index, text, cursorOffset) },
                    onBlockFocusChange = { index, focused -> viewModel.onBlockFocusChange(index, focused) },
                    onAnyBlockFocusChange = { focused -> isContentFocused = focused },
                    contentFocusTrigger = contentFocusTrigger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttachmentManagerDialog(
    attachments: List<com.easymd.data.attachment.AttachmentEntry>,
    onDismiss: () -> Unit,
    onDelete: (com.easymd.data.attachment.AttachmentEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("附件管理") },
        text = {
            if (attachments.isEmpty()) {
                Text(
                    text = "暂无附件",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(attachments.size) { index ->
                        val entry = attachments[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.isImage()) Icons.Outlined.Image
                                    else Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.originalName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${entry.storedName} · ${formatFileSize(entry.size)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(entry) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${"%.1f".format(size / 1024.0)} KB"
        else -> "${"%.1f".format(size / (1024.0 * 1024.0))} MB"
    }
}

@Composable
private fun TagEditorDialog(
    currentTags: List<String>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var newTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            Column {
                if (currentTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { onRemoveTag(tag) },
                                label = { Text(tag, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "移除",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("添加标签") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newTag.isNotBlank()) {
                                onAddTag(newTag.trim())
                                newTag = ""
                            }
                        },
                        enabled = newTag.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "添加")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
