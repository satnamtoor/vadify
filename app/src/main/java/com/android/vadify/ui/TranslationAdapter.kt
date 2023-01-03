package com.android.vadify.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.R
import com.android.vadify.data.api.models.TranslateModel

class TranslationAdapter (private var dataList: ArrayList<TranslateModel>, private val context: Context) : RecyclerView.Adapter<TranslationAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.translate_adapter, parent, false))
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataModel=dataList.get(position)

        holder.textFrom.text=dataModel.key
        holder.textTo.text=dataModel.value



    }


    class ViewHolder(itemLayoutView: View) : RecyclerView.ViewHolder(itemLayoutView) {
        lateinit var textFrom: TextView
        lateinit var textTo: TextView



        init {
            textFrom=itemLayoutView.findViewById(R.id.textFrom)
            textTo=itemLayoutView.findViewById(R.id.textTo)

        }

    }

}