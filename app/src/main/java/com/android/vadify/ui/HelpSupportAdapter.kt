package com.android.vadify.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.android.vadify.R
import com.android.vadify.data.api.models.YoutubeResponse
import com.android.vadify.ui.util.imageUrl
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.startActivity




class HelpSupportAdapter (private var dataList: List<YoutubeResponse.Item>, private val context: Context) : RecyclerView.Adapter<HelpSupportAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_home, parent, false))
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataModel=dataList.get(position)

        holder.textViewName.text=dataModel.snippet.title
        holder.textViewVersion.text=dataModel.snippet.description
        holder.imageView.imageUrl(dataModel.snippet.thumbnails.default.url,R.drawable.user_placeholder)

        holder.card_view.setOnClickListener( View.OnClickListener {

            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v="+dataModel.id.videoId)
                )
            )

        })

    }


    class ViewHolder(itemLayoutView: View) : RecyclerView.ViewHolder(itemLayoutView) {
        lateinit var textViewName: TextView
        lateinit var textViewVersion: TextView
        lateinit var imageView: ImageView
        lateinit var card_view: CardView


        init {
            textViewName=itemLayoutView.findViewById(R.id.textViewName)
            textViewVersion=itemLayoutView.findViewById(R.id.textViewVersion)
            imageView=itemLayoutView.findViewById(R.id.imageView)
            card_view=itemLayoutView.findViewById(R.id.card_view)

        }

    }

}