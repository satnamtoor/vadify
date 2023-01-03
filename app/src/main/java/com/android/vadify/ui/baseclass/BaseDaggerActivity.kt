package com.android.vadify.ui.baseclass

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.AbsListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.vadify.R
import com.android.vadify.di.ViewModelFactory
import com.android.vadify.ui.util.PagedListViewModel
import com.android.vadify.ui.util.ResourceViewModel
import com.google.android.material.snackbar.Snackbar
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundPagedListAdapter
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


abstract class BaseDaggerActivity : DaggerAppCompatActivity() {


    var onTouchScroll = false

    /**
     * [ViewModelFactory] which uses Dagger2 for dependency injection
     */
    @Inject
    lateinit var viewModelFactory: ViewModelFactory


    /*This is for viewPagerAdapter Adapter*/

    fun <X : DataBoundAdapterClass<T, *>, T> X.setup(
        viewPager: ViewPager2,
        data: List<T>
    ) {
        viewPager.adapter = this
        submitList(data)
    }


    protected fun <X : DataBoundPagedListAdapter<T, *>, T> chatAdapter(
        adapter: X,
        recyclerView: RecyclerView,
        viewModel: PagedListViewModel<T>,
        hasFixedSize: Boolean = true,
        isNewMessage: MutableLiveData<Boolean> = mutableLiveData(false),
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recyclerView.setHasFixedSize(hasFixedSize)
        recyclerView.adapter = adapter

        subscriptions += adapter.retryClicks.subscribe(viewModel::retry)
        clickHandler?.let { subscribe(adapter.clicks, it) }
        isNewMessage.observe(this, Observer {
            if (it) onTouchScroll = false
        })
        viewModel.items.observe(this, Observer {
            adapter.submitList(it)
            if (!onTouchScroll) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(200)
                    CoroutineScope(Dispatchers.Main).launch {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }
            }
        })
        subscribe(adapter.retryClicks, viewModel::retry)
        viewModel.frontLoadingState.observe(this, adapter::setNetworkState)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    onTouchScroll = true
                }
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && !recyclerView.canScrollVertically(
                        1
                    )
                ) {
                    onTouchScroll = false
                }
            }
        })
        return adapter
    }


    protected fun <X : DataBoundAdapterClass<T, *>, T> initAdapter(
        adapter: X,
        recycler: RecyclerView,
        list: LiveData<List<T>?>,
        clickHandler: ((T) -> Unit)? = null
    ): X {
        recycler.adapter = adapter
        list.observe(this, Observer {
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
        list.observe(this, adapter::submitList)
        adapter.retryClicks.subscribe(viewModel::retry)
        viewModel.networkState.observe(this, adapter::setNetworkState)
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


    /**
     * Wrapper for [ComponentActivity.viewModels]
     * Uses the dagger viewModelFactory by default to avoid having to specify it each time.
     */
    @MainThread
    inline fun <reified VM : ViewModel> viewModels() = viewModels<VM>
    { viewModelFactory }


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
     * Container for RxJava subscriptions.
     */
    private val subscriptions = CompositeDisposable()


    fun showMessage(message: String?) {
        message?.let {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }


    fun locationSnackMessage() {
        Snackbar.make(
            findViewById(android.R.id.content),
            R.string.GPS_Validation,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.setting) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }.show()
    }


    fun showSnackMessage(message: String?) {
        message?.let {
            val snackBar =
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT)
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
        networkState.observe(this, Observer {
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

    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        return if (cursor != null) {
            val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } else null
    }

    private fun requireBoolean(tag: String): Boolean {
        return intent.getBooleanExtra(tag, false)
    }


    private fun requireString(tag: String): String {
        var field = ""
        intent.getStringExtra(tag)?.let {
            field = it
        }
        return field
    }


}
