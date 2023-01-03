package com.android.vadify.ui.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.common.livedataext.switchMap
import com.sdi.joyersmajorplatform.uiview.NetworkState


/**
 * A wrapper for IListResource meant to be used in a ViewModel.
 * It provides LiveData for items, refresh status and network status.
 * It also provides functions to retry any failed requests and to refresh the resource.
 */
class PagedListViewModel<EntityType>(private val resource: LiveData<IListResource<EntityType>>) {

    constructor(list: IListResource<EntityType>) : this(MutableLiveData<IListResource<EntityType>>()) {
        (resource as? MutableLiveData)?.value = list
    }

    companion object {

        /**
         * Initialize with a [LiveData] trigger and a lambda that returns an [IResource].
         */
        operator fun <TriggerType, EntityType> invoke(
            src: LiveData<TriggerType>,
            f: (TriggerType) -> IListResource<EntityType>
        ): PagedListViewModel<EntityType> {
            return PagedListViewModel(src.map(f))
        }
    }

    /**
     * True if the resource is refreshing it's data.
     */

    val isRefreshing: LiveData<Boolean> = resource.switchMap {
        it.isRefreshing
    }

    /**
     * The network state of the resource.
     */
    val networkState: LiveData<NetworkState> = resource.switchMap {
        it.networkState
    }

    /**
     * The network state of the front loader
     */
    val frontLoadingState: LiveData<NetworkState> = resource.switchMap {
        it.networkStateBefore
    }

    /**
     * The network state of the
     */
    val endLoadingState: LiveData<NetworkState> = resource.switchMap {
        it.networkStateAfter
    }

    /**
     * The resource items.
     */
    val items: LiveData<PagedList<EntityType>> = resource.switchMap {
        it.data
    }


    fun retry(networkState: NetworkState) {
        resource.value?.retry(networkState)
    }

    /**
     * Refresh resource data.
     */
    fun refresh() {
        resource.value?.refresh()
    }
}
