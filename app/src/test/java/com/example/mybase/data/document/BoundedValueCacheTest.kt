package com.example.mybase.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BoundedValueCacheTest {
    @Test fun lruEvictsByWeightAndIdentityChangesProduceDistinctThumbnailKeys() {
        val cache = BoundedValueCache<String, String>(6) { it.length }
        cache.put("first", "123")
        cache.put("second", "45")
        assertEquals("123", cache.get("first"))
        cache.put("third", "678")
        assertNull(cache.get("second"))
        assertEquals(2, cache.size())
        assertEquals("content://p/1|application/pdf", PdfThumbnailStore.cacheKey("content://p/1", "application/pdf"))
        assertEquals("content://p/2|application/pdf", PdfThumbnailStore.cacheKey("content://p/2", "application/pdf"))
        cache.remove("first")
        assertNull(cache.get("first"))
        assertNotEquals(PdfThumbnailStore.cacheKey("content://p/1", "application/pdf"), PdfThumbnailStore.cacheKey("content://p/1", "image/png"))
    }
}
