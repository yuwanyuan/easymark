package com.easymd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CmText
import org.commonmark.parser.Parser

// ─── Block types ───────────────────────────────────────────

enum class BlockType {
    HEADING,
    PARAGRAPH,
    UNORDERED_LIST,
    ORDERED_LIST,
    TASK_LIST_DONE,
    TASK_LIST_TODO,
    CODE_BLOCK,
    BLOCKQUOTE,
    HORIZONTAL_RULE
}

data class ParsedBlock(
    val type: BlockType,
    val displayText: String,
    val headingLevel: Int = 0,
    val listNumber: Int = 1,
    val taskChecked: Boolean = false
)

fun detectBlockType(text: String): BlockType {
    val t = text.trimStart()
    return when {
        t.matches(Regex("^#{1,6} ")) || t.matches(Regex("^#{1,6}\\S")) -> BlockType.HEADING
        t.startsWith("- [x] ") || t.startsWith("- [X] ") -> BlockType.TASK_LIST_DONE
        t.startsWith("- [ ] ") -> BlockType.TASK_LIST_TODO
        t.startsWith("- ") || t.startsWith("* ") -> BlockType.UNORDERED_LIST
        t.matches(Regex("^\\d+\\.\\s.*")) -> BlockType.ORDERED_LIST
        t.startsWith("> ") -> BlockType.BLOCKQUOTE
        t.startsWith("```") -> BlockType.CODE_BLOCK
        t.matches(Regex("^-{3,}$|^\\*{3,}$|^_{3,}$")) -> BlockType.HORIZONTAL_RULE
        else -> BlockType.PARAGRAPH
    }
}

fun parseBlock(text: String): ParsedBlock {
    val type = detectBlockType(text)
    return when (type) {
        BlockType.HEADING -> {
            val level = text.trimStart().takeWhile { it == '#' }.length
            val display = text.trimStart().removePrefix("#".repeat(level)).trimStart()
            ParsedBlock(type, display, headingLevel = level)
        }
        BlockType.TASK_LIST_DONE -> {
            val display = text.trimStart().removePrefix("- [x] ").removePrefix("- [X] ").trimStart()
            ParsedBlock(type, display, taskChecked = true)
        }
        BlockType.TASK_LIST_TODO -> {
            val display = text.trimStart().removePrefix("- [ ] ").trimStart()
            ParsedBlock(type, display, taskChecked = false)
        }
        BlockType.UNORDERED_LIST -> {
            val display = text.trimStart().removePrefix("- ").removePrefix("* ").trimStart()
            ParsedBlock(type, display)
        }
        BlockType.ORDERED_LIST -> {
            val numStr = text.trimStart().takeWhile { it.isDigit() }
            val num = numStr.toIntOrNull() ?: 1
            val display = text.trimStart().removePrefix("$numStr. ").trimStart()
            ParsedBlock(type, display, listNumber = num)
        }
        BlockType.BLOCKQUOTE -> {
            val display = text.lines().joinToString("\n") { line ->
                line.trimStart().removePrefix("> ").removePrefix(">")
            }
            ParsedBlock(type, display)
        }
        BlockType.CODE_BLOCK -> {
            val lines = text.lines()
            val display = if (lines.size >= 2) {
                lines.subList(1, lines.size - 1).joinToString("\n")
            } else {
                text.removeSurrounding("```")
            }
            ParsedBlock(type, display)
        }
        BlockType.HORIZONTAL_RULE -> ParsedBlock(type, "")
        BlockType.PARAGRAPH -> ParsedBlock(type, text)
    }
}

// ─── Block parser ──────────────────────────────────────────

