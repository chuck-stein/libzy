package com.chuckstein.libzy.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground
import com.chuckstein.libzy.viewmodel.SelectGenresViewModel


class SelectGenresActivity : AppCompatActivity() {

    companion object {
        val TAG = SelectGenresActivity::class.java.simpleName
    }

    private val model: SelectGenresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_genres)
        initializeBackground()

        model.albumsGroupedByGenre.observe(this, Observer { albumsGroupedByGenre ->
            // TODO: fill UI
        })
    }

}
