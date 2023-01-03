package com.android.vadify.data.api.models

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.android.vadify.data.db.chat.Chat

data class AudioFileView(
    val previousChat: Chat?,
    val previousPauseButton: AppCompatImageView,
    val previousplayButton: AppCompatImageView
) {
    fun updatePreviousView(pauseVisiblility: Int, playVisiblilty: Int) {
        previousPauseButton.visibility = pauseVisiblility
        previousplayButton.visibility = playVisiblilty
    }

    fun resetPreviousData() {
        previousChat?.let { it.isAudioPlaying = false }
        updatePreviousView(View.GONE, View.VISIBLE)
    }

    fun isCurrentMusicPlay(chatData: Chat) =
        if (chatData.localUrl.isBlank() || previousChat?.localUrl.isNullOrBlank()) false else
            previousChat?.localUrl.equals(
                chatData.localUrl,
                ignoreCase = true
            ) && previousChat?.isAudioPlaying == true
}