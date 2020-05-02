package com.chuckstein.libzy.view.activity.common

import android.graphics.drawable.AnimationDrawable
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R

abstract class GradientBackgroundActivity : AppCompatActivity() {

    private var backgroundGradient: AnimationDrawable? = null

    override fun onStart() {
        super.onStart()
        val rootView: ViewGroup = findViewById(android.R.id.content) // TODO: check if this is a safe cast, otherwise make it safe (how is it even letting me treat it as a ViewGroup?)
        val masterLayout = rootView.getChildAt(0)
        masterLayout.setBackgroundResource(R.drawable.bkg_gradient_anim)
        backgroundGradient = masterLayout.background as? AnimationDrawable // TODO: is it better practice to use the unsafe cast when I know it will work, or make backgroundGradient non-nullable since it doesn't make sense to be null?
        backgroundGradient?.setEnterFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_fade_in_duration))
        backgroundGradient?.setExitFadeDuration(resources.getInteger(R.integer.bkg_gradient_anim_transition_duration))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) backgroundGradient?.start()
        else backgroundGradient?.stop()
    }

}