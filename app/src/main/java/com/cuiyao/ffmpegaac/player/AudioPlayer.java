package com.cuiyao.ffmpegaac.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.cuiyao.ffmpegaac.lib.FFmpegAacNativeLib;

public class AudioPlayer {

    BufferQueue dataqueue;
    String m_CurrentUrl;
    private int m_audio_data_size;
    boolean m_decoder_running;
    Thread m_decoder_thread;

    public boolean isBuffered;//是否缓冲中
    private int m_mediaState;//0:stop;1:play;2:pause;
    private int m_out_buf_size = 0;

    protected byte[] m_out_bytes;
    AudioTrack m_out_trk = null;
    boolean m_player_running;
    Thread m_player_thread;
    boolean isPlayPrepare;


    public AudioPlayer() {
        this.dataqueue = new BufferQueue(4096 * 600);
        this.m_decoder_running = false;
        this.m_player_running = false;
        this.m_decoder_thread = null;
        this.m_player_thread = null;
        this.m_audio_data_size = 0;
        setMediaState(MediaState.STOP);
    }

    private AudioStateChangeListener audioStateChangeListener;

    public void setAudioStateChangeListener(AudioStateChangeListener audioStateChangeListener) {
        this.audioStateChangeListener = audioStateChangeListener;
    }

    public interface AudioStateChangeListener {
        void onStateChange(int mediaState);
    }

    public boolean Play(String paramString) {
        if (isPlayPrepare) {
            return true;
        }
        isPlayPrepare = true;
        System.out.println("AudioPalyer will play now");
        if (paramString == null || paramString.trim().length() == 0) {
            isPlayPrepare = false;
            return false;
        }
        if (this.m_mediaState == MediaState.PLAY && this.m_CurrentUrl != null && this.m_CurrentUrl.equals(paramString.trim())) {
            isPlayPrepare = false;
            return true;
        }
        if (this.m_mediaState == MediaState.PLAY) {
            System.out.println("AudioPalyer is playing,so we stop now............");
            stop();
        }
        while (true) {

            if (this.m_decoder_thread == null && this.m_player_thread == null) {
                Log.i("FFmpegTest", "wait to null");
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        this.m_CurrentUrl = paramString.trim();
        System.out.println("get playinfo " + m_CurrentUrl);
        String playinfo = FFmpegAacNativeLib.getInstance().audioPlayerOpenFile(this.m_CurrentUrl);
        Log.i("ffmpegjni", "playinfo=" + playinfo);
        if (playinfo == null || playinfo.length() <= 0) {
            isPlayPrepare = false;
            return false;
        }
        String[] playinfoArray = playinfo.trim().split(",");
        int samplerate = Integer.parseInt(playinfoArray[0]);
        int channel = AudioFormat.CHANNEL_CONFIGURATION_MONO - 1 + Integer.parseInt(playinfoArray[1]);
        if (samplerate < 0) {
            System.out.println("audioPlayerOpenM3U8 failed............");
            isPlayPrepare = false;
            return false;
        }
        System.out.println("samplerate = " + samplerate);
        this.m_out_buf_size = AudioTrack.getMinBufferSize(samplerate,
                channel,
                AudioFormat.ENCODING_PCM_16BIT);
        //this.m_out_buf_size = this.m_out_buf_size * 3;
        Log.w("原始channel", channel + "");
        Log.w("原始m_out_buf_size", this.m_out_buf_size + "");
        /*if(this.m_out_buf_size > 0){
	    	while(this.m_out_buf_size < 4000){
		    	this.m_out_buf_size = 2 * this.m_out_buf_size;
		    }
	    } else {
	    	this.m_out_buf_size = 4096;
	    }*/
        Log.w("m_out_buf_size", this.m_out_buf_size + "");
        AudioTrack localAudioTrack = null;
        try {
            localAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    samplerate, channel,
                    AudioFormat.ENCODING_PCM_16BIT,
                    m_out_buf_size * 3,
                    AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            Log.w("localAudioTrack", e.toString() + "msg");
        }

        this.m_out_trk = localAudioTrack;
        if (this.m_out_trk.getState() == AudioTrack.STATE_INITIALIZED) {
            this.m_out_trk.play();
        } else {
            this.m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC,
                    samplerate, channel,
                    AudioFormat.ENCODING_PCM_16BIT,
                    m_out_buf_size * 3,
                    AudioTrack.MODE_STREAM);
            if (this.m_out_trk.getState() != AudioTrack.STATE_INITIALIZED) {
                isPlayPrepare = false;
                return false;
            }
        }

        this.m_out_bytes = new byte[this.m_out_buf_size];

        while (true) {
            if (this.m_decoder_thread == null && this.m_player_thread == null) {
                Log.i("FFmpegTest", "wait to null");
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        this.m_decoder_running = true;
        this.m_decoder_thread = new DecodeThread(this);
        this.m_decoder_thread.start();

        this.m_player_running = true;
        this.m_player_thread = new PlayerThread(this);
        this.m_player_thread.start();

        isPlayPrepare = false;
        setMediaState(MediaState.PLAY);
        return true;
    }

    public boolean isPlaying() {
        return m_player_running;
    }

    public void stop() {
        if (this.m_player_thread != null && this.m_player_thread.isAlive()) {
            Log.i("FFmpegTest", "---------------m_player_running=false");
            m_player_running = false;
        } else {
            Log.i("FFmpegTest", "---------------audioPlayerStop");
            FFmpegAacNativeLib.getInstance().audioPlayerStop();
            setMediaState(MediaState.STOP);
        }
//		if(m_mediaState==MediaState.STOP)
        //		return;

    }

    public int getM_out_buf_size() {
        return m_out_buf_size;
    }

    public int getMediaState() {
        return this.m_mediaState;
    }

    public void setMediaState(int state) {
        this.m_mediaState = state;
        if(audioStateChangeListener!=null){
            audioStateChangeListener.onStateChange(m_mediaState);
        }
        //开始尝试重连
        if (state == MediaState.ERROR) {
            //Play(this.m_CurrentUrl);
        } else if (state == MediaState.STOP) {
            if (this.dataqueue != null) {
                this.dataqueue.datareset();
            }
        }
    }


}
