package com.easymd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun MarkdownSyntax.settingPreview(): AnnotatedString {
    return when (this) {
        MarkdownSyntax.H1 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("H1") } }
        MarkdownSyntax.H2 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("H2") } }
        MarkdownSyntax.H3 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("H3") } }
        MarkdownSyntax.H4 -> buildAnnotatedString { append("H4") }
        MarkdownSyntax.H5 -> buildAnnotatedString { append("H5") }
        MarkdownSyntax.H6 -> buildAnnotatedString { append("H6") }
        MarkdownSyntax.BOLD -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("B") } }
        MarkdownSyntax.ITALIC -> buildAnnotatedString { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("I") } }
        MarkdownSyntax.STRIKETHROUGH -> buildAnnotatedString { withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append("S") } }
        MarkdownSyntax.INLINE_CODE -> buildAnnotatedString { withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("</>") } }
        MarkdownSyntax.CODE_BLOCK -> buildAnnotatedString { withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("```") } }
        MarkdownSyntax.BLOCKQUOTE -> buildAnnotatedString { append(">") }
        MarkdownSyntax.UNORDERED_LIST -> buildAnnotatedString { append("-") }
        MarkdownSyntax.ORDERED_LIST -> buildAnnotatedString { append("1.") }
        MarkdownSyntax.TASK_LIST -> buildAnnotatedString { append("[ ]") }
        MarkdownSyntax.LINK -> buildAnnotatedString { append("[]") }
        MarkdownSyntax.IMAGE -> buildAnnotatedString { append("![]") }
        MarkdownSyntax.HORIZONTAL_RULE -> buildAnnotatedString { append("---") }
        MarkdownSyntax.TABLE -> buildAnnotatedString { append("||") }
        MarkdownSyntax.FOOTNOTE -> buildAnnotatedString { append("[^]") }
    }
}

@Composable
fun ToolbarSettingsDialog(
    enabledItems: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val currentEnabled = remember { mutableStateOf(enabledItems.toMutableSet()) }

    val syntaxGroups = MarkdownSyntax.entries.groupBy { it.group }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义快捷栏") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择要在快捷栏中显示的语法按钮",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                syntaxGroups.forEach { (groupName, items) ->
                    Text(
                        text = groupName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    items.forEach { syntax ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = syntax.name in currentEnabled.value,
                                onCheckedChange = { checked ->
                                    currentEnabled.value = currentEnabled.value.toMutableSet().apply {
                                        if (checked) add(syntax.name) else remove(syntax.name)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = syntax.settingPreview(),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = syntax.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        currentEnabled.value = MarkdownSyntax.entries.map { it.name }.toMutableSet()
                    }) {
                        Text("全选")
                    }
                    TextButton(onClick = {
                        currentEnabled.value = emptySet<String>().toMutableSet()
                    }) {
                        Text("清空")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentEnabled.value) },
                enabled = currentEnabled.value.isNotEmpty()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
