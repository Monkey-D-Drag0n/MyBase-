package com.example.mybase.data.local

import com.example.mybase.core.model.PdfHighlight
import java.util.Base64

internal object PdfHighlightCodec {
    private const val VERSION = "H1"
    fun encode(values: List<PdfHighlight>): String = values.filter(PdfHighlight::isValid).joinToString("\n") { h ->
        listOf(VERSION, pack(h.id), pack(h.documentId), h.page.toString(), h.left.toString(), h.top.toString(), h.right.toString(), h.bottom.toString(), h.colorArgb.toString()).joinToString("|")
    }
    fun decode(raw: String): List<PdfHighlight> = raw.lineSequence().filter(String::isNotBlank).mapNotNull { row ->
        runCatching {
            val f = row.split('|')
            if (f.size != 9 || f[0] != VERSION) return@mapNotNull null
            PdfHighlight(packDecode(f[1]), packDecode(f[2]), f[3].toInt(), f[4].toFloat(), f[5].toFloat(), f[6].toFloat(), f[7].toFloat(), f[8].toInt()).takeIf(PdfHighlight::isValid)
        }.getOrNull()
    }.toList()
    private fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun packDecode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}
