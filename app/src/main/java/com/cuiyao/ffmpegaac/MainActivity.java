package com.cuiyao.ffmpegaac;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cuiyao.ffmpegaac.player.AudioPlayer;
import com.cuiyao.ffmpegaac.player.MediaState;
import com.cuiyao.ffmpegaac.recoder.AudioRecoder;

public class MainActivity extends AppCompatActivity {
    private AudioRecoder audioRecoder;
    private AudioPlayer audioPlayer;

    private Button playBtn;
    private Button recordBtn;
    private TextView infoTxt;

    private String testFilePath;
    private Handler recordHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case 0:
                    // 延时关闭ad对话框

                    break;
                case 1:
                    // 改变录音音量提示动画

                    break;
                case 2:
                    // 录音完成
                    recordBtn.setText("Start recode");
                    break;
                case 3:
                    // 录音时长超过上限
                    recordBtn.setText("Start recode");
                    break;
                case 4:
                    // sd卡无法使用

                    break;
                case 5:
                    // 录音完成，但是录取失败
                    recordBtn.setText("Start recode");
                default:
                    break;
            }
        }
    };


    private Handler playHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    playBtn.setText("Start play");
                    break;
                case 1:
                    playBtn.setText("Stop play");
                    break;
            }
        }
    };
    private static final int REQUEST_CODE = 130; // 请求码

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };



    public boolean hasPermission(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    protected void requestPermission(int code, String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recordBtn = (Button)findViewById(R.id.recode_btn);
        playBtn = (Button)findViewById(R.id.play_btn);
        infoTxt = (TextView)findViewById(R.id.info_txt);


        if(!hasPermission(PERMISSIONS)){
            requestPermission(REQUEST_CODE,PERMISSIONS);
        }


        testFilePath = "/sdcard/test.aac";

        infoTxt.setText("testfile path : "+testFilePath);
        recordBtn.setText("Start recode");
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleRecordStat();
            }
        });
        playBtn.setText("Start play");
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayStat();
            }
        });


    }

    private void toggleRecordStat(){
        if(audioRecoder==null){
            audioRecoder = new AudioRecoder(this,recordHandler);
        }
        if(audioPlayer!=null&&audioPlayer.isPlaying()){
            audioPlayer.stop();
            audioPlayer = null;

        }
        if(audioRecoder.isRecording()){
            audioRecoder.recordStop(false);
            audioRecoder = null;

        }else{
            audioRecoder.init(testFilePath);
            audioRecoder.start();
            recordBtn.setText("Stop recode");
        }
    }

    private void togglePlayStat(){
        if(audioPlayer==null){
            audioPlayer = new AudioPlayer();
            audioPlayer.setAudioStateChangeListener(new AudioPlayer.AudioStateChangeListener() {
                @Override
                public void onStateChange(int mediaState) {
                    switch (mediaState){
                        case MediaState.PLAY:
                            playHandler.sendEmptyMessage(1);
                            break;
                        case MediaState.FEOF:
                        case MediaState.STOP:
                        case MediaState.PAUSE:
                        case MediaState.ERROR:
                            playHandler.sendEmptyMessage(0);
                            break;
                    }
                }
            });
        }
        if(audioRecoder!=null&&audioRecoder.isRecording()){
            audioRecoder.recordStop(false);
            audioRecoder = null;

        }
        if(audioPlayer.isPlaying()){
            audioPlayer.stop();
            audioPlayer = null;

        }else{
            audioPlayer.Play(testFilePath);

        }
    }

}
