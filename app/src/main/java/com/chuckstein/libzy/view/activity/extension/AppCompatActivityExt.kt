package com.chuckstein.libzy.view.activity.extension

import android.graphics.drawable.AnimationDrawable
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.chuckstein.libzy.R

// TODO: is this extension the best design? is there a better way to do something in every onCreate method? should the background be a fragment? its own view? is it okay that the method is public?

fun AppCompatActivity.initializeBackground() {
    val rootView : ViewGroup = findViewById(android.R.id.content)
    val masterLayout = rootView.getChildAt(0)
    masterLayout.setBackgroundResource(R.drawable.bkg_gradient_anim)
    val bkgGradient = masterLayout.background as AnimationDrawable
    bkgGradient.setEnterFadeDuration(10)
    bkgGradient.setExitFadeDuration(5000)
    bkgGradient.start()
}