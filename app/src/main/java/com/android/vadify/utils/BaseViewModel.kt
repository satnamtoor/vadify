package com.android.vadify.utils

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.vadify.ui.util.Event
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

abstract class BaseViewModel : ViewModel() {

    // Mutable/LiveData of String resource reference Event
    private val _message = MutableLiveData<Event<Int>>()

    val eventMessage : LiveData<Event<Int>>
        get() = _message

    // Post in background thread
    fun postMessage(@StringRes message: Int) {
        _message.postValue(Event(message))
    }

    // Post in main thread
    fun setMessage(@StringRes message: Int) {
        _message.value = Event(message)
    }

    /**
     * Container for RxJava subscriptions.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val subscriptions = CompositeDisposable()

    /**
     * Subscribes to a [Maybe], and optionally disposes the request when the ViewModel is being destroyed
     */
    protected fun <T> subscribe(
        stream: Maybe<T>,
        unsubscribeOnClear: Boolean = false,
        onSuccess: ((T) -> Unit)? = null
    ) {
        val subscription = stream.subscribe(onSuccess ?: {}) { }
        if (unsubscribeOnClear) subscriptions.add(subscription)
    }

    protected fun <T> subscribe(
        stream: Single<T>,
        unsubscribeOnClear: Boolean = false,
        onSuccess: ((T) -> Unit)? = null
    ) {
        val subscription = stream.subscribe(onSuccess ?: {}) { }
        if (unsubscribeOnClear) subscriptions.add(subscription)
    }


    override fun onCleared() {
        super.onCleared()
        subscriptions.dispose()
    }


}