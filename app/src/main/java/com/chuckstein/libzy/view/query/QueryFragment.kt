package com.chuckstein.libzy.view.query

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.view.selectgenres.SelectGenresViewModel
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_query.acousticness_question as acousticnessQuestion
import kotlinx.android.synthetic.main.fragment_query.continue_button as continueButton
import kotlinx.android.synthetic.main.fragment_query.current_favorite_button as currentFavoriteButton
import kotlinx.android.synthetic.main.fragment_query.danceability_question as danceabilityQuestion
import kotlinx.android.synthetic.main.fragment_query.energy_question as energyQuestion
import kotlinx.android.synthetic.main.fragment_query.familiarity_question as familiarityQuestion
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

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<SelectGenresViewModel> { viewModelFactory }

    private val navArgs: QueryFragmentArgs by navArgs()

    private lateinit var questionViews: Array<Group>
    private var currentQuestionIndex = 0
    private val lastQuestionIndex = 6

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

        questionViews = arrayOf(
            familiarityQuestion, instrumentalnessQuestion, acousticnessQuestion,
            valenceQuestion, energyQuestion, danceabilityQuestion, genreQuestion
        )
        currentQuestionIndex = navArgs.initialQuestionIndex
        questionViews[currentQuestionIndex].visibility = View.VISIBLE

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBackPressed() }

        noPreferenceButton.setOnClickListener { onClickNoPreferenceButton() }
        continueButton.setOnClickListener { onClickContinueButton() }
        currentFavoriteButton.setOnClickListener { onClickCurrentFavoriteButton() }
        reliableClassicButton.setOnClickListener { onClickReliableClassicButton() }
        underappreciatedGemButton.setOnClickListener { onClickUnderappreciatedGemButton() }
        instrumentalButton.setOnClickListener { onClickInstrumentalButton() }
        vocalButton.setOnClickListener { onClickVocalButton() }
        readyButton.setOnClickListener { onClickReadyButton() }
    }

    private fun changeQuestion(index: Int) {
        questionViews[currentQuestionIndex].visibility = View.GONE // TODO: fade out
        currentQuestionIndex = index
        questionViews[currentQuestionIndex].visibility = View.VISIBLE // TODO: fade in
    }

    private fun onBackPressed() {
        if (currentQuestionIndex > 0) changeQuestion(currentQuestionIndex - 1)
    }

    private fun onClickNoPreferenceButton() {
        if (currentQuestionIndex == lastQuestionIndex) {} // TODO: navigate to ResultsFragment
        else {
            // TODO: tell ViewModel that there's no preference for current query
            changeQuestion(currentQuestionIndex + 1)
        }
    }

    private fun onClickContinueButton() {
        // TODO: tell ViewModel about current question answer
        changeQuestion(currentQuestionIndex + 1)
    }

    private fun onClickCurrentFavoriteButton() {

    }

    private fun onClickReliableClassicButton() {

    }

    private fun onClickUnderappreciatedGemButton() {

    }

    private fun onClickInstrumentalButton() {

    }

    private fun onClickVocalButton() {

    }

    private fun onClickReadyButton() {

    }

}