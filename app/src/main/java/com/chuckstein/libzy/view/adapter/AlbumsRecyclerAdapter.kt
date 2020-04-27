package com.chuckstein.libzy.view.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chuckstein.libzy.R
import kotlinx.android.synthetic.main.layout_album_result.view.*

class AlbumsRecyclerAdapter : RecyclerView.Adapter<AlbumsRecyclerAdapter.ViewHolder>() {

    companion object {
        private val TAG = AlbumsRecyclerAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =  LayoutInflater.from(parent.context).inflate(R.layout.layout_album_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder called");

        // TODO: figure out data source (probably a data class, but I get it from the model, so need to learn more architecture stuff)
        with(holder) {
//            albumArt.setImageURI(get from spotify) // TODO: use Glide for image loading?
//            albumTitle = get from spotify
//            albumArtist = get from spotify
        }
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.albumArt
        val albumTitle: TextView = itemView.albumTitle
        val albumArtist: TextView = itemView.albumArtist
    }

}