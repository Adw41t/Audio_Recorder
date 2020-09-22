package com.unique.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class RecordingService extends Service {
    private static final int ENCODING_BIT_RATE=96000;
    private static final int RECORDER_SAMPLERATE = 44100;
    public MediaRecorder recorder;
    private NotificationManager notificationManager;
    private final IBinder mBinder = new LocalBinder();
    private LocalBroadcastManager localBroadcastManager;
    Intent mainintent,pauseintent,stopintent,resumeintent;
    PendingIntent pIntent,pauseIntent,stopIntent,resumeIntent;
    NotificationCompat.Action stopaction;
    NotificationCompat.Builder n;
    String fileName;
    boolean isRecording=false;
    boolean isStarted=false;
    public RecordingService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

         mainintent= new Intent(RecordingService.this, MainActivity.class);
         pIntent= PendingIntent.getActivity(this, 101, mainintent, 0);
        pauseintent = new Intent(getString(R.string.fromService) +".pause");
        resumeintent = new Intent(getString(R.string.fromService) +".resume");
        stopintent = new Intent(getString(R.string.fromService) +".stop");
        stopIntent = PendingIntent.getBroadcast(this, 101, stopintent, 0);
        stopaction = new NotificationCompat.Action.Builder(R.drawable.black_stop_icon, "Stop", stopIntent).build();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            fileName=new FileUtils().generateFilename(this);
            if(fileName!=null){
                recorder = new MediaRecorder();
                int audioSource = MediaRecorder.AudioSource.MIC;

                try {
                    recorder.setAudioSource(audioSource);

                } catch (RuntimeException e) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                }


                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(RECORDER_SAMPLERATE);
                recorder.setAudioEncodingBitRate(ENCODING_BIT_RATE);
                //recorder.setOutputFile(file.getFileDescriptor());
                recorder.setOutputFile(fileName);
                recorder.prepare();
                recorder.start();
                isRecording=true;
                isStarted=true;
                showRecordingNotification(true);
                Intent intent1 = new Intent(getString(R.string.fromService));
                intent1.putExtra(getString(R.string.isRecording),true);
                localBroadcastManager.sendBroadcast(intent1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void showRecordingNotification(boolean showPause) {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Action pausePlayaction;
        if(showPause) {
            pauseIntent = PendingIntent.getBroadcast(this, 101, pauseintent, 0);
            pausePlayaction = new NotificationCompat.Action.Builder(R.drawable.black_pause_icon, "Pause", pauseIntent).build();
        }
        else{
            resumeIntent = PendingIntent.getBroadcast(this, 101, resumeintent, 0);
            pausePlayaction = new NotificationCompat.Action.Builder(R.drawable.black_play_icon, "Resume", resumeIntent).build();
        }
        // build notification

        n = new NotificationCompat.Builder(this)
                .setContentText("Recording... ")
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.mic_logo)
                .setContentIntent(pIntent)
                .addAction(stopaction)
                .setAutoCancel(false);
        // the addAction re-use the same intent to keep the example short
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = getString(R.string.app_name)+"_id";
                CharSequence channelName = getString(R.string.app_name);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel notificationChannel = new NotificationChannel(channelId,channelName, importance);
                notificationManager.createNotificationChannel(notificationChannel);

                        n.setChannelId(channelId);

            }
            else{
                        n.setSound(null)
                        .setPriority(Notification.PRIORITY_LOW);

            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

                n.setColor(ContextCompat.getColor(getApplicationContext(),R.color.mic_logo_red));
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                n.addAction(pausePlayaction);
            }

//        notificationManager.notify(101, n.build());
        startForeground(101,n.build());
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            stopRecording();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
    public class LocalBinder extends Binder {
        public RecordingService getServiceInstance(){
            return RecordingService.this;
        }
    }

    public void pauseRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(recorder!=null) {
                recorder.pause();
                isRecording=false;
                showRecordingNotification(false);
            }
        }
    }

    public void resumeRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(recorder!=null) {
                recorder.resume();
                isRecording=true;
                showRecordingNotification(true);
            }
        }
    }

    public void stopRecording(){
        if(recorder!=null) {
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
            isRecording=false;
            isStarted=false;
        }
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(fileName)));
//        notificationManager.cancel(101);
        stopForeground(true);
        Intent intent1 = new Intent(getString(R.string.fromService));
        intent1.putExtra(getString(R.string.isRecording),false);
        localBroadcastManager.sendBroadcast(intent1);
    }

}
