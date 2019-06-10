package com.example.mp3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    MP3Service mp3Service;
    private String TAG = "g53mdp";

    private ListView lv;
    private File list[];
    private Intent myIntent;
    boolean buttonChecked = false;
    private File selectedFromList;
    private Integer fileNumber;

    private ServiceConnection mp3Connection;

    private TextView songDuration;
    private TextView songName;
    private SeekBar seekBar;
    private CheckBox repeat;
    private Button playPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myIntent = new Intent(this, MP3Service.class);

        songDuration = (TextView) findViewById(R.id.songDuration);
        songName = (TextView) findViewById(R.id.songName);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        repeat = (CheckBox) findViewById(R.id.repeat);
        playPause = (Button) findViewById(R.id.playPause);

        //Listener for repeat checkbox
        repeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked){
                    //if checkbox is checked, repeat is on
                    buttonChecked = true;
                }else{
                    //if checkbox isn't checked, repeat is off
                    buttonChecked = false;
                }
                Log.i(TAG, String.valueOf(buttonChecked));
            }
        });

        //Setting for seek bar
        setSeekBar();

        //retrieve song list from SD card to let user choose song(s)
        musicList();
    }

    public void musicList() {

        lv = (ListView) findViewById(R.id.listView);

        //path to get files from Music folder
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/");

        //put the song(s) into list
        list = musicDir.listFiles();

        Log.d(TAG, "Size: "+ list.length);

        for (int i = 0; i < list.length; i++)
        {
            Log.d(TAG, "FileName:" + list[i].getName());
        }

        //show the list of song(s)
        lv.setAdapter(new ArrayAdapter<File>(this, android.R.layout.simple_list_item_1, list));

        //when user choose a song from list
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            //do when one of the list item was clicked
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt,long mylng) {

                //get the selected file
                selectedFromList = (File) (lv.getItemAtPosition(myItemInt));

                //get the song's position in the list
                fileNumber = myItemInt;

                //terminate the service
                if(mp3Connection != null){
                    unbindService(mp3Connection);
                    mp3Service.stop();
                }

                //get the service connnected
                mp3Connection = new ServiceConnection(){

                    //if service connected
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder service){
                        Log.d(TAG, "Service Connected");

                        //call service in service class binder
                        MP3Service.MP3Binder binder = (MP3Service.MP3Binder) service;

                        //get service
                        mp3Service = binder.getService();
                    }

                    //if service not connected
                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                        Log.d(TAG, "Service Disconnected");
                    }
                };

                //start service in service class
                startMP3Service(selectedFromList.getAbsolutePath());

                //set play pause button image
                playPause.setBackgroundResource(R.drawable.pause);

                //set the song name
                songName.setText(list[myItemInt].getName());

                //update and display song duration
                songDuration.postDelayed(durationUpdate, 1000);

                Log.d(TAG, String.valueOf(myItemInt));
                Log.d(TAG, selectedFromList.getAbsolutePath());

                // do something with selectedFromList...
            }
        });
    }

    //update every second to display song duration
    private Runnable durationUpdate = new Runnable() {
        @Override
        public void run() {

            if(seekBar != null && mp3Service.getDuration() != 0 ) {

                //set seek bar progress
                seekBar.setProgress(mp3Service.getProgress() * seekBar.getMax() / mp3Service.getDuration());

                //display the duration
                songDuration.setText(convertMsecToString(mp3Service.getProgress()) + "/" + convertMsecToString(mp3Service.getDuration()));

                //if the song is playing, keep update it
                if(mp3Service.getState().equals(MP3Player.MP3PlayerState.PLAYING))
                    seekBar.postDelayed(durationUpdate, 1000);

                //if the song ended, play next song automatically
                autoPlayNextSong();
            }
        }
    };

    //automatic play next song in list
    public void autoPlayNextSong(){
//        Log.i(TAG, "In autoPlayNextSong function");
//        Log.i(TAG, String.valueOf(mp3Service.getState()));
//        Log.i(TAG, String.valueOf(mp3Service.getDuration()));
//        Log.i(TAG, String.valueOf(mp3Service.getProgress()));

        if(mp3Service != null) {
            //if song ended
            if (mp3Service.getProgress() >= mp3Service.getDuration()) {
                Log.i(TAG, "Auto Play Next Song");
                if (mp3Service != null) {
                    //stop current ended song
                    mp3Service.stop();
                    playPause.setBackgroundResource(R.drawable.play);;
                    //get the next song position
                    fileNumber++;

                    //if the current song is the last song in the list
                    if (fileNumber > list.length - 1) {

                        //if repeat button is checked
                        if(buttonChecked){
                            //set next song to the first song in the list
                            fileNumber = 0;
                            selectedFromList = (File) (lv.getItemAtPosition(fileNumber));
                            mp3Service.load(selectedFromList.getAbsolutePath());
                            playPause.setBackgroundResource(R.drawable.pause);;
                            songName.setText(list[fileNumber].getName());
                            seekBar.postDelayed(durationUpdate, 1000);

                        //if repeat button isn't checked
                        }else {
                            //stop playing next song
                            mp3Service.stop();
                            playPause.setBackgroundResource(R.drawable.play);;
                        }

                    //if there is next song after current song
                    }else {
                        //load and play the next song
                        selectedFromList = (File) (lv.getItemAtPosition(fileNumber));
                        mp3Service.load(selectedFromList.getAbsolutePath());
                        playPause.setBackgroundResource(R.drawable.pause);;
                        songName.setText(list[fileNumber].getName());
                        seekBar.postDelayed(durationUpdate, 1000);
                    }
                }
            }
        }
    }

    //convert getProgress and getDuration (millisecond) into time string
    private String convertMsecToString(long msec){

        int newMsec = (int) msec;
        String hourInString = "";
        String minuteInString = "";
        String secondInString = "";

        //convert hour, minute, second in millisecond
        int hour = 1000*60*60;
        int minute = 1000*60;
        int second = 1000;

        //get the hour(s) and display
        if(newMsec >= hour){
            hourInString = String.valueOf(newMsec / hour) + ":";
            //store the remainder
            newMsec = newMsec % hour;
        }

        //get the minute(s) and display
        if(newMsec >= minute){
            //if less than 10minutes, display a '0' infront
            if(newMsec / minute < 10) {
                minuteInString = "0" + String.valueOf(newMsec / minute) + ":";
                newMsec = newMsec % minute;
            }else{
                minuteInString = String.valueOf(newMsec / minute) + ":";
                newMsec = newMsec % minute;
            }
        //if no minute, display '00'
        }else if(newMsec < minute){
            minuteInString = "00:";
        }

        //get the second(s) and display
        if(newMsec >= second){
            //if less than 10seconds, display a '0' infront
            if(newMsec / second < 10) {
                secondInString = "0" + String.valueOf(newMsec / second);
                newMsec = newMsec % second;
            }else{
                secondInString = String.valueOf(newMsec / second);
                newMsec = newMsec % second;
            }
            //if no second, display '00'
        }else if(newMsec < second){
            secondInString = "00";
        }

        //return string in form HH:mm:ss
        return hourInString + minuteInString + secondInString;
    }

    //set seek bar
    private void setSeekBar() {

        //seek bar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            //when seek bar on touch released, seek to the released time
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mp3Service.seekTo(mp3Service.getDuration() * seekBar.getProgress() / seekBar.getMax());
            }
        });
    }

    //start service
    public void startMP3Service(String filePath){

        //pass intent to MP3 service class
        myIntent = new Intent(this, MP3Service.class);

        //put file path into intent
        myIntent.putExtra("filePath", filePath);

        //bind service
        bindService(myIntent, mp3Connection, Context.BIND_AUTO_CREATE);
    }

    //when play pause button clicked
    public void onPlayPause(View v){
        if(mp3Service != null) {

            //if current state is PAUSED
            if(mp3Service.getState() == MP3Player.MP3PlayerState.PAUSED) {
                //change to PLAYING state
                mp3Service.play();
                playPause.setBackgroundResource(R.drawable.pause);
                //seek bar will update once song is played
                seekBar.postDelayed(durationUpdate, 1000);

            //if current state is PLAYING
            }else if(mp3Service.getState() == MP3Player.MP3PlayerState.PLAYING){
                //change to PAUSE state
                mp3Service.pause();
                playPause.setBackgroundResource(R.drawable.play);;
            }
        }
    }

    //when stop button is clicked
    public void onStop(View v){
        if(mp3Service != null){

            //stop the current playing song
            mp3Service.stop();
            playPause.setBackgroundResource(R.drawable.play);

            //song name and seek bar set back to default
            songName.setText("");
            seekBar.setProgress(0);
        }
    }


    //when next song button is clicked
    public void onNext(View v){

        if(mp3Service != null){

            //stop current playing song
            mp3Service.stop();

            //get the next song position
            fileNumber++;

            //if current song is last song, play the first song in the list
            if(fileNumber > list.length-1){
                fileNumber = 0;
            }

            //load the next song
            selectedFromList = (File) (lv.getItemAtPosition(fileNumber));
            mp3Service.load(selectedFromList.getAbsolutePath());
            songName.setText(list[fileNumber].getName());
            seekBar.postDelayed(durationUpdate, 1000);
        }
    }

    //when next song button is clicked
    public void onPrev(View v){

        if(mp3Service != null){

            //stop current playing song
            mp3Service.stop();

            //get the previous song position
            fileNumber--;

            //if current song is first song, play the last song in the list
            if(fileNumber < 0){
                fileNumber = list.length-1;
            }

            //load previous song
            selectedFromList = (File) (lv.getItemAtPosition(fileNumber));
            mp3Service.load(selectedFromList.getAbsolutePath());
            songName.setText(list[fileNumber].getName());
            seekBar.postDelayed(durationUpdate, 1000);
        }
    }

    //destroy when app is closed
    @Override
    protected void onDestroy() {
        unbindService(mp3Connection);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }
}
