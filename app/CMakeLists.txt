cmake_minimum_required(VERSION 3.4.1)

# 这里是要添加的库的定义，也就是说打包成so库的名字，包含了哪些类
add_library( # Sets the name of the library.
        frame-creator

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/FFmpegFrameCreator.cpp src/main/cpp/logger.cpp)

#这里可以定义系统有的类，你需要的，通过变量名存储
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

#----------------------ffmpeg的库文件------------
# 加载头文件
include_directories(src/main/cpp/include)

# 加载avcodec-57库
add_library(avcodec
        SHARED
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavcodec.so)

#add_library(avdevice
#        SHARED
#        IMPORTED)
#set_target_properties(avdevice
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavdevice.so)
add_library(avfilter
        SHARED
        IMPORTED)
set_target_properties(avfilter
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavfilter.so)
add_library(avformat
        SHARED
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavformat.so)
add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libavutil.so)
add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libswresample.so)
add_library(swscale
        SHARED
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libswscale.so)
#----------------------end-----------------------


#这里可以定义，你的库需要依赖哪些库。
target_link_libraries( # Specifies the target library.
        frame-creator
        ${log-lib}
        # Links the target library to the log library
        # included in the NDK.
        #ffmpeg------start----------
        avcodec
#        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        #ffmpeg------end------------
)