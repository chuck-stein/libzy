package io.libzy.view.query

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import io.libzy.R
import io.libzy.model.AlbumResult
import kotlinx.android.synthetic.main.list_item_album_result.view.album_art as albumArt
import kotlinx.android.synthetic.main.list_item_album_result.view.album_artist as albumArtist
import kotlinx.android.synthetic.main.list_item_album_result.view.album_title as albumTitle

class AlbumsRecyclerAdapter(
    private val glide: RequestManager,
    private val albumArtPlaceholder: Drawable?,
    private val onAlbumClick: (spotifyUri: String) -> Unit
) :
    RecyclerView.Adapter<AlbumsRecyclerAdapter.ViewHolder>() {

    var albums = listOf<AlbumResult>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = albums.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_album_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]
        with(holder) {
            if (album.artworkUrl != null) glide.load(album.artworkUrl).placeholder(albumArtPlaceholder).into(albumArt)
            else if (albumArtPlaceholder != null) albumArt.setImageDrawable(albumArtPlaceholder)
            albumTitle.text = album.title
            albumArtist.text = album.artists

            if (album.isPlaceholder) itemView.isClickable = false
            else if (album.spotifyUri != null) itemView.setOnClickListener { onAlbumClick(album.spotifyUri) }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.albumArt
        val albumTitle: TextView = itemView.albumTitle
        val albumArtist: TextView = itemView.albumArtist
    }

}
