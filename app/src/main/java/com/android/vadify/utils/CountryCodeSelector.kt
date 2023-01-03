package com.android.vadify.utils

import android.content.Context
import com.hbb20.CountryCodePicker
import kotlinx.android.synthetic.main.fragment_another_user_contact.*

class CountryCodeSelector(var context: Context) {

    fun removeCountryCode(number: String): String {

        if (!number.isNullOrBlank())
        {
            var countryCode = CountryCodePicker(context)
            countryCode.fullNumber = number
            return number.drop(countryCode.selectedCountryCodeWithPlus.length)
        }
        else{
            return ""
        }

    }
}