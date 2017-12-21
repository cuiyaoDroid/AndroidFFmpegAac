package com.cuiyao.ffmpegaac.player;

import android.util.Log;

import com.cuiyao.ffmpegaac.lib.FFmpegAacNativeLib;

public class DecodeThread extends Thread implements Runnable
{

	
	private AudioPlayer audioPlayer;
	//private boolean isStop = false;
	private byte[] arrayOfByte1;
	int tryconnect=0;
	public DecodeThread(AudioPlayer audioPlayer){
		this.audioPlayer=audioPlayer;
		this.arrayOfByte1=new byte[10000];
	}
	@Override
	public void run()
	{
		long i =0;
		// TODO Auto-generated method stub
		while (true)
		{
			Log.i("DecodeThread", ++i+"");
			System.out.println("DecodeThread will play now");
			if (audioPlayer.m_decoder_running==false)
			{
				System.out.println("DecodeThread be stopped............");
				break;
			}
			Log.i("Test", "before getPCM");
			int len= FFmpegAacNativeLib.getInstance().audioPlayerGetPCM(arrayOfByte1);
			Log.i("Test", "after getPCM");
			System.out.println("len"+len);
			if(len<0){
				if(len==-5){//file end
					audioPlayer.setMediaState(MediaState.FEOF);
					break;
				}
				tryconnect=tryconnect+1;
				try {
					sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(tryconnect>=10){
					tryconnect=0;
					System.out.println("decode thread will exit......");
					break;
				}
			}
			if(arrayOfByte1!=null&&len > 0){
				//防止缓冲区溢出
				while(audioPlayer.dataqueue.getCount() + len > audioPlayer.dataqueue.getSize()){
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//向缓冲区添加数据
				audioPlayer.dataqueue.append(arrayOfByte1, 0, len);
				System.out.println("dataqueue append.........");
				Log.i("Test","len" + len+"append"+ audioPlayer.dataqueue.getCount());
			}
			/*if(audioPlayer.dataqueue.getCount()>(audioPlayer.dataqueue.getSize()/2)){
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}*/
		}
		FFmpegAacNativeLib.getInstance().audioPlayerStop();
		audioPlayer.m_decoder_running=false;
		audioPlayer.m_decoder_thread=null;
		/*if(audioPlayer.getMediaState() != MediaState.FEOF 
				&& audioPlayer.getMediaState() != MediaState.BUFFER){
			audioPlayer.setMediaState(MediaState.STOP);
		}*/		
	}	
	
}
