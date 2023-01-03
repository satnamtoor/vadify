package com.android.vadify.ui.dashboard.fragment.call

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.db.callLogs.CallLogs
import com.android.vadify.databinding.FragmentCallBinding
import com.android.vadify.ui.baseclass.BaseDaggerListFragment
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.fragment.call.adapter.CallLogsAdapter
import com.android.vadify.ui.dashboard.viewmodel.CallLogViewModel
import com.android.vadify.widgets.onTextChange
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.widget.EmptyRecyclerView
import kotlinx.android.synthetic.main.fragment_call.*
import kotlinx.android.synthetic.main.fragment_call.searchText

class CallFragment : BaseDaggerListFragment<FragmentCallBinding>() {

    val viewModel: CallLogViewModel by viewModels()


    companion object {
        const val PAGE_COUNT = 1
        const val TOTAL_ITEMS = "30"
        const val ALL_KEY = "all"
        const val MISSED_KEY = "missed"
    }

    override val layoutRes: Int
        get() = R.layout.fragment_call


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.textView2)
        allBtn.isChecked = true
        val callType = requireArguments().getString("ACTIVITY_TYPE","")
        if (callType == "audio")
            title.text = "Audio Calls"
        else  if (callType == "video")
            title.text = "Video Calls"
        viewModel.getCallList.items.observe(requireActivity(), Observer {
           // Log.d( "onCallLogs: ", Gson().toJson(it))
            var callLogsList: List<CallLogs> = if (callType=="")
                it
            else
                it.filter { it1-> it1.mode == callType }
           // Log.d( "onCallLogs: ", Gson().toJson(callLogsList))
            view.findViewById<EmptyRecyclerView>(R.id.callList).adapter = CallLogsAdapter(callLogsList)
        })
        subscribe(addCall.throttleClicks()) {
            runWithPermissions(Permission.CAMERA, Permission.RECORD_AUDIO) {
                startActivity(Intent(requireContext(), DirectCallActivity::class.java))
            }
        }
        subscribe(missedBtn.throttleClicks()) {
            viewModel.updateResult(MISSED_KEY)
            viewModel.updateTextColor(false, true)
        }
        subscribe(allBtn.throttleClicks()) {
            viewModel.updateResult(ALL_KEY)
            viewModel.updateTextColor(true, false)
        }
        subscribe(searchText.onTextChange()) { _ ->
            if (!viewModel.firstTimeVisit) {
                viewModel.searchQuery(viewModel.filterKey.value ?: ALL_KEY)
            }
            viewModel.firstTimeVisit = false
        }
    }
    override fun onResume() {
        super.onResume()
        if (viewModel.filterKey.value.isNullOrBlank()) {
            viewModel.updateResult(ALL_KEY)
        } else {
            viewModel.filterKey.value?.let { viewModel.updateResult(it) }
        }
    }
    override fun onBindView(binding: FragmentCallBinding) {
        binding.vm = viewModel
    }

}