fun parseBlocks(text: String): List<String> {
    if (text.isBlank()) return listOf("")

    val blocks = mutableListOf<String>()
    val current = StringBuilder()
    var inCodeBlock = false
    var codeBlockFence = ""

    for (line in text.lines()) {
        val trimmed = line.trimStart()
        if (!inCodeBlock && trimmed.startsWith("```")) {
            if (current.isNotEmpty()) {
                blocks.add(current.toString().trimEnd())
                current.clear()
            }
            current.appendLine(line)
            inCodeBlock = true
            codeBlockFence = trimmed.takeWhile { it == '`' }
        } else if (inCodeBlock) {
            current.appendLine(line)
            if (trimmed.startsWith(codeBlockFence) && trimmed.count { it == '`' } >= codeBlockFence.length && trimmed.trimEnd() == codeBlockFence) {
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

// ─── Inline markdown → AnnotatedString ─────────────────────

private val inlineParser = Parser.builder().build()

fun renderInlineMarkdown(text: String, linkColor: Color, codeBg: Color): AnnotatedString {
    if (text.isBlank()) return AnnotatedString(text)
    val document = inlineParser.parse(text)
    return buildAnnotatedString {
        appendNode(document, linkColor, codeBg)
    }
}

private fun AnnotatedString.Builder.appendNode(node: Node, linkColor: Color, codeBg: Color) {
    when (node) {
        is CmText -> append(node.literal)
        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                var child = node.firstChild
                while (child != null) { appendNode(child, linkColor, codeBg); child = child.next }
            }
        }
        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                var child = node.firstChild
                while (child != null) { appendNode(child, linkColor, codeBg); child = child.next }
            }
        }
        is Code -> {
            withStyle(SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                background = codeBg
            )) {
                append(node.literal)
            }
        }
        is Link -> {
            withStyle(SpanStyle(color = linkColor)) {
                var child = node.firstChild
                while (child != null) { appendNode(child, linkColor, codeBg); child = child.next }
            }
        }
        else -> {
            var child = node.firstChild
            while (child != null) { appendNode(child, linkColor, codeBg); child = child.next }
        }
    }
}

// ─── Cursor position helper ─────────────────────────────────

private fun findCursorInBlocks(blocks: List<String>, cursorPosition: Int): Pair<Int, Int> {
    var pos = 0
    for ((i, block) in blocks.withIndex()) {
        val blockLen = block.length
        if (cursorPosition >= pos && cursorPosition <= pos + blockLen) {
            return Pair(i, (cursorPosition - pos).coerceIn(0, blockLen))
        }
        pos += blockLen + 2
    }
    val lastIdx = (blocks.size - 1).coerceAtLeast(0)
    val lastLen = blocks.getOrElse(lastIdx) { "" }.length
    return Pair(lastIdx, lastLen)
}

// ─── Markdown syntax highlighting for editing mode ───────────

