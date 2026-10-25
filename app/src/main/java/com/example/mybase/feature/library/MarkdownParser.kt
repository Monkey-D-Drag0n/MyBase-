package com.example.mybase.feature.library

enum class MarkdownBlockType { Heading, Paragraph, Quote, Code, BulletList, OrderedList, Rule, Table }

data class MarkdownBlock(
    val type: MarkdownBlockType,
    val text: String = "",
    val level: Int = 0,
    val items: List<String> = emptyList(),
    val table: List<List<String>> = emptyList(),
    val links: List<String> = emptyList()
)

/** Small native renderer for common Markdown constructs; unsupported syntax remains readable as paragraph text. */
object MarkdownParser {
    private val heading = Regex("^(#{1,6})\\s+(.+)$")
    private val bullet = Regex("^\\s*[-*+]\\s+(.+)$")
    private val ordered = Regex("^\\s*\\d+[.)]\\s+(.+)$")
    private val link = Regex("\\[([^]]+)]\\((https?://[^)]+)\\)", RegexOption.IGNORE_CASE)

    fun parse(source: String): List<MarkdownBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').lines()
        val result = mutableListOf<MarkdownBlock>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank()) { index++; continue }
            if (line.trimStart().startsWith("```")) {
                index++
                val code = mutableListOf<String>()
                while (index < lines.size && !lines[index].trimStart().startsWith("```")) code += lines[index++]
                if (index < lines.size) index++
                result += block(MarkdownBlockType.Code, code.joinToString("\n"))
                continue
            }
            val headingMatch = heading.matchEntire(line)
            if (headingMatch != null) { result += block(MarkdownBlockType.Heading, headingMatch.groupValues[2], headingMatch.groupValues[1].length); index++; continue }
            if (line.trim().matches(Regex("(?:---+|___+|\\*\\*\\*+)"))) { result += MarkdownBlock(MarkdownBlockType.Rule); index++; continue }
            if (line.trimStart().startsWith(">")) {
                val quote = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith(">")) quote += lines[index++].trimStart().removePrefix(">").trimStart()
                result += block(MarkdownBlockType.Quote, quote.joinToString("\n")); continue
            }
            if (index + 1 < lines.size && line.contains('|') && isTableDivider(lines[index + 1])) {
                val rows = mutableListOf(splitTableRow(line))
                index += 2
                while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) rows += splitTableRow(lines[index++])
                val links = rows.flatten().flatMap(::linksIn)
                result += MarkdownBlock(MarkdownBlockType.Table, table = rows, links = links)
                continue
            }
            val listType = when { bullet.matches(line) -> MarkdownBlockType.BulletList; ordered.matches(line) -> MarkdownBlockType.OrderedList; else -> null }
            if (listType != null) {
                val items = mutableListOf<String>()
                while (index < lines.size) {
                    val match = if (listType == MarkdownBlockType.BulletList) bullet.matchEntire(lines[index]) else ordered.matchEntire(lines[index])
                    if (match == null) break
                    items += match.groupValues[1]; index++
                }
                result += MarkdownBlock(listType, items = items, links = items.flatMap(::linksIn))
                continue
            }
            val paragraph = mutableListOf<String>()
            while (index < lines.size && lines[index].isNotBlank() && !startsBlock(lines, index)) paragraph += lines[index++]
            if (paragraph.isEmpty()) { paragraph += lines[index++] }
            result += block(MarkdownBlockType.Paragraph, paragraph.joinToString(" "))
        }
        return result
    }

    fun displayText(markdown: String): String = link.replace(markdown) { it.groupValues[1] }
        .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
        .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
        .replace("**", "").replace("__", "").replace("`", "")

    private fun block(type: MarkdownBlockType, text: String, level: Int = 0) = MarkdownBlock(type, text, level, links = linksIn(text))
    private fun linksIn(text: String) = link.findAll(text).map { it.groupValues[2] }.distinct().toList()
    private fun startsBlock(lines: List<String>, i: Int): Boolean = heading.matches(lines[i]) || lines[i].trimStart().startsWith(">") || lines[i].trimStart().startsWith("```") || bullet.matches(lines[i]) || ordered.matches(lines[i]) || lines[i].trim().matches(Regex("(?:---+|___+|\\*\\*\\*+)")) || (i + 1 < lines.size && lines[i].contains('|') && isTableDivider(lines[i + 1]))
    private fun isTableDivider(line: String) = line.contains('|') && line.trim().removePrefix("|").removeSuffix("|").split('|').all { it.trim().matches(Regex(":?-{3,}:?")) }
    private fun splitTableRow(line: String) = line.trim().removePrefix("|").removeSuffix("|").split('|').map(String::trim)
}
