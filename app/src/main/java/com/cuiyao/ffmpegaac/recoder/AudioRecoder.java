package com.cuiyao.ffmpegaac.recoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cuiyao.ffmpegaac.lib.FFmpegAacNativeLib;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class AudioRecoder extends Thread implements Runnable {

    private static final String TAG = AudioRecoder.class.getSimpleName();

    private static AudioRecord m_in_rec;
    private AudioManager am;
    private int m_in_buf_size;//最小缓冲区
    private volatile boolean flag = true;  //录音结束标志
    private boolean flag0 = true;
    private long stopedtime = 0;  //抬起时间
    private long shouldstoptime = 0;//延时结束时间
    private long timeInF = 0;
    private long timeInN = 0;
    private long countTime = 0;
    private long timeLimit = 0;
    private byte[] m_in_bytes;//读取音频的数组
    private byte[] amrbuf;
    private byte[] aacHead;
    private volatile boolean m_keep_running;//录音标志
    private volatile boolean isInterrupt;//录音中断标志
    private long length;
    private long startTime;
    private long stopTime;
    private File file;
    private Handler recordHandler;
    private int currentVolume = 0;
    private int rec_len = 0;

    private static int MAX_TIMELENGTH = 60000;

    public AudioRecoder(Context context, Handler recordHandler) {
        this.recordHandler = recordHandler;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void run() {
        try {
            if (am != null) {
                currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        0, AudioManager.FLAG_PLAY_SOUND);
            }

            //创建文件输入流
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);

            FFmpegAacNativeLib.getInstance().audioEncodePCMToAACInit();

            m_in_rec.startRecording();//开始录音
            startTime = System.currentTimeMillis();
            while (m_keep_running || flag) {
                if (timeInF == 0) {
                    timeInF = System.currentTimeMillis();
                    rec_len = m_in_rec.read(m_in_bytes, 0, m_in_buf_size);//每次取1024字节
                    countValue(m_in_bytes);
                    int amrlen = FFmpegAacNativeLib.getInstance().audioEncodePCMToAAC(m_in_bytes, rec_len, amrbuf);

                    dos.write(amrbuf, 0, amrlen);
                } else {
                    timeInN = System.currentTimeMillis();
                    countTime = timeInN - timeInF;
                    if (countTime <= 850) {
                        if (!m_keep_running) {
                            flag = false;
                            Log.e("audioClient", "countTime:" + countTime);
                        } else {
                            rec_len = m_in_rec.read(m_in_bytes, 0, m_in_buf_size);
                            countValue(m_in_bytes);
                            int amrlen = FFmpegAacNativeLib.getInstance().audioEncodePCMToAAC(m_in_bytes, rec_len, amrbuf);
                            dos.write(amrbuf, 0, amrlen);
                        }
                    } else {
                        timeLimit = System.currentTimeMillis();
                        countTime = timeLimit - timeInF;
                        if (countTime >= MAX_TIMELENGTH) {
                            m_keep_running = false;
                            flag = false;
                            //语音长度超过限制
                            recordHandler.sendEmptyMessage(3);
                        } else {
                            rec_len = m_in_rec.read(m_in_bytes, 0, m_in_buf_size);
                            countValue(m_in_bytes);
                            int amrlen = FFmpegAacNativeLib.getInstance().audioEncodePCMToAAC(m_in_bytes, rec_len, amrbuf);

                            dos.write(amrbuf, 0, amrlen);
                            //判断延时
                            if (!m_keep_running && flag0) {
                                stopedtime = System.currentTimeMillis();
                                flag0 = false;
                            }
                            if (!m_keep_running) {
                                shouldstoptime = System.currentTimeMillis();
                                if (shouldstoptime - stopedtime > 200) {
                                    //int count=(int)(shouldstoptime-stopedtime);
                                    flag = false;
                                }
                            }
                        }
                    }
                }
            }
            stopTime = System.currentTimeMillis();
            length = stopTime - startTime;
            m_in_rec.stop();
            FFmpegAacNativeLib.getInstance().audioEncodeStop();
            if (!flag && countTime > 850) {
                if (!isInterrupt) {
                    //录音完成
                    if (file != null && file.exists()) {
                        Message message = new Message();
                        message.what = 2;
                        Bundle bundle = message.getData();
                        bundle.putString("audioPath", file.getAbsolutePath());
                        bundle.putLong("audioLen", length);
                        //通知发送语音
                        recordHandler.sendMessage(message);
                    } else {
                        //语音录取失败
                        recordHandler.sendEmptyMessage(5);
                    }
                }
            }
            dos.close();

            m_in_rec.release();
            m_in_rec = null;
            m_in_bytes = null;
            if (am != null) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC,
                        currentVolume, AudioManager.FLAG_PLAY_SOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getCountTime() {
        return countTime;
    }

    /**
     * 将byte数组转成short数组
     *
     * @param data
     * @param items
     * @return
     */
    public short[] byteArray2ShortArray(byte[] data, int items) {
        short[] retVal = new short[items];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] & 0xff) << 8);
        return retVal;
    }

    /**
     * 计算音量并改变提示动画
     *
     * @param buffer
     */
    public void countValue(byte[] buffer) {
        short[] buffer0 = byteArray2ShortArray(buffer, buffer.length / 2);
        int volume = 0;
        for (int i = 0; i < buffer0.length; i++) {
            volume += buffer0[i] * buffer0[i];
        }

        int voiceValue = (int) (Math.abs((int) (volume / (float) rec_len) / 10000) >> 1) * 6;

//		Log.i("Volumn","v"+voiceValue);
        Message message = new Message();
        message.what = 1;
        Bundle bundle = new Bundle();
        bundle.putInt("recordVolume", voiceValue);
        message.setData(bundle);
        //更改音量提示图片
        recordHandler.sendMessage(message);
    }

    /**
     * 录音初始化
     */
    public void init(String path) {
        Log.w(TAG, "init()");
        //该size设置为AudioRecord.getMinBufferSize(mSampleRateHz, mChannelConfig, mAudioFormat); 编码aac时会失败。
//		m_in_buf_size =  AudioRecord.getMinBufferSize(8000,
//				AudioFormat.CHANNEL_IN_MONO,
//				AudioFormat.ENCODING_PCM_16BIT);
        m_in_buf_size = 2048;
        m_in_rec = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, m_in_buf_size);
        m_in_bytes = new byte[m_in_buf_size];
        amrbuf = new byte[m_in_buf_size];
        file = new File(path);


        m_keep_running = true;
    }

    /**
     * 判断录音是否终止
     */
    public boolean isRecording() {
        return m_keep_running;
    }

    /**
     * 停止录音
     *
     * @param interrupt
     */
    public void recordStop(boolean interrupt) {
        m_keep_running = false;
        isInterrupt = interrupt;
    }
}
