package com.unique.audiorecorder

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main2.*


class MainActivity2 : AppCompatActivity(){
    var filepath1:String? = null
    var recordingList:ArrayList<HashMap<String,String>> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        filepath1=FileUtils().getOutputDirectory(this).absolutePath+"/"
        Log.d("getAudioFiles","filepath1= ${filepath1.toString()}")
        if(recordingList.isEmpty()) recordingList=FileUtils().getAudioFile(filepath1!!)
        Log.d("getAudioFiles","recordingList= ${recordingList.toString()}")
        recordingListView.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(context)
            // set the custom adapter to the RecyclerView
            adapter = RecordingListAdapter(recordingList,context)
        }

    }
}