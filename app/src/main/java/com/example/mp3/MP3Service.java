package com.example.mp3;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MP3Service extends Service {

    MP3Player myMP3;
    Intent notiIntent;
    NotificationManager notiManager;
    NotificationCompat.Builder notiBuilder;

    private String TAG = "g53mdp Service";
    private final IBinder myBinder = new MP3Binder();

    //use IBinder on bind
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        //get the file path from MainActivity intent
        String filePath = intent.getStringExtra("filePath");

        //when binding, call load file
        load(filePath);

        //return IBinder object
        return myBinder;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        //create a new MP3Player object
        myMP3 = new MP3Player();

        //get intent from MainActivity class
        notiIntent = new Intent(this, MainActivity.class);

        //notification service
        notiManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        //build notification service
        notiBuilder = new NotificationCompat.Builder(this)
                .setContentIntent(PendingIntent.getActivity(this, 0, notiIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("MP3 App")
                .setContentText("running...");

        notiManager.notify(1, notiBuilder.build());
        Log.i(TAG, "service onCreate");
        Log.i(TAG, "notification created");
    }

    public class MP3Binder extends Binder {
        MP3Service getService() {
            return MP3Service.this;
        }
    }

    public MP3Player.MP3PlayerState getState(){
        return myMP3.getState();
    }

    public String getFilePath(){
        return myMP3.getFilePath();
    }

    public int getProgress(){
        return myMP3.getProgress();
    }

    public int getDuration(){
        return myMP3.getDuration();
    }

    public void load(String filePath){
        myMP3.load(filePath);
        Log.i("MP3", filePath);
    }

    public void play(){
        myMP3.play();
        Log.i("MP3","playing");
    }

    public void pause(){
        myMP3.pause();
        Log.i("MP3","paused");
    }

    public void stop(){
        myMP3.stop();
        Log.i("MP3", "stop");
    }

    public void seekTo(int msec){
        myMP3.seekTo(msec);
    }

    //unbind to stop the service
    @Override
    public boolean onUnbind(Intent intent) {
        notiManager.cancel(1);
        myMP3.stop();
        Log.i(TAG, "onUnbind");
        Log.i(TAG, "notification cancelled");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
    }
}
