package com.android.vadify.utils

import android.content.Context
import java.io.File


object LocalStorage {

    const val DOWNLOAD_AUDIO_FILE_PATH = "vaidfy/audio/download"
    const val DOWNLOAD_VIDEO_FILE_PATH = "vaidfy/video/download"
    const val DOWNLOAD_DOCUMENT_FILE_PATH = "vaidfy/document/download"
    private const val AUDIO_NAME = "andoidAudio_"
    private const val VIDEO_NAME = "andoidVideo_"
    private const val AUDIO_EXTENSION = ".acc"
    private const val VIDEO_EXTENSION = ".mp4"


    fun Context.getFilePath(path: String): String {
        var localPath = ""
        var finalPath = ""
        path.split("/").map {
            localPath += "/$it"
            val dir = File(this.getExternalFilesDir(null)?.absolutePath + localPath)
            if (!dir.exists()) {
                dir.mkdir()
            }
            finalPath = dir.absolutePath
        }
        return finalPath
    }

    fun getAudioPath(path: String): String {
        return path + "/" + AUDIO_NAME + System.currentTimeMillis() + AUDIO_EXTENSION
    }

    fun getDownloadFilePath(path: String): String {
        return path + "/" + VIDEO_NAME + System.currentTimeMillis() + VIDEO_EXTENSION
    }

    fun String.getDocumentFilePath(path: String): String {
        return path + "/" + DOWNLOAD_DOCUMENT_FILE_PATH + System.currentTimeMillis() + this
    }


}