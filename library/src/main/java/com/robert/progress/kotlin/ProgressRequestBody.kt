package com.robert.progress.kotlin

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.IOException

class ProgressRequestBody(
    private val mRequestBody: RequestBody,
    private val progressListener: ProgressCallback?
) : RequestBody() {

    override fun contentType(): MediaType? {
        return mRequestBody.contentType()
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return mRequestBody.contentLength()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        if (progressListener == null) {
            mRequestBody.writeTo(sink)
            return
        }
        val progressOutputStream =
            ProgressOutputStream(sink.outputStream(), progressListener, contentLength())
        val progressSink = progressOutputStream.sink().buffer()
        mRequestBody.writeTo(progressSink)
        progressSink.flush()
    }

}