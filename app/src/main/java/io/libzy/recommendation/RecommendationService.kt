package io.libzy.recommendation

import io.libzy.domain.Query
import io.libzy.domain.RecommendationCategory
import io.libzy.domain.toAlbumResult
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.util.capitalizeAllWords
import io.libzy.util.combinationsOfSize
import io.libzy.util.joinToUserFriendlyString
import javax.inject.Inject
import kotlin.math.abs

/**
 * Holds functionality related to searching through a set of albums to find recommendations based on a certain mood.
 */
class RecommendationService @Inject constructor() {

    /**
     * Recommend genres contained within the given [libraryAlbums],
     * from highest to lowest relevance to the given mood-based [query].
     */
    fun recommendGenres(query: Query, libraryAlbums: List<LibraryAlbum>): List<String> {
        val genresToRelevance = mutableMapOf<String, Float>()

        // calculate album relevance to recommend genres based on all non-genre parameters of the given query
        libraryAlbums.map { it.calculateRelevance(query.copy(genres = null)) }.forEach { albumWithRelevance ->
            albumWithRelevance.libraryAlbum.genres.forEach { genre ->
                genresToRelevance[genre] = genresToRelevance.getOrPut(genre, defaultValue = { 0f }) +
                        albumWithRelevance.overallRelevance.toFloat() + albumWithRelevance.portionOfParametersMatched
            }
        }

        return genresToRelevance.keys.sortedByDescending { genresToRelevance[it] }
    }

    // TODO: ensure I'm following every step listed in: https://chilipot.atlassian.net/browse/LIB-281
    /**
     * Recommend albums from the given list according to their relevance to the given mood-based [query].
     * Recommendations are divided into categories, each representing a match to a certain portion of the query.
     */
    fun recommendAlbums(query: Query, libraryAlbums: List<LibraryAlbum>): List<RecommendationCategory> {
        val recommendationCategories = mutableListOf<RecommendationCategory>()

        val possibleAlbums: MutableSet<PossibleAlbumRecommendation> = libraryAlbums
            .map { it.calculateRelevance(query) }
            .filter { it.isPartiallyRelevant && it.overallRelevance > OVERALL_RELEVANCE_THRESHOLD }
            .toMutableSet()

        createBestMatchCategory(possibleAlbums, recommendationCategories)

        val possibleCategories = createPossibleCategories(possibleAlbums, query)
        selectCategories(possibleCategories, recommendationCategories)

        return recommendationCategories
    }

    private fun LibraryAlbum.calculateRelevance(query: Query) = PossibleAlbumRecommendation(
        libraryAlbum = this,
        relevanceParameters = setOfNotNull(
            query.familiarity?.let { preferredFamiliarity ->
                FamiliarityRelevanceParameter(matchesFamiliarity(preferredFamiliarity))
            },
            query.instrumental?.let { prefersInstrumental ->
                SpectrumBasedRelevanceParameter(Query.Parameter.INSTRUMENTALNESS, instrumentalnessRelevance(prefersInstrumental))
            },
            query.acousticness?.let { preferredAcousticness ->
                SpectrumBasedRelevanceParameter(Query.Parameter.ACOUSTICNESS, audioFeatures.acousticness.relevanceTo(preferredAcousticness))
            },
            query.valence?.let { preferredValence ->
                SpectrumBasedRelevanceParameter(Query.Parameter.VALENCE, audioFeatures.valence.relevanceTo(preferredValence))
            },
            query.energy?.let { preferredEnergy ->
                SpectrumBasedRelevanceParameter(Query.Parameter.ENERGY, audioFeatures.energy.relevanceTo(preferredEnergy))
            },
            query.danceability?.let { preferredDanceability ->
                SpectrumBasedRelevanceParameter(Query.Parameter.DANCEABILITY, audioFeatures.danceability.relevanceTo(preferredDanceability))
            },
            query.genres?.let { preferredGenres ->
                GenreRelevanceParameter(genresMatching(preferredGenres))
            },
        )
    )

    private fun LibraryAlbum.matchesFamiliarity(preferredFamiliarity: Query.Familiarity) =
        when (preferredFamiliarity) {
            Query.Familiarity.CURRENT_FAVORITE -> familiarity.recentlyPlayed || familiarity.shortTermFavorite || familiarity.mediumTermFavorite
            Query.Familiarity.RELIABLE_CLASSIC -> familiarity.longTermFavorite
            Query.Familiarity.UNDERAPPRECIATED_GEM -> familiarity.isLowFamiliarity()
        }

    private fun LibraryAlbum.instrumentalnessRelevance(prefersInstrumental: Boolean) =
        if (prefersInstrumental) {
            audioFeatures.instrumentalness
        } else {
            SPECTRUM_BASED_PARAM_MAX_VAL - audioFeatures.instrumentalness
        }

