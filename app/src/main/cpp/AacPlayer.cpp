/*
 * AacCoder.cpp
 *
 *  Created on: 2017年1月9日
 *      Author: zpy
 */
#include <stdio.h>


#include "AacPlayer.h"
#include "Log.h"

#undef LOG_TAG
#define LOG_TAG "ffmpegjni"


AacPlayer::AacPlayer()
	:pFormatCtx(NULL),
	 frame(NULL),
	 swrCtx(NULL),
	 codecCtx(NULL)
{

}

int AacPlayer::start(const char *input, char* info){
	ALOGW("start");
//    myinput = (char*)malloc(sizeof(input));
//    strcpy(myinput,input);
	ALOGI("%s", "sound");
	//注册组件
	av_register_all();
	pFormatCtx = avformat_alloc_context();
	//打开音频文件
	if (avformat_open_input(&pFormatCtx, input, NULL, NULL) != 0) {
		ALOGI("%s", "无法打开音频文件");
		return NULL;
	}
	//获取输入文件信息
	if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
		ALOGI("%s", "无法获取输入文件信息");
		return NULL;
	}
	//获取音频流索引位置
	int i = 0;
	for (; i < pFormatCtx->nb_streams; i++) {
		if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
			audio_stream_idx = i;
			break;
		}
	}

	//获取解码器
	codecCtx = pFormatCtx->streams[audio_stream_idx]->codec;
	AVCodec *codec = avcodec_find_decoder(codecCtx->codec_id);
	if (codec == NULL) {
		ALOGI("%s", "无法获取解码器");
		return NULL;
	}
	//打开解码器
	if (avcodec_open2(codecCtx, codec, NULL) < 0) {
		ALOGI("%s", "无法打开解码器");
		return NULL;
	}
	//压缩数据

	//解压缩数据
	frame = av_frame_alloc();
	//frame->16bit 44100 PCM 统一音频采样格式与采样率
	swrCtx = swr_alloc();

	//重采样设置参数-------------start
	//输入的采样格式
	enum AVSampleFormat in_sample_fmt = codecCtx->sample_fmt;
	//输出采样格式16bit PCM
	out_sample_fmt = AV_SAMPLE_FMT_S16;
	//输入采样率
	int in_sample_rate = codecCtx->sample_rate;
	//输出采样率
	int out_sample_rate = in_sample_rate;
	//获取输入的声道布局
	//根据声道个数获取默认的声道布局（2个声道，默认立体声stereo）
	//av_get_default_channel_layout(codecCtx->channels);
	uint64_t in_ch_layout = codecCtx->channel_layout;
	//输出的声道布局（立体声）
	uint64_t out_ch_layout = AV_CH_LAYOUT_MONO;

	swr_alloc_set_opts(swrCtx,
					   out_ch_layout, out_sample_fmt, out_sample_rate,
					   in_ch_layout, in_sample_fmt, in_sample_rate,
					   0, NULL);


    ALOGI("in_samplde_rate %d",in_sample_rate);

	swr_init(swrCtx);

	//输出的声道个数
	out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);
    sprintf(info,"%d,%d",in_sample_rate,out_channel_nb);

	//重采样设置参数-------------end
	return 1;
}

int AacPlayer::getpcmbuff(uint8_t* out_buffer){
	//16bit 44100 PCM 数据
    AVPacket packet;
    av_init_packet(&packet);
	int got_frame = 0, index = 0, ret;
	int out_buffer_size = 0;
	//不断读取压缩数据
	if(av_read_frame(pFormatCtx, &packet) >= 0){
		//解码音频类型的Packet
		if (packet.stream_index == audio_stream_idx) {
			//解码
			ret = avcodec_decode_audio4(codecCtx, frame, &got_frame, &packet);
			if (ret < 0) {
				ALOGI("%s", "解码完成");
			}
			//解码一帧成功
			if (got_frame > 0) {
				ALOGI("解码：%d", index++);
				swr_convert(swrCtx, &out_buffer, MAX_AUDIO_FRME_SIZE,
							(const uint8_t **) frame->data, frame->nb_samples);
				//获取sample的size
				out_buffer_size = av_samples_get_buffer_size(NULL, out_channel_nb,
																 frame->nb_samples, out_sample_fmt,
																 1);
			}
		}
	}else{
        av_free_packet(&packet);
        return -1;
    }
    av_free_packet(&packet);
    return out_buffer_size;
}

void AacPlayer::stop(){
//    free(myinput);
	av_frame_free(&frame);
	swr_free(&swrCtx);
	avcodec_close(codecCtx);
	avformat_close_input(&pFormatCtx);
}
