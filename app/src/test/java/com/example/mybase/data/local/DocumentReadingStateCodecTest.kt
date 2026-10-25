package com.example.mybase.data.local

import com.example.mybase.core.model.DocumentReadingState
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentReadingStateCodecTest {
    @Test fun readingStateAndBookmarksRoundTripWithUriDelimitersAndUnicode() {
        val state = DocumentReadingState("content://provider/文件|1", lastPage = 12, bookmarks = listOf(2, 8, 2))
        assertEquals(listOf(state.copy(bookmarks = listOf(2, 8))), DocumentReadingStateCodec.decode(DocumentReadingStateCodec.encode(listOf(state))))
    }

    @Test fun malformedMetadataIsIgnored() {
        assertEquals(emptyList<DocumentReadingState>(), DocumentReadingStateCodec.decode("bad|no-page|oops\nnot-three-fields"))
    }
}
