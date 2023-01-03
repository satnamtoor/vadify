package com.android.vadify.ui.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.widget.doOnTextChanged
import com.android.vadify.R
import com.android.vadify.databinding.ActivityContactBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.contact.adapter.ContactAdapter
import com.android.vadify.ui.contact.viewmodel.ContactViewModel
import com.android.vadify.widgets.sticklist.SideBar
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.activity_contact.*


class ContactActivity : DataBindingActivity<ActivityContactBinding>(),
    SideBar.OnTouchingLetterChangedListener {

    override val layoutRes: Int
        get() = R.layout.activity_contact

    private var contactAdapter: ContactAdapter? = null


    val viewModel: ContactViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sideBar.setOnTouchingLetterChangedListener(this)
        sideBar.setTextView(dialog)

        subscribe(cancelBtn.throttleClicks()) {
            finish()
        }

        searchText.doOnTextChanged { text, _, _, _ ->
            val datalist = viewModel.filterData(text.toString())
            contactAdapter?.updateList(datalist)
        }


        viewModel.listOfContact.observe(this, androidx.lifecycle.Observer {
            contactAdapter = ContactAdapter(it)
            contactList.adapter = contactAdapter
        })


        subscribe(doneBtn.throttleClicks()) {
            val listOfNumber =
                contactAdapter?.contactList?.filter { it.selection }?.map { it.phone }
                    ?.joinToString(",") { it }
            if (!listOfNumber.isNullOrEmpty()) {
                shareApplicationInfoByMessage(listOfNumber)
            }
        }
    }


    override fun onTouchingLetterChanged(content: String?) {
        content?.let {
            val position: Int? = contactAdapter?.getPositionForSection(it[0].toInt())
            position?.let {
                if (it != -1) {
                    contactList.setSelection(position)
                }
            }
        }
    }


    private fun shareApplicationInfoByMessage(listOfNumber: String) {
        val uri = Uri.parse("smsto:$listOfNumber")
        val it = Intent(Intent.ACTION_SENDTO, uri)
        it.putExtra(
            "sms_body",
            resources.getString(R.string.share_message)+"\n\n"
                    + "  " + resources.getString(R.string.android_app)+"\n\n"
                    + "  " + resources.getString(R.string.ios_app)

        )
        startActivity(it)
        finish()
    }


}