    private fun LibraryAlbum.genresMatching(preferredGenres: Set<String>) = genres.filter { it in preferredGenres }.toSet()

    private fun Float.relevanceTo(preferredValue: Float) = 1 - abs(preferredValue - this)

    private fun createBestMatchCategory(
        possibleAlbums: MutableSet<PossibleAlbumRecommendation>,
        recommendationCategories: MutableList<RecommendationCategory>
    ) {
        val fullyRelevantAlbums = possibleAlbums.filter { it.isFullyRelevant }
        if (fullyRelevantAlbums.isNotEmpty()) {
            val bestMatchCategory = RecommendationCategory(
                title = "Best Overall Match",
                albumResults = fullyRelevantAlbums
                    .sortedByDescending { it.overallRelevance }
                    .map { it.libraryAlbum.toAlbumResult() }
            )
            recommendationCategories.add(bestMatchCategory)
            possibleAlbums.minusAssign(fullyRelevantAlbums)
        }
    }

    /**
     * Create a map of [PossibleRecommendationCategory]s that are partially relevant to the given mood-based [query],
     * with a set of [PossibleAlbumRecommendation]s associated with each category,
     * representing the albums (picked from [possibleAlbums]) that can belong in that category.
     */
    private fun createPossibleCategories(
        possibleAlbums: MutableSet<PossibleAlbumRecommendation>,
        query: Query
    ): MutableMap<PossibleRecommendationCategory, MutableSet<PossibleAlbumRecommendation>> {
        val possibleCategories = mutableMapOf<PossibleRecommendationCategory, MutableSet<PossibleAlbumRecommendation>>()

        possibleAlbums.forEach { possibleAlbum ->

            // Adjectives are strings that describe how a category matches the query (e.g. "Acoustic").
            // They can be compounded with other categories to make more specific categories (e.g. "Acoustic & Chill").
            val matchedAdjectives = mutableSetOf<String>()
            var matchedGenres = emptySet<String>()
            var matchesFamiliarity = false

            fun placeAlbumInCategory(title: String, numRelevantParameters: Int) {
                val category = PossibleRecommendationCategory(title, numRelevantParameters)
                possibleCategories.getOrPut(category, defaultValue = { mutableSetOf() }).add(possibleAlbum)
            }

            fun checkForHighOrLowCategory(preferredValue: Float?, actualValue: Float, lowTitle: String, highTitle: String) {
                if (preferredValue?.isLowParam == true && actualValue.isLowParam) {
                    placeAlbumInCategory(title = lowTitle, numRelevantParameters = 1)
                    matchedAdjectives.add(lowTitle)
                } else if (preferredValue?.isHighParam == true && actualValue.isHighParam) {
                    placeAlbumInCategory(title = highTitle, numRelevantParameters = 1)
                    matchedAdjectives.add(highTitle)
                }
            }

            val albumAudioFeatures = possibleAlbum.libraryAlbum.audioFeatures

            possibleAlbum.matchedParameters.forEach { matchedParam ->
                when (matchedParam) {
                    is GenreRelevanceParameter -> {
                        matchedGenres = matchedParam.matchedGenres
                        matchedParam.matchedGenres.forEach { genre ->
                            placeAlbumInCategory(title = genre.capitalizeAllWords(), numRelevantParameters = 1)
                        }
                    }
                    is FamiliarityRelevanceParameter -> {
                        matchesFamiliarity = true
                        query.familiarity?.let {
                            placeAlbumInCategory(title = it.stringValue.capitalizeAllWords(), numRelevantParameters = 1)
                        }
                    }
                    is SpectrumBasedRelevanceParameter -> {
                        when (matchedParam.parameterType) {
                            Query.Parameter.INSTRUMENTALNESS -> {
                                query.instrumental?.let { prefersInstrumental ->
                                    val categoryTitle = if (prefersInstrumental) "Instrumental" else "Vocal"
                                    placeAlbumInCategory(categoryTitle, numRelevantParameters = 1)
                                    matchedAdjectives.add(categoryTitle)
                                }
                            }
                            Query.Parameter.ACOUSTICNESS -> {
                                checkForHighOrLowCategory(
                                    preferredValue = query.acousticness,
                                    actualValue = albumAudioFeatures.acousticness,
                                    lowTitle = "Electric/Electronic",
                                    highTitle = "Acoustic"
                                )
                            }
                            Query.Parameter.VALENCE -> {
                                checkForHighOrLowCategory(
                                    preferredValue = query.valence,
                                    actualValue = albumAudioFeatures.valence,
                                    lowTitle = "Negative Emotion",
                                    highTitle = "Positive Emotion"
                                )
                            }
                            Query.Parameter.ENERGY -> {
                                checkForHighOrLowCategory(
                                    preferredValue = query.energy,
                                    actualValue = albumAudioFeatures.energy,
                                    lowTitle = "Chill",
                                    highTitle = "Energetic"
                                )
                            }
                            Query.Parameter.DANCEABILITY -> {
                                checkForHighOrLowCategory(
                                    preferredValue = query.danceability,
                                    actualValue = albumAudioFeatures.danceability,
                                    lowTitle = "Arrhythmic",
                                    highTitle = "Danceable"
                                )
                            }
                            else -> {
                                // no-op (TODO: can remove this else branch if we are when-ing over a child sealed interface Query.Parameter.Adjective or something similar, after a refactor
                            }
                        }
                    }
                }
            }

            fun createAdjectiveCombos(size: Int) = matchedAdjectives.combinationsOfSize(size).onEach { adjectives ->
                placeAlbumInCategory(title = adjectives.joinToUserFriendlyString(), numRelevantParameters = size)
            }

            val twoAdjectiveCombos = createAdjectiveCombos(2)
            val threeAdjectiveCombos = createAdjectiveCombos(3)
            createAdjectiveCombos(4)

            if (matchesFamiliarity) {
                matchedAdjectives.forEach { adj ->
                    placeAlbumInCategory(
                        title = "$adj, ${query.familiarity?.stringValue?.capitalizeAllWords()}",
                        numRelevantParameters = 2
                    )
                }

                twoAdjectiveCombos.forEach { adjectives ->
                    placeAlbumInCategory(
                        title = "${adjectives.joinToUserFriendlyString()} ${query.familiarity?.stringValue?.capitalizeAllWords()}", // TODO: improve up string formatting, see examples
                        numRelevantParameters = 3
                    )
                }
            }

            matchedGenres.forEach { genre ->
                if (matchesFamiliarity) {

                    placeAlbumInCategory(
                        title = "${genre.capitalizeAllWords()}, ${query.familiarity?.stringValue?.capitalizeAllWords()}",
                        numRelevantParameters = 2
                    )

                    matchedAdjectives.forEach { adj ->
                        placeAlbumInCategory(
                            title = "$adj, ${genre.capitalizeAllWords()}, ${query.familiarity?.stringValue?.capitalizeAllWords()}",
                            numRelevantParameters = 3
                        )
                    }

                    twoAdjectiveCombos.forEach { adjectives ->
                        placeAlbumInCategory(
                            title = "${adjectives.joinToUserFriendlyString()}, ${genre.capitalizeAllWords()}, ${query.familiarity?.stringValue?.capitalizeAllWords()}",
                            numRelevantParameters = 4
                        )
                    }
                }

                matchedAdjectives.forEach { adj ->
                    placeAlbumInCategory(
                        title = "$adj ${genre.capitalizeAllWords()}",
                        numRelevantParameters = 2
                    )
                }

                twoAdjectiveCombos.forEach { adjectives ->
                    placeAlbumInCategory(
                        title = "${adjectives.joinToUserFriendlyString()} ${genre.capitalizeAllWords()}",
                        numRelevantParameters = 3
                    )
                }

                threeAdjectiveCombos.forEach { adjectives ->
                    placeAlbumInCategory(
                        title = "${adjectives.joinToUserFriendlyString()} ${genre.capitalizeAllWords()}",
                        numRelevantParameters = 4
                    )
                }
            }
        }

        return possibleCategories
    }

