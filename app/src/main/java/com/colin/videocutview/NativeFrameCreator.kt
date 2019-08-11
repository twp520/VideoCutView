package com.colin.videocutview

/**
 * create by colin 2019-08-11
 */
class NativeFrameCreator {

    init {
        System.loadLibrary("frame-creator")
    }

    external fun getStringByNative(): String


    external fun openVideoFile(videoPath: String)
}