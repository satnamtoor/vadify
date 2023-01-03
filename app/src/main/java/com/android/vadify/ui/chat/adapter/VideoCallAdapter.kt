package com.android.vadify.ui.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.R
import com.twilio.video.VideoTrack
import com.twilio.video.VideoView

class VideoCallAdapter(private val mList: ArrayList<VideoTrack>) : RecyclerView.Adapter<VideoCallAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoData = mList[position]
        videoData.removeSink(holder.videoView)
        holder.videoView.mirror = true
        videoData.addSink(holder.videoView)
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    fun addVideo(videoTrack: VideoTrack) {
        mList.add(videoTrack)
        notifyDataSetChanged()
    }

    fun removeParticipant(videoTrack: VideoTrack) {
        mList.remove(videoTrack)
        notifyDataSetChanged()

    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val videoView: VideoView = itemView.findViewById(R.id.primary_video_view)
    }
}