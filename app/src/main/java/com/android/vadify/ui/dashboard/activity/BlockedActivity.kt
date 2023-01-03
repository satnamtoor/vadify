package com.android.vadify.ui.dashboard.activity

import android.os.Bundle
import android.util.Log
import com.android.vadify.R
import com.android.vadify.databinding.ActivityBlockedBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.adapter.BlockedContentAdapter
import com.android.vadify.ui.dashboard.viewmodel.BlockedContentViewModel
import com.android.vadify.utils.CountryCodeSelector
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_blocked.*

class BlockedActivity : DataBindingActivity<ActivityBlockedBinding>() {

    private val viewModel: BlockedContentViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.activity_blocked

    lateinit var adapter: BlockedContentAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter =
            initAdapter(BlockedContentAdapter(), rvBlockedContact, viewModel.blockContentList) {}
        //bindNetworkState(viewModel.demoUpdateBlockDataUser())

        subscribe(adapter.itemClicks) {
            //Log.d("un-block-user",Gson().toJson(it))
            var result = CountryCodeSelector(this).removeCountryCode(it.number!!)
           // var numberData = it.
           // numberData.number = result

            //it.number =result
            bindNetworkState(viewModel.getBlockUnBlockUser(it,result)) {
                adapter.notifyDataSetChanged()
            }
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }


        //   bindNetworkState(viewModel.getBlockUnBlockUser())

    }

    override fun onBindView(binding: ActivityBlockedBinding) {
        binding.vm = viewModel
    }
}

