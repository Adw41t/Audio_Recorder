package com.unique.audiorecorder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File


class RecordingListAdapter(val list:ArrayList<HashMap<String,String>>, val context:Context): RecyclerView.Adapter<RecordingListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){

        var title=itemView.findViewById<TextView>(R.id.recording_title)
        var date=itemView.findViewById<TextView>(R.id.recording_date)
        var more_options=itemView.findViewById<ImageButton>(R.id.more_options)
        fun bindView(recordingHashMap: HashMap<String,String>){
            title.text=recordingHashMap.get("title")
           // date.text=recordingHashMap.get("date")
            val file=File(recordingHashMap.get("fileName")!!)
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
            more_options.setOnClickListener { v->
                val popupMenu=PopupMenu(context,v)
                popupMenu.inflate(R.menu.overflow)
                popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.overflow_delete -> {                        //handle menu1 click
                            delete(file.path,adapterPosition)
                            true
                        }
                        R.id.overflow_share -> {
                            share(uri)//handle menu2 click
                            true
                        }
                        else -> false
                    }
                })
                popupMenu.show()
            }

            itemView.setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri,"audio/mp3")
                context.startActivity(intent)
            }
        }

        private fun delete(filename:String?,position:Int) {
            if(filename!=null){
                val file= File(filename)
                if(file.delete()){
                    list.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeRemoved(position,list.size)
                    Toast.makeText(context,"Deleted",Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun share(uri: Uri) {
            if(uri!=null){
                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.type="audio/mp3"
                intent.putExtra(Intent.EXTRA_STREAM,uri)
                //intent.setDataAndType(Uri.parse(filename),"audio/mp3")
                context.startActivity(Intent.createChooser(intent, "Share recording File"))
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=LayoutInflater.from(context).inflate(R.layout.recording_item,parent,false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(list.get(position))
    }
}