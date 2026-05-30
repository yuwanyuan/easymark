package com.easymd.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class MarkdownSyntax(
    val displayName: String,
    val prefix: String,
    val suffix: String = "",
    val newLine: Boolean = false,
    val group: String
) {
    H1("一级标题", prefix = "# ", newLine = true, group = "标题"),
    H2("二级标题", prefix = "## ", newLine = true, group = "标题"),
    H3("三级标题", prefix = "### ", newLine = true, group = "标题"),
    H4("四级标题", prefix = "#### ", newLine = true, group = "标题"),
    H5("五级标题", prefix = "##### ", newLine = true, group = "标题"),
    H6("六级标题", prefix = "###### ", newLine = true, group = "标题"),
    BOLD("粗体", prefix = "**", suffix = "**", group = "文本样式"),
    ITALIC("斜体", prefix = "*", suffix = "*", group = "文本样式"),
    STRIKETHROUGH("删除线", prefix = "~~", suffix = "~~", group = "文本样式"),
    INLINE_CODE("行内代码", prefix = "`", suffix = "`", group = "代码"),
    CODE_BLOCK("代码块", prefix = "```\n", suffix = "\n```", newLine = true, group = "代码"),
    BLOCKQUOTE("引用", prefix = "> ", newLine = true, group = "块级元素"),
    UNORDERED_LIST("无序列表", prefix = "- ", newLine = true, group = "列表"),
    ORDERED_LIST("有序列表", prefix = "1. ", newLine = true, group = "列表"),
    TASK_LIST("任务列表", prefix = "- [ ] ", newLine = true, group = "列表"),
    LINK("链接", prefix = "[", suffix = "](url)", group = "链接与图片"),
    IMAGE("图片", prefix = "![", suffix = "](url)", group = "链接与图片"),
    HORIZONTAL_RULE("分隔线", prefix = "---", newLine = true, group = "块级元素"),
    TABLE("表格", prefix = "| 标题 | 标题 |\n| --- | --- |\n| 内容 | 内容 |", newLine = true, group = "扩展语法"),
    FOOTNOTE("注脚", prefix = "[^1]", suffix = "\n\n[^1]: ", newLine = false, group = "扩展语法")
}

private fun MarkdownSyntax.toolbarLabel(): AnnotatedString {
    return when (this) {
        MarkdownSyntax.H1 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) { append("H1") } }
        MarkdownSyntax.H2 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)) { append("H2") } }
        MarkdownSyntax.H3 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp)) { append("H3") } }
        MarkdownSyntax.H4 -> buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)) { append("H4") } }
        MarkdownSyntax.H5 -> buildAnnotatedString { withStyle(SpanStyle(fontSize = 11.sp)) { append("H5") } }
        MarkdownSyntax.H6 -> buildAnnotatedString { withStyle(SpanStyle(fontSize = 10.sp)) { append("H6") } }
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
fun MarkdownToolbar(
    onSyntaxClick: (MarkdownSyntax) -> Unit,
    enabledItems: List<MarkdownSyntax> = MarkdownSyntax.entries,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                enabledItems.forEach { syntax ->
                    IconButton(
                        onClick = { onSyntaxClick(syntax) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = syntax.toolbarLabel(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
