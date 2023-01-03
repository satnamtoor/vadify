package com.android.vadify.ui.baseclass

import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment

abstract class BaseBackStack : DaggerFragment() {


    companion object {
        const val BACK_STACK_DESCRIPTION = "BACK_STACK_DESCRIPTION"
        const val BACK_STACK_DOUBLE = "BACK_STACK_DOUBLE"
        const val BACK_STACK_INT = "BACK_STACK_INT"
        const val BACK_STACK_BOOLEAN = "BACK_STACK_BOOLEAN"
        const val ANOTHER_USER_ID = "ANOTHER_USER_ID"
        const val ANOTHER_USER_NAME = "ANOTHER_USER_NAME"
        const val GROUP_ID = "GROUP_ID"
        const val MOTHER_LANGUAGE = "MOTHER_LANGUAGE"
        const val LANGUAGE_SWITCH = "LANGUAGE_SWITCH"

        const val PHONE_NUMBER = "PHONE_NUMBER"

        const val TYPE = "TYPE"
        const val ANOTHER_USER_LANGUAGE_CODE = "ANOTHER_USER_LANGUAGE_CODE"
        const val GOTO_SPEECH_TO_TEXT = "goToSpeechToTextView"
        const val ANOTHER_USER_URL = "ANOTHER_USER_URL"
        const val IMAGE_PATH = "IMAGE_PATH"
        const val ROOM_ID = "ROOM_ID"
        const val GROUP_NAME = "GROUP_NAME"
        const val MESSAGE_ID = "MESSAGE_ID"
        const val FIRST_TIME_START_CHAT = "FIRST_TIME_START_CHAT"
        const val FORWARD_MESSAGE = "FORWARD_MESSAGE"
        const val IS_BLOCK = "IS_BLOCK"
        const val GROUP_TYPE = "GROUP_TYPE"
        const val GROUP_IMAGE = "GROUP_IMAGE"
    }

    fun backStackGetBooleanData(key: String): MutableLiveData<Boolean>? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData(key)
    }

    fun backStackPutBooleanData(key: String, value: Boolean) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
    }

    fun backStackGetData(key: String): MutableLiveData<String>? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(key)
    }

    fun backStackPutData(key: String, value: String) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
    }


    fun backStackPutDouble(key: String, value: Double) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
    }

    fun backStackGetDoubleData(key: String): MutableLiveData<Double>? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Double>(key)
    }

    fun backStackPutInt(key: String, value: Int) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, value)
    }

    fun backStackGetIntData(key: String): MutableLiveData<Int>? {
        return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>(key)
    }

}