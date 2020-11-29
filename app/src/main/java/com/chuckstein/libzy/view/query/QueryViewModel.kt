package com.chuckstein.libzy.view.query

import androidx.lifecycle.ViewModel
import com.chuckstein.libzy.model.Query
import com.chuckstein.libzy.recommendation.RecommendationService
import com.chuckstein.libzy.repository.UserLibraryRepository
import javax.inject.Inject

class QueryViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService
) : ViewModel() {

    val query = Query()

    // TODO: replace this placeholder implementation with a call to a getGenreSuggestions() function in a RecommendationService,
    //       that takes in userLibraryRepository.libraryGenres as input and gives a list of genres applicable to results of
    //       questions answered so far, sorted by number of such results for each genre)
    //       (think about how to handle the libraryGenres is null case -- when would it be null?
    //        maybe show a loading screen if it will be not null soon, or just skip the genres question)
    fun getGenreSuggestions() = recommendationService.recommendGenres(userLibraryRepository.libraryAlbums, query)

    fun updateSelectedGenres(genreOptions: List<String>) {
        query.genres.let { previousGenres ->
            if (previousGenres != null) query.genres = genreOptions.filter { previousGenres.contains(it) }.toSet()
        }
    }

}