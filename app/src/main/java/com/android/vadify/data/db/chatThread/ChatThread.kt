package com.android.vadify.data.db.chatThread

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.vadify.data.api.models.ListOfUserResponse
import com.android.vadify.viewmodels.EncryptionViewModel
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ChatThread", inheritSuperIndices = false)
data class ChatThread(
    @PrimaryKey
    val id: String,
    val type: String,
    val lastMessageFrom: String,
    val lastMessageType: String,
    val lastMessage: String?,
    val lastMessageSender: String?,
    val lastMessageReceiver: String?,
    val lastMessageDate: String?,
    val name:String?,
    val profileImage: String?,
    var members: List<ListOfUserResponse.Data.Member>,
    var user: ListOfUserResponse.Data.User,
    val unreadCount: Int,
    var isUserBlock: Boolean

) {
    object Mapper {
        fun from(
            response: ListOfUserResponse,
            isDecrypting: Boolean = true
        ): List<ChatThread> {

            Log.d("fromSingle: ", "" + response)
            return response.run {

                response.data.map { data ->


                    ChatThread(
                        id = data._id,
                        type = data.type,
                        members = data.members,
                        user = data.user,
                        lastMessage = if (isDecrypting) EncryptionViewModel.decryptString(data.user.lastMessage) else data.user.lastMessage,
                        name = data.name,
                        //lastMessage = data.lastMessage,
                        lastMessageSender = data.lastMessageSender,
                        lastMessageReceiver = data.lastMessageReceiver,
                        lastMessageDate = data.lastMessageDate,
                        lastMessageFrom = data.lastMessageFrom,
                        //lastMessageType  = EncryptionViewModel.decryptString(data.lastMessageType),
                        profileImage = data.profileImage,
                        lastMessageType = data.lastMessageType,
                        unreadCount = data.unreadCount,
                        isUserBlock = data.isUserBlock
                    )
                }
            }
        }

        fun fromSingle(
            response: ListOfUserResponse.Data,
            isDecrypting: Boolean = false
        ): ChatThread {


            return response.run {

                ChatThread(
                    id = _id,
                    type = type,
                    members = members,
                    user = user,
                    lastMessage = if (isDecrypting) EncryptionViewModel.decryptString(user.lastMessage) else EncryptionViewModel.decryptString(lastMessage),
                    name = name,
                    profileImage = profileImage,
                    //lastMessage = data.lastMessage,
                    lastMessageSender = if (isDecrypting) EncryptionViewModel.decryptString(
                        lastMessageSender
                    ) else lastMessageSender,
                    lastMessageReceiver = if (isDecrypting) EncryptionViewModel.decryptString(
                        lastMessageReceiver
                    ) else lastMessageReceiver,
                    lastMessageDate = lastMessageDate,
                    lastMessageFrom = lastMessageFrom,
                    //lastMessageType  = EncryptionViewModel.decryptString(data.lastMessageType),

                    lastMessageType = lastMessageType,
                    unreadCount = unreadCount,
                    isUserBlock = isUserBlock
                )
            }
        }
    }
}

@Dao
interface ChatThreadCache {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: ChatThread)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: List<ChatThread>)

    @Query("SELECT * FROM ChatThread")
    fun getAll(): LiveData<List<ChatThread>?>

    @Query("SELECT * FROM ChatThread")
    fun getAllChatThreadsSync(): List<ChatThread>

    @Query("SELECT * FROM ChatThread WHERE id = :roomId")
    fun getChatThread(roomId: String): ChatThread

    @Query("DELETE FROM ChatThread WHERE id = :roomId")
    fun deleteChatThread(roomId: String)

    @Query("DELETE FROM ChatThread")
    fun deleteAll()
}
