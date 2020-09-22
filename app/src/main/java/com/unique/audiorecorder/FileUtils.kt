package com.unique.audiorecorder

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileUtils {
     fun generateFilename(context:Context): String {
        val outputDirectory=getOutputDirectory(context)
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.US
            ).format(System.currentTimeMillis()) + ".mp3")
        return photoFile.absolutePath
    }
    fun getOutputDirectory(context:Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    fun getAudioFile(path:String):ArrayList<HashMap<String,String>>{
        val audioList:ArrayList<HashMap<String,String>> = ArrayList()
        Log.d("Files", "Path: $path")
        val directory = File(path)
        val files = directory.listFiles()
        Log.d("Files", "Size: " + files?.size)
        if(files!=null) {
            for (i in files.indices) {
                Log.d("Files", "FileName:" + files[i].name)
                Log.d("Files", "FileExtension:" + files[i].extension)
                Log.d("Files", "FileDate:" + files[i].lastModified())

                if (files[i].extension.equals("mp3")) {
                    val audioHashMap:HashMap<String,String> = HashMap()
                    val title: String = files[i].name
                    val fileName: String = files[i].path
                    val date: String =getFormattedDateFromTimestamp(files[i].lastModified())?:""
                    audioHashMap["title"] = title
                    audioHashMap.put("fileName",fileName)
                    audioHashMap["date"] = date
                    audioList.add(audioHashMap)
                }
            }
        }
        return audioList
    }
    fun getFormattedDateFromTimestamp(timestampInMilliSeconds: Long): String? {
        val date = Date()
        date.setTime(timestampInMilliSeconds)
        return SimpleDateFormat("dd/MM/yyyy").format(date)
    }
}