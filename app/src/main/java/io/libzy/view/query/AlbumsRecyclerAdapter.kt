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

            // TODO: use DiffUtil if I'll ever need to update data set after filling the initial data (see Udacity course for details... does replacing loading placeholders count?)
            // TODO: see if DiffUtil can prevent jump to beginning of album row if # results is the same because it was just loading screen being replaced (might be irrelevant if horizontal scrolling is prevented on loading screen or if actual data set just replaces album fields not album itself)
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
            // TODO: look into Glide usage, error handling, null handling, other builder functions (e.g. centerCrop), etc.
            if (album.artworkUrl != null) glide.load(album.artworkUrl).placeholder(albumArtPlaceholder).into(albumArt)
            else if (albumArtPlaceholder != null) albumArt.setImageDrawable(albumArtPlaceholder)
            albumTitle.text = album.title
            albumArtist.text = album.artists

            if (album.isPlaceholder) itemView.isClickable = false
            else if (album.spotifyUri != null) itemView.setOnClickListener { onAlbumClick(album.spotifyUri) }
            // TODO: if it violates separation of concerns to have the spotify URI in the view data, then instead pass in a 2d genre/album position for the ViewModel to figure out the spotify URI from
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.albumArt
        val albumTitle: TextView = itemView.albumTitle
        val albumArtist: TextView = itemView.albumArtist
    }

}