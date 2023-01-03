package com.android.vadify.ui.dashboard.fragment.call.adapter

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.R
import com.android.vadify.data.db.callLogs.CallLogs
import com.android.vadify.databinding.ItemCallLogsBinding
import com.android.vadify.ui.util.getAlphabetCharacter
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.gson.Gson



class CallLogsAdapter (private var dataList: List<CallLogs>) : RecyclerView.Adapter<CallLogsAdapter.ViewHolder>() {
    lateinit var defaultSharedPreferences: SharedPreferences

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCallLogsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(parent.context)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
           // Log.d( "onCallLogs: ", Gson().toJson(dataList))
            holder.binding.item = dataList[position]

            val myMessage = dataList[position].members.filter { it.userId == dataList[position].from }
            if(dataList[position].type=="Single")
            {
                holder.binding.textView26.visibility=  View.GONE
                holder.binding.textView23.text= dataList[position].fromName
                if(myMessage[0].profileImage.isEmpty())
                {
                   // android:text="@{DataConverterKt.getAlphabetCharacter(item.roomName)}"
                    holder.binding.nameLabel.text = getAlphabetCharacter(dataList[position].fromName)
                   holder.binding.circularImageView.visibility = View.GONE
                }
                else{
                    holder.binding.circularImageView.visibility = View.VISIBLE
                    holder.binding.circularImageView.imageUrl(myMessage[0].profileImage,R.drawable.user_placeholder)

                }
            }
            else{
                holder.binding.textView23.text= myMessage[0].name
                holder.binding.textView26.visibility=  View.VISIBLE
                holder.binding.textView26.text= dataList[position].roomName

                if(myMessage[0].profileImage.isEmpty())
                {
                    // android:text="@{DataConverterKt.getAlphabetCharacter(item.roomName)}"
                    holder.binding.nameLabel.text = getAlphabetCharacter(myMessage[0].name)
                    holder.binding.circularImageView.visibility = View.GONE
                }
                else{
                    holder.binding.circularImageView.visibility = View.VISIBLE
                    holder.binding.circularImageView.imageUrl(myMessage[0].profileImage,R.drawable.user_placeholder)

                }
            }
        }


    class ViewHolder(val binding: ItemCallLogsBinding) :
        RecyclerView.ViewHolder(binding.root)

    }


