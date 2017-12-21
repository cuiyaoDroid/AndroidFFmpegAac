/*
 * AacCodec.h
 *
 *  Created on: 2017年1月9日
 *      Author: zpy
 */

#ifndef AACPLAYER_H_
#define AACPLAYER_H_
#define MAX_AUDIO_FRME_SIZE 10000
#include <jni.h>

#ifdef __cplusplus
extern "C"{
#endif

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include <libswresample/swresample.h>

class AacPlayer {
public:
	AacPlayer();
	int start(const char *input,char* info);
	int getpcmbuff(uint8_t* out_buffer);
	void stop();

private:
	AVFormatContext *pFormatCtx;
//	AVPacket *packet;
	AVFrame *frame;
	SwrContext *swrCtx;
	int out_channel_nb=0;
	AVCodecContext *codecCtx;
	enum AVSampleFormat out_sample_fmt;
	int audio_stream_idx=-1;
//	char *myinput;
};
#ifdef __cplusplus
}
#endif
#endif /* AACPLAYER_H_ */
