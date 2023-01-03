/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.vadify.ui.chat.viewmodel
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.DownloadUploadStatus
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.di.WorkerKey
import com.android.vadify.utils.LocalStorage
import com.android.vadify.utils.LocalStorage.getFilePath
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.util.*
import javax.inject.Inject
class DownloadFileWorker @Inject constructor(
    var context: Context,
    params: WorkerParameters,
    private var chatListCache: ChatListCache
) : CoroutineWorker(context, params) {
    companion object {
        const val URL = "url"
        const val TYPE = "type"
    }
    var isResultOk = false
    var NOTIFICATION_ID = 1234
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    lateinit var workerManager: WorkManager
    private val notificationBuilder = NotificationCompat.Builder(
        applicationContext,
        UploadFileWorker.CHANNEL_ID
    ).setSmallIcon(R.drawable.logo)
    override suspend fun doWork(): Result {
        workerManager = WorkManager.getInstance(context)
        var count: Int
        var demoUrl = ""
        try {
            inputData.getString(URL)?.let { it ->
                val fileType = inputData.getString(TYPE)
                val random = Random()
                val generatedPassword = java.lang.String.format("%04d", random.nextInt(10000))
                NOTIFICATION_ID = generatedPassword.toInt()
                notificationBuilder.setContentTitle("Downloading $fileType...")
                createNotificationChannel()
                setForeground(ForegroundInfo(NOTIFICATION_ID, notificationBuilder.build()))
                updateUrl(DownloadUploadStatus.IN_PROGRESS.value, it)
                demoUrl = it
                val file = File(it)
                val url = URL(it)
                val conexion: URLConnection = url.openConnection()
                conexion.connect()
                // this will be useful so that you can show a tipical 0-100% progress bar
                val localPath = filterFileName(file.name)
                val lenghtOfFile: Int = conexion.contentLength
                val input: InputStream = BufferedInputStream(url.openStream())
                val output: OutputStream = FileOutputStream(localPath)
                val data = ByteArray(1024)
                var total: Long = 0
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    val progress = (total * 100 / lenghtOfFile).toInt()
                    setProgress(workDataOf(UploadFileWorker.ARG_PROGRESS to progress))
                    showProgress(progress)
                    VadifyApplication.progressDownload.postValue(hashMapOf(it to progress))
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                input.close()
                updateUrlOnDataBase(localPath, it)
                workerManager.cancelWorkById(this@DownloadFileWorker.id)
            }
        } catch (e: Exception) {
            updateUrl(DownloadUploadStatus.FAILED.value, demoUrl)
            Log.e("response are ", "" + e.message.toString())
        }
        while (!isResultOk) {
            delay(100)
            Log.d("progress----- ", "uploading")
        }
        return Result.success()
    }
    private fun updateUrl(status: Int, liveUrl: String) {
        if (liveUrl.isNotBlank()) {
            chatListCache.updateDownloadStatus(status, liveUrl)
        }
    }

    private fun updateUrlOnDataBase(localPath: String, liveUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            chatListCache.updateLocalUrl(localPath, liveUrl)
            updateUrl(DownloadUploadStatus.SUCCESS.value, liveUrl)
        }
    }
    private fun filterFileName(fileName: String): String {
        var filePath =
            context.getFilePath(LocalStorage.DOWNLOAD_DOCUMENT_FILE_PATH) + "/" + fileName
        inputData.getString(UploadFileWorker.TYPE)?.let {
            filePath = when (it) {
                MessageType.DOCUMENT.value -> context.getFilePath(LocalStorage.DOWNLOAD_DOCUMENT_FILE_PATH) + "/" + fileName
                MessageType.VIDEO.value -> context.getFilePath(LocalStorage.DOWNLOAD_VIDEO_FILE_PATH) + "/" + fileName
                MessageType.AUDIO.value -> context.getFilePath(LocalStorage.DOWNLOAD_AUDIO_FILE_PATH) + "/" + fileName
                else -> context.getFilePath(LocalStorage.DOWNLOAD_DOCUMENT_FILE_PATH) + "/" + fileName
            }
        }
        return filePath
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                notificationManager?.getNotificationChannel(UploadFileWorker.CHANNEL_ID)
            if (notificationChannel == null) {
                notificationManager?.createNotificationChannel(
                    NotificationChannel(
                        UploadFileWorker.CHANNEL_ID,
                        UploadFileWorker.TAG,
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }
    private suspend fun showProgress(progress: Int) {
        val notification = notificationBuilder
            .setProgress(100, progress, false)
            .build()
        val foregroundInfo = ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)
    }

    @Module
    abstract class Builder {
        @Binds
        @IntoMap
        @WorkerKey(DownloadFileWorker::class)
        abstract fun bindHelloWorldWorker(worker: DownloadFileWorker): CoroutineWorker
    }
}