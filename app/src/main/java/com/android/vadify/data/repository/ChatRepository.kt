package com.android.vadify.data.repository

import android.annotation.SuppressLint
import android.util.Log
import com.android.vadify.R
import com.android.vadify.data.api.AuthApi
import com.android.vadify.data.api.models.CallLogResponse
import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.data.db.callLogs.CallListCache
import com.android.vadify.data.db.callLogs.CallLogs
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.network.AppExecutors
import com.android.vadify.data.network.PagedListNetworkCallWithDataBase
import com.android.vadify.data.network.PaginationDatabaseList
import com.android.vadify.ui.dashboard.fragment.call.CallFragment.Companion.MISSED_KEY
import com.android.vadify.ui.util.IListResource
import com.android.vadify.utils.isPreviousDate
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
import javax.inject.Inject


class ChatRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val authApi: AuthApi,
    private val chatListCache: ChatListCache,
    private val callListCache: CallListCache
) {
    fun getChatList(
        roomId: String,
        pageNo: Int,
        count: String,
        languageCode: String
    ): IListResource<Chat> {


        var localPageNumber = pageNo
        var currentDate = ""
        return PagedListNetworkCallWithDataBase(
            dataSourceFactory = chatListCache.getAll(roomId),
            paginationNetworkResource = object :
                PaginationDatabaseList<Chat, MessageResponseList>(appExecutors) {
                override fun loadBefore(): Single<Response<MessageResponseList>> {
                    return authApi.getChatList(roomId, localPageNumber, count)
                }

                override fun loadAfter(): Single<Response<MessageResponseList>> {
                    localPageNumber += 1
                    return authApi.getChatList(roomId, localPageNumber, count)
                }

                override fun mapToLocal(items: MessageResponseList): List<Chat> {
                    return items.data.data.mapIndexed { index, data ->
                        if (currentDate.isEmpty()) {
                            currentDate = data.createdAt
                        }
                        if (index + 1 < items.data.data.size && isPreviousDate(
                                currentDate,
                                items.data.data[index + 1].createdAt
                            )
                        ) {
                            Chat.Mapper.from(data, isdateVisible = currentDate)
                                .also { currentDate = items.data.data[index + 1].createdAt }
                        } else {
                            Chat.Mapper.from(data)
                        }
                    }
                }

                override fun deleteCache() {
                    chatListCache.deleteAll()
                }

                override fun insertLocalData(item: List<Chat>) {
                    chatListCache.insert(item)
                }
            })
    }

    @SuppressLint("CheckResult")
    fun getChatMessagesFromAPI(roomId: String,
                               pageNo: Int,
                               count: String) {
        if (!roomId.isNullOrBlank()) {

            authApi.getChatList(roomId, pageNo, count).subscribeOn(Schedulers.io())
                .subscribeBy(onSuccess =
                { it ->
                    if (it.isSuccessful) {
                        it.body()?.let {
                            val mappedList = it.data.data.map {it1->
                                Log.d( "initAlls: ", Gson().toJson(it1))
                                if (it1.type != "text") {
                                    Chat.Mapper.from(it1, isDecrypting = true)
                                } else {
                                    Chat.Mapper.from(it1)
                                }

                            }
                            //   Log.d( "initAlls: ", Gson().toJson(mappedList))
                            chatListCache.insert(mappedList)
                        }
                    }
                }, onError = {

                })
        }
    }


    fun getCallLog(
        pageNo: Int, count: String,
        userId: String?,
        filterKey: String,
        searchKey: String?
    ): IListResource<CallLogs> {
        var localPageNumber = pageNo
        val data = when {
            filterKey.equals(
                MISSED_KEY,
                ignoreCase = true
            ) && searchKey?.isBlank() == true -> callListCache.getAll(filterKey)
            filterKey.equals(
                MISSED_KEY,
                ignoreCase = true
            ) && searchKey?.isNotBlank() == true -> callListCache.searchQueryInMissedCall(
                filterKey,
                "%$searchKey%"
            )
            !filterKey.equals(
                MISSED_KEY,
                ignoreCase = true
            ) && searchKey?.isNotBlank() == true -> callListCache.searchQueryWithALLCall("%$searchKey%")
            else ->
            {
                Log.d( "getCallLog:",Gson().toJson(callListCache.getAll()))
                callListCache.getAll()
            }
            }
        return PagedListNetworkCallWithDataBase(
            dataSourceFactory = data,
            paginationNetworkResource = object :
                PaginationDatabaseList<CallLogs, CallLogResponse>(appExecutors) {
                override fun loadAfter(): Single<Response<CallLogResponse>> {
                    localPageNumber += 1
                    return authApi.getCallLogs(localPageNumber, count)
                }

                override fun loadBefore(): Single<Response<CallLogResponse>> {
                    return authApi.getCallLogs(localPageNumber, count)
                }

                override fun mapToLocal(items: CallLogResponse): List<CallLogs> {
                   // Log.d( "insertcallLocalData: ", Gson().toJson(items.data.data))
                    return items.data.data.map { CallLogs.Mapper.from(it, userId) }
                }

                override fun deleteCache() {
                    callListCache.deleteAll()
                }

                override fun insertLocalData(item: List<CallLogs>) {
                  //  Log.d( "insertcallLocalData: ", Gson().toJson(item))
                    callListCache.insert(item)
                }
            })
    }
}
