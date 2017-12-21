/*
 * AacRecoder.h
 *
 *  Created on: 2017年1月9日
 *      Author: zpy
 */

#ifndef AACCODEC_H_
#define AACCODEC_H_

#include <jni.h>

#ifdef __cplusplus
extern "C"{
#endif

#include "libavcodec/avcodec.h"

class AacRecoder {
public:
	AacRecoder();
	int start();
	int encode_pcm_data(void* pIn, int frameSize,jbyte * pOut);
	void stop();

private:
	AVCodec *mAVCodec;

	AVCodecContext *mAVCodecContext;
	AVFrame *mAVFrame;
	int mBufferSize;
	uint8_t *mEncoderData;


	void short2float(short* in, void* out, int len);
	void addADTSheader(uint8_t * in, int packet_size);
};
#ifdef __cplusplus
}
#endif
#endif /* AACCODEC_H_ */
