package com.android.vadify.ui.login.fragment.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.android.vadify.R
import com.android.vadify.data.api.models.LocalLanguageModel
import com.android.vadify.databinding.PopupLocalLangaugeBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.android.vadify.ui.login.fragment.PersonalInformationFragment
import com.android.vadify.ui.login.fragment.adapter.LocalLanguageAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.popup_local_langauge.*


class LocalLanguagePopUp(var selectedLanguage: (LocalLanguageModel.LocalLanguageModelItem) -> Unit) :
    BaseDialogDaggerListFragment<PopupLocalLangaugeBinding>() {
    override val layoutRes: Int
        get() = R.layout.popup_local_langauge


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAdapter(LocalLanguageAdapter(), languageList, loadJSONFromAsset()) {
            selectedLanguage.invoke(it)
            dialog?.dismiss()
        }
    }

    private fun loadJSONFromAsset(): ArrayList<LocalLanguageModel.LocalLanguageModelItem>? {
        var listOfLangauge: ArrayList<LocalLanguageModel.LocalLanguageModelItem>? = null
        try {
            val json = requireActivity().assets.open(PersonalInformationFragment.LOCAL_FILE)
                .bufferedReader().use { it.readText() }
            listOfLangauge = Gson().fromJson(
                json,
                object : TypeToken<List<LocalLanguageModel.LocalLanguageModelItem>?>() {}.type
            ) as ArrayList<LocalLanguageModel.LocalLanguageModelItem>?
        } catch (e: Exception) {
        }
        return listOfLangauge
    }
}
