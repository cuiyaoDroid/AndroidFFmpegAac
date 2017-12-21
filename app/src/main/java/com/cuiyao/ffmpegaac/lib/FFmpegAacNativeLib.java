package com.cuiyao.ffmpegaac.lib;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.cuiyao.ffmpegaac.player.AudioPlayer;

public class FFmpegAacNativeLib
{

	public long mNativeContextRecoder;
	public long mNativeContextPlayer;

	static {
		System.loadLibrary("ffmpeg_aac_jni");
	}
	//declare the jni functions
	public native String audioPlayerOpenFile(String path);
	public native int audioPlayerGetPCM(byte[] pcmbuffer);
	public native int audioPlayerStop();

	public native int audioEncodePCMToAACInit();
	public native int audioEncodePCMToAAC(byte[] pcmbuf,int len,byte[] amrbuf);
	public native void audioEncodeStop();


	//Singleton
	private static FFmpegAacNativeLib instance=null;

	public static FFmpegAacNativeLib getInstance() {
		if(instance==null)
			instance=new FFmpegAacNativeLib();
		return instance;
	}
}