    private val Float.isLowParam: Boolean
        get() = this < SPECTRUM_BASED_PARAM_MAX_VAL - SPECTRUM_BASED_PARAM_RELEVANCE_THRESHOLD

    private val Float.isHighParam: Boolean
        get() = this > SPECTRUM_BASED_PARAM_RELEVANCE_THRESHOLD

    /**
     * Add categories to the given list of [recommendationCategories] from the given list of [possibleCategories],
     * in order of most to least relevant, ensuring any albums in a selected category are removed from the other
     * possible categories so that they cannot be recommended twice.
     */
    private fun selectCategories(
        possibleCategories: MutableMap<PossibleRecommendationCategory, MutableSet<PossibleAlbumRecommendation>>,
        recommendationCategories: MutableList<RecommendationCategory>
    ) {
        val categoryComparator =
            compareBy<Map.Entry<PossibleRecommendationCategory, Set<PossibleAlbumRecommendation>>> { (category, _) ->
                category.numRelevantParameters
            }.thenBy { (_, albumsInCategory) ->
                albumsInCategory.map { it.overallRelevance }.average()
            }

        val albumComparator =
            compareByDescending<PossibleAlbumRecommendation> {
                it.matchedParameters.size
            }.thenByDescending {
                it.overallRelevance
            }

        do {
            val chosenCategory = possibleCategories
                .filterValues { it.size >= MIN_ALBUMS_PER_CATEGORY }
                .maxWithOrNull(categoryComparator)

            chosenCategory?.let { (category, albumsInChosenCategory) ->
                recommendationCategories.add(
                    RecommendationCategory(
                        category.title,
                        albumsInChosenCategory.toList().sortedWith(albumComparator).map { it.libraryAlbum.toAlbumResult() }
                    )
                )
                possibleCategories.remove(category)

                possibleCategories.values.forEach { albumsInOtherCategory ->
                    albumsInOtherCategory.minusAssign(albumsInChosenCategory)
                }
            }
        } while (chosenCategory != null)
    }
}

