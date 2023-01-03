package com.android.vadify.data.network

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.android.vadify.R
import com.android.vadify.ui.util.IListResource
import com.android.vadify.utils.PagingRequestHelper
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import retrofit2.Response


abstract class PageListNetworkResource<LocalType> : PagedList.BoundaryCallback<LocalType>() {
    abstract val networkState: LiveData<NetworkState>
    abstract val isRefreshing: LiveData<Boolean>
    abstract val networkStateBefore: LiveData<NetworkState>
    abstract val networkStateAfter: LiveData<NetworkState>
    abstract fun refresh(showIndicator: Boolean = false)
    abstract fun retry()
}

abstract class PagedListNetworkResourceDatabase<LocalType, Key>(
    private val dataSourceFactory: DataSource.Factory<Key, LocalType>,
    private val pagingCallback: PageListNetworkResource<LocalType>,
    config: PagedList.Config = DEFAULT_CONFIG
) : IListResource<LocalType> {
    override val networkStateBefore: LiveData<NetworkState> = pagingCallback.networkStateBefore
    override val networkStateAfter: LiveData<NetworkState> = pagingCallback.networkStateAfter
    override fun refresh() {
        // pagingCallback.invalidate()
    }

    final override val data: LiveData<PagedList<LocalType>>
    override val isRefreshing: LiveData<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val networkState: LiveData<NetworkState> = pagingCallback.networkState

    override fun retry(networkState: NetworkState) = pagingCallback.retry()

    init {
        val builder = LivePagedListBuilder(dataSourceFactory, config)
        builder.setBoundaryCallback(pagingCallback)
        data = builder.build()
    }

    companion object {
        val DEFAULT_CONFIG: PagedList.Config = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setInitialLoadSizeHint(30)
            .setPageSize(30)
            .build()
    }
}

abstract class NetworkListResourceBoundaryCallback<LocalType, RemoteType>(private val appExecutors: AppExecutors) :
    PageListNetworkResource<LocalType>() {
    var helper = PagingRequestHelper(appExecutors.networkIO())
    override val networkState: MutableLiveData<NetworkState> = MutableLiveData()
    override val isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    override val networkStateBefore: MutableLiveData<NetworkState> = MutableLiveData()
    override val networkStateAfter: MutableLiveData<NetworkState> = MutableLiveData()


    override fun refresh(showIndicator: Boolean) {}

    override fun retry() {
        onZeroItemsLoaded()
    }

    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        val resource = loadAfter()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) { cb ->
            networkState.postValue(NetworkState.loading)
            networkStateBefore.postValue(NetworkState.loading)
            resource
                .subscribeOn(Schedulers.io())
                .subscribeBy(onSuccess =
                { it ->
                    if (it.isSuccessful) {
                        it.body()?.let {
                            insertLocalData(mapToLocal(it))
                            networkState.postValue(NetworkState.success)
                            networkStateBefore.postValue(NetworkState.success)
                            cb.recordSuccess()
                        }
                    } else {
                        networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                        networkStateBefore.postValue(NetworkState.error(R.string.network_error_unknown))
                        cb.recordFailure(Throwable("Unknown network error"))
                    }
                }, onError = {
                    networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                    networkStateBefore.postValue(NetworkState.success)
                    cb.recordFailure(Throwable("Unknown network error"))
                })
        }

    }

    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
    }

    @MainThread
    override fun onZeroItemsLoaded() {
        val resource = loadBefore()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            networkState.postValue(NetworkState.loading)
            networkStateBefore.postValue(NetworkState.loading)
            resource.subscribeOn(Schedulers.io())
                .subscribeBy(
                    onSuccess = {
                        if (it.isSuccessful) {
                            it.body()?.let {
                                insertLocalData(mapToLocal(it))
                                networkState.postValue(NetworkState.success)
                                networkStateBefore.postValue(NetworkState.success)

                            }
                        } else {
                            networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                            networkStateBefore.postValue(NetworkState.error(R.string.network_error_unknown))

                        }
                    },
                    onError = {
                        networkState.postValue(NetworkState.error(R.string.network_error_unknown))
                        networkStateBefore.postValue(NetworkState.success)
                      //  networkStateBefore.postValue(NetworkState.error(R.string.network_error_unknown))
                    }
                )
        }

    }


    protected abstract fun loadAfter(): Single<Response<RemoteType>>
    protected abstract fun loadBefore(): Single<Response<RemoteType>>
    protected abstract fun mapToLocal(items: RemoteType): List<LocalType>
    protected abstract fun deleteCache()
    protected abstract fun insertLocalData(item: List<LocalType>)

}

