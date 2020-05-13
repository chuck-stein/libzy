package com.chuckstein.libzy.view.browseresults.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.browseresults.data.AlbumResult
import kotlinx.android.synthetic.main.list_item_album_result.view.album_art as albumArt
import kotlinx.android.synthetic.main.list_item_album_result.view.album_title as albumTitle
import kotlinx.android.synthetic.main.list_item_album_result.view.album_artist as albumArtist

class AlbumsRecyclerAdapter(
    private val albums: List<AlbumResult>,
    private val glide: RequestManager,
    private val albumArtPlaceholder: Drawable,
    private val onAlbumClick: (spotifyUri: String) -> Unit
) :
    RecyclerView.Adapter<AlbumsRecyclerAdapter.ViewHolder>() {

    companion object {
        private val TAG = AlbumsRecyclerAdapter::class.java.simpleName
    }

    override fun getItemCount() = albums.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_album_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder called");
        val album = albums[position]
        with(holder) {
            // TODO: look into Glide usage, error handling, null handling, other builder functions (e.g. centerCrop), etc.
            if (album.artworkUri != null) glide.load(album.artworkUri).placeholder(albumArtPlaceholder).into(albumArt)
            else albumArt.setImageDrawable(albumArtPlaceholder)
            albumTitle.text = album.title
            albumArtist.text = album.artist

            // TODO: if it violates separation of concerns to have the spotify URI in the view data, then instead pass in a 2d genre/album position for the ViewModel to figure out the spotify URI from
            itemView.setOnClickListener { onAlbumClick(album.spotifyUri) }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.albumArt
        val albumTitle: TextView = itemView.albumTitle
        val albumArtist: TextView = itemView.albumArtist
    }

}