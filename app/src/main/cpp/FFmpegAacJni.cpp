#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <jni.h>
#include "AacRecoder.h"
#include "AacPlayer.h"
#include "Log.h"

#undef LOG_TAG
#define LOG_TAG "ffmpegjni"



static jint audioEncodePCMToAACInit(JNIEnv *env, jobject thiz) {

    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextRecoder", "J");

    jlong object = env->GetLongField(thiz,fieldId);
    AacRecoder *aacCodec;
	ALOGD("Java_aacEncoder_jni_FFAacEncoderJni_native_start");
    if(object!=0){
        aacCodec = (AacRecoder *)object;
        aacCodec->stop();
        delete aacCodec;
        aacCodec = NULL;
    }
    aacCodec = new AacRecoder();
	aacCodec->start();


    env->SetLongField(thiz,fieldId,(jlong)aacCodec);
	return 0;

}

static jint audioEncodePCMToAAC(JNIEnv *env, jobject thiz, jbyteArray pcmbuf_,jint len,
												  jbyteArray amrbuf_) {

	jbyte *pcmData = env->GetByteArrayElements(pcmbuf_, 0);
    jbyte *armData = env->GetByteArrayElements(amrbuf_, 0);
	if(!pcmData){
		ALOGE("set pcm data fail");
		return -1;
	}
    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextRecoder", "J");
    jlong object = env->GetLongField(thiz,fieldId);

	AacRecoder *aacCodec = (AacRecoder *)object;
    int length = aacCodec->encode_pcm_data(pcmData, len,armData);
	env->ReleaseByteArrayElements(pcmbuf_, pcmData, 0);
    env->SetByteArrayRegion(amrbuf_,0,length,armData);
    env->ReleaseByteArrayElements(amrbuf_, armData, 0);

	return length;
}

static void audioEncodeStop(JNIEnv *env, jobject thiz) {
	ALOGD("Java_aacEncoder_jni_FFAacEncoderJni_native_stop");
    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextRecoder", "J");
    jlong object = env->GetLongField(thiz,fieldId);
    if(object!=0){
        AacRecoder *aacCodec = (AacRecoder *)object;
        aacCodec->stop();
        delete aacCodec;
        aacCodec = NULL;
    }
    env->SetLongField(thiz,fieldId,0);
}


static jstring audioPlayerOpenFile(JNIEnv *env, jobject thiz, jstring path) {
    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextPlayer", "J");
    jlong object = env->GetLongField(thiz,fieldId);
    AacPlayer *aacPlayer;
    if(object!=0){
        aacPlayer = (AacPlayer *)object;
        aacPlayer->stop();
        delete aacPlayer;
        aacPlayer = NULL;
    }
    const char *input = env->GetStringUTFChars( path, 0);

    aacPlayer = new AacPlayer();

    char* info = (char*)malloc(10);
    int state = aacPlayer->start(input,info);
    if(state==NULL){
        env->ReleaseStringUTFChars(path,input);
        free(info);
        return NULL;
    }

    ALOGI("info %s",info);
    jstring strinfo=env->NewStringUTF(info);
    env->ReleaseStringUTFChars(path,input);
    free(info);

    env->SetLongField(thiz,fieldId,(jlong)aacPlayer);
//    ALOGI("info %ld",(jlong)aacPlayer);
    return strinfo;
}

static jint audioPlayerGetPCM(JNIEnv *env, jobject thiz, jbyteArray pcmbuffer_) {
    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextPlayer", "J");
    AacPlayer *aacPlayer = (AacPlayer*)env->GetLongField(thiz,fieldId);
//    ALOGI("info %ld",(jlong)aacPlayer);
    uint8_t *out_buffer = (uint8_t *) av_malloc(MAX_AUDIO_FRME_SIZE);
    int out_buffer_size =aacPlayer->getpcmbuff(out_buffer);

    if(out_buffer_size<0){
        free(out_buffer);
        return out_buffer_size;
    }


    jbyte *sample_bytep = env->GetByteArrayElements( pcmbuffer_, NULL);
    memcpy(sample_bytep, out_buffer, out_buffer_size);
    free(out_buffer);

    env->SetByteArrayRegion(pcmbuffer_,0,out_buffer_size,sample_bytep);
    env->ReleaseByteArrayElements( pcmbuffer_, sample_bytep, 0);
//    env->DeleteLocalRef( audio_sample_array);
    return out_buffer_size;
}

static jint audioPlayerStop(JNIEnv *env, jobject thiz) {
    // TODO

    jclass thclass = env->GetObjectClass(thiz);
    jfieldID fieldId = env->GetFieldID(thclass, "mNativeContextPlayer", "J");

    jlong object = env->GetLongField(thiz,fieldId);
    AacPlayer *aacPlayer;
    if(object!=0){
        aacPlayer = (AacPlayer *)object;
        aacPlayer->stop();
        delete aacPlayer;
        aacPlayer = NULL;
    }

    env->SetLongField(thiz,fieldId,0);
    return 0;
}



static JNINativeMethod gMethods[] = {
		{ "audioEncodePCMToAACInit", "()I", (void *)audioEncodePCMToAACInit },
		{ "audioEncodePCMToAAC", "([BI[B)I", (void *)audioEncodePCMToAAC },
		{ "audioEncodeStop", "()V", (void *)audioEncodeStop },
        { "audioPlayerOpenFile", "(Ljava/lang/String;)Ljava/lang/String;", (void *)audioPlayerOpenFile },
        { "audioPlayerGetPCM", "([B)I", (void *)audioPlayerGetPCM },
        { "audioPlayerStop", "()I", (void *)audioPlayerStop }
};


jint JNI_OnLoad(JavaVM* vm, void* reserved){
	ALOGD("JNI_OnLoad");
	JNIEnv* env = NULL;
	jint result = -1;
	jclass clazz;

	if ((*vm).GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		ALOGE("GetEnv fail");
		return result;
	}
	assert(env != NULL);

	clazz = (*env).FindClass("com/cuiyao/ffmpegaac/lib/FFmpegAacNativeLib");
	if (clazz == NULL) {
		ALOGE("com/cuiyao/ffmpegaac/lib/FFmpegAacNativeLib not found");
		return result;
	}
	// 注册native方法到java中
	if ((*env).RegisterNatives(clazz, gMethods, sizeof(gMethods)/sizeof(gMethods[0])) < 0) {
		ALOGE("RegisterNatives methods fail");
		return result;
	}
	// 返回jni的版本
	return JNI_VERSION_1_4;
}

