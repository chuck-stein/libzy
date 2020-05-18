package com.chuckstein.libzy.view.browseresults.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.browseresults.data.GenreResult
import io.reactivex.rxjava3.core.Observable
import kotlinx.android.synthetic.main.list_item_genre_result.view.albums_recycler as albumsRecycler
import kotlinx.android.synthetic.main.list_item_genre_result.view.genre_name as genreName

class GenresRecyclerAdapter(
    private val loadingAnimationTimer: Observable<Long>,
    private val glide: RequestManager,
    private val albumArtPlaceholder: Drawable,
    private val onAlbumClick: (spotifyUri: String) -> Unit
) :
    RecyclerView.Adapter<GenresRecyclerAdapter.ViewHolder>() {

    var genres = listOf<GenreResult>()
        set(value) {
            field = value

            // TODO: use DiffUtil if I'll ever need to update data set after filling the initial data (see Udacity course for details... does replacing loading placeholders count?)
            // TODO: see if DiffUtil can prevent jump to beginning of album row if # results is the same because it was just loading screen being replaced (might be irrelevant if horizontal scrolling is prevented on loading screen or if actual data set just replaces album fields not album itself)
            notifyDataSetChanged()
        }

    private val albumsViewPool = RecyclerView.RecycledViewPool()

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
            adapter = AlbumsRecyclerAdapter(genre.albums, loadingAnimationTimer, glide, albumArtPlaceholder, onAlbumClick)
            setRecycledViewPool(albumsViewPool)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val genreName: TextView = itemView.genreName
        val albumsRecycler: RecyclerView = itemView.albumsRecycler
    }

}