/**
 * An intermediate data type for the recommendation algorithm, representing a contender for a [RecommendationCategory],
 * containing the title that the category would use, as well as the number of query parameters that
 * this category matches (i.e. how relevant the albums in the category are to the user's mood).
 */
private data class PossibleRecommendationCategory(
    val title: String,
    val numRelevantParameters: Int
)

/**
 * An intermediate data type for the recommendation algorithm, representing an album from the user's library, coupled
 * with a set of [RelevanceParameter]s, one for each parameter of the user's mood, each representing how relevant the
 * [libraryAlbum] is to that parameter of the user's mood. From this we can determine how relevant the album is overall,
 * as well as which [RecommendationCategory]s it could be placed in.
 */
private data class PossibleAlbumRecommendation(
    val libraryAlbum: LibraryAlbum,
    val relevanceParameters: Set<RelevanceParameter>
) {
    val matchedParameters = relevanceParameters.filter { it.isRelevant }
    val isPartiallyRelevant = matchedParameters.isNotEmpty() || relevanceParameters.isEmpty()
    val isFullyRelevant = matchedParameters.size == relevanceParameters.size
    val overallRelevance = if (relevanceParameters.isEmpty()) 1.0 else relevanceParameters.map { it.relevance }.average()
    val portionOfParametersMatched = when {
        relevanceParameters.isNotEmpty() -> matchedParameters.size.toFloat() / relevanceParameters.size
        else -> 1f
    }
}

/**
 * Represents how relevant an album is to a single parameter of a user's mood-based [Query].
 * @property relevance a score of how relevant the album is to the query parameter, from 0 (not at all) to 1 (fully).
 * @property isRelevant whether or not the album is relevant enough to the query parameter
 *                      that this parameter should make the album more likely to be recommended.
 */
private sealed interface RelevanceParameter {
    val relevance: Float
    val isRelevant: Boolean
}

/**
 * A [RelevanceParameter] for a parameter of a user's mood that lies on a continuous spectrum,
 * e.g. the spectrum of positive emotion to negative emotion or of energetic to chill.
 */
private data class SpectrumBasedRelevanceParameter(
    val parameterType: Query.Parameter,
    override val relevance: Float
) : RelevanceParameter {
    override val isRelevant = relevance > SPECTRUM_BASED_PARAM_RELEVANCE_THRESHOLD
}

/**
 * A [RelevanceParameter] for the [Query.Parameter.FAMILIARITY] parameter of a user's mood.
 */
private data class FamiliarityRelevanceParameter(
    val matchesFamiliarity: Boolean
) : RelevanceParameter {
    override val isRelevant = matchesFamiliarity
    override val relevance = if (isRelevant) 1f else 0f
}

/**
 * A [RelevanceParameter] for the [Query.Parameter.GENRES] parameter of a user's mood.
 */
private data class GenreRelevanceParameter(
    val matchedGenres: Set<String>
) : RelevanceParameter {
    override val isRelevant = matchedGenres.isNotEmpty()
    override val relevance = if (isRelevant) 1f else 0f
}


/**
 * The minimum threshold of how relevant a [SpectrumBasedRelevanceParameter] must be to the user's mood,
 * in order to consider the album relevant in regards to that parameter.
 * Also represents the point on such a spectrum at which values are considered "close" to the extreme.
 */
private const val SPECTRUM_BASED_PARAM_RELEVANCE_THRESHOLD = 7f

/** The highest value a spectrum-based [Query.Parameter] can be. */
private const val SPECTRUM_BASED_PARAM_MAX_VAL = 1

/**
 * The minimum threshold of how high the average of all [RelevanceParameter]s of an album can be,
 * in order for that album to be recommended.
 */
private const val OVERALL_RELEVANCE_THRESHOLD = 0.5

/**
 * The minimum number of albums that can constitute a [RecommendationCategory].
 */
private const val MIN_ALBUMS_PER_CATEGORY = 2
