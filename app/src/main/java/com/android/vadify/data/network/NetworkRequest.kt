package com.android.vadify.data.network

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.postUpdate
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

/**
 * Class that performs a single network request
 */
open class NetworkRequest<ResponseType>
@MainThread constructor(
    private val appExecutors: AppExecutors,
    private val cb: INetworkRequestCallback<ResponseType>
) : IRequest<ResponseType> {


    //TODO  not implemented yet
    override fun retry(networkState: NetworkState) {

    }


//    private val responseLiveData : MediatorLiveData<ResponseType> = MediatorLiveData()
//
//    override val responseData: MutableLiveData<ResponseType>
//        get() =  responseLiveData


    private val _networkState: MediatorLiveData<NetworkState> = MediatorLiveData()

    override val networkState: LiveData<NetworkState> get() = _networkState

    override val request = prepareNetworkRequest()

    private fun prepareNetworkRequest(): Maybe<ResponseType> {
        return createNetworkRequest()
    }

    protected open fun createNetworkRequest(): Maybe<ResponseType> {
        val request = cb.createNetworkRequest()
        return request.doOnError {
            //Log.d("error-reposen" ,"test"+Gson().toJson(cb.getExceptionState(it)))
            _networkState.postUpdate(cb.getExceptionState(it))
            // Timber.e(it)
        }.doOnSubscribe {
            _networkState.postUpdate(NetworkState.loading)
        }
            .subscribeOn(Schedulers.from(appExecutors.networkIO()))
            .observeOn(Schedulers.from(appExecutors.diskIO()))
            .filter(this::filterAndUpdateNetworkState)
            .doOnSuccess(cb::onSuccess)
            .onErrorComplete()
    }

    private fun filterAndUpdateNetworkState(response: ResponseType): Boolean {
        val filter = cb.isSuccess(response)
        if (filter) {
            //responseLiveData.postUpdate(response)
            _networkState.postUpdate(NetworkState.success)
        } else {
            _networkState.postUpdate(cb.getErrorState(response))
        }
        return filter
    }
}

/**
 * Callback for [NetworkRequest]
 */
interface INetworkRequestCallback<ResponseType> {
    /**
     * Called if [isSuccess] returns false.
     * @return A [NetworkState] representing the error.
     */
    fun getErrorState(response: ResponseType): NetworkState

    /**
     * Create and return the network request.
     */
    @MainThread
    fun createNetworkRequest(): Single<ResponseType>

    /**
     * Filter http errors.
     * @return True if successful, false otherwise.
     */
    fun isSuccess(response: ResponseType): Boolean

    /**
     * Called when the fetch operation throws an exception.
     * @return A [NetworkState] representing the exception.
     */
    fun getExceptionState(it: Throwable): NetworkState {
        return NetworkState.error(it.localizedMessage)
    }


    /**
     * Called on success
     */
    fun onSuccess(response: ResponseType) {}
}

