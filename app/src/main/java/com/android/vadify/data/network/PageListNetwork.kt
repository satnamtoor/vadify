package com.android.vadify.data.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import com.android.vadify.R
import com.android.vadify.ui.util.IListResource
import com.sdi.joyersmajorplatform.uiview.NetworkState
import retrofit2.Call
import retrofit2.Response
import java.io.IOException


abstract class PageListNetwork<LocalType> : PageKeyedDataSource<Int, LocalType>() {
    abstract val networkState: LiveData<NetworkState>
    abstract val isRefreshing: LiveData<Boolean>
    abstract val networkStateBefore: LiveData<NetworkState>
    abstract val networkStateAfter: LiveData<NetworkState>
    abstract fun refresh(showIndicator: Boolean = false)
    abstract fun retry()
}

abstract class PagedListNetworkResource<LocalType, Key>(
    private val dataSourceFactory: DataSource.Factory<Key, LocalType>,
    private val pagingCallback: PageListNetwork<LocalType>,
    config: PagedList.Config = DEFAULT_CONFIG
) :
    IListResource<LocalType> {
    override val networkStateBefore: LiveData<NetworkState> = pagingCallback.networkStateBefore
    override val networkStateAfter: LiveData<NetworkState> = pagingCallback.networkStateAfter
    override fun refresh() {
        pagingCallback.invalidate()
    }

    final override val data: LiveData<PagedList<LocalType>>
    override val isRefreshing: LiveData<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val networkState: LiveData<NetworkState> = pagingCallback.networkState
    override fun retry(networkState: NetworkState) = pagingCallback.retry()

    init {
        val builder = LivePagedListBuilder(dataSourceFactory, config)
        data = builder.build()
    }

    companion object {
        val DEFAULT_CONFIG: PagedList.Config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(1)
            .setPageSize(1)
            .build()
    }
}


abstract class NetworkListResourceBoundary<LocalType, RemoteType>(private val appExecutors: AppExecutors) :
    PageListNetwork<LocalType>() {
    override val networkState: MutableLiveData<NetworkState> = MutableLiveData()
    override val isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    override val networkStateBefore: MutableLiveData<NetworkState> = MutableLiveData()
    override val networkStateAfter: MutableLiveData<NetworkState> = MutableLiveData()
    private var retry: (() -> Any)? = null
    override fun refresh(showIndicator: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retry() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            appExecutors.networkIO().execute {
                it.invoke()
            }
        }
    }

    protected abstract fun loadAfter(page: Int): Call<RemoteType>
    protected abstract fun loadBefore(page: Int): Call<RemoteType>
    protected abstract fun mapToLocal(items: RemoteType): List<LocalType>

    override fun loadInitial(
        params: PageKeyedDataSource.LoadInitialParams<Int>,
        callback: PageKeyedDataSource.LoadInitialCallback<Int, LocalType>
    ) {
        networkState.postValue(NetworkState.loading)
        networkStateBefore.postValue(NetworkState.loading)
        val request = loadBefore(params.requestedLoadSize)
        try {
            val response = request.execute()
            retry = null
            if (response.isSuccessful) {
                response.body()?.let { callback.onResult(mapToLocal(it), null, 1) }
                networkState.postValue(NetworkState.success)
                networkStateBefore.postValue(NetworkState.success)
            } else {
                retry = { loadInitial(params, callback) }
                networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                networkStateBefore.postValue(NetworkState.error(R.string.network_error_unknown))
            }
        } catch (ioException: IOException) {
            retry = { loadInitial(params, callback) }
            networkState.postValue(
                NetworkState.error(
                    ioException.message,
                    R.string.network_error_unknown
                )
            )
            networkStateBefore.postValue(NetworkState.error(ioException.localizedMessage))
        }
    }

    override fun loadAfter(
        params: PageKeyedDataSource.LoadParams<Int>,
        callback: PageKeyedDataSource.LoadCallback<Int, LocalType>
    ) {
        networkState.postValue(NetworkState.loading)
        networkStateAfter.postValue(NetworkState.loading)
        loadAfter(params.key).enqueue(object : retrofit2.Callback<RemoteType> {
            override fun onFailure(call: Call<RemoteType>, t: Throwable) {
                retry = { loadAfter(params, callback) }
                networkState.postValue(
                    NetworkState.error(
                        t.message,
                        R.string.network_error_unknown
                    )
                )
                networkStateAfter.postValue(NetworkState.error(t.localizedMessage))
            }

            override fun onResponse(call: Call<RemoteType>, response: Response<RemoteType>) {
                when {
                    response.isSuccessful -> {
                        response.body()?.let { callback.onResult(mapToLocal(it), params.key + 1) }
                        networkState.postValue(NetworkState.success)
                        networkStateAfter.postValue(NetworkState.success)
                    }
                    else -> {
                        networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                        networkStateAfter.postValue(NetworkState.error(R.string.network_error_unknown))
                    }
                }
            }
        })
    }

    override fun loadBefore(
        params: PageKeyedDataSource.LoadParams<Int>,
        callback: PageKeyedDataSource.LoadCallback<Int, LocalType>
    ) {

    }
}

