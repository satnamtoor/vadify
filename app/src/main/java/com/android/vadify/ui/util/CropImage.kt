package com.android.vadify.ui.util

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.canhub.cropper.CropImage

fun tryHandleCropImageResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    onSuccess: (CropImage.ActivityResult) -> Unit
): Boolean {
    if (requestCode != CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) return false
    val result = CropImage.getActivityResult(data)
    if (resultCode == Activity.RESULT_OK) {
        onSuccess(result!!)
    } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//        Timber.e(result.error)
        Log.e("error", "" + result?.error)
    }
    return true
}
