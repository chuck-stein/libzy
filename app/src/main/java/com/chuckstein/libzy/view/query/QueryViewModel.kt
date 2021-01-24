package com.chuckstein.libzy.view.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.chuckstein.libzy.model.Query
import com.chuckstein.libzy.recommendation.RecommendationService
import com.chuckstein.libzy.repository.UserLibraryRepository
import javax.inject.Inject

class QueryViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository, // TODO: don't store this as a value after switching over to computed value for recommendationService
    private val recommendationService: RecommendationService // TODO: don't store this as a value after switching over to computed value for recommendationService (as long as the Transformations.map caches it somehow -- look into this)
) : ViewModel() {

    val query = Query()

    // TODO: switch over to computed property, and remove getGenreSuggestions()
//    val recommendedGenres = userLibraryRepository.libraryAlbums.map {
//        recommendationService.recommendGenres(it, query)
//    }

    fun getGenreSuggestions() = userLibraryRepository.libraryAlbums.map {
        recommendationService.recommendGenres(it, query)
    }

    fun updateSelectedGenres(genreOptions: List<String>) {
        query.genres.let { previousGenres ->
            if (previousGenres != null) query.genres = genreOptions.filter { previousGenres.contains(it) }.toSet()
        }
    }

}