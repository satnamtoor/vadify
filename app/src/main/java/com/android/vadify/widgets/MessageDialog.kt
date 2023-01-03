package com.android.vadify.widgets

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.android.vadify.R


const val FIRST_STATE = 0
const val SECOND_STATE = 1


fun Context.messageDialog(message: String, callBack: (() -> Unit)? = null) {
    val alertDialogBuilder = AlertDialog.Builder(this).also {
        it.setMessage(message)
        it.setTitle(R.string.app_name)
        it.setCancelable(true)
    }
    alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
        dialog.dismiss()
        callBack?.invoke()
    }
    val alertDialog = alertDialogBuilder.create()
    alertDialog.show()
}


fun Context.messagePicker(options: Array<CharSequence>, response: (Int) -> Unit) {
    val builder = android.app.AlertDialog.Builder(this)
    builder.setTitle(R.string.choice)
    builder.setItems(options) { dialog, item ->
        response.invoke(item)
    }
    builder.show()
}














