package com.android.vadify.ui.chat.adapter

import android.media.MediaPlayer
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.data.api.models.AudioFileView
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.databinding.ItemChatBinding
import com.android.vadify.widgets.currentSeconds
import com.android.vadify.widgets.seconds
import com.google.android.material.textview.MaterialTextView
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundPagedListAdapter
import java.io.File
import java.io.IOException
import java.util.*

abstract class ChatAudioPlayer(
    diffCallback: DiffUtil.ItemCallback<Chat>,
    private val filePath: String
) : DataBoundPagedListAdapter<Chat, ItemChatBinding>(diffCallback) {

    var mMediaPlayer: MediaPlayer? = null
    var audioFileView: AudioFileView? = null
    var timer: Timer? = null

    abstract fun isAudioPlayerStartOrStop(isStart: Boolean)

    private fun playAudio(url: String) {
        try {
            stopAudio()
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setDataSource(url)
            mMediaPlayer?.prepare()
            mMediaPlayer?.setOnCompletionListener {
                stopAudio()
                audioFileView?.resetPreviousData()
             ///   isAudioPlayerStartOrStop(false)
            }

        } catch (IoEx: IOException) {
            Log.e("response are", "" + IoEx.message.toString())
        }
    }

    private fun voiceRecordTimerCountMethod(
        seekBar: SeekBar,
        timer: MaterialTextView
    ) {
        mMediaPlayer?.let {
            seekBar.max = it.seconds
            seekBar.progress = 0
            this.timer = Timer()
            val monitor = object : TimerTask() {
                override fun run() {
                    try {
                        seekBar.progress = it.currentSeconds
                        timer.text = String.format("%02d:%02d", 0, it.currentSeconds)
                    } catch (e: Exception) {
                    }
                }
            }
            this.timer?.scheduleAtFixedRate(monitor, 0, 100)
        }
    }

    fun SeekBar.callLocalDirectory(
        chatData: Chat,
        pauseMusic: AppCompatImageView,
        playMusic: AppCompatImageView,
        timer: MaterialTextView
    ) {
        audioFileView?.resetPreviousData()
        chatData.localUrl = checkLocalPathExist(chatData)
        chatData.isAudioPlaying = true
        playMusic(chatData, this, timer)
        audioFileView = AudioFileView(chatData, pauseMusic, playMusic)
        audioFileView?.updatePreviousView(View.VISIBLE, View.GONE)
    }


    fun stopAudio() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
        }
    }

    fun checkLocalPathExist(chatData: Chat): String {
        val path = filePath + "/" + File(chatData.url).name
        return when {
            chatData.localUrl.isNotEmpty() || File(chatData.localUrl).exists() -> chatData.localUrl
            File(path).exists() -> path
            else -> ""
        }
    }

    private fun playMusic(
        chatData: Chat,
        seekBar: SeekBar,
        timer: MaterialTextView
    ) {
        this.timer?.cancel()
        playAudio(chatData.localUrl)
        voiceRecordTimerCountMethod(seekBar, timer)
        mMediaPlayer?.start()
    }

}