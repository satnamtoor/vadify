package com.android.vadify.ui

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.vadify.R
import com.android.vadify.data.api.ApiClient
import com.android.vadify.data.api.ApiYouTubeClient
import com.android.vadify.data.api.models.SupportResponse
import com.android.vadify.data.api.models.YoutubeResponse
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.databinding.ActivityChatAccountBinding
import com.android.vadify.databinding.HelpScreenBinding
import com.android.vadify.databinding.HomeBinding
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.viewmodel.VadifyFriendViewModel
import com.android.vadify.ui.util.getAlphabetCharacter
import com.android.vadify.ui.util.imageUrl
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_blocked.*
import kotlinx.android.synthetic.main.activity_blocked.toolbar
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.help_screen.*
import kotlinx.android.synthetic.main.home.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HelpSupport : DataBindingActivity<HelpScreenBinding>(){

    override val layoutRes: Int
        get() = R.layout.help_screen
    lateinit var adapter:HelpSupportAdapter

    val viewModelChat: ChatViewModel by viewModels()
    var dataList = ArrayList<YoutubeResponse.Item>()
    var friendList = mutableListOf<ChatThread>()
    val viewModel: VadifyFriendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        bindNetworkState(
            viewModelChat.getSupport()
        )

        viewModel.getChatThreads().observe(this, Observer {
            friendList = it as MutableList<ChatThread>

        })

        userList.adapter= HelpSupportAdapter(dataList,this)
        userList.layoutManager= LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)

        getData()
        toolbar.setNavigationOnClickListener {
            finish()
        }


        viewModelChat.getsupportData.observe(this, Observer {
         //   Log.d( "support-response: ", Gson().toJson(it))
          if (it!=null)
          {
            textViewName.text = it.name.toString()
            textViewVersion.text = it.number.toString()
            if (it.profileImage.isNullOrBlank())
            {
                nameLabelHelp.setText(getAlphabetCharacter(it.name))
                imageView.visibility = View.GONE
            }
            else{

                imageView.visibility = View.VISIBLE
                imageView.imageUrl(it.profileImage,R.drawable.user_placeholder)
            }


            callChatList(it,false)
          }
        })

        subscribe(card_view.throttleClicks()) {
            Log.d( "click-click ","click")
            viewModelChat.getsupportData.observe(this, Observer {
                Log.d( "support-response: ", Gson().toJson(it))

                textViewName.text = it.name.toString()
                textViewVersion.text = it.number.toString()
                imageView.imageUrl(it.profileImage,R.drawable.user_placeholder)

                callChatList(it,true)

            })
        }
    }

    private fun callChatList(data: SupportResponse.Data?,Isclick: Boolean) {
        if (Isclick)
        {
            val friendData = friendList?.filter {
                it.members[0].userId == data!!._id && it.members[0].number == data!!.number
            }

        val intent = Intent(this, ChatActivity::class.java).also {

            if (friendData?.size!! > 0 && friendData[0].type == "Single") {
                it.putExtra(BaseBackStack.ANOTHER_USER_ID, data!!._id)
                it.putExtra(BaseBackStack.FIRST_TIME_START_CHAT, false)
                it.putExtra(BaseBackStack.ROOM_ID, friendData[0].id)
            }
            it.putExtra(BaseBackStack.ANOTHER_USER_NAME, data!!.name)
            it.putExtra(BaseBackStack.ANOTHER_USER_URL, data!!.profileImage)

            it.putExtra(BaseBackStack.LANGUAGE_SWITCH, true)
           // it.putExtra(BaseBackStack.MOTHER_LANGUAGE, data.language)
            it.putExtra(BaseBackStack.PHONE_NUMBER, data!!.number)
           // it.putExtra(BaseBackStack.ANOTHER_USER_LANGUAGE_CODE, data.languageCode)
            it.putExtra(BaseBackStack.GOTO_SPEECH_TO_TEXT, false)


        }
        startActivity(intent)
    }
    }

   private fun getData() {
        val call: Call<YoutubeResponse> = ApiYouTubeClient.getClient.getPhotos()
        call.enqueue(object : Callback<YoutubeResponse> {

            override fun onResponse(call: Call<YoutubeResponse>?, response: Response<YoutubeResponse>?) {
               // progerssProgressDialog.dismiss()
             //   dataList.addAll(response!!.body()!!)
               // recyclerView.adapter.notifyDataSetChanged()

                Log.d( "youtubeonResponse: " ,Gson().toJson(response!!.body()))
                if (response.body()!=null)
                {
                response!!.body()?.items?.let { dataList.addAll(it) }
               userList.adapter?.notifyDataSetChanged()
                }
                }

            override fun onFailure(call: Call<YoutubeResponse>?, t: Throwable?) {
             //  progerssProgressDialog.dismiss()
                Log.d( "youtubeonResponse: " ,"fail..")
            }


    })

}
}