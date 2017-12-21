package com.cuiyao.ffmpegaac.player;

import android.media.AudioTrack;
import android.util.Log;

public class PlayerThread extends Thread implements Runnable
{

	private AudioPlayer audioPlayer;
	private int playerFlag;
	private int iserror=0;
	private int bufferNum = 10;
	
	public PlayerThread(AudioPlayer audioPlayer){
		this.audioPlayer=audioPlayer;
		audioPlayer.isBuffered = false;
	}
	
	@Override
	public void run()
	{
		long i = 0;
		// TODO Auto-generated method stub
		while (true)
		{
			
			Log.i("PlayerThread", ++i+"");
			System.out.println("PlayerThread will play now");
			if (audioPlayer.m_player_running==false)
			{
				System.out.println("PlayerThread be stopped............");
				audioPlayer.m_decoder_running=false;
				break;
			}
			if(audioPlayer.m_decoder_running==false&&audioPlayer.dataqueue.getCount()==0){

				break;
			}
			int time = 0;
			//缓存了10个数据
			while(playerFlag==1){
				audioPlayer.isBuffered = true;
				if(time > 600){
					//抛出了网络异常
					System.out.println("network error jump...........");
					if(audioPlayer.getMediaState()!=MediaState.FEOF)
						iserror=1;
					break;
				}
				Log.i("Test", "dataqueue.getCount()"+audioPlayer.dataqueue.getCount());
				Log.i("Test", "10*audioPlayer.getM_out_buf_size()"+audioPlayer.getM_out_buf_size()*10);
				if(audioPlayer.dataqueue.getCount()>=audioPlayer.getM_out_buf_size()*bufferNum){
					playerFlag=0;
					if(bufferNum == 10){
						bufferNum = 220;
					}
					break;
				}
				if (audioPlayer.m_player_running==false||audioPlayer.m_decoder_running==false){
					System.out.println("PlayerThread be stopped............");
					break;
				}
				System.out.println("dataqueue is not enough,so waiting...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				time++;
			}
			audioPlayer.isBuffered = false;
			audioPlayer.dataqueue.read(audioPlayer.m_out_bytes, 0, audioPlayer.getM_out_buf_size());
			if(audioPlayer.m_out_bytes!=null&&audioPlayer.m_out_bytes.length>0)
				
				if(audioPlayer.m_out_trk != null){
					Log.i("Test", "audioPlayer.getM_out_buf_size()"+audioPlayer.getM_out_buf_size());
					audioPlayer.m_out_trk.write(audioPlayer.m_out_bytes, 0, audioPlayer.m_out_bytes.length);
					Log.i("Test", "audioPlayer.m_out_trk.write end");
				}
			
			if(audioPlayer.dataqueue.getCount()<audioPlayer.getM_out_buf_size()){
				Log.i("Test", "2dataqueue.getCount()"+audioPlayer.dataqueue.getCount());
				Log.i("Test", "audioPlayer.getM_out_buf_size()"+audioPlayer.getM_out_buf_size());
				playerFlag=1;
			}
			if(iserror == 1){
				audioPlayer.m_decoder_running = false;
				break;
			}
		}
		
		if(audioPlayer.m_out_trk != null){
			if(audioPlayer.m_out_trk.getPlayState() != AudioTrack.PLAYSTATE_STOPPED){
				audioPlayer.m_out_trk.stop();			
			}
			audioPlayer.m_out_trk.release();
			audioPlayer.m_out_trk = null;
		}		
		audioPlayer.m_player_thread=null;
		audioPlayer.m_player_running=false;
		if(iserror != 1){
			audioPlayer.m_CurrentUrl = null;
		}
		audioPlayer.setMediaState(iserror==1?MediaState.ERROR:MediaState.STOP);								
	}

}
