package com.chuckstein.libzy.view.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chuckstein.libzy.R
import com.chuckstein.libzy.viewmodel.data.AlbumData
import kotlinx.android.synthetic.main.layout_album_result.view.album_art as albumArt
import kotlinx.android.synthetic.main.layout_album_result.view.album_title as albumTitle
import kotlinx.android.synthetic.main.layout_album_result.view.album_artist as albumArtist

class AlbumsRecyclerAdapter(private val albums: List<AlbumData>) :
    RecyclerView.Adapter<AlbumsRecyclerAdapter.ViewHolder>() {

    companion object {
        private val TAG = AlbumsRecyclerAdapter::class.java.simpleName
    }

    override fun getItemCount() = albums.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_album_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder called");
        val album = albums[position]
        with(holder) {
            albumArt.setImageURI(album.albumArtUri) // TODO: use Glide for image loading?
            albumTitle.text = album.albumTitle
            albumArtist.text = album.albumArtist
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.albumArt
        val albumTitle: TextView = itemView.albumTitle
        val albumArtist: TextView = itemView.albumArtist
    }

}