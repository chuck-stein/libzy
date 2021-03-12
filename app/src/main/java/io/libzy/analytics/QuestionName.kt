package io.libzy.analytics

import io.libzy.analytics.AnalyticsConstants.Events

/**
 * Contains the possible values for [Events.VIEW_QUESTION].
 */
enum class QuestionName(val value: String) {
    FAMILIARITY("familiarity"),
    INSTRUMENTALNESS("instrumentalness"),
    ACOUSTICNESS("acousticness"),
    VALENCE("valence"),
    ENERGY("energy"),
    DANCEABILITY("danceability"),
    GENRES("genres")
}
