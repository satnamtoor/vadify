package com.android.vadify.data.db.chat

import android.util.Log
import android.util.Patterns
import android.webkit.URLUtil
import androidx.paging.DataSource
import androidx.room.*
import com.android.vadify.data.api.enums.DownloadUploadStatus
import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.viewmodels.EncryptionViewModel
import java.net.MalformedURLException
import java.net.URL

@Entity(tableName = "chat", inheritSuperIndices = false)
data class Chat(
    val v: Int,
    @PrimaryKey
    val id: String,
    val createdAt: String,
    val message: String,
    val messageSender: String,
    val messageReceiver: String,
    val roomId: String,
    val type: String,
    val updatedAt: String,
    var members: List<MessageResponseList.Data.DataX.Member>,
    var from_id: String,
    var from_name: String,
    var from_number: String,
    var profileImage: String,
    var isdateVisible: String,
    val url: String,
    var localUrl: String,
    val downloadStatus: Int,
    var isAudioPlaying: Boolean,
    var localOrignalPath: String,
    val lat_location: String?,
    val long_location: String?,
    val contact_name: String,
    val contact_number: String,
    val translatedText: String,
    val replyToMessageId: String,
    var forwarded: Boolean,
    var progress : Int
) {
    object Mapper {
        fun from(
            response: MessageResponseList.Data.DataX,
            isdateVisible: String = "",
            localFileUrl: String = "",
            localOrignalPath: String = "",
            downloadStatus: Int = DownloadUploadStatus.NONE.value,
            isDecrypting: Boolean = false
        ): Chat {

            return response.run {

                val decryptedURL = if (isDecrypting) {
                    EncryptionViewModel.decryptString(url)
                } else {
                    response.url ?: ""
                }


                Chat(
                    message = "hiii",
                    messageSender = EncryptionViewModel.decryptString(messageSender),
                    messageReceiver = EncryptionViewModel.decryptString(messageReceiver),
                    lat_location = lat,
                    long_location = lng,
                    contact_name = contact?.name ?: "",
                    contact_number = contact?.number ?: "",
                    v = __v,
                    id = _id,
                    createdAt = createdAt,

                    roomId = roomId,
                    type = type,
                    updatedAt = updatedAt,
                    members = members,

                    from_id = from?._id ?:"",
                    from_name = from?.name ?:"",
                    from_number = from?.number ?:"",
                    profileImage = from?.profileImage ?:"",

                    isdateVisible = isdateVisible,
                    url =  decryptedURL,
                    localUrl = localFileUrl,
                    downloadStatus = downloadStatus,
                    isAudioPlaying = false,
                    localOrignalPath = localOrignalPath,
                    translatedText = translatedText ?: message ?: "",
                    replyToMessageId = replyToMessageId ?: "",
                    forwarded = forwarded ?: false,
                    progress = progress?:0
                )
            }
        }
    }
}

fun isValidUrl(urlString: String?): Boolean {
    try {
        val url = URL(urlString)
        return URLUtil.isValidUrl(urlString) && Patterns.WEB_URL.matcher(urlString).matches()
    } catch (ignored: MalformedURLException) {
    }
    return false
}
@Dao
interface ChatListCache {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: Chat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: List<Chat>)

    @Query("SELECT * FROM chat")
    fun getAll(): List<Chat>

    @Query("SELECT * FROM chat WHERE roomId = :roomId ORDER BY updatedAt DESC LIMIT 1")
    fun getLastMessage(roomId: String): Chat

    @Query("SELECT * FROM chat WHERE id = :id")
    fun getMessage(id: String): Chat

    @Query("SELECT * FROM chat WHERE roomId = :roomId ORDER BY updatedAt DESC")
    fun getAll(roomId: String): DataSource.Factory<Int, Chat>

    @Query("UPDATE chat SET localUrl = :localPath WHERE url = :liveUrl")
    fun updateLocalUrl(localPath: String, liveUrl: String)

    @Query("UPDATE chat SET downloadStatus = :status WHERE url = :liveUrl")
    fun updateDownloadStatus(status: Int, liveUrl: String)

    @Query("UPDATE chat SET downloadStatus = :status WHERE localOrignalPath = :localOrignalPath")
    fun updateDummyStatus(status: Int, localOrignalPath: String)

    @Query("UPDATE chat SET progress = :status WHERE localOrignalPath = :localOrignalPath")
    fun updateProgressStatus(status: Int, localOrignalPath: String)

    @Query("UPDATE chat SET v = :v,id = :id,  message=:message, messageSender=:messageSender, messageReceiver=:messageReceiver, members=:members, roomId= :roomId, type=:type,from_id = :from_id, from_name=:from_name ,from_number=:from_number ,profileImage=:from_profileImage,isDateVisible=:isDateVisible,url =:url,downloadStatus=:downloadStatus,isAudioPlaying =:isAudioPlaying  WHERE localOrignalPath = :localOrignalPath")
    fun updateDummyView(
        v: Int,
        id: String,
        members: List<MessageResponseList.Data.DataX.Member>,
        message: String,
        messageSender: String,
        messageReceiver: String,
        roomId: String,
        type: String,
        from_id: String,
        from_name: String,
        from_number: String,
        from_profileImage: String,
        isDateVisible: String,
        url: String,
        downloadStatus: Int,
        isAudioPlaying: Boolean,
        localOrignalPath: String
    )

    @Query("DELETE FROM chat")
    fun deleteAll()

    @Query("DELETE FROM chat WHERE roomId = :roomId")
    fun deleteSpecificChat(roomId: String)

    @Query("DELETE FROM chat WHERE id = :messageId")
    fun deleteMessage(messageId: String)
}
