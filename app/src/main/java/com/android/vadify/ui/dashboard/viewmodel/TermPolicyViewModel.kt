package com.android.vadify.ui.dashboard.viewmodel

import androidx.lifecycle.MutableLiveData
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import javax.inject.Inject

class TermPolicyViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService
) :
    BaseViewModel() {

    var privacyPolicy = MutableLiveData<Boolean>()
    var termPolicy = MutableLiveData<Boolean>()


    fun updateTermLabel() {
        termPolicy.value = true
    }


    fun updatePolicyLabel() {
        privacyPolicy.value = true
    }


}