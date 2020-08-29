package com.chuckstein.libzy.view.query

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.constraintlayout.widget.Group
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.common.children
import com.chuckstein.libzy.common.observeOnce
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_query.slider
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_query.acousticness_question as acousticnessQuestion
import kotlinx.android.synthetic.main.fragment_query.back_button as backButton
import kotlinx.android.synthetic.main.fragment_query.continue_button as continueButton
import kotlinx.android.synthetic.main.fragment_query.current_favorite_button as currentFavoriteButton
import kotlinx.android.synthetic.main.fragment_query.danceability_question as danceabilityQuestion
import kotlinx.android.synthetic.main.fragment_query.energy_question as energyQuestion
import kotlinx.android.synthetic.main.fragment_query.familiarity_question as familiarityQuestion
import kotlinx.android.synthetic.main.fragment_query.genre_chips as genreChips
import kotlinx.android.synthetic.main.fragment_query.genre_chips_scroll_view as genreChipsScrollView
import kotlinx.android.synthetic.main.fragment_query.genre_question as genreQuestion
import kotlinx.android.synthetic.main.fragment_query.instrumental_button as instrumentalButton
import kotlinx.android.synthetic.main.fragment_query.instrumentalness_question as instrumentalnessQuestion
import kotlinx.android.synthetic.main.fragment_query.no_preference_button as noPreferenceButton
import kotlinx.android.synthetic.main.fragment_query.ready_button as readyButton
import kotlinx.android.synthetic.main.fragment_query.reliable_classic_button as reliableClassicButton
import kotlinx.android.synthetic.main.fragment_query.underappreciated_gem_button as underappreciatedGemButton
import kotlinx.android.synthetic.main.fragment_query.valence_question as valenceQuestion
import kotlinx.android.synthetic.main.fragment_query.vocal_button as vocalButton


class QueryFragment : Fragment() {

    companion object {
        private const val LAST_QUESTION_INDEX = 6
        private const val DEFAULT_SLIDER_VAL = 0.5f
        private const val FADE_ANIMATION_TIME = 200L // in milliseconds
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<QueryViewModel> { viewModelFactory }

    private val navArgs: QueryFragmentArgs by navArgs()

    private lateinit var questionViews: List<Group>
    private var currentQuestionIndex = 0
    private var changingQuestions = false
    private lateinit var prevQuestionOnBackCallback: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as LibzyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_query, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        questionViews = listOf(
            familiarityQuestion, instrumentalnessQuestion, acousticnessQuestion,
            valenceQuestion, energyQuestion, danceabilityQuestion, genreQuestion
        )
        currentQuestionIndex = navArgs.initialQuestionIndex
        questionViews[currentQuestionIndex].visibility = View.VISIBLE

        prevQuestionOnBackCallback =
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBackPressed() }
        updateBackNavigation()

        backButton.setOnClickListener { onBackPressed() }
        noPreferenceButton.setOnClickListener { onClickNoPreferenceButton() }
        continueButton.setOnClickListener { onClickContinueButton() }
        currentFavoriteButton.setOnClickListener { onClickCurrentFavoriteButton() }
        reliableClassicButton.setOnClickListener { onClickReliableClassicButton() }
        underappreciatedGemButton.setOnClickListener { onClickUnderappreciatedGemButton() }
        instrumentalButton.setOnClickListener { onClickInstrumentalButton() }
        vocalButton.setOnClickListener { onClickVocalButton() }
        readyButton.setOnClickListener { onClickReadyButton() }

