LOCAL_PATH := $(call my-dir)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := avcodec
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libavcodec.a
else
  LOCAL_SRC_FILES := libarm64/libavcodec.a
endif
include $(PREBUILT_STATIC_LIBRARY)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := avfilter
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libavfilter.a
else
  LOCAL_SRC_FILES := libarm64/libavfilter.a
endif
include $(PREBUILT_STATIC_LIBRARY)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := avformat
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libavformat.a
else
  LOCAL_SRC_FILES := libarm64/libavformat.a
endif
include $(PREBUILT_STATIC_LIBRARY)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := avutil
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libavutil.a
else
  LOCAL_SRC_FILES := libarm64/libavutil.a
endif
include $(PREBUILT_STATIC_LIBRARY)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := swresample
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libswresample.a
else
  LOCAL_SRC_FILES := libarm64/libswresample.a
endif
include $(PREBUILT_STATIC_LIBRARY)

# prepare libX
include $(CLEAR_VARS)
TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_MODULE    := swscale
ifeq ($(APP_ABI), armeabi-v7a)
  LOCAL_SRC_FILES := libarmv7a/libswscale.a
else
  LOCAL_SRC_FILES := libarm64/libswscale.a
endif
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)

TARGET_ARCH_ABI := armeabi-v7a arm64-v8a
LOCAL_MODULE     := ffmpeg_aac_jni
LOCAL_SRC_FILES  := FFmpegAacJni.cpp AacRecoder.cpp AacPlayer.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_CFLAGS     := -D__STDC_CONSTANT_MACROS -Wno-sign-compare -Wno-switch -Wno-pointer-sign -DHAVE_NEON=1 -mfpu=neon -mfloat-abi=softfp -fPIC -DANDROID
LOCAL_STATIC_LIBRARIES := avfilter avformat avcodec swresample swscale avutil
LOCAL_LDLIBS     := -L$(NDK_ROOT)/platforms/$(APP_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -llog -ljnigraphics -landroid -lz -ldl -lm

include $(BUILD_SHARED_LIBRARY)
