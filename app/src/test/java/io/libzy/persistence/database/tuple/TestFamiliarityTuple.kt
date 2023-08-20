package io.libzy.persistence.database.tuple

import org.junit.Assert.assertEquals
import org.junit.Test

class TestFamiliarityTuple {

    @Test
    fun `sort familiarity tuples`() {
        val familiarityTuples = listOf(
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = true), // 8
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = false), // 4
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = false), // 2
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = false), // 1
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = false), // 3
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = false), // 6
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = true), // 12
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = false), // 5
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = true), // 10
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = true), // 9
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = false), // 7
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = true), // 14
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = true), // 13
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = true), // 11
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = true), // 15
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = false) // 0
        )

        val expectedSortOrder = listOf(
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = false), // 0
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = false), // 1
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = false), // 2
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = false), // 3
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = false), // 4
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = false), // 5
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = false), // 6
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = false), // 7
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = true), // 8
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = false, longTermFavorite = true), // 9
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = true), // 10
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = false, longTermFavorite = true), // 11
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = true), // 12
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = false, mediumTermFavorite = true, longTermFavorite = true), // 13
            FamiliarityTuple(recentlyPlayed = false, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = true), // 14
            FamiliarityTuple(recentlyPlayed = true, shortTermFavorite = true, mediumTermFavorite = true, longTermFavorite = true), // 15
        )

        assertEquals(expectedSortOrder, familiarityTuples.sorted())
    }
}