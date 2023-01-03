package com.android.vadify.data.network

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.sdi.joyersmajorplatform.common.livedataext.postUpdate
import com.sdi.joyersmajorplatform.uiview.NetworkState

import io.reactivex.Flowable
import io.reactivex.Maybe


/**
 * Represents a basic network resource that provides a mechanism for retry, refresh and network status.
 * */
open class NetworkResource<LocalType, RemoteType, ResponseType>
@MainThread constructor(
    private val appExecutors: AppExecutors,
    private val cb: INetworkResourceCallback<LocalType, RemoteType, ResponseType>,
    init: Boolean = true
) : IResource<LocalType> {

    private val networkRequest = NetworkRequest(appExecutors, cb)

    final override val data = MediatorLiveData<LocalType>()

    final override val networkState = networkRequest.networkState


    private val _isRefreshing: MutableLiveData<Boolean> =
        com.sdi.joyersmajorplatform.common.livedataext.mediatorLiveData(networkState) {
            it?.let { if (it.status != NetworkState.Status.RUNNING) postValue(false) }
        }

    override val isRefreshing: LiveData<Boolean>
        get() = _isRefreshing

    init {
        if (init) {
            val request = prepareNetworkRequest()
            data.addSource(request) {
                data.value = it
                data.removeSource(request)
            }
        }
    }


    override fun refresh() {
        _isRefreshing.postUpdate(true)
        val request = prepareNetworkRequest()
        // TODO: refactor executor/threading layer
        appExecutors.mainThread().execute {
            data.addSource(request) {
                data.removeSource(request)
                data.value = it
            }
        }
    }

    override fun retry(networkState: NetworkState) {
        val request = prepareNetworkRequest()
        data.addSource(request) {
            data.removeSource(request)
            data.value = it
        }
    }

    private fun prepareNetworkRequest(): LiveData<LocalType> {
        val request = createNetworkRequest()
        return toLiveData(toFlowable(request))
    }

    protected open fun createNetworkRequest(): Maybe<LocalType> {
        // TODO
        // return a new maybe, and then on onSubscribe we either attach the request,
        // or return immediately depending on network limiter
        return networkRequest.request
            .map { cb.extractData(it) }
            .doOnSuccess { cb.onSuccessPreMap(it) }
            .map { cb.mapToLocal(it) }
            .doOnSuccess { cb.onSuccessPostMap(it) }
    }

    /**
     * Convert the [Maybe] to [Flowable].
     * May be used to add additional operations to the chain.
     */
    protected open fun toFlowable(request: Maybe<LocalType>): Flowable<LocalType> {
        return request.toFlowable()
    }

    protected open fun toLiveData(flowable: Flowable<LocalType>): LiveData<LocalType> {
        return flowable.toLiveData()
    }
}

/**
 * Callback for [NetworkResource]
 */
interface INetworkResourceCallback<LocalType, RemoteType, ResponseType> :
    INetworkRequestCallback<ResponseType> {

//    var networkLimiter: NetworkLimiter?
//    var networkId: Int

    /**
     * Called on success before mapping
     */
    fun onSuccessPreMap(it: RemoteType) {}

    /**
     * Called on success after mapping
     */
    fun onSuccessPostMap(it: LocalType) {}

    /**
     * Map the [RemoteType] to [LocalType]
     */
    @WorkerThread
    fun mapToLocal(response: RemoteType): LocalType

    /**
     * Extract and return the response data.
     * @return The response data
     */
    fun extractData(response: ResponseType): RemoteType

//    fun addNetworkLimiter(networkLimiter: NetworkLimiter, networkId: Long = 0) {
////        this.networkLimiter = networkLimiter
////        this.networkId = networkId
//    }
}
