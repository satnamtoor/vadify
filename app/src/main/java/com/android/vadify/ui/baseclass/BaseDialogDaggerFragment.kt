package com.android.vadify.ui.baseclass

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.vadify.di.ViewModelFactory
import com.android.vadify.ui.util.ResourceViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sdi.joyersmajorplatform.uiview.NetworkState
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

abstract class BaseDialogDaggerFragment<TBinding : ViewDataBinding> : BottomSheetDialogFragment(),
    HasSupportFragmentInjector {


    @Inject
    lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment>? {
        return childFragmentInjector
    }

//    /**
//     * The layout resource ID for the fragment. This is inflated automatically.
//     */
//    abstract var popUpDialog: Dialog?

    lateinit var activityContext: Context

    /**
     * The layout resource ID for the fragment. This is inflated automatically.
     */
    abstract val layoutRes: Int
        @LayoutRes get

    protected open lateinit var binding: TBinding

    @Inject
    lateinit var viewModelFactory: ViewModelFactory


    /**
     * Called during onCreate, immediately after the view has been inflated.
     * Override this to bind values to the view.
     * [ViewDataBinding.executePendingBindings] will be executed after this method.
     */
    protected open fun onBindView(binding: TBinding) {}


    /**
     * Wrapper for [Fragment.createViewModelLazy]
     */
    @MainThread
    inline fun <reified VM : ViewModel> viewModels(
        noinline ownerProducer: () -> ViewModelStoreOwner = { this }
    ) = createViewModelLazy(VM::class, { ownerProducer().viewModelStore }, { viewModelFactory })


    @MainThread
    inline fun <reified VM : ViewModel> activityViewModels() =
        createViewModelLazy(VM::class, { requireActivity().viewModelStore }, { viewModelFactory })


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
     * Container for RxJava subscriptions.
     */
    private val subscriptions = CompositeDisposable()


    override fun onDestroy() {
        super.onDestroy()
        // subscriptions.dispose()
    }

    private fun showMessage(message: String?) {
        message?.let {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun bindNetworkState(
        networkState: LiveData<NetworkState>,
        @StringRes success: Int? = null,
        @StringRes error: Int? = null,
        loadingIndicator: View? = null,
        onError: (() -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        networkState.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                NetworkState.Status.RUNNING -> {
                    dialog?.show()
                    loadingIndicator?.visibility = View.VISIBLE
                }
                NetworkState.Status.FAILED -> {
                    dialog?.dismiss()
                    loadingIndicator?.visibility = View.GONE
                    showMessage(it.msg)
                }
                NetworkState.Status.SUCCESS -> {
                    dialog?.dismiss()
                    loadingIndicator?.visibility = View.GONE
                    success?.let {
                        showMessage(resources.getString(it))
                    }
                    onSuccess?.invoke()
                }
            }
        })
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


    @SuppressLint("CheckResult")
    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: LiveData<List<T>?>,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recycler.adapter = adapter
        list.observe(viewLifecycleOwner) {
            adapter.submitList(it?.toList())
        }
        clickHandler?.let { subscribe(adapter.clicks, it) }
        return adapter
    }


    fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        viewPager: ViewPager2,
        data: LiveData<List<T>?>,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        viewPager.adapter = adapter
        data.observe(this, adapter::submitList)
        clickHandler?.let { subscribe(adapter.clicks, it) }
        return adapter
    }
}