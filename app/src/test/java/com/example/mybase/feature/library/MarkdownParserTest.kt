package com.example.mybase.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test fun parsesHeadingsEmphasisCodeQuotesListsTablesAndLinks() {
        val blocks = MarkdownParser.parse("""# Heading
Paragraph with **bold**, *italic*, and `code` [site](https://example.com).
> A quote
- one
- two
1. first
2. second
| Name | Value |
| --- | --- |
| A | B |
```kotlin
val x = 1
```
---""")
        assertEquals(MarkdownBlockType.Heading, blocks[0].type)
        assertEquals(MarkdownBlockType.Paragraph, blocks[1].type)
        assertEquals(listOf("https://example.com"), blocks[1].links)
        assertTrue(MarkdownParser.displayText(blocks[1].text).contains("bold"))
        assertTrue(blocks.any { it.type == MarkdownBlockType.Quote })
        assertTrue(blocks.any { it.type == MarkdownBlockType.BulletList })
        assertTrue(blocks.any { it.type == MarkdownBlockType.OrderedList })
        assertTrue(blocks.any { it.type == MarkdownBlockType.Table })
        assertTrue(blocks.any { it.type == MarkdownBlockType.Code })
        assertTrue(blocks.any { it.type == MarkdownBlockType.Rule })
    }
}
