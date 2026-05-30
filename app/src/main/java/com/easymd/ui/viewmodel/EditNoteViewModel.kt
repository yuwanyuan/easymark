package com.easymd.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easymd.data.MediaHelper
import com.easymd.data.Note
import com.easymd.data.NoteRepository
import com.easymd.data.attachment.AttachmentEntry
import com.easymd.ui.components.MarkdownSyntax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditNoteViewModel : ViewModel() {

    private var currentNote: Note? = null
    private var repository: NoteRepository? = null

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _contentTextField = MutableStateFlow(TextFieldValue(""))
    val contentTextField: StateFlow<TextFieldValue> = _contentTextField

    val content: String
        get() = _contentTextField.value.text

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags

    private val _attachments = MutableStateFlow<List<AttachmentEntry>>(emptyList())
    val attachments: StateFlow<List<AttachmentEntry>> = _attachments

    private val _focusedBlockIndex = MutableStateFlow(-1)
    val focusedBlockIndex: StateFlow<Int> = _focusedBlockIndex

    fun loadNote(context: Context, noteId: String?) {
        if (repository == null) {
            repository = NoteRepository(context.applicationContext)
        }

        if (noteId != null) {
            viewModelScope.launch {
                val note = withContext(Dispatchers.IO) {
                    repository?.getNoteById(noteId)
                }
                note?.let {
                    currentNote = it
                    _title.value = it.title
                    _contentTextField.value = TextFieldValue(it.content)
                    _tags.value = it.tags
                }
                loadAttachments(noteId)
            }
        } else {
            currentNote = null
            _title.value = ""
            _contentTextField.value = TextFieldValue("")
            _tags.value = emptyList()
            _attachments.value = emptyList()
        }
    }

    private fun loadAttachments(noteId: String) {
        viewModelScope.launch {
            _attachments.value = withContext(Dispatchers.IO) {
                repository?.getAttachmentsForNote(noteId) ?: emptyList()
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
    }

    fun onContentChange(newContent: TextFieldValue) {
        _contentTextField.value = newContent
    }

    fun onBlockContentChange(blockIndex: Int, newBlockText: String, cursorOffsetInBlock: Int = -1) {
        val blocks = splitBlocks(_contentTextField.value.text).toMutableList()
        if (blockIndex in blocks.indices) {
            blocks[blockIndex] = newBlockText
            val fullText = blocks.joinToString("\n\n")
            val globalCursor = if (cursorOffsetInBlock >= 0) {
                computeGlobalPosition(blocks, blockIndex, cursorOffsetInBlock.coerceIn(0, newBlockText.length))
            } else {
                fullText.length
            }
            _contentTextField.value = TextFieldValue(
                text = fullText,
                selection = TextRange(globalCursor.coerceIn(0, fullText.length))
            )
        }
    }

    private fun computeGlobalPosition(blocks: List<String>, blockIndex: Int, offsetInBlock: Int): Int {
        var pos = 0
        for (i in 0 until blockIndex) {
            pos += blocks[i].length + 2  // "\n\n" separator
        }
        return pos + offsetInBlock
    }

    fun onBlockFocusChange(blockIndex: Int, focused: Boolean) {
        _focusedBlockIndex.value = if (focused) blockIndex else -1
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && tag !in _tags.value) {
            _tags.value = _tags.value + tag
        }
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    fun insertMarkdownSyntax(syntax: MarkdownSyntax) {
        val current = _contentTextField.value
        val text = current.text
        val selection = current.selection

        val selectedText = text.substring(selection.min, selection.max)

        val beforeCursor = text.substring(0, selection.min)
        val afterCursor = text.substring(selection.max)

        val needNewLineBefore = syntax.newLine && beforeCursor.isNotEmpty() && !beforeCursor.endsWith("\n")
        val prefix = (if (needNewLineBefore) "\n" else "") + syntax.prefix
        val suffix = syntax.suffix

        val insertedText = if (selectedText.isNotEmpty()) {
            prefix + selectedText + suffix
        } else {
            prefix + suffix
        }

        val newText = beforeCursor + insertedText + afterCursor

        val newCursorPos = if (selectedText.isNotEmpty()) {
            selection.min + insertedText.length
        } else {
            selection.min + prefix.length
        }

        _contentTextField.value = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }

    fun insertFileReference(context: Context, uri: Uri) {
        viewModelScope.launch {
            ensureNoteSaved(context)
            val noteId = currentNote?.id ?: return@launch
            val attachmentManager = MediaHelper.getAttachmentManager(context)
            val entry = withContext(Dispatchers.IO) {
                attachmentManager.addAttachment(noteId, uri)
            }

            if (entry != null) {
                val markdown = attachmentManager.getMarkdownReference(entry)
                insertTextAtCursor(markdown)
                loadAttachments(noteId)
            }
        }
    }

    fun insertImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            ensureNoteSaved(context)
            val noteId = currentNote?.id ?: return@launch
            val attachmentManager = MediaHelper.getAttachmentManager(context)
            val entry = withContext(Dispatchers.IO) {
                attachmentManager.addAttachment(noteId, uri)
            }

            if (entry != null) {
                val markdown = attachmentManager.getMarkdownReference(entry)
                insertTextAtCursor(markdown)
                loadAttachments(noteId)
            }
        }
    }

    private suspend fun ensureNoteSaved(context: Context) {
        if (repository == null) {
            repository = NoteRepository(context.applicationContext)
        }
        if (currentNote == null) {
            val note = Note(
                title = _title.value,
                content = _contentTextField.value.text,
                tags = _tags.value
            )
            currentNote = withContext(Dispatchers.IO) {
                repository?.saveNote(note, forceUpdateTimestamp = false)
            }
        }
    }

    private fun insertTextAtCursor(text: String) {
        val current = _contentTextField.value
        val currentText = current.text
        val selection = current.selection

        val beforeCursor = currentText.substring(0, selection.min)
        val afterCursor = currentText.substring(selection.max)

        val needNewLine = beforeCursor.isNotEmpty() && !beforeCursor.endsWith("\n")
        val insertedText = (if (needNewLine) "\n" else "") + text

        val newText = beforeCursor + insertedText + afterCursor
        val newCursorPos = selection.min + insertedText.length

        _contentTextField.value = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }

    private var saveJob: kotlinx.coroutines.Job? = null

    fun saveNote(context: Context) {
        saveJob?.cancel()
        if (repository == null) {
            repository = NoteRepository(context.applicationContext)
        }

        val titleText = _title.value.trim()
        val contentText = _contentTextField.value.text.trim()

        if (currentNote == null && titleText.isEmpty() && contentText.isEmpty()) {
            return
        }

        saveJob = viewModelScope.launch {
            val note = if (currentNote != null) {
                currentNote!!.copy(
                    title = _title.value,
                    content = _contentTextField.value.text,
                    tags = _tags.value
                )
            } else {
                Note(
                    title = _title.value,
                    content = _contentTextField.value.text,
                    tags = _tags.value
                )
            }

            currentNote = withContext(Dispatchers.IO) {
                repository?.saveNote(note, forceUpdateTimestamp = false)
            }
        }
    }

    fun deleteNote(context: Context) {
        if (repository == null) {
            repository = NoteRepository(context.applicationContext)
        }

        currentNote?.let { note ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository?.deleteNote(note.id)
                }
            }
        }
    }

    fun getShareIntent(context: Context): Intent {
        val title = _title.value
        val content = _contentTextField.value.text
        val shareText = if (title.isNotBlank()) "# $title\n\n$content" else content

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    fun getExportMarkdown(): String {
        val title = _title.value
        val content = _contentTextField.value.text
        return if (title.isNotBlank()) "# $title\n\n$content" else content
    }

    fun deleteAttachment(context: Context, entry: AttachmentEntry) {
        val attachmentManager = MediaHelper.getAttachmentManager(context)
        attachmentManager.deleteAttachment(entry.attachId)
        currentNote?.id?.let { loadAttachments(it) }
    }

    fun checkAttachmentIntegrity(context: Context): com.easymd.data.attachment.AttachmentCheckResult? {
        val noteId = currentNote?.id ?: return null
        val attachmentManager = MediaHelper.getAttachmentManager(context)
        return attachmentManager.checkReferences(noteId, _contentTextField.value.text)
    }

    fun cleanOrphanAttachments(context: Context): Int {
        val noteId = currentNote?.id ?: return 0
        val attachmentManager = MediaHelper.getAttachmentManager(context)
        val count = attachmentManager.cleanOrphanAttachments(noteId, _contentTextField.value.text)
        loadAttachments(noteId)
        return count
    }
}

