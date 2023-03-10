package com.robert.progress.kotlin

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source

/**
 * Progress response body
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: ProgressCallback?
) : ResponseBody() {
    private var progressSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }


    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (progressListener == null) {
            return responseBody.source()
        }
        val progressInputStream = ProgressInputStream(
            responseBody.source().inputStream(),
            progressListener,
            contentLength()
        )
        progressSource = progressInputStream.source().buffer()
        return progressSource as BufferedSource
    }

    override fun close() {
        if (progressSource != null) {
            try {
                progressSource!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}