package com.android.vadify.service

data class MessageResponse(val message: CharSequence,
                           val timestamp: Long,
                           val from: CharSequence)