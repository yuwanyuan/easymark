package com.easymd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val parser = Parser.builder().build()
    val document = parser.parse(markdown)

    Column(modifier = modifier) {
        renderNode(document)
    }
}

@Composable
private fun renderNode(node: Node) {
    var currentNode = node.firstChild
    while (currentNode != null) {
        when (currentNode) {
            is Paragraph -> {
                Text(
                    text = renderInline(currentNode),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            is Heading -> {
                val style = when (currentNode.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    3 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                }
                Text(
                    text = renderInline(currentNode),
                    style = style,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            is BulletList -> {
                var listItem = currentNode.firstChild
                while (listItem != null) {
                    if (listItem is ListItem) {
                        Text(
                            text = buildAnnotatedString {
                                append("• ")
                                append(renderInline(listItem))
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 0.dp, bottom = 2.dp)
                        )
                    }
                    listItem = listItem.next
                }
            }
            is OrderedList -> {
                var listItem = currentNode.firstChild
                var index = 1
                while (listItem != null) {
                    if (listItem is ListItem) {
                        Text(
                            text = buildAnnotatedString {
                                append("$index. ")
                                append(renderInline(listItem))
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 0.dp, bottom = 2.dp)
                        )
                        index++
                    }
                    listItem = listItem.next
                }
            }
            is Code -> {
                Text(
                    text = currentNode.literal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            is FencedCodeBlock -> {
                Text(
                    text = currentNode.literal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            is BlockQuote -> {
                Text(
                    text = renderInline(currentNode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
                )
            }
            is ThematicBreak -> {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                )
            }
            is HardLineBreak -> {
                Spacer(modifier = Modifier.height(8.dp))
            }
            is SoftLineBreak -> {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        currentNode = currentNode.next
    }
}

@Composable
private fun renderInline(node: Node): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var current = node.firstChild
        while (current != null) {
            when (current) {
                is Text -> append(current.literal)
                is Emphasis -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(renderInline(current).text)
                    }
                }
                is StrongEmphasis -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(renderInline(current).text)
                    }
                }
                is Code -> {
                    withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(current.literal)
                    }
                }
                is Link -> {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(renderInline(current).text)
                    }
                }
                else -> {
                    if (current.firstChild != null) {
                        append(renderInline(current).text)
                    }
                }
            }
            current = current.next
        }
    }
}
