//
// Created by Colin on 2019-08-11.
//

#include <iostream>
#include <jni.h>
#include <cstring>
#include <android/log.h>

#define LOGTAG "ffmpeg_log"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#import "android/bitmap.h"
}


using namespace std;



extern "C"


void printLog(const char *msg) {
    __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "%s", msg);
}

void printLog(const char *msg, const char *params) {
    __android_log_print(ANDROID_LOG_ERROR, LOGTAG, msg, params);
}

jstring getFrameByFFmpeg(JNIEnv *jniEnv, jclass jclass1) {

    printLog(avcodec_configuration());

    return jniEnv->NewStringUTF("fuck you");
}

void testOpen(JNIEnv *jniEnv, jclass jclass1, jstring video_path) {
    AVFormatContext *formatContext = nullptr;
    const char *path = jniEnv->GetStringUTFChars(video_path, JNI_FALSE);
    __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "video path = %s", path);
    //2、打开视频文件
    int open = avformat_open_input(&formatContext, path, nullptr, nullptr);
    if (open < 0) {
        printLog("文件打开失败");
        return;
    }
    //3、获取视频信息
    if (avformat_find_stream_info(formatContext, nullptr) < 0) {
        printLog("Cannot find stream");
        if (formatContext)
            avformat_free_context(formatContext);
        avformat_close_input(&formatContext);
        return;
    }

    int count = formatContext->nb_streams;
    int64_t duration = formatContext->duration;
    __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "count = %d , duration = %lld", count, duration);

    //找到视频流
    int videoStream = -1;
    for (int i = 0; i < formatContext->nb_streams; i++) {
        if (formatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStream = i;
            break;
        }
    }
    if (videoStream == -1) {
        printLog("video stream not find");
        return; // Didn't find a video stream.
    }

    AVCodecContext *pCodecCtxOrig = formatContext->streams[videoStream]->codec;
    // Find the decoder for the video stream.
    AVCodec *pCodec = avcodec_find_decoder(pCodecCtxOrig->codec_id);
    if (pCodec == nullptr) {
        printLog("Unsupported codec!\n");
        return; // Codec not found.
    }
    // Copy context.
    AVCodecContext *pCodecCtx = avcodec_alloc_context3(pCodec);
    if (avcodec_copy_context(pCodecCtx, pCodecCtxOrig) != 0) {
        printLog("Couldn't copy codec context");
        return; // Error copying codec context.
    }
    //Open codec.
    if (avcodec_open2(pCodecCtx, pCodec, nullptr) < 0) {
        return; // Could not open codec.
    }

    //找到视频流了就可以获取帧
    AVFrame *pFrame = av_frame_alloc();

    // Allocate an AVFrame structure.
    AVFrame *pFrameRGB = av_frame_alloc();
    if (pFrameRGB == nullptr) {
        return;
    }

    int numBytes;
    uint8_t *buffer = nullptr;
    // Determine required buffer size and allocate buffer.
    numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, pCodecCtx->width, pCodecCtx->height, 1);
    buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    //关联帧和buffer
    av_image_fill_arrays(pFrameRGB->data, pFrameRGB->linesize, buffer, AV_PIX_FMT_RGB24, pCodecCtx->width,
                         pCodecCtx->height, 1);

    AVPacket packet;
    int frameFinished;
    struct SwsContext *sws_ctx = nullptr;
    // Initialize SWS context for software scaling.
    sws_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width,
                             pCodecCtx->height, AV_PIX_FMT_RGB24, SWS_BILINEAR, NULL, NULL, NULL);
    // Read frames and save first five frames to disk.
    int i = 0;
//    while (av_read_frame(formatContext, &packet) >= 0) {
    // Is this a packet from the video stream?
    int arf = av_read_frame(formatContext, &packet);
    if (packet.stream_index == videoStream) {
        // Decode video frame
        avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
        // Did we get a video frame?
        if (frameFinished) {
            // Convert the image from its native format to RGB.
            sws_scale(sws_ctx, (uint8_t const *const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
                      pFrameRGB->data, pFrameRGB->linesize);

            // Save the frame to disk.
//                if (++i <= 5) {
//                    SaveFrame(pFrameRGB, pCodecCtx->width, pCodecCtx->height, i);
//                }
        }
    }
    // Free the packet that was allocated by av_read_frame.
    av_packet_unref(&packet);
//    }
// Free the RGB image.
    av_free(buffer);
    av_frame_free(&pFrameRGB);
// Free the YUV frame.
    av_frame_free(&pFrame);
// Close the codecs.
    avcodec_close(pCodecCtx);
    avcodec_close(pCodecCtxOrig);
// Close the video file.
    avformat_close_input(&formatContext);
}


jint JNI_OnLoad(JavaVM *jvm, void *reserved) {

    JNIEnv *env = NULL;

    jint result = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (result != JNI_OK) {
        printLog("JniEnv获取失败");
        return -1;
    }
    const char *a = "com/colin/videocutview/NativeFrameCreator";
    jclass class1 = env->FindClass(a);
    JNINativeMethod methods[] = {{"getStringByNative", "()Ljava/lang/String;",  (void *) getFrameByFFmpeg},
                                 {"openVideoFile",     "(Ljava/lang/String;)V", (void *) testOpen}
    };
    //动态注册一个加法函数
    result = env->RegisterNatives(class1, methods, 2);
    if (result != JNI_OK) {
        printLog("动态注册失败");
        return -1;
    }

    av_register_all();

    return JNI_VERSION_1_6;
}