class MarkdownEditorVisualTransformation(
    private val textColor: Color,
    private val syntaxColor: Color,
    private val codeBg: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        var i = 0
        while (i < raw.length) {
            val lineStart = i
            val lineEnd = raw.indexOf('\n', lineStart).let { if (it == -1) raw.length else it + 1 }
            val line = raw.substring(lineStart, lineEnd)

            when {
                line.matches(Regex("^#{1,6}[ \t].*")) -> {
                    val hashEnd = line.indexOfFirst { it != '#' }
                    val spaceEnd = line.indexOfFirst { it != ' ' && it != '\t' }
                    builder.addStyle(SpanStyle(color = syntaxColor), lineStart, lineStart + hashEnd)
                    if (spaceEnd > hashEnd) {
                        builder.addStyle(SpanStyle(color = syntaxColor), lineStart + hashEnd, lineStart + spaceEnd)
                    }
                }
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    builder.addStyle(SpanStyle(color = syntaxColor), lineStart, lineStart + 6)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    builder.addStyle(SpanStyle(color = syntaxColor), lineStart, lineStart + 2)
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val dotIdx = line.indexOf('.')
                    builder.addStyle(SpanStyle(color = syntaxColor), lineStart, lineStart + dotIdx + 2)
                }
                line.startsWith("> ") -> {
                    builder.addStyle(SpanStyle(color = syntaxColor), lineStart, lineStart + 2)
                }
            }

            highlightInlinePatterns(builder, line, lineStart, syntaxColor, codeBg)

            i = lineEnd
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightInlinePatterns(
        builder: AnnotatedString.Builder,
        line: String,
        lineOffset: Int,
        syntaxColor: Color,
        codeBg: Color
    ) {
        val codeRanges = mutableListOf<IntRange>()
        val codePattern = Regex("`[^`]+`")
        for (match in codePattern.findAll(line)) {
            codeRanges.add(match.range.first..match.range.last)
        }

        val boldPattern = Regex("\\*\\*[^*]+\\*\\*")
        for (match in boldPattern.findAll(line)) {
            val r = match.range
            if (codeRanges.none { r.first in it || r.last in it }) {
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.first, lineOffset + r.first + 2)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), lineOffset + r.first + 2, lineOffset + r.last - 1)
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.last - 1, lineOffset + r.last + 1)
            }
        }

        val italicPattern = Regex("(?<!\\*)\\*(?!\\*)[^*]+\\*(?!\\*)")
        for (match in italicPattern.findAll(line)) {
            val r = match.range
            if (codeRanges.none { r.first in it || r.last in it }) {
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.first, lineOffset + r.first + 1)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), lineOffset + r.first + 1, lineOffset + r.last)
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.last, lineOffset + r.last + 1)
            }
        }

        val strikethroughPattern = Regex("~~[^~]+~~")
        for (match in strikethroughPattern.findAll(line)) {
            val r = match.range
            if (codeRanges.none { r.first in it || r.last in it }) {
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.first, lineOffset + r.first + 2)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), lineOffset + r.first + 2, lineOffset + r.last - 1)
                builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.last - 1, lineOffset + r.last + 1)
            }
        }

        for (match in codePattern.findAll(line)) {
            val r = match.range
            builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.first, lineOffset + r.first + 1)
            builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, background = codeBg), lineOffset + r.first + 1, lineOffset + r.last)
            builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.last, lineOffset + r.last + 1)
        }

        val linkPattern = Regex("!?\\[[^]]*\\]\\([^)]*\\)")
        for (match in linkPattern.findAll(line)) {
            val r = match.range
            if (codeRanges.none { r.first in it || r.last in it }) {
                val matchStr = match.value
                val hasImage = matchStr.startsWith("!")
                if (hasImage) {
                    builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + r.first, lineOffset + r.first + 1)
                }
                val bracketOpen = line.indexOf('[', r.first)
                val bracketClose = line.indexOf(']', bracketOpen)
                val parenOpen = line.indexOf('(', bracketClose)
                val parenClose = line.indexOf(')', parenOpen)
                if (bracketOpen in line.indices) builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + bracketOpen, lineOffset + bracketOpen + 1)
                if (bracketClose in line.indices) builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + bracketClose, lineOffset + bracketClose + 1)
                if (parenOpen in line.indices) builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + parenOpen, lineOffset + parenOpen + 1)
                if (parenClose in line.indices) builder.addStyle(SpanStyle(color = syntaxColor), lineOffset + parenClose, lineOffset + parenClose + 1)
            }
        }
    }
}

// ─── Editor ────────────────────────────────────────────────

@Composable
fun WysiwygEditor(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    onBlockContentChange: (Int, String, Int) -> Unit,
    onBlockFocusChange: (Int, Boolean) -> Unit,
    onAnyBlockFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentFocusTrigger: Int = 0
) {
    val blocks = remember(content.text) { parseBlocks(content.text) }
    var focusedIndex by remember { mutableStateOf(-1) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val (cursorBlockIndex, cursorOffsetInBlock) = remember(content.text, content.selection.start) {
        findCursorInBlocks(blocks, content.selection.start)
    }

    // When external change (toolbar) moves cursor to different block, auto-switch focus
    var prevContentText by remember { mutableStateOf(content.text) }
    LaunchedEffect(content.text) {
        if (content.text != prevContentText && focusedIndex >= 0 && cursorBlockIndex != focusedIndex) {
            focusedIndex = cursorBlockIndex
        }
        prevContentText = content.text
    }

    // Handle focus trigger from title field "Next" action
    LaunchedEffect(contentFocusTrigger) {
        if (contentFocusTrigger > 0 && blocks.isNotEmpty()) {
            focusedIndex = 0
            onBlockFocusChange(0, true)
            onAnyBlockFocusChange(true)
        }
    }

    val blockFocusRequesters = remember(blocks.size) {
        blocks.indices.map { FocusRequester() }
    }

    // When focusedIndex changes, request focus on that block
    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0 && focusedIndex < blockFocusRequesters.size) {
            try { blockFocusRequesters[focusedIndex].requestFocus() } catch (_: Exception) {}
        }
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(blocks, key = { index, _ -> "block_$index" }) { index, blockText ->
            val parsed = parseBlock(blockText)

            EditingBlock(
                text = blockText,
                parsed = parsed,
                focusRequester = blockFocusRequesters.getOrElse(index) { FocusRequester() },
                onTextChange = { newText, cursorOffset ->
                    onBlockContentChange(index, newText, cursorOffset)
                },
                onFocusGained = {
                    // Only update if focus genuinely moved to a different block
                    if (focusedIndex != index) {
                        focusedIndex = index
                        onBlockFocusChange(index, true)
                    }
                    onAnyBlockFocusChange(true)
                },
                initialCursorOffset = if (index == cursorBlockIndex) cursorOffsetInBlock else null
            )
        }

        // Bottom spacer — tap to dismiss focus or start editing first block
        item {
            Spacer(
                modifier = Modifier
                    .height(200.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (focusedIndex >= 0) {
                            focusManager.clearFocus()
                            onBlockFocusChange(focusedIndex, false)
                            onAnyBlockFocusChange(false)
                            focusedIndex = -1
                        } else if (blocks.isNotEmpty()) {
                            focusedIndex = 0
                            onBlockFocusChange(0, true)
                            onAnyBlockFocusChange(true)
                        }
                    }
            )
        }
    }
}

