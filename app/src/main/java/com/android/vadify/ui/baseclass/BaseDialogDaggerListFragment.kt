package com.android.vadify.ui.baseclass

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.di.ViewModelFactory
import com.sdi.joyersmajorplatform.uiview.NetworkState
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundPagedListAdapter
import dagger.android.support.DaggerDialogFragment
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

/**
 * [BaseDaggerFragment] that adds helper methods for displaying lists.
 */
abstract class BaseDialogDaggerListFragment<TBinding : ViewDataBinding> : DaggerDialogFragment() {


    lateinit var activityContext: Context


    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    /**
     * The layout resource ID for the fragment. This is inflated automatically.
     */
    abstract val layoutRes: Int
        @LayoutRes get

    protected open lateinit var binding: TBinding

    /**
     * Container for RxJava subscriptions.
     */
    private val subscriptions = CompositeDisposable()

    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: ArrayList<T>?,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recycler.adapter = adapter
        adapter.submitList(list)
        clickHandler?.let { subscribe(adapter.clicks, it) }
        return adapter
    }


    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: MutableLiveData<ArrayList<T>>
    ): X {
        recycler.adapter = adapter
        list.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        return adapter
    }


    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: LiveData<ArrayList<T>>
    ): X {
        recycler.adapter = adapter
        list.observe(viewLifecycleOwner, Observer {
            //  adapter.submitList(it)
        })
        return adapter
    }


    protected fun <X : DataBoundPagedListAdapter<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: PagedList<T>
    ): X {
        recycler.adapter = adapter
        adapter.submitList(list)
        return adapter
    }


    /**
     * Subscribes to a [Observable] and handles disposing.
     */
    fun <T> subscribe(stream: Observable<T>?, handler: (T) -> Unit) {
        if (stream == null) return
        subscriptions += stream.subscribe(handler) {
            //  Timber.e(it)
        }
    }


    /**
     * Creates the [ViewDataBinding] for this view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        activityContext = requireContext()
        onBindView(binding)
        binding.executePendingBindings()
        return binding.root
    }


    /**
     * Called during onCreate, immediately after the view has been inflated.
     * Override this to bind values to the view.
     * [ViewDataBinding.executePendingBindings] will be executed after this method.
     */
    protected open fun onBindView(binding: TBinding) {}

    @Suppress("UNUSED_PARAMETER")
    protected fun bindNetworkState(
        networkState: LiveData<NetworkState>,
        dialog: AlertDialog? = null,
        @StringRes success: Int? = null,
        @StringRes error: Int? = null,
        loadingIndicator: View? = null,
        onError: (() -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        networkState.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                NetworkState.Status.RUNNING -> {
                    loadingIndicator?.visibility = View.VISIBLE
                    dialog?.show()
                }
                NetworkState.Status.FAILED -> {
                    loadingIndicator?.visibility = View.GONE
                    dialog?.dismiss()
                    onError?.invoke()

                }
                NetworkState.Status.SUCCESS -> {
                    loadingIndicator?.visibility = View.GONE
                    dialog?.dismiss()
                    onSuccess?.invoke()
                }
            }
        })
    }

    fun showMessage(message: String?) {
        message?.let {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

}