package com.dubl.character.android.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class DevelopmentScreenCacheTest {
    @Test
    fun identicalKeyReusesPreparedValue() {
        val cache = RetainedPreparationCache<String, Any>(maximumEntries = 2)
        var preparations = 0

        val first = cache.getOrPut("character-a:v1") { Any().also { preparations++ } }
        val second = cache.getOrPut("character-a:v1") { Any().also { preparations++ } }

        assertSame(first, second)
        assertEquals(1, preparations)
    }

    @Test
    fun changedCharacterRevisionCreatesFreshValue() {
        val cache = RetainedPreparationCache<String, Any>(maximumEntries = 2)

        val first = cache.getOrPut("character-a:development-1") { Any() }
        val second = cache.getOrPut("character-a:development-2") { Any() }

        assertNotSame(first, second)
    }

    @Test
    fun oldestPreparationIsEvictedWhenBoundIsExceeded() {
        val cache = RetainedPreparationCache<String, Any>(maximumEntries = 2)
        var firstPreparations = 0

        cache.getOrPut("first") { Any().also { firstPreparations++ } }
        cache.getOrPut("second") { Any() }
        cache.getOrPut("third") { Any() }
        cache.getOrPut("first") { Any().also { firstPreparations++ } }

        assertEquals(2, firstPreparations)
    }
}
