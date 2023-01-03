package com.android.vadify.data.db.callLogs

import android.util.Log
import androidx.paging.DataSource
import androidx.room.*
import com.android.vadify.data.api.enums.CallLogStatus
import com.android.vadify.data.api.models.CallLogResponse
import com.google.gson.Gson


@Entity(tableName = "CallLogs", inheritSuperIndices = false)
data class CallLogs(
    val v: Int,
    @PrimaryKey
    val id: String,
    val createdAt: String,
    var members: List<CallLogResponse.DataX.Member>,
    val updatedAt: String,
    val roomId: String,
    val roomName: String?,
    val type: String,
    val mode: String,
    val from: String,
    val callStatus: String,
    val duration: Int,
    val fromName: String,
    val fromProfileImage: String
) {
    object Mapper {
        fun from(response: CallLogResponse.DataX, userId: String?): CallLogs {
            val userInfomration = fromNameProfileImage(response, userId)
            return response.run {
                CallLogs(
                    v = __v,
                    id = _id,
                    createdAt = createdAt,
                    members = members,
                    updatedAt = updatedAt,
                    roomId = roomId._id,
                    roomName = roomId.name,
                    type = type,
                    mode = mode,
                    from = from,
                    callStatus = iterationMethod(response, userId),
                    duration = durationMethod(response),
                    fromName = userInfomration.name,
                    fromProfileImage = userInfomration.profileImage
                )
            }
        }


        private fun fromNameProfileImage(
            data: CallLogResponse.DataX,
            userId: String?
        ): UserInformation {
            val userInformation = UserInformation("", "")
            data.members.singleOrNull { it.userId != userId }?.let {
                userInformation.name = it.name
                userInformation.profileImage = it.profileImage
            }
            return userInformation
        }

        private fun iterationMethod(data: CallLogResponse.DataX, userId: String?): String {

            var result = CallLogStatus.CONNECTED.value
            data.members.map {
                if (it.status != CallLogStatus.CONNECTED.value) {
                    Log.d("call-logs--status", it.status)
                    if (it.status == CallLogStatus.DECLINED.value) {
                        Log.d("call-logs--decline", it.status)
                        result = CallLogStatus.DECLINED.value
                    } else {
                        result = CallLogStatus.MISSED.value
                    }
                }
            }
            val finalResult =
                if (data.from == userId) CallLogStatus.OUTGOING.value else CallLogStatus.INCOMING.value
            return if (result != CallLogStatus.MISSED.value) finalResult else result
        }

        private fun durationMethod(data: CallLogResponse.DataX): Int {
            var duration = 0
            data.members.mapIndexed { index, member ->
                if (index == 0) {
                    duration = member.duration
                } else {
                    if (duration > member.duration) {
                        duration = member.duration
                    }
                }
            }
            return duration
        }
    }
}

data class UserInformation(var name: String, var profileImage: String)

@Dao
interface CallListCache {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: CallLogs)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: List<CallLogs>)

    @Query("UPDATE chat SET localUrl = :localPath WHERE url = :liveUrl")
    fun updateLocalUrl(localPath: String, liveUrl: String)

    @Query("SELECT * FROM CallLogs WHERE callStatus = :callStatus AND fromName LIKE :args")
    fun searchQueryInMissedCall(callStatus: String, args: String): DataSource.Factory<Int, CallLogs>

    @Query("SELECT * FROM CallLogs WHERE fromName LIKE :args")
    fun searchQueryWithALLCall(args: String): DataSource.Factory<Int, CallLogs>

    @Query("SELECT * FROM CallLogs WHERE callStatus = :callStatus")
    fun getAll(callStatus: String): DataSource.Factory<Int, CallLogs>

    @Query("SELECT * FROM CallLogs")
    fun getAll(): DataSource.Factory<Int, CallLogs>

   /* @Query("SELECT * FROM CallLogs where mode =")
    fun getAll(): DataSource.Factory<Int, CallLogs>*/

    @Query("DELETE FROM CallLogs")
    fun deleteAll()

}
