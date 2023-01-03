package com.android.vadify.ui.baseclass

import android.annotation.SuppressLint
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.vadify.ui.util.PagedListViewModel
import com.android.vadify.ui.util.ResourceViewModel
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundPagedListAdapter
import io.reactivex.rxkotlin.plusAssign

/**
 * [BaseDaggerFragment] that adds helper methods for displaying lists.
 */
abstract class BaseDaggerListFragment<TBinding : ViewDataBinding> : BaseDaggerFragment<TBinding>() {


    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: LiveData<List<T>?>,
        hasFixedSize: Boolean = true,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recycler.setHasFixedSize(hasFixedSize)
        recycler.adapter = adapter
        list.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        clickHandler?.let { subscribe(adapter.clicks, it) }
        return adapter
    }


    // TODO Need to refactor retryClick  funtionality
    @SuppressLint("CheckResult")
    protected fun <X : DataBoundAdapterClass<T, *>, T, R> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        viewModel: ResourceViewModel<R>,
        list: LiveData<List<T>?>,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recycler.adapter = adapter
        list.observe(viewLifecycleOwner, adapter::submitList)
        adapter.retryClicks.subscribe(viewModel::retry)
        viewModel.networkState.observe(viewLifecycleOwner, adapter::setNetworkState)
        clickHandler?.let { subscribe(adapter.clicks, it) }
        return adapter
    }

    protected fun <X : DataBoundPagedListAdapter<T, *>, T> initAdapter(
        adapter: X,
        recyclerView: RecyclerView,
        viewModel: PagedListViewModel<T>,
        hasFixedSize: Boolean = true,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recyclerView.setHasFixedSize(hasFixedSize)
        recyclerView.adapter = adapter
        subscriptions += adapter.retryClicks.subscribe(viewModel::retry)
        clickHandler?.let { subscribe(adapter.clicks, it) }
        viewModel.items.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            adapter.submitList(it)
        })
        viewModel.frontLoadingState.observe(viewLifecycleOwner, adapter::setNetworkState)
        viewModel.endLoadingState.observe(viewLifecycleOwner, adapter::setNetworkState)

        return adapter
    }


    fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        viewPager: ViewPager2,
        data: LiveData<List<T>?>,
        clickHandler: ((T) -> Unit)? = null
    ) {
        viewPager.adapter = adapter
        data.observe(viewLifecycleOwner, adapter::submitList)
        clickHandler?.let { subscribe(adapter.clicks, it) }
    }
}