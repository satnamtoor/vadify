package com.android.vadify.ui.dashboard.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.android.vadify.R
import kotlinx.android.synthetic.main.activity_blocked.toolbar
import kotlinx.android.synthetic.main.activity_faq.*

class FaqActivity : AppCompatActivity() {


    companion object {
        const val URL = "https://www.vadify.com/#faqs"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        webview.settings.also { it.javaScriptEnabled = true }
        webview.loadUrl(URL)

        webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar2.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar2.visibility = View.GONE
            }
        }


    }


}

