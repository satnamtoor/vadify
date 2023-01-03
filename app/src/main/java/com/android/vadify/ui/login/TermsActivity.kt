/*
package com.android.vadify.ui.login

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.vadify.R
import com.android.vadify.databinding.ActivityTermPolicyBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.activity.TermActivity
import com.android.vadify.ui.dashboard.viewmodel.TermPolicyViewModel
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.activity_blocked.*
import kotlinx.android.synthetic.main.activity_term_policy.*

class TermsActivity : DataBindingActivity<ActivityTermPolicyBinding>() {

    companion object {
        const val URL = "https://www.vadify.com/privacy"
    }

    private val viewModel: TermPolicyViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.activity_term_policy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.updateTermLabel()
        // textView37.movementMethod = ScrollingMovementMethod()

        textView37.settings.also { it.javaScriptEnabled =true }
        textView37.loadUrl(URL)

        textView37.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar3.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar3.visibility = View.GONE
            }
        }
        toolbar.setNavigationOnClickListener {
            finish()
        }

        subscribe(terms.throttleClicks()) {
            startActivity(Intent(this, TermActivity::class.java))
        }
    }

    override fun onBindView(binding: ActivityTermPolicyBinding) {
        binding.vm = viewModel
    }
}*/
