package com.android.vadify.ui.chat.videoplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.android.vadify.R
import kotlinx.android.synthetic.main.activity_video_player.*
import java.io.File


class VideoPlayerActivity : AppCompatActivity() {
    private var uri = ""

    companion object {
        fun start(activity: Activity, uri: String) {
            val intent = Intent(activity, VideoPlayerActivity::class.java)
                .putExtra("uri", uri)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        intent?.extras?.let {
            uri = it.getString("uri", "")
        }


        toolbar.setOnClickListener {
            finish()
        }
    }



    private fun play(uri: Uri) {


        val videoView = findViewById<VideoView>(R.id.ep_video_view);

        videoView.setVideoURI(uri)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
        videoView.start()

    }

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    private fun playVideo() {
        val file = File(uri)
        val localUri = Uri.fromFile(file)
        play(localUri)
    }

}