fun splitBlocks(text: String): List<String> {
    if (text.isBlank()) return listOf("")

    val blocks = mutableListOf<String>()
    val current = StringBuilder()
    var inCodeBlock = false
    var fence = ""

    for (line in text.lines()) {
        val trimmed = line.trimStart()
        if (!inCodeBlock && trimmed.startsWith("```")) {
            if (current.isNotEmpty()) {
                blocks.add(current.toString().trimEnd())
                current.clear()
            }
            current.appendLine(line)
            inCodeBlock = true
            fence = trimmed.takeWhile { it == '`' }
        } else if (inCodeBlock) {
            current.appendLine(line)
            if (trimmed.startsWith(fence) && trimmed.trimEnd() == fence) {
                blocks.add(current.toString().trimEnd())
                current.clear()
                inCodeBlock = false
            }
        } else if (line.isBlank()) {
            if (current.isNotEmpty()) {
                blocks.add(current.toString().trimEnd())
                current.clear()
            }
        } else {
            current.appendLine(line)
        }
    }

    if (current.isNotEmpty()) {
        blocks.add(current.toString().trimEnd())
    }

    return blocks.ifEmpty { listOf("") }
}

fun joinBlocks(blocks: List<String>): String {
    return blocks.joinToString("\n\n")
}
