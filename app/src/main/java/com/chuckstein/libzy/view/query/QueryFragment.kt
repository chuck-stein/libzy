package com.chuckstein.libzy.view.query

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.airbnb.paris.extensions.style
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.common.children
import com.chuckstein.libzy.model.Query
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_query.slider
import java.time.LocalTime
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
import kotlinx.android.synthetic.main.fragment_query.greeting_text as greetingText
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
        private val TAG = QueryFragment::class.java.simpleName

        private const val LAST_QUESTION_INDEX = 6
        private const val DEFAULT_SLIDER_VAL = 0.5f
        private const val FADE_ANIMATION_TIME = 100L // in milliseconds
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by activityViewModels<QueryResultsViewModel> { viewModelFactory }

    private val navArgs: QueryFragmentArgs by navArgs()

    private lateinit var questionViews: List<Group>
    private var currQuestionIndex = 0
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
        setGreetingText()
        initializeQuestions()
        setOnClickListeners()
        prevQuestionOnBackCallback =
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBackPressed() }
        updateBackNavigation()
        model.recommendedGenres.observe(viewLifecycleOwner) { fillGenreChips(it) }
    }

    private fun setGreetingText() {
        greetingText.text = getString(
            when (LocalTime.now().hour) {
                in 4..11 -> R.string.morning_greeting_text
                in 12..16 -> R.string.afternoon_greeting_text
                else -> R.string.evening_greeting_text
            }
        )
    }

    private fun initializeQuestions() {
        questionViews = listOf(
            familiarityQuestion, instrumentalnessQuestion, acousticnessQuestion,
            valenceQuestion, energyQuestion, danceabilityQuestion, genreQuestion
        )
        currQuestionIndex = navArgs.initialQuestionIndex
        questionViews[currQuestionIndex].visibility = View.VISIBLE
    }

    private fun setOnClickListeners() {
        backButton.setOnClickListener { onBackPressed() }
        noPreferenceButton.setOnClickListener { onClickNoPreferenceButton() }
        continueButton.setOnClickListener { onClickContinueOrReadyButton() }
        currentFavoriteButton.setOnClickListener { onClickCurrentFavoriteButton() }
        reliableClassicButton.setOnClickListener { onClickReliableClassicButton() }
        underappreciatedGemButton.setOnClickListener { onClickUnderappreciatedGemButton() }
        instrumentalButton.setOnClickListener { onClickInstrumentalButton() }
        vocalButton.setOnClickListener { onClickVocalButton() }
        readyButton.setOnClickListener { onClickContinueOrReadyButton() }
    }

    private fun advanceQuestion() {
        if (currQuestionIndex < LAST_QUESTION_INDEX) changeQuestion(currQuestionIndex + 1)
        else findNavController().navigate(
            QueryFragmentDirections.actionQueryFragmentToResultsFragment()
        )
    }

    private fun changeQuestion(index: Int) {
        if (index < 0 || index > LAST_QUESTION_INDEX || changingQuestions) return

        changingQuestions = true
        backButton.isClickable = false
        noPreferenceButton.isClickable = false
        fadeOutGroup(questionViews[currQuestionIndex]) {
            currQuestionIndex = index
            loadAnswer()
            updateBackNavigation()
            fadeInGroup(questionViews[currQuestionIndex]) {
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

    private fun loadAnswer() {
        fun setSliderValue(value: Float?, flipValueDirection: Boolean = false) {
            slider.value = when {
                value == null -> DEFAULT_SLIDER_VAL
                flipValueDirection -> 1 - value
                else -> value
            }
        }

        when (questionViews[currQuestionIndex]) {
            acousticnessQuestion -> setSliderValue(model.acousticness, true)
            valenceQuestion -> setSliderValue(model.valence)
            energyQuestion -> setSliderValue(model.energy)
            danceabilityQuestion -> setSliderValue(model.danceability)
        }
    }

    // TODO: ensure I wrote this method in the most efficient way
    // TODO: fix lag on first call via loading screen + coroutine or faster function
    private fun fillGenreChips(genreSuggestions: List<String>) {
        for (genre in genreSuggestions.take(50)) { // TODO: remove magic number
            genreChips.addView(Chip(requireContext()).apply {
                style(R.style.Chip)
                text = genre
            })
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
    }

    private fun getGenreOptions() = genreChips.children.filterIsInstance<Chip>()

    private fun onBackPressed() {
        if (currQuestionIndex > 0) changeQuestion(currQuestionIndex - 1)
    }

    private fun updateBackNavigation() {
        val onFirstQuestion = currQuestionIndex == 0
        backButton.visibility = if (onFirstQuestion) View.GONE else View.VISIBLE
        prevQuestionOnBackCallback.isEnabled = !onFirstQuestion
    }

    private fun onClickNoPreferenceButton() {
        when (questionViews[currQuestionIndex]) {
            familiarityQuestion -> model.familiarity = null
            instrumentalnessQuestion -> model.instrumental = null
            acousticnessQuestion -> model.acousticness = null
            valenceQuestion -> model.valence = null
            energyQuestion -> model.energy = null
            danceabilityQuestion -> model.danceability = null
            genreQuestion -> model.genres = null
        }
        advanceQuestion()
    }

    private fun onClickContinueOrReadyButton() {
        when (questionViews[currQuestionIndex]) {
            familiarityQuestion, instrumentalnessQuestion ->
                Log.w(TAG, "Can only save an answer to the current question via answer buttons")
            acousticnessQuestion -> model.acousticness = 1 - slider.value
            valenceQuestion -> model.valence = slider.value
            energyQuestion -> model.energy = slider.value
            danceabilityQuestion -> model.danceability = slider.value
            genreQuestion -> {
                val checkedGenreChips = getGenreOptions().filter { it.isChecked }
                model.genres =
                    if (checkedGenreChips.isEmpty()) null
                    else checkedGenreChips.map { it.text.toString() }.toSet()
            }
        }
        advanceQuestion()
    }

    private fun onClickCurrentFavoriteButton() {
        model.familiarity = Query.Familiarity.CURRENT_FAVORITE
        advanceQuestion()
    }

    private fun onClickReliableClassicButton() {
        model.familiarity = Query.Familiarity.RELIABLE_CLASSIC
        advanceQuestion()
    }

    private fun onClickUnderappreciatedGemButton() {
        model.familiarity = Query.Familiarity.UNDERAPPRECIATED_GEM
        advanceQuestion()
    }

    private fun onClickInstrumentalButton() {
        model.instrumental = true
        advanceQuestion()
    }

    private fun onClickVocalButton() {
        model.instrumental = false
        advanceQuestion()
    }
}