        model.receivedSpotifyNetworkError.observe(
            viewLifecycleOwner,
            Observer { if (it) onSpotifyNetworkError() }) // TODO: abstract this
    }

    private fun changeQuestion(index: Int, noPreferenceForCurrQuestion: Boolean = false) {
        if (index < 0 || index > LAST_QUESTION_INDEX || changingQuestions) return

        fun getSliderValue(flipValueDirection: Boolean = false) =
            when {
                noPreferenceForCurrQuestion -> null
                flipValueDirection -> 1 - slider.value
                else -> slider.value
            }

        fun setSliderValue(value: Float?, flipValueDirection: Boolean = false) {
            slider.value = when {
                value == null -> DEFAULT_SLIDER_VAL
                flipValueDirection -> 1 - value
                else -> value
            }
        }

        when (questionViews[currentQuestionIndex]) {
            acousticnessQuestion -> model.acousticness = getSliderValue(true)
            valenceQuestion -> model.valence = getSliderValue()
            energyQuestion -> model.energy = getSliderValue()
            danceabilityQuestion -> model.danceability = getSliderValue()
            genreQuestion -> model.genres =
                if (noPreferenceForCurrQuestion) null
                else getGenreOptions().filter { it.isChecked }.map { it.text.toString() }.toSet()
        }

        changingQuestions = true
        backButton.isClickable = false
        noPreferenceButton.isClickable = false
        fadeOutGroup(questionViews[currentQuestionIndex]) {
            currentQuestionIndex = index

            when (questionViews[currentQuestionIndex]) {
                acousticnessQuestion -> setSliderValue(model.acousticness, true)
                valenceQuestion -> setSliderValue(model.valence)
                energyQuestion -> setSliderValue(model.energy)
                danceabilityQuestion -> setSliderValue(model.danceability)
                genreQuestion -> initGenreChips()
            }

            updateBackNavigation()
            fadeInGroup(questionViews[currentQuestionIndex]) {
                changingQuestions = false
                backButton.isClickable = true
                noPreferenceButton.isClickable = true
            }
        }
    }

    private fun fadeOutGroup(group: Group, onComplete: () -> Unit) {
        val viewsToAnimate = getViewsInGroup(group)
        for (view in viewsToAnimate) view.isClickable = false
        fadeViews(viewsToAnimate, true) {
            for (view in viewsToAnimate) {
                view.visibility = View.GONE
                view.alpha = 1f
                view.isClickable = true
            }
            onComplete()
        }
    }

    private fun fadeInGroup(group: Group, onComplete: () -> Unit) {
        val viewsToAnimate = getViewsInGroup(group)
        for (view in viewsToAnimate) {
            view.alpha = 0f
            view.isClickable = false
            view.visibility = View.VISIBLE
        }
        fadeViews(viewsToAnimate, false) {
            for (view in viewsToAnimate) view.isClickable = true
            onComplete()
        }
    }

    private fun getViewsInGroup(group: Group) = group.referencedIds.map { requireView().findViewById<View>(it) }

    private fun fadeViews(views: List<View>, fadeOut: Boolean, onComplete: () -> Unit) {
        val targetAlpha = if (fadeOut) 0f else 1f
        AnimatorSet().apply {
            playTogether(views.map { ObjectAnimator.ofFloat(it, "alpha", targetAlpha) })
            duration = FADE_ANIMATION_TIME
            interpolator = if (fadeOut) AccelerateInterpolator() else DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    // TODO: ensure I wrote this method in the most efficient way
    // TODO: fix lag on first call via loading screen + coroutine or faster function
    private fun initGenreChips() {
        model.getGenreSuggestions().observeOnce(viewLifecycleOwner, Observer { genreSuggestions ->
            for (genre in genreSuggestions) {
                genreChips.addView(Chip(requireContext()).apply { text = genre })
            }

            fun chipIsOffScreen(chipIndex: Int) =
                chipIndex > 0 && genreChips.getChildAt(chipIndex).bottom > genreChipsScrollView.height

            genreChipsScrollView.doOnLayout {
                // remove any chips beyond the bottom of the scroll view window, so they fill up the whole space but no more
                var chipIndex = genreChips.childCount - 1
                while (chipIsOffScreen(chipIndex)) {
                    genreChips.removeViewAt(chipIndex)
                    chipIndex--
                }
                model.updateSelectedGenres(getGenreOptions().map { it.text.toString() })
                for (chip in getGenreOptions()) {
                    chip.isChecked = model.genres?.contains(chip.text) ?: false
                }
            }
        })
    }

    private fun getGenreOptions() = genreChips.children.filterIsInstance<Chip>()

    private fun onBackPressed() {
        if (currentQuestionIndex > 0) changeQuestion(currentQuestionIndex - 1)
    }

    private fun updateBackNavigation() {
        val onFirstQuestion = currentQuestionIndex == 0
        backButton.visibility = if (onFirstQuestion) View.GONE else View.VISIBLE
        prevQuestionOnBackCallback.isEnabled = !onFirstQuestion
    }

    private fun onClickNoPreferenceButton() {
        if (currentQuestionIndex == LAST_QUESTION_INDEX) {
        } // TODO: call a generic function to set no preference on any question index, passing in LAST_QUESTION_INDEX (changeQuestion may also want to use said function), then navigate to ResultsFragment
        else changeQuestion(currentQuestionIndex + 1, true)
    }

    private fun onClickContinueButton() {
        // TODO: tell ViewModel about current question answer
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickCurrentFavoriteButton() {
        model.familiarity = QueryViewModel.Familiarity.CURRENT_FAVORITE
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickReliableClassicButton() {
        model.familiarity = QueryViewModel.Familiarity.RELIABLE_CLASSIC
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickUnderappreciatedGemButton() {
        model.familiarity = QueryViewModel.Familiarity.UNDERAPPRECIATED_GEM
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickInstrumentalButton() {
        model.instrumental = true
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickVocalButton() {
        model.instrumental = false
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickReadyButton() {
        // TODO: instead call a generic function that sets the value of the given question index, passing in LAST_QUESTION_INDEX (changeQuestion will probably also want to use said function)
        model.genres =
            genreChips.children.filterIsInstance<Chip>().filter { it.isChecked }.map { it.text.toString() }.toSet()
        model.submitQuery()
    }

    // TODO: abstract this
    private fun onSpotifyNetworkError() {
        val networkErrorNavAction = QueryFragmentDirections.actionQueryFragmentToConnectSpotifyFragment()
        networkErrorNavAction.networkErrorReceived = true
        requireView().findNavController().navigate(networkErrorNavAction)
    }
}