// ─── Editing block ─────────────────────────────────────────

@Composable
private fun EditingBlock(
    text: String,
    parsed: ParsedBlock,
    focusRequester: FocusRequester,
    onTextChange: (String, Int) -> Unit,
    onFocusGained: () -> Unit,
    initialCursorOffset: Int? = null
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    var lastSyncedText by remember { mutableStateOf(text) }

    // Sync from external changes (e.g., toolbar insertion)
    LaunchedEffect(text) {
        if (text != lastSyncedText) {
            lastSyncedText = text
            val cursor = initialCursorOffset ?: text.length
            fieldValue = TextFieldValue(text, selection = TextRange(cursor.coerceIn(0, text.length)))
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val syntaxHighlight = remember(surfaceColor, secondaryColor, surfaceVariant) {
        MarkdownEditorVisualTransformation(
            textColor = surfaceColor,
            syntaxColor = secondaryColor,
            codeBg = surfaceVariant.copy(alpha = 0.4f)
        )
    }

    // Pick text style based on block type
    val textStyle = when (parsed.type) {
        BlockType.HEADING -> when (parsed.headingLevel) {
            1 -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 36.sp)
            2 -> TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 32.sp)
            3 -> TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 28.sp)
            4 -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp)
            else -> TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp)
        }
        BlockType.CODE_BLOCK -> TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        BlockType.BLOCKQUOTE -> TextStyle(
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        else -> TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        )
    }

    // Block decoration
    val blockModifier = when (parsed.type) {
        BlockType.CODE_BLOCK -> Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(12.dp)
        BlockType.BLOCKQUOTE -> Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
        else -> Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    }

    Box(modifier = blockModifier) {
        // Blockquote left border
        if (parsed.type == BlockType.BLOCKQUOTE) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            )
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val processed = applyHeadingAutoSpace(newValue)
                fieldValue = processed
                lastSyncedText = processed.text
                onTextChange(processed.text, processed.selection.start)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (parsed.type == BlockType.BLOCKQUOTE) Modifier.padding(start = 12.dp) else Modifier)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) onFocusGained()
                },
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            visualTransformation = syntaxHighlight,
            decorationBox = { innerTextField ->
                Box {
                    if (fieldValue.text.isEmpty()) {
                        Text(
                            text = "输入内容...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ─── Heading auto-space ────────────────────────────────────

private fun applyHeadingAutoSpace(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val sel = value.selection

    // Find if any line starts with #{1,6} followed by non-space, non-newline
    val pattern = Regex("^(#{1,6})([^ \\t\\n])", RegexOption.MULTILINE)
    val match = pattern.find(text) ?: return value

    val hashes = match.groupValues[1]
    val afterChar = match.groupValues[2]
    val matchStart = match.range.first

    // Insert space after # markers
    val before = text.substring(0, matchStart)
    val after = text.substring(matchStart + match.value.length)
    val newText = before + hashes + " " + afterChar + after

    // Adjust cursor: if cursor is after the insertion point, shift by +1
    val spaceInsertPos = matchStart + hashes.length
    val newSel = TextRange(
        if (sel.start > spaceInsertPos) sel.start + 1 else sel.start,
        if (sel.end > spaceInsertPos) sel.end + 1 else sel.end
    )

    return TextFieldValue(newText, selection = newSel)
}
