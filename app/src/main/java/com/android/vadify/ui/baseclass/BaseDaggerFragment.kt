package com.android.vadify.ui.baseclass

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.android.vadify.di.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

abstract class BaseDaggerFragment<TBinding : ViewDataBinding> : BaseBackStack() {


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
            Log.e("message", "" + it.message)
            //  Timber.e(it)
        }
    }

    /**
     * Container for RxJava subscriptions.
     */
    val subscriptions = CompositeDisposable()


    override fun onDestroy() {
        super.onDestroy()

    }

    fun showMessage(message: String?) {
        message?.let {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }


    fun showSnackMessage(message: String?) {
        message?.let {
            val snackBar = Snackbar.make(requireView(), it, Snackbar.LENGTH_SHORT)
            snackBar.show()
        }
    }

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
                    showMessage(it.msg)
                    loadingIndicator?.visibility = View.GONE
                    dialog?.dismiss()
                    onError?.invoke()

                }
                NetworkState.Status.SUCCESS -> {
                    success?.let { showMessage(resources.getString(success)) }
                    loadingIndicator?.visibility = View.GONE
                    dialog?.dismiss()
                    onSuccess?.invoke()
                }
            }
        })
    }


}
