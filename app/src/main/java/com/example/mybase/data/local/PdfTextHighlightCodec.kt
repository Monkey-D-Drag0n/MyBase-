package com.example.mybase.data.local

import com.example.mybase.core.model.PdfTextHighlight
import com.example.mybase.core.model.PdfTextHighlightBounds
import java.util.Base64

/** Independent, versioned store; existing H1 region records are never rewritten or discarded. */
internal object PdfTextHighlightCodec {
    private const val VERSION = "TH1"

    fun encode(values: List<PdfTextHighlight>): String = values.filter(PdfTextHighlight::isValid).joinToString("\n") { h ->
        listOf(VERSION, pack(h.id), pack(h.documentId), h.page.toString(), pack(h.text), h.colorArgb.toString(),
            h.bounds.joinToString(";") { listOf(it.left, it.top, it.right, it.bottom).joinToString(",") }).joinToString("|")
    }

    fun decode(raw: String): List<PdfTextHighlight> = raw.lineSequence().filter(String::isNotBlank).mapNotNull { row ->
        runCatching {
            val f = row.split('|')
            if (f.size != 7 || f[0] != VERSION) return@mapNotNull null
            val bounds = f[6].split(';').take(2000).map { coords ->
                val p = coords.split(',')
                require(p.size == 4)
                PdfTextHighlightBounds(p[0].toFloat(), p[1].toFloat(), p[2].toFloat(), p[3].toFloat())
            }
            PdfTextHighlight(packDecode(f[1]), packDecode(f[2]), f[3].toInt(), packDecode(f[4]), bounds, f[5].toInt())
                .takeIf(PdfTextHighlight::isValid)
        }.getOrNull()
    }.toList()

    private fun pack(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun packDecode(value: String) = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}
