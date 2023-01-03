@file:Suppress("unused", "UNCHECKED_CAST")

package com.android.vadify.ui.util

import android.graphics.Paint
import android.os.Build
import android.text.Html
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.R
import com.bumptech.glide.Glide
import java.io.File

const val SIZE_MULTIPLIER = 0.1f
const val NO_EVENT_BULLET_COLOR = -1
const val IMAGE_REQ_WIDTH = 80
const val IMAGE_REQ_HEIGHT = 80
const val NO_EVENT_STATUS = "-1"
const val MAX_ATTACHMENT_COLUMNS = 1
const val SUBLIST_START_OFFSET = 0
const val ITEM_SPACING = 0


class BindingAdapters


@BindingAdapter("app:setText")
fun TextView.setTextResource(resource: Int) {
    this.text = resources.getString(resource)
}


@BindingAdapter("app:url", "app:drawable")
fun ImageView.imageUrl(url: String?, drawable: Int) {
    if (!url.isNullOrEmpty() && url.contains("http")) {
        Glide.with(this.context).load(url).placeholder(R.drawable.ic_videoplay_icon).into(this)
    } else if (!url.isNullOrEmpty()) {
        Glide.with(this.context).load(File(url)).into(this)
    }
}


@BindingAdapter("app:url")
fun ImageView.imageUrlWithoutPlaceHolder(url: String?) {
    if (!url.isNullOrEmpty()) {
        if (url.contains("http")) {
            Glide.with(this.context).load(url).placeholder(drawable).into(this)
        } else if (url.contains("jpeg") || url.contains("jpg") || url.contains("png") || url.contains(
                "mp4"
            )
        ) {
            Glide.with(this.context).load(File(url)).into(this)
        }
    }
}


@BindingAdapter("app:listData")
fun <T> RecyclerView.listData(list: List<T>?) {
    (adapter as? ListAdapter<T, *>)?.submitList(list)
}


@BindingAdapter("app:strike")
fun TextView.strike(text: String) {
    this.text = text
    this.paintFlags = this.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
}

@BindingAdapter("app:src")
fun ImageView.setImageDrawable(resource: Int?) {
    resource?.let { this.setImageResource(it) }
}

@BindingAdapter("android:loadData")
fun WebView.setText(checkString: String?) {
    if (checkString != null && !checkString.equals("", ignoreCase = true)) {
        this.loadData(checkString, "text/html; charset=utf-8", "utf-8")
    }
}

@BindingAdapter("android:loadhtmlData")
fun AppCompatTextView.htmlText(checkString: String?) {
    checkString?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.text = Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
        } else {
            this.text = Html.fromHtml(it)
        }
    }
}






