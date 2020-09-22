package com.unique.audiorecorder

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_PERMISSION = 200
class MainActivity : AppCompatActivity() {
    val REPEAT_INTERVAL = 40
    private var isRecording = false
    private val handler: Handler = Handler()
    private var fileName: String = ""
    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE)
    var mStartRecording = true
    private var mPausePlay = true
    private var timer_count:Long=0
    var serviceIntent: Intent? = null
    var recordingService: RecordingService? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        ///startRecording()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        }
        else {
            startService(serviceIntent) //Starting the service
        }
    } else {
        ///stopRecording()
        recordingService?.stopRecording()
    }
    private fun onPausePlay(start: Boolean) = if (start) {
        pauseRecording()
    } else {
        resumeRecording()
    }

    fun pauseRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //recorder?.pause()
            recordingService?.pauseRecording()
            isRecording=false
            timer_count=timer.base-SystemClock.elapsedRealtime()
            timer.stop()
            recordingPausedUI()
        }
    }

    fun resumeRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //recorder?.resume()
            recordingService?.resumeRecording()
            isRecording=true
            timer.base=SystemClock.elapsedRealtime()+timer_count
            timer.start()
            recordingResumedUI()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION )
        serviceIntent = Intent(this@MainActivity, RecordingService::class.java)
        applicationContext.bindService(serviceIntent, mConnection,Context.BIND_AUTO_CREATE) //Binding to the service!
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val s = intent.getBooleanExtra(getString(R.string.isRecording),false)
                val pausePlay = intent.getIntExtra(getString(R.string.pausePlay),0)
                if (intent.action.equals(getString(R.string.fromService))) {
                    if (s && pausePlay == 0) {
                        mStartRecording = !mStartRecording
                        isRecording = true
                        timer.base = SystemClock.elapsedRealtime()
                        timer.start()
                        recordingStartedUI()
                    }
                    else{
                        stopService(serviceIntent)
                        timer.stop()
                        isRecording=false
                        fab_pauseplay.hide()
                        fab_recordButton.setImageResource(R.drawable.white_mic)
                        timer.base= SystemClock.elapsedRealtime()
                        visualizerView.clear()
                        visualizerView.visibility= INVISIBLE
                        mStartRecording = !mStartRecording
                        Toast.makeText(this@MainActivity, "Recording Saved", Toast.LENGTH_SHORT).show()
                    }
                }
                else if(intent.action.equals(getString(R.string.fromService) +".resume")){
                    resumeRecording()
                }
                else if(intent.action.equals(getString(R.string.fromService)+".pause")){
                    pauseRecording()
                }
                else if(intent.action.equals(getString(R.string.fromService) +".stop")){
                    recordingService?.stopRecording()
                }
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRecording",isRecording)
        outState.putBoolean("mStartRecording",mStartRecording)
        outState.putBoolean("mPausePlay",mPausePlay)
        outState.putLong("timer_count",timer_count)
        outState.putString("fileName",fileName)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isRecording=savedInstanceState.getBoolean("isRecording",false)
        mStartRecording=savedInstanceState.getBoolean("mStartRecording",false)
        mPausePlay=savedInstanceState.getBoolean("mPausePlay",false)
        timer_count=savedInstanceState.getLong("timer_count")
    }
    override fun onResume() {
        super.onResume()
        val intentFilter =  IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        // set the custom action
        intentFilter.addAction(getString(R.string.fromService) +".stop")
        (broadcastReceiver)?.let { LocalBroadcastManager.getInstance(this).registerReceiver(it, IntentFilter(getString(R.string.fromService))) }
        registerReceiver(broadcastReceiver,intentFilter)

        val intentFilter1 =  IntentFilter()
        intentFilter1.addCategory(Intent.CATEGORY_DEFAULT)
        // set the custom action
        intentFilter1.addAction(getString(R.string.fromService) +".pause")
        registerReceiver(broadcastReceiver,intentFilter1)
        val intentFilter2 =  IntentFilter()
        intentFilter2.addCategory(Intent.CATEGORY_DEFAULT)
        // set the custom action
        intentFilter2.addAction(getString(R.string.fromService) +".resume")
        registerReceiver(broadcastReceiver,intentFilter2)

        isRecording=recordingService?.isRecording?:isRecording
        if(recordingService?.isStarted?:false){
            timer.text=getString(R.string.recording)
            recordingStartedUI()
            if(isRecording){
                recordingResumedUI()
            }
            else{
                recordingPausedUI()
            }
        }
    }
    // updates the visualizer every 50 milliseconds
    private var updateVisualizer: Runnable = object : Runnable {
        override fun run() {
            if (isRecording) // if we are already recording
            {
                // get the current amplitude
                //val x = recorder!!.maxAmplitude
                val x=recordingService?.recorder?.maxAmplitude
                if(x!=null) {
                    visualizerView.addAmplitude(x.toFloat()) // update the VisualizeView
                    visualizerView.invalidate() // refresh the VisualizerView
                }
            }
            // update in 40 milliseconds
            handler.postDelayed(this, REPEAT_INTERVAL.toLong())
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,service: IBinder) {
            //Toast.makeText(this@MainActivity, "onServiceConnected called", Toast.LENGTH_SHORT).show()
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            val binder: RecordingService.LocalBinder = service as RecordingService.LocalBinder
            recordingService = binder.serviceInstance //Get instance of your service!
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            //Toast.makeText(this@MainActivity, "onServiceDisconnected called", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unbindService(mConnection)
        handler.removeCallbacks(updateVisualizer)
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver?.let { LocalBroadcastManager.getInstance(this).unregisterReceiver(it) }
        unregisterReceiver(broadcastReceiver)
    }

    private fun recordingStartedUI(){
        fab_recordButton.setImageResource(R.drawable.white_stop_icon)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fab_pauseplay.show()
        }
        handler.post(updateVisualizer)
    }

    private fun recordingPausedUI(){
        fab_pauseplay.setImageResource(R.drawable.white_play_icon)
    }

    private fun recordingResumedUI(){
        fab_pauseplay.setImageResource(R.drawable.white_pause_icon)
    }

    private fun initUI(){

        setContentView(R.layout.activity_main)
        val clicker = OnClickListener {
            onRecord(mStartRecording)
        }
        fab_recordButton.setOnClickListener(clicker)
        fab_pauseplay.setOnClickListener { view: View? ->
            onPausePlay(mPausePlay)
            mPausePlay=!mPausePlay
        }
        savedFiles.setOnClickListener { view ->
            val intent=Intent(this,MainActivity2::class.java)
            startActivity(intent)
        }
    }
}