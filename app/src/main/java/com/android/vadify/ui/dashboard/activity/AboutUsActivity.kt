package com.android.vadify.ui.dashboard.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.vadify.R
import com.android.vadify.databinding.ActivityAboutUsBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import kotlinx.android.synthetic.main.activity_about_us.*
import kotlinx.android.synthetic.main.activity_blocked.toolbar

class AboutUsActivity : DataBindingActivity<ActivityAboutUsBinding>() {

    // private val viewModel: BlockedContentViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.activity_about_us

    companion object {
        const val URL = "https://www.vadify.com/about"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        about_us.settings.also { it.javaScriptEnabled = true }
        about_us.loadUrl(URL)
        about_us.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                aboutUsProgress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                aboutUsProgress.visibility = View.GONE
            }
        }

    }
}