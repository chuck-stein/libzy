package com.chuckstein.libzy.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chuckstein.libzy.R
import com.chuckstein.libzy.viewmodel.data.GenreData
import kotlinx.android.synthetic.main.list_item_genre_result.view.genre_name as genreName
import kotlinx.android.synthetic.main.list_item_genre_result.view.albums_recycler as albumsRecycler

class GenresRecyclerAdapter : RecyclerView.Adapter<GenresRecyclerAdapter.ViewHolder>() {

    var genres = listOf<GenreData>()
        set(value) {
            field = value
            notifyDataSetChanged() // TODO: do this in a better way (see Udacity lesson)
        }

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemCount() = genres.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_genre_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genres[position]
        holder.genreName.text = genre.name
        val albumsLayoutManager = LinearLayoutManager(holder.albumsRecycler.context, RecyclerView.HORIZONTAL, false)
        albumsLayoutManager.initialPrefetchItemCount = 5 // TODO: determine ideal value
        with(holder.albumsRecycler) {
            layoutManager = albumsLayoutManager
            adapter = AlbumsRecyclerAdapter(genre.albums)
            setRecycledViewPool(viewPool)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val genreName: TextView = itemView.genreName
        val albumsRecycler: RecyclerView = itemView.albumsRecycler